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
 * **Askel 2 (§9):** WebView + JS-silta. Vastaanotetut paketit vain lokitetaan
 * (WebBridge -> Log.d), BLE ei ole vielä kytkettynä. Askel 3 korvaa
 * lokitus-callbackin BleClient-jonoon työntämisellä ja lisää yhteystilan
 * yläpalkin (§7) RepeaterScreeniin.
 *
 * **BLE-permissiot (BLUETOOTH_SCAN/CONNECT) puuttuvat tarkoituksella tästä
 * versiosta** — pyydetään vasta askeleessa 3 kun niitä oikeasti tarvitaan.
 * Sijaintilupa (ACCESS_FINE_LOCATION) pyydetään jo nyt, koska Tackcallin GPS
 * (§6) ei toimi ilman sitä eikä WebView'n geolocation-silta ole täysin
 * testattavissa ilman myönnettyä lupaa.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ruutu pysyy päällä sovelluksen ollessa edustalla (§7/§8) — estää
        // BLE-katkon näytön sammuessa konseptitestissä. Foreground service
        // (taustakäyttö ruutu sammutettuna) ei ole Vaihe 1:n piirissä (§0).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val webBridge = WebBridge(onPacket = { packet ->
            // Askel 2: pelkkä lokitus. Askel 3 korvaa tämän BleClientin
            // säieturvalliseen operaatiojonoon työntämisellä.
            Log.d(TAG, "Paketti valmiina lähetettäväksi: $packet")
        })

        setContent {
            RequestLocationPermission()
            RepeaterScreen(webBridge = webBridge)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

/**
 * Pyytää ACCESS_FINE_LOCATION-luvan kerran käynnistyksessä, jos sitä ei jo ole
 * myönnetty (§6). Ei estä WebView'n latautumista odottamalla — sovellus itse
 * käsittelee GPS:n puuttumisen asteittaisen degradoitumisen periaatteella,
 * kuten muukin Tackcallin sensoridata.
 */
@Composable
private fun RequestLocationPermission() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result -> granted = result }

    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
