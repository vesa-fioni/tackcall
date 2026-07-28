package app.tackcall.repeater

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.tackcall.repeater.ui.RepeaterScreen

/**
 * Compose-host. Ei purjehduslogiikkaa — kaikki laskenta tapahtuu Tackcall-
 * sovelluksessa (tackcall.app) WebView'n sisällä (valmistusohje v4, §0).
 *
 * **Askel 3 (§9):** WebBridge -> BleClient -> ESP32/nRF Connect. Yhteystilan
 * yläpalkki (§7) näyttää todellista BleClient-tilaa RepeaterScreenin läpi.
 */
class MainActivity : ComponentActivity() {

    private var bleClient: BleClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ruutu pysyy päällä sovelluksen ollessa edustalla (§7/§8) — estää
        // BLE-katkon näytön sammuessa konseptitestissä.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
            val permissionsGranted = rememberRequiredPermissions()

            LaunchedEffect(permissionsGranted) {
                if (permissionsGranted && bleClient == null) {
                    val client = BleClient(applicationContext) { state ->
                        connectionState = state
                    }
                    bleClient = client
                    client.start()
                }
            }

            val webBridge = remember {
                WebBridge(onPacket = { packet ->
                    val client = bleClient
                    if (client != null) {
                        client.send(packet)
                    } else {
                        Log.w(TAG, "BleClient ei ole vielä valmis, paketti hylätty: $packet")
                    }
                })
            }

            RepeaterScreen(webBridge = webBridge, connectionState = connectionState)
        }
    }

    override fun onDestroy() {
        // Sulje GATT siististi (§8).
        bleClient?.stop()
        bleClient = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

/**
 * Pyytää sijainti- ja BLE-luvat kerran käynnistyksessä (§6): ACCESS_FINE_LOCATION
 * Tackcallin GPS:ää varten, BLUETOOTH_SCAN/CONNECT repeaterin BLE-yhteyttä varten.
 * Nämä ovat kaksi erillistä lupatarvetta, ei toinen korvaa toista.
 */
@Composable
private fun rememberRequiredPermissions(): Boolean {
    val context = LocalContext.current
    val permissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
    var granted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> granted = result.values.all { it } }

    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(permissions)
        }
    }
    return granted
}
