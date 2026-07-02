# Tackcall — Polaari, vaihe 2a -toteutusohjeet (PÄÄTETTY)

Jatkoa vaiheille 0.5 ja 1. Tämä on vaihe 2 jaettuna kahteen osaan: **2a on laskenta,
näkymätön mittari.html:ssä** — validoidaan P90+kernel-logiikka oikealla kenttädatalla
polar-debug.html:ssä ennen kuin sille annetaan näkyvä paikka (2b). Kaikki päätökset
käyty läpi ennen koodausta (ks. polar-vaihe-2-ohjeet-luonnos.md).

## Päätökset (tiivistelmä ennen toteutusta)
0. Vaihe 2 jaettu: **2a = laskenta + debug-näyttö, 2b = UI mittari.html:ään.** ✓
1. Polaari lasketaan **aina koko lokista pyynnöstä** — ei inkrementaalista välimuistia. ✓
2. **P90 kiinteänä**, SOG-pohjainen paino (alarajan ja yläpään välinen lineaarinen rampi,
   tarkat kynnykset nimettyinä konstantteina alla), windAge ei mukana painossa vielä. ✓
3. **Gradienttisidottu kernel kaksivaiheisena** (ks. luku 3 alla), ei symmetria-peilausta. ✓
4. **Ei mitään mittari.html:n UI:hin** — polar-debug.html saa näyttää lasketun tuloksen. ✓
5. **SOG-ristivalidointi:** tallennetaan ratio (mean-asetushetken SOG / P90 samalla
   TWA-alueella), ei kynnysarvoa, ei aktiivista varoitusta. ✓ (Ks. kohta 5 alla —
   yksi auki oleva arkkitehtuurikysymys ennen toteutusta.)

---

## Kohta 1 — TWA johdannainen raakapisteistä (lukuvaiheessa, ei tallennuksessa)

Raakaloki tallentaa headingin + meanit, ei TWA:ta (vaihe 1 -päätös: raakadata pysyy
raakana). Laskentavaiheessa johdetaan TWA per piste:

```
windDir = circMean(meanStb, meanBb)   // sama logiikka kuin estWind() koodissa
twa = abs(norm(heading - windDir))    // 0–180°, halssi kertoo puolen
```

`circMean` kahdelle kulmalle: `atan2(sin(stb)+sin(bb), cos(stb)+cos(bb))` asteina.
`norm` tuo tuloksen -180..180-välille.

Pisteen mukana kulkeva `tack`-kenttä kertoo kummalle puolelle TWA osuu (STB/BB).
Polaari käyttää absoluuttista TWA:ta (0–180°) ja `tack`:ia erillisenä dimensiona
(pidetään halssikohtaisesti erillään, kuten white paper luku 6.2 suosittaa).

**Hylätään piste jos:**
- `meanStb` tai `meanBb` on null (tuulen suuntaa ei voida johtaa)
- TWA < 15° (no-go-vyöhyke, ei luotettavaa dataa)
- TWA > 175° (käytännössä suoraan myötätuuleen, harva piste)

## Kohta 2 — SOG-pohjainen paino

Jokainen piste saa painon joka vaimenee matalilla nopeuksilla (GPS epäluotettava):

```
w_sog(sog) = clamp((sog - SOG_MIN_KN) / (SOG_MAX_KN - SOG_MIN_KN), 0, 1)
```

**Nimetyt konstantit (säädettävissä kenttädatalla, ei kovakoodattu):**
```js
const SOG_MIN_KN  = 0.5;  // alle tämän paino = 0 (GPS-kohina)
const SOG_MAX_KN  = 2.0;  // tästä ylöspäin täysi paino 1
```

Nämä ovat ensimmäinen arvaus — tärkeää on että ne ovat nimettyjä konstantteja eikä
magiikkaa, jotta säätö on helppoa kenttädatan jälkeen.

## Kohta 3 — Gradienttisidottu kernel (kaksivaiheinen)

Ydinalgoritmi. Ratkaisee kana-muna-ongelman (O6): gradientin estimointi vaatii sileän
käyrän, sileän käyrän laskeminen vaatii sopivan kernelleveyden.

