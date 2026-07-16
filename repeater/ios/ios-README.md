# ios — iOS-natiivikuori (Swift)

**Vaihe 2:** toteutetaan kun kehityskone tukee (macOS Sequoia 15.6+ / Xcode 26,
iPhone 16e / iOS 26). WKWebView + CoreBluetooth + JS-silta.

## Tekninen
- **WKWebView** lataa Tackcall-sovelluksen (tackcall.app); `WKScriptMessageHandler` nimellä `tackcallBLE`
  vastaanottaa `pushToRepeater`:in datan (silta valmiina sovelluksessa v1.1+)
- **CoreBluetooth:** yhdistä `TACKCALL-RPT`, kirjoita 10 tavua (§4.2)
- **Tavupakkaus** Swiftissä — sama protokolla ja testivektorit kuin Android/sim

## Tila
Odottaa Vaihe 2:ta (kehityskoneen päivitys). Ks. valmistusohje §6b, §11 (../docs/).
