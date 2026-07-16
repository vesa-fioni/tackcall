# firmware-esp32 — ESP32-firmware

"Tyhmä toistin": vastaanottaa BLE:llä 10 tavun paketin (1×/s), tarkistaa MAGIC+CRC8,
piirtää 192×64-layoutin. Ei laske mitään itse.

## Tekninen
- **Kieli:** C++ (Arduino-ydin ESP32:lle), kääntö PlatformIO tai Arduino IDE 2.x
- **Näyttö:** RG19264A-FHW-X, ohjain NT7107/NT7108 → U8g2 KS0108-luokka, 6800-väylä
- **BLE:** NUS-tyylinen periaali, laitenimi `TACKCALL-RPT`
- **Kytkentä:** 5 V näyttö ↔ 3,3 V ESP32 → tasomuunnin (2× TXS0108E) pakollinen

## Tila
Ei vielä toteutettu. Ensimmäinen askel: näyttö näkyviin kovakoodatulla paketilla
(ei BLE:tä), sitten BLE-jäsennin. Ks. valmistusohje §7, §11 (../docs/).