**Nimetyt konstantit:**
```js
const KERNEL_SIGMA_SMOOTH = 20;  // 1. vaiheen kiinteä leveys (asteina), sileytyspasskki
const KERNEL_SIGMA_BASE   = 15;  // 2. vaiheen perusleveys ennen gradienttikorjausta
const KERNEL_K            = 0.3; // gradienttiherkkyysvakio — säädettävä kenttädatalla
const POLAR_STEP_DEG      = 5;   // laskentapisteiden väli TWA-akselilla (asteina)
```

**Vaihe 1 — sileytyspassi (Gaussian, kiinteä σ):**
Lasketaan painotettu SOG-keskiarvo jokaiselle POLAR_STEP_DEG:n välein olevalle
TWA-arvolle koko TWA-akselilla (15°–175°):

```
K_smooth(Δtwa) = exp(-0.5 × (Δtwa / KERNEL_SIGMA_SMOOTH)²)
sog_smooth[q]  = Σ w_sog(i) × K_smooth(twa[q] - twa[i]) × sog[i]
               / Σ w_sog(i) × K_smooth(twa[q] - twa[i])
```

**Gradientin estimointi sileästä käyrästä:**
Numerinen derivaatta sileytyspassin tuloksesta (ei raakadatasta):

```
gradient[q] = (sog_smooth[q+1] - sog_smooth[q-1]) / (2 × POLAR_STEP_DEG)
```

Päissä (q=0, q=viimeinen) käytetään yksipuolista derivaattaa tai nollataan gradientti.

**Vaihe 2 — adaptiivinen kernel, painotettu P90:**
Lasketaan adaptiivinen kernelleveys jokaiselle kyselypisteelle:

```
σ(q) = KERNEL_SIGMA_BASE / (1 + KERNEL_K × |gradient[q]|)
K_adaptive(Δtwa, q) = exp(-0.5 × (Δtwa / σ(q))²)
```

Lopullinen estimaatti kyselypisteelle on **painotettu P90**, ei keskiarvo:

```
// Kullekin kyselypisteelle q:
pisteet järjestetään SOG:n mukaan kasvavaan järjestykseen
w_total[q]  = Σ w_sog(i) × K_adaptive(twa[q]-twa[i], q)  // efektiivinen massa
kumulatiivinen paino kasvatetaan pisteen painolla yksi kerrallaan järjestyksessä
P90[q]      = SOG-arvo jossa kumulatiivinen paino ylittää 0.90 × w_total[q]
```

**Efektiivinen massa** (= `w_total[q]`) tallennetaan jokaisen kyselypisteen rinnalle —
se on luotettavuusarvio jota käytetään myöhemmin kirkkauden päätökseen (2b).

## Kohta 4 — Ei UI-muutoksia mittari.html:ään

Tässä vaiheessa mittari.html ei muutu visuaalisesti millään tavalla. Piirto, audio,
meanit, evalShift — kaikki ennallaan. Ainoa kysymys on kohdasta 5 (alla).

## Kohta 5 — SOG-ristivalidointi (yksi auki oleva arkkitehtuurikysymys)

**[PÄÄTÄ ennen toteutusta tämä yksi jäljellä oleva kohta:]**

SOG-ristivalidointi tallentaa "mean-asetushetken SOG / P90 samalla TWA-alueella" -suhteen
(`meanStbSogRatio` / `meanBbSogRatio`) localStorage-avaimeen means-rakenteen yhteyteen.
Tämä vaatii P90-laskentaa `setMean()`-hetkellä — eli polaarilaskennan on oltava
saatavilla mittari.html:ssä, ei vain polar-debug.html:ssä.

**Kaksi vaihtoehtoa:**

**(a) Laskentalogikka mittari.html:ään heti (täysi 2a):**
Polar-moduuli (`Polar`) saa uuden sisäisen funktion `computePolar()` joka lukee lokin
ja laskee kaksivaiheisen kerneliarvion. Kutsutaan kerran käynnistyksessä ja tallennetaan
haihtuvaan (ei localStorage) välimuistiin. `onMeanSet()` käyttää tätä välimuistia
ratiolaskentaan. polar-debug.html lukee raakalokin suoraan ja laskee oman versionsa
samalla logiikalla (koodi toistetaan tai jaetaan sisäisen apufunktion kautta).

