# Tackcall — ulkoinen BLE-näyttötoistin (repeater)

Custom-laite joka toistaa Tackcallin lasketut lukemat kirkkaassa auringonvalossa
luettavalla LCD-paneelilla (192×64, transflektiivinen). Puhelin laskee kaiken,
laite vain näyttää (tyhmä toistin).

Tämä kansio sisältää **kaiken repeater-spesifin**. Repon juuri pysyy identtisenä
`main`-branchin kanssa (web-sovellus, jonka GitHub Pages tarjoilee osoitteessa
tackcall.app); repeater-lisäys elää täällä `/repeater/`-kansion alla.

## Rakenne
- `docs/` — valmistusohje (spesifikaatio) ja whitepaperit
- `sim/` — selainsimulaattorit ohjelmistoketjun todistamiseen ilman rautaa
- `firmware-esp32/` — ESP32-firmware (näyttöajuri + BLE-periaali)
- `android/` — Android-natiivikuori (Kotlin) — Vaihe 1, konseptin todistus
- `ios/` — iOS-natiivikuori (Swift) — Vaihe 2

## Arkkitehtuuri lyhyesti
puhelin (WebView + Tackcall-sovellus) → BLE (10 tavua/s) → ESP32 → LCD.
Silta sovelluksessa: `pushToRepeater()` (v1.2+), no-op tavallisessa selaimessa.

Ajantasainen suunnitelma: `docs/` (uusin valmistusohje).
