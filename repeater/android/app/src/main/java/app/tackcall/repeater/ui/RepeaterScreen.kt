package app.tackcall.repeater.ui

import android.annotation.SuppressLint
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.tackcall.repeater.WebBridge

/**
 * Kanoninen juuriosoite (valmistusohje v4, §1/§2) — sama datalähde kuin live-sim,
 * ei /index.html.
 */
private const val TACKCALL_URL = "https://tackcall.app/"

/**
 * Koko näyttö = WebView (§7). Askeleessa 2 ei vielä yhteystilan yläpalkkia —
 * se liitetään mukaan askeleessa 3 kun ConnectionState/BleClient on olemassa
 * (palkin täytyy näyttää todellista tilaa, ei placeholderia).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RepeaterScreen(webBridge: WebBridge) {
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
                        // WebView ei automaattisesti välitä sivun geolocation-pyyntöjä
                        // vaikka Androidilla olisi ACCESS_FINE_LOCATION myönnetty.
                        // Ilman tätä Tackcallin GPS (watchPosition/enableHighAccuracy)
                        // ei koskaan käynnisty WebView'ssä (§6, kriittinen).
                        callback?.invoke(origin, true, false)
                    }
                }

                // sovelluksen pushToRepeater() -> window.AndroidBridge.postMessage(json)
                addJavascriptInterface(webBridge, "AndroidBridge")

                loadUrl(TACKCALL_URL)
            }
        },
    )
}