Hyöty: SOG-ratio tallentuu heti oikeaan data-rakenteeseen.
Riski: iso lisäys mittari.html:ään yhdellä kertaa — enemmän koodattavaa, enemmän
validoitavaa ennen kuin on nähty yhtään oikeaa kenttädataa laskennasta.

**(b) Laskentalogikka ensin vain polar-debug.html:ään, SOG-ratio lykätään 2b:hen:**
polar-debug.html saa kaiken laskennan; mittari.html pysyy koskemattomana vaiheessa 2a.
SOG-ratio tallennetaan vasta vaiheessa 2b kun laskentalogiikka on validoitu
kenttädatalla ja siirretään mittari.html:n Polar-moduuliin.

Hyöty: 2a pysyy puhtaasti "validointivaiheena" ilman mittari.html-muutoksia.
Piirros: SOG-ratio puuttuu lokilta koko 2a-testausvaiheen ajan, tulee vasta 2b:ssä.

**Oma suositukseni: (b).** Perusteluina:
- 2a:n koko pointti on validoida laskenta ennen kuin sille annetaan lisää roolia
- Laskennan kopioiminen kahteen paikkaan (mittari.html + debug.html) on riskialtista —
  jos logiikka muuttuu säädön yhteydessä, päivitetään se vain toiseen
- SOG-ratio on "tallenna talteen" -kirjanpitoa, ei kiireellistä — viive 2b:hen ei
  menetä mitään peruuttamatonta (toisin kuin raakadata, joka pitää kerätä heti)

**Vahvista tämä valinta (a tai b) ennen toteutusta.**

## Mitä polar-debug.html näyttää vaiheen 2a jälkeen

- Per-TWA-taulukko (esim. 5° välein): TWA°, P90 [kn], efektiivinen massa, σ(q)
- Pisteiden kokonaismäärä, hyväksyttyjen pisteiden määrä (TWA + SOG -suodatuksen jälkeen)
- Tämänhetkinen TWA (jos molemmat meanit asetettu ja GPS päällä): laskettu arvo +
  estimaatti + "suorituskyky X%" laskettu (näkyy vain debug-sivulla, ei mittarissa)
- Gradienttikäyrä taulukkomuodossa (gradient[q] per TWA) — näkyy suoraan onko
  kernelleveys käyttäytymässä oikein eri TWA-alueilla

## Mitä EI tehdä tässä vaiheessa
- Ei UI-muutoksia mittari.html:ään (pl. mahdollinen kohta 5a).
- Ei symmetria-peilausta (myöhemmin).
- Ei confidence-laskentaa istuntotasolla (vaihe 3).
- Ei windAge-painotusta P90-laskennassa (vaihe 3).
- Ei P90/P95-valintaa käyttäjälle.
- Ei mitään sekundaarilukua ruudulle.

## Reunaehdot (samat kuin aiemmin)
- Polar-dev-v1-branch, surgical str_replace, base64-logo koskematon, JS-syntaksi
  validoitu ennen palautusta.
- Kommentit suomeksi, UI englanniksi, versio + CHANGELOG päivitetään.
- try/catch kaikessa localStorage-lukemisessa.
- Jos loki on tyhjä tai liian pieni (< 5 hyväksyttyä pistettä), funktio palauttaa
  siististi null — ei koskaan kaada sovellusta.
- KERNEL_SIGMA_SMOOTH, KERNEL_SIGMA_BASE, KERNEL_K, SOG_MIN_KN, SOG_MAX_KN,
  POLAR_STEP_DEG — kaikki nimettyjä konstantteja, ei magiikkaa koodissa.

## Avoin ennen toteutusta
- **Kohta 5: (a) vai (b)?** (SOG-ratio heti 2a:ssa vai lykätään 2b:hen)
  Kaikki muu on päätetty — tähän tarvitaan vielä vastaus.
