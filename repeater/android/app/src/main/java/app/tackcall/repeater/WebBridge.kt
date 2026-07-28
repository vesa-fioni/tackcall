package app.tackcall.repeater

import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * JS-silta WebView'n ja Kotlinin välillä (Android-kuoren valmistusohje v4, §3).
 *
 * Sovellus (tackcall.app, mittari.html v1.2+) kutsuu:
 *   window.AndroidBridge.postMessage(JSON.stringify(o))
 * kentillä: meansValid, gpsValid, state, hdg, shift, perf, vmg, sog, seq.
 *
 * **Säikeisyys (kriittinen, §3):** [postMessage] kutsutaan WebView'n JS-säikeeltä,
 * EI UI- eikä BLE-säikeeltä. Tämä luokka parsii JSONin heti kutsuvalla säikeellä
 * ja välittää valmiin [RepeaterPacket]-olion [onPacket]-callbackin kautta — se ei
 * itse kosketa BLE- tai UI-tilaa. Kutsujan (MainActivity) vastuulla on tehdä
 * [onPacket]:sta säieturvallinen (esim. työntää jonoon) ennen kuin BLE liitetään
 * mukaan (§9 askel 3). Askeleessa 2 [onPacket] vain lokittaa.
 */
class WebBridge(
    private val onPacket: (RepeaterPacket) -> Unit,
) {

    @JavascriptInterface
    fun postMessage(json: String) {
        val packet = try {
            parse(json)
        } catch (e: Exception) {
            // Virheellinen/puutteellinen JSON ei saa kaataa siltaa — nielaistaan
            // ja odotetaan seuraavaa pakettia (sama periaate kuin muuallakin
            // sovelluksessa: rikkinäinen yksittäinen näyte ei saa katkaista ketjua).
            Log.w(TAG, "Virheellinen JSON sovelluksesta: $json", e)
            return
        }
        Log.d(TAG, "Vastaanotettu paketti: $packet")
        onPacket(packet)
    }

    companion object {
        private const val TAG = "WebBridge"

        /**
         * Purkaa sovelluksen lähettämän JSON-olion [RepeaterPacket]:ksi.
         *
         * OLETUS (ei vahvistettu tässä keskustelussa, tarkista mittari.html:n
         * pushToRepeater()-toteutuksesta): puuttuva/ei-dataa-arvo lähetetään
         * JSON-kentässä `null`:ina (esim. `"hdg": null`), ei esim. poissaolevana
         * avaimena. Jos sovellus jättää kentän kokonaan pois JSON-oliosta null:in
         * sijaan, tämä pitää päivittää käyttämään `o.has(...)`.
         */
        internal fun parse(json: String): RepeaterPacket {
            val o = JSONObject(json)
            val meansValid = o.getBoolean("meansValid")

            val stateStr = o.getString("state")
            val matchedState = RepeaterState.values().firstOrNull {
                it.name.equals(stateStr, ignoreCase = true)
            }
            val state = matchedState ?: run {
                // Kentältä varmistettu: sovellus lähettää myös "nomean"-tilan
                // kylmäkäynnistyksessä (meansValid=false), ennen kuin FLAGS-tavun
                // 2-bittinen state-enum (§4.2, 4 arvoa) riittää kuvaamaan tilannetta.
                // ESP32:n Renderer.cpp (tarkistettu) lukee d.state VAIN kun
                // d.meansValid==true — kun means ei ole validi, gauge ja label
                // piirtävät "--" state-arvosta riippumatta. GROOVE on siis
                // vaikutukseton placeholder tässä haarassa.
                if (meansValid) {
                    // Tätä ei pitäisi tapahtua: jos means on validi, sovelluksen
                    // pitäisi lähettää yksi neljästä tunnetusta tilasta. Lokitetaan
                    // näkyvästi, mutta ei kaadeta pakettia — muu data on yhä käyttökelpoista.
                    Log.w(
                        TAG,
                        "Tuntematon state '$stateStr' meansValid=true — pitäisi olla " +
                            "groove/lift/header/tack. Käytetään GROOVE-placeholderia, " +
                            "tarkista sovelluksen state-logiikka.",
                    )
                }
                RepeaterState.GROOVE
            }

            return RepeaterPacket(
                meansValid = meansValid,
                gpsValid = o.getBoolean("gpsValid"),
                state = state,
                heading = if (o.isNull("hdg")) null else o.getInt("hdg"),
                // Kentältä varmistettu: sovellus lähettää shift:null kun meansValid=
                // false (ei mielekästä shift-arvoa vielä). Protokollan SHIFT-kentällä
                // (§4.2) ei ole "ei dataa" -sentinelliä — Renderer.cpp:n drawGauge()
                // ei muutenkaan piirrä mitään kun meansValid=false, niin 0 on
                // vaikutukseton placeholder tässäkin.
                shift = if (o.isNull("shift")) 0 else o.getInt("shift"),
                perf = if (o.isNull("perf")) null else o.getInt("perf"),
                vmg = if (o.isNull("vmg")) null else o.getDouble("vmg"),
                sog = if (o.isNull("sog")) null else o.getDouble("sog"),
                seq = o.getInt("seq"),
            )
        }
    }
}
