# Tackcall — Polaari, vaihe 1 -toteutusohjeet (PÄÄTETTY, toteutettu mittari.html v0.30)

Jatkoa polar-vaihe-0_5-ohjeet.md:lle. Päätökset alla tehty keskustellen ennen koodausta
(ks. polar-vaihe-1-ohjeet-luonnos.md avoimien kohtien listana).

## Päätökset
1. **TWA:** EI lisätä raakapisteeseen. Raakaloki pysyy raakana (heading + meanit), TWA
   lasketaan vasta jalostusvaiheessa `estWind()`:n avulla.
2. **Mean-vakaus:** lasketaan `setMean()`-hetkellä 10 sekunnin ikkunalla `S.rangeBuf`:ista
   (kiertosuure, mean resultant length, 0–1). Tallennetaan meanin yhteyteen. Ei kynnysarvoa,
   ei käytetä päätöksiin tässä vaiheessa.
3. **SOG-ristivalidointi:** siirretty vaiheeseen 2 (vaatii P90-referenssin).
4. **Tuulennollapisteen virkistys:** pelkkä `windAge`-kirjanpitokenttä raakapisteeseen
   (sekunteja viimeisimmästä mean-asetuksesta). Automaattinen meanin päivittäminen on
   oma erillinen, tuleva päätös — ei tässä vaiheessa.

## Toteutettu (mittari.html v0.30)
- `S.meanStbStability` / `S.meanBbStability` — lasketaan ja tallennetaan `Polar.onMeanSet()`:ssa.
- `Polar` localStorage-rakenne (`tackcall_polar_means_v1`) laajennettu sisältämään vakaudet.
- Raakapisteeseen (`tackcall_polar_log_v1`) lisätty `windAge`-kenttä.
- Ei UI-muutoksia, ei mitään näkyvää. setMean/evalShift/estWind/piirto ennallaan.

## Mitä EI tehty (jää vaiheeseen 2+)
- TWA-johdannainen, P90, kernel, confidence, SOG-ristivalidointi, automaattinen
  meanin päivitys, prosenttiluku, mikä tahansa UI.

Ks. CHANGELOG.md [mittari v0.30] -merkintä yksityiskohdille.
