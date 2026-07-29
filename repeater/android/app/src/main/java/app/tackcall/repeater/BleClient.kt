package app.tackcall.repeater

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * BLE-tilakone Tackcall-repeaterille (valmistusohje v4, §5).
 *
 * Tilasiirtymät: scanning -> connecting -> connected ->
 * (disconnected -> auto-reconnect: takaisin scanning).
 *
 * **Operaatiojono (§5, kriittinen):** yksi kirjoitus kerrallaan. [send] tallentaa
 * vain viimeisimmän paketin ([pendingPacket], koko 1) — jos silta työntää uuden
 * paketin ennen kuin edellinen on ehtinyt lähteä, vanha korvautuu hiljaa (vanha
 * data on turhaa 1 Hz -tahdissa, ei kasvateta jonoa).
 *
 * **Luvat:** kutsujan (MainActivity) vastuulla on varmistaa BLUETOOTH_SCAN ja
 * BLUETOOTH_CONNECT myönnettyinä ENNEN [start]-kutsua. Tämä luokka tarkistaa
 * luvat puolustavasti ja lokittaa jos ne puuttuvat, mutta ei itse pyydä niitä.
 *
 * **Säikeisyys:** [BluetoothGattCallback]/[ScanCallback] kutsutaan Binder-
 * säikeeltä, ei UI-säikeeltä — [onStateChanged] postitetaan aina pääsäikeelle
 * ennen kutsua, jotta se on turvallinen kytkeä suoraan Compose-tilaan.
 */
class BleClient(
    private val context: Context,
    private val onStateChanged: (ConnectionState) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingPacket: RepeaterPacket? = null
    private var stopped = true

    /** Aloittaa skannauksen. Vaatii BLE-luvat myönnettynä (ks. luokkatason kommentti). */
    @SuppressLint("MissingPermission")
    fun start(): Unit {
        stopped = false
        startScan()
    }

    /** Pysäyttää kaiken BLE-toiminnan siististi (§8, onDestroy). Ei auto-reconnectia enää. */
    @SuppressLint("MissingPermission")
    fun stop(): Unit {
        stopped = true
        mainHandler.removeCallbacksAndMessages(null)
        if (hasBlePermissions()) {
            scanner?.stopScan(scanCallback)
        }
        gatt?.close()
        gatt = null
        txCharacteristic = null
        setState(ConnectionState.DISCONNECTED)
    }

    /**
     * Uusi paketti lähetettäväksi. Korvaa mahdollisen aiemman odottavan paketin
     * (§5) — vain viimeisin arvo on merkityksellinen 1 Hz -tahdissa.
     */
    fun send(packet: RepeaterPacket) {
        pendingPacket = packet
        trySendPending()
    }

    // ---- Skannaus ----

    private var scanner: android.bluetooth.le.BluetoothLeScanner? = null

    @SuppressLint("MissingPermission")
    private fun startScan(): Unit {
        if (!hasBlePermissions()) {
            Log.w(TAG, "BLE-luvat puuttuvat, ei voida skannata")
            return
        }
        val adapter = bluetoothAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth ei käytössä, yritetään uudelleen ${RECONNECT_DELAY_MS}ms kuluttua")
            scheduleReconnect()
            return
        }
        setState(ConnectionState.SCANNING)
        scanner = adapter.bluetoothLeScanner
        // Kaksi suodatinta = OR-ehto (osuma riittää kumpaan tahansa). Laitenimi
        // toimii ESP32:n kanssa, mutta iOS:n CoreBluetooth ei mainosta
        // laitenimeä luotettavasti — service UUID on siksi varmempi ehto
        // testattaessa nRF Connectilla iOS-puolella.
        val filters = listOf(
            ScanFilter.Builder().setDeviceName(DEVICE_NAME).build(),
            ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(SERVICE_UUID)).build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(filters, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Lopeta skannaus heti kun löytyy — säästää virtaa (§5).
            scanner?.stopScan(scanCallback)
            connectTo(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Skannaus epäonnistui, virhekoodi=$errorCode")
            scheduleReconnect()
        }
    }

    // ---- Yhdistys ----

    @SuppressLint("MissingPermission")
    private fun connectTo(device: BluetoothDevice): Unit {
        setState(ConnectionState.CONNECTING)
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    txCharacteristic = null
                    g.close()
                    gatt = null
                    setState(ConnectionState.DISCONNECTED)
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "discoverServices epäonnistui, status=$status")
                disconnectAndRetry(g)
                return
            }
            val characteristic = g.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
            if (characteristic == null) {
                Log.w(TAG, "TX-karakteristiikkaa ei löytynyt palvelusta $SERVICE_UUID")
                disconnectAndRetry(g)
                return
            }
            txCharacteristic = characteristic
            setState(ConnectionState.CONNECTED)
            trySendPending()
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectAndRetry(g: BluetoothGatt): Unit {
        g.disconnect()
        g.close()
        gatt = null
        setState(ConnectionState.DISCONNECTED)
        scheduleReconnect()
    }

    private fun scheduleReconnect(): Unit {
        if (stopped) return
        mainHandler.postDelayed({ if (!stopped) startScan() }, RECONNECT_DELAY_MS)
    }

    // ---- Kirjoitus ----

    @SuppressLint("MissingPermission")
    private fun trySendPending(): Unit {
        val g = gatt ?: return
        val characteristic = txCharacteristic ?: return
        val packet = pendingPacket ?: return
        pendingPacket = null

        val bytes = packet.toBytes()
        val ok = writeNoResponse(g, characteristic, bytes)
        if (!ok) {
            Log.w(TAG, "writeCharacteristic epäonnistui paketille: $packet")
        }
    }

    /**
     * Kirjoittaa write-without-response-tyyppisenä (§5). minSdk 31:llä ajettaessa
     * API-taso voi olla alle 33, jolloin uusi synkroninen
     * writeCharacteristic(char, value, type):Int -ylikuormitus ei ole käytössä
     * (se vaatii API 33). Tästä syystä tässä on kaksi haaraa — tätä ei ollut
     * eksplisiittisesti auki kirjoitettu valmistusohjeessa (§1 mainitsee vain
     * targetSdk 34+ -puolen), lisätty tänne yhteensopivuuden vuoksi.
     */
    @SuppressLint("MissingPermission", "DEPRECATION")
    private fun writeNoResponse(
        g: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        bytes: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = g.writeCharacteristic(
                characteristic,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            )
            status == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            characteristic.value = bytes
            g.writeCharacteristic(characteristic)
        }
    }

    // ---- Apufunktiot ----

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    private fun hasBlePermissions(): Boolean {
        val scanOk = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        val connectOk = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        return scanOk && connectOk
    }

    private fun setState(newState: ConnectionState): Unit {
        mainHandler.post { onStateChanged(newState) }
    }

    companion object {
        private const val TAG = "BleClient"

        const val DEVICE_NAME = "TACKCALL-RPT"
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        // §10 avoin kysymys #2: kiinteä 1-2s vs. kasvava backoff, ratkeaa vesillä.
        private const val RECONNECT_DELAY_MS = 1500L
    }
}
