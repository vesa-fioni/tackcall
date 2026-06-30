# Changelog

Kaikki merkittävät muutokset Tackcalliin kirjataan tähän tiedostoon.

## [mittari v0.29] — polar-dev-v1 — Polaari vaihe 0.5 (näkymätön taustakeruu)

Ei vaikuta UI:hin eikä olemassa olevaan logiikkaan (setMean, evalShift, estWind, piirto).
Puhdas additiivinen taustakerros suorituskykypolaaria varten (ks. white paper v12,
polar-vaihe-0_5-ohjeet.md).

### Lisätty
- Uusi `Polar`-moduuli (IIFE), eristetty omaan rajapintaan, ei kosketa globaalia `S`:ää
  suoraan paitsi tarkoituksella means-kentistä.
- **Meanit pysyviksi:** `S.meanStb`/`S.meanBb` tallennetaan aikaleimalla
  (`S.meanStbTs`/`S.meanBbTs`) `localStorage`-avaimeen `tackcall_polar_means_v1`
  aina kun `setMean()` kutsutaan.
- **Vanhenemissääntö:** käynnistyksessä vanha mean ladataan aktiiviseksi referenssiksi
  vain jos se on alle 3 tuntia vanha; vanhempi jätetään tyhjäksi (ei hiljaista virheellistä
  latausta — eilisen tuuli ei saa tulla tämän päivän referenssiksi).
- **Reset laajennettu:** `btnReset` tyhjentää nyt myös persistoidut meanit ja niiden
  aikaleimat (`Polar.onReset()`), ei vain istunnonaikaisia muuttujia.
- **Append-only raakahavaintoloki:** uusi `localStorage`-avain `tackcall_polar_log_v1`.
  Kerätään 1×/s (erillinen ajastin, ei `requestAnimationFrame`-silmukassa). Yksi piste:
  `{ts, heading, meanStb, meanBb, tack, sog}`. SOG tallennetaan 2 desimaalilla
  (näyttö pyöristää 1:een). Piste kerätään vain kun GPS on päällä ja SOG saatavilla —
  ei heading-only-pisteitä.
- Kaikki `localStorage`-kirjoitukset/-luvut try/catch-suojattu (ei kaada sovellusta jos
  tallennustila täynnä tai estetty).

### Ei muutettu
- setMean(), evalShift(), estWind(), piirtologiikka, audio — täysin entiset.
- Mitään UI-elementtiä ei lisätty eikä prosenttia näytetä. Puhdas taustakeruu.

### Tunnetut rajoitukset / avoimet jatkokysymykset
- Mean-vakautta (stability) ei loggata pisteisiin, koska sitä logiikkaa ei ole
  vielä koodissa (ks. white paper O1) — tulee vasta vaiheessa 1/2.
- Raakaloki kasvaa rajattomasti istunnon sisällä (koko arvioitu ~400 KB/kesä,
  reilusti alle 5–10 MB:n rajan) — siivous/jalostus tehdään myöhemmin lukuvaiheessa,
  ei tallennusvaiheessa.
