# Changelog

Kaikki merkittävät muutokset Tackcalliin kirjataan tähän tiedostoon.

## [polar-debug v2a] — polar-dev-v1 — Polaari vaihe 2a (laskenta, vain debug-näyttö)

mittari.html pysyy koskemattomana (v0.30). Kaikki uusi koodi polar-debug.html:ssä.
Ks. polar-vaihe-2a-ohjeet.md päätöksistä.

### Lisätty (polar-debug.html)
- **computePolar(log):** kaksivaiheinen gradienttisidottu kernel-P90-laskenta koko
  raakalokista. Ei inkrementaalista välimuistia — lasketaan aina pyynnöstä.
- **Vaihe 1 (sileytyspassi):** painotettu SOG-keskiarvo kiinteällä Gaussian-kernelillä
  (σ=20°) jokaiselle 5° TWA-kyselypisteelle. Pohja gradientin estimoinnille.
- **Gradientin estimointi:** numerinen derivaatta sileästä käyrästä (ei raakadatasta
  — vakaa myös harvalla datalla).
- **Vaihe 2 (adaptiivinen kernel, P90):** σ(TWA) = KERNEL_SIGMA_BASE / (1 + KERNEL_K
  × |grad|) — kapea jyrkillä alueilla (no-go), leveä loivilla (myötätuuli). Painotettu
  P90 per kyselypiste (sortattu SOG-jakauma, 90% kumulatiivinen paino).
- **SOG-pohjainen paino:** lineaarinen rampi [SOG_MIN_KN=0.5, SOG_MAX_KN=2.0].
- **TWA-johdannainen lukuvaiheessa:** `circMean2(meanStb,meanBb)` → windDir,
  `abs(norm(heading-windDir))` → TWA. Ei tallenneta raakalokiin (vaihe 1 -päätös).
- **Efektiivinen massa** per kyselypiste — luotettavuusarvio myöhempää kynnystä varten.
- **Nykyinen suorituskyky:** viimeisimmän lokipisteen TWA + SOG → P90-haku → %.
- **TWA-taulukko debug-sivulla:** TWA° | P90(kn) | Massa | σ(°) | Gradientti.
- Kaikki nimettyinä konstantteina: SOG_MIN_KN, SOG_MAX_KN, KERNEL_SIGMA_SMOOTH,
  KERNEL_SIGMA_BASE, KERNEL_K, POLAR_STEP_DEG — helposti säädettävissä kenttädatan
  jälkeen ilman muutoksia rakenteeseen.

### Tietoisesti rajattu pois (ks. polar-vaihe-2a-ohjeet.md)
- **SOG-ristivalidointi lykätty vaiheeseen 2b** — vaatisi laskennan mittari.html:ään,
  2a validoi logiikan ensin debug-sivulla.
- Symmetria-peilaus, windAge-paino, P90/P95-valinta, confidence — myöhemmät vaiheet.
- **Ei mitään muutoksia mittari.html:ään** — piirto, audio, meanit, evalShift ennallaan.

 — polar-dev-v1 — Polaari vaihe 1 (rajattu, ks. avoimet päätökset alla)

Jatkaa vaihetta 0.5 samalla periaatteella: näkymätön taustakeruu, ei UI-muutoksia, ei
jalostusta. Toteutettu polar-vaihe-1-ohjeet-luonnos.md:n päätösten mukaisesti — kolme
neljästä avoimesta kohdasta rajattiin ulos tästä vaiheesta (ks. alla).

### Lisätty
- **Mean-vakaus (`meanStbStability`/`meanBbStability`):** lasketaan `setMean()`-hetkellä
  `S.rangeBuf`:in viimeisen 10s ikkunan headingeista kiertosuureena (mean resultant
  length, 0–1). Tallennetaan meanin yhteyteen `tackcall_polar_means_v1`-avaimeen,
  aikaleiman rinnalle. **Arvo tallennetaan, mutta sitä ei käytetä mihinkään päätökseen
  tässä vaiheessa** — kynnysarvot (mikä on "vakaa") vaativat kenttädataa (O4/O5).
- **windAge-kirjanpito:** jokainen raakalokin piste sisältää nyt `windAge`-kentän
  (sekunteja viimeisimmän mean-asetuksen jälkeen, kumpi tahansa halssi). Pelkkä
  kirjanpito — ei kytketty mihinkään automatiikkaan.
- Polar-debug.html päivitetty näyttämään mean-vakaus prosentteina.

### Tietoisesti rajattu pois (ks. polar-vaihe-1-ohjeet-luonnos.md)
- **TWA ei lisätty raakapisteeseen.** Raakaloki pidetään mahdollisimman raakana
  (heading + meanit, kuten vaiheessa 0.5) — TWA lasketaan jatkossakin vasta
  lukuvaiheessa/jalostuksessa `estWind()`:n avulla, ei tallenneta valmiiksi laskettuna.
- **SOG-ristivalidointi siirretty vaiheeseen 2.** Vaatii P90-referenssin, jota ei ole
  olemassa ennen kuin lukuvaiheen kvantiililaskenta on toteutettu.
- **Automaattista, näkymätöntä meanin päivittämistä (luku 4.5) ei toteutettu.**
  windAge on vasta kirjanpitoa sille — itse automatiikka on oma, erillinen päätös,
  joka vaatii oman ohjeensa ennen koodausta (white paperin nimeämä "vaarallisin
  yksittäinen virhelähde").

### Ei muutettu
- setMean():n perustoiminta, evalShift(), estWind(), piirtologiikka, audio — ennallaan.
- 1 Hz keräysväli pidetty tarkoituksella ennallaan tässä kehitysvaiheessa (tiedostettu
  kapasiteettirajoite koko kesän osalta — ei korjattu, päätetty erikseen myöhemmin).

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
