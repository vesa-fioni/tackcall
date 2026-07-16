# android — Android-natiivikuori (Kotlin)

**Vaihe 1: konseptin todistus.** WebView(tackcall.app) + JS-silta + Android BLE.
Kääntyy nykyisellä kehityskoneella; BLE testattavissa oikealla laitteella heti.

## Tekninen
- **WebView** lataa Tackcall-sovelluksen tackcall.app:sta; `@JavascriptInterface`-olio
  `AndroidBridge.postMessage(json)` vastaanottaa `pushToRepeater`:in datan
- **BLE:** yhdistä `TACKCALL-RPT`, kirjoita 10 tavua (§4.2), write-without-response,
  operaatiojono (odota callback ennen seuraavaa)
- **Tavupakkaus** Kotlinissa — yksikkötestattava JVM:llä (samat testivektorit kuin sim/)
- **Permissiot:** BLUETOOTH_SCAN + BLUETOOTH_CONNECT (Android 12+)
- **Ei foreground serviceä ensivaiheessa** (ruutu pysyy päällä)

## Tila
Ei vielä toteutettu. Ks. valmistusohje §6a, §11 (../docs/).
