package app.tackcall.repeater.ui

import android.annotation.SuppressLint
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.tackcall.repeater.BleClient
import app.tackcall.repeater.ConnectionState
import app.tackcall.repeater.WebBridge

/**
 * Kanoninen juuriosoite (valmistusohje v4, §1/§2) — sama datalähde kuin live-sim,
 * ei /index.html.
 */
private const val TACKCALL_URL = "https://tackcall.app/"

private val STATUS_BAR_HEIGHT = 22.dp
private val COLOR_CONNECTED = Color(0xFF2E7D32)
private val COLOR_SCANNING = Color(0xFFF9A825)
private val COLOR_DISCONNECTED = Color(0xFFC62828)
private val COLOR_BAR_BACKGROUND = Color(0xFF1A1A1A)

/**
 * Koko näyttö = WebView + ohut yhteystilan yläpalkki (§7, variant A, lukittu).
 * WebView sijoitetaan palkin ALLE, ei sen päälle — ettei palkki peitä
 * Tackcallin omaa yläreunan UI:ta.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RepeaterScreen(webBridge: WebBridge, connectionState: ConnectionState) {
    Box(modifier = Modifier.fillMaxSize()) {
        // WebView täyttää koko ruudun taustalla.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true // sovelluksen localStorage/IndexedDB (§3)

                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?,
                        ) {
                            // WebView ei automaattisesti välitä sivun geolocation-
                            // pyyntöjä vaikka Androidilla olisi ACCESS_FINE_LOCATION
                            // myönnetty. Ilman tätä Tackcallin GPS ei koskaan
                            // käynnisty WebView'ssä (§6, kriittinen).
                            callback?.invoke(origin, true, false)
                        }
                    }

                    // sovelluksen pushToRepeater() -> window.AndroidBridge.postMessage(json)
                    addJavascriptInterface(webBridge, "AndroidBridge")

                    loadUrl(TACKCALL_URL)
                }
            },
        )

        // Ohut yhteystilapalkki ylimpänä. Hienosäätö (esim. contentInset,
        // palkin ohentaminen/piilottaminen vakaalla yhteydellä) tehdään vasta
        // jos vesitesti osoittautuu sen tarpeelliseksi (§7).
        ConnectionStatusBar(connectionState)
    }
}

@Composable
private fun ConnectionStatusBar(state: ConnectionState) {
    val (color, text) = when (state) {
        ConnectionState.CONNECTED -> COLOR_CONNECTED to "Yhdistetty · ${BleClient.DEVICE_NAME}"
        ConnectionState.SCANNING, ConnectionState.CONNECTING -> COLOR_SCANNING to "Etsitään laitetta…"
        ConnectionState.DISCONNECTED -> COLOR_DISCONNECTED to "Katkennut · ${BleClient.DEVICE_NAME}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(STATUS_BAR_HEIGHT)
            .background(COLOR_BAR_BACKGROUND)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White, fontSize = 11.sp)
    }
}
