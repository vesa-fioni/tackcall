# Tackcall — Polaari, vaihe 2b-laskenta toteutusohjeet (LUONNOS v1)

Jatkoa vaiheille 0.5, 1 ja 2a. Tämä vaihe integroi polar-debug.html:ssä validoidun
P90-kernel-laskennan mittari.html:n Polar-moduuliin ja kytkyy tuloksen PERF-näyttöön.

## Päätökset

- **Laskennan ajoitus:** käynnistettäessä (init) + erikseen asetusvalikosta.
  Ei automaattista jatkuvaa laskentaa — ei hidasta 60Hz piirtosilmukkaa.
- **Mitä localStoragessa on** (mittari.html kirjoittaa, polar-debug.html lukee):
  - `tackcall_polar_means_v1` — meanit, aikaleimat, vakaudet
  - `tackcall_polar_log_v1` — raakaloki: {ts, heading, sog, tack, windAge} per piste
  Tämä on totuus josta kaikki johdetaan. polar-debug.html lukee tämän raakalogiin
  ja **laskee itse** P90/kernel-näytön siitä — ei lue eikä kirjoita valmiiksi
  laskettua polaaria localStorageen.
- **Mitä ei tallenneta** (lasketaan aina raakalokista):
  P90-estimaatit per TWA-kulma, kernel-leveydet, efektiiviset massat. Nämä ovat
  haihtuvaa välimuistia — lasketaan käynnistyksessä/pyynnöstä, pidetään muistissa,
  katoavat sivun sulkeutuessa. Raakaloki on totuus; laskettu polaari on johdettu
  näkymä (white paper luku 9.2). Tämä mahdollistaa laskennan parantamisen myöhemmin
  ilman että vanha data menee hukkaan.
- **PERF kolmiportainen:** "--" (ei dataa) / himmeä % (vähän dataa) / kirkas %
  (riittävästi dataa). Kynnysarvot nimettyinä konstantteina, säädettävissä myöhemmin.
- **SOG-ristivalidointi** edelleen lykätty — ei tässä vaiheessa.

---

## 1. Koodi siirrettävä polar-debug.html:stä Polar-moduuliin

Seuraavat funktiot kopioidaan **sellaisinaan** polar-debug.html:stä Polar-IIFEn
sisälle (ennen `return {}`-lausetta). Kommentit päivitetään suomeksi konvention mukaan.

```js
// Konstantit — samat kuin polar-debug.html:ssä, nimettyinä säätöä varten
const SOG_MIN_KN          = 0.5;
const SOG_MAX_KN          = 2.0;
const KERNEL_SIGMA_SMOOTH = 20;
const KERNEL_SIGMA_BASE   = 15;
const KERNEL_K            = 0.3;
const POLAR_STEP_DEG      = 5;
const TWA_MIN             = 15;
const TWA_MAX             = 175;
// PERF-näytön kynnykset efektiiviselle massalle (säädettävissä kenttädatalla, O5)
const PERF_MASS_DIM       = 3;   // alle tämän: himmeä prosentti
const PERF_MASS_BRIGHT    = 8;   // tästä ylöspäin: kirkas prosentti

// Haihtuvaan välimuistiin laskettu polaari (ei localStorage)
let _polar = null;

// Apufunktiot (samat kuin polar-debug.html)
function _circMean2(a, b){ ... }   // kahden kulman kiertosuure-keskiarvo
function _normAngle(d){ ... }      // normalisoi -180..180
function _sogW(sog){ ... }         // SOG-paino lineaarisella rampilla
function _gauss(delta, sigma){ ... }
function _pointTWA(p){ ... }       // johtaa TWA:n raakapisteestä
function _makeQueries(){ ... }     // laskentapisteet TWA-akselilla

// Pääfunktio: kaksivaiheinen gradienttisidottu kernel-P90
function _computePolar(log){ ... }

// Hakee P90-estimaatin ja efektiivisen massan annetulle TWA:lle
function _perfAtTWA(polar, twa){
  if(!polar || twa==null) return null;
  let best=null, bestDist=Infinity;
  polar.queries.forEach((q,i)=>{
    const d=Math.abs(q-twa);
    if(d<bestDist && polar.results[i].p90!=null){ bestDist=d; best=i; }
  });
  if(best==null || bestDist>POLAR_STEP_DEG*1.5) return null;
  return { p90: polar.results[best].p90, mass: polar.results[best].mass };
}
```

**Huom:** `_circMean2`, `_normAngle`, `_sogW`, `_gauss`, `_pointTWA`, `_makeQueries`
ja `_computePolar` kopioidaan **kirjaimellisesti** polar-debug.html:n vastaavista
(funktiot `circMean2`, `normAngle`, `sogW`, `gauss`, `pointTWA`, `makeQueries`,
`computePolar`) — etuliite `_` erottaa ne mahdollisista muista saman nimisistä
funktioista moduulin ulkopuolella.

---

## 2. Polar-moduulin public API — lisäykset

`return {}`-lauseeseen lisätään kaksi uutta metodia:

```js
return {
  init(){ loadMeans(); loadLog(); setInterval(collectPoint, LOG_INTERVAL_MS);
          _polar = _computePolar(log); },   // lasketaan heti käynnistyksessä
  onMeanSet(){ ... },   // ennallaan
  onReset(){ ... },     // ennallaan
  logLength(){ return log.length; },
  // Uudet:
  recompute(){
    // Kutsutaan asetusvalikosta käyttäjän pyynnöstä
    _polar = _computePolar(log);
    return _polar ? _polar.nPts : 0; // palauttaa hyväksyttyjen pisteiden määrän
  },
  perf(twa, sog){
    // Palauttaa {pct, mass, state} tai null
    // state: 'none' | 'dim' | 'bright'
    if(!_polar || twa==null || sog==null) return null;
    const r = _perfAtTWA(_polar, twa);
    if(!r || r.p90==null || r.p90<=0) return null;
    const pct = Math.round((sog / r.p90) * 100);
    const state = r.mass < PERF_MASS_DIM   ? 'none'
                : r.mass < PERF_MASS_BRIGHT ? 'dim'
                : 'bright';
    return { pct, mass: r.mass, state };
  }
};
```

---

## 3. Asetusvalikko — "Update polar" -nappi

Settings-paneliin lisätään uusi nappi `btnReset`-napin yläpuolelle:

```html
<button class="close" id="btnPolarUpdate">Update polar</button>
```

JS-tapahtumankäsittelijä:
```js
$("btnPolarUpdate").addEventListener("click", ()=>{
  const n = Polar.recompute();
  const btn = $("btnPolarUpdate");
  // Lyhyt palaute: näytetään pisteiden määrä 2 sekuntia
  btn.textContent = n > 0 ? `✓ ${n} pts` : '✗ Not enough data';
  setTimeout(()=>{ btn.textContent='Update polar'; }, 2000);
});
```

---

## 4. PERF-näyttö render()-funktiossa

Nykyinen PERF-placeholder (aina "--" tummana) korvataan:

```js
// PERF — polaarilaskenta (2b)
const wind = estWind();
const twaNow = (wind!=null && S.heading!=null)
  ? Math.abs(angDiff(wind, S.heading)) : null;
const sogNow = (S.gps.on && S.gps.sog!=null) ? S.gps.sog : null;
const perfData = Polar.perf(twaNow, sogNow);

let perfStr, perfCol;
if(!perfData || perfData.state==='none'){
  perfStr='--'; perfCol='#3a3a3a';                    // ei dataa
} else if(perfData.state==='dim'){
  perfStr=perfData.pct+'%'; perfCol='#7a4040';        // oppii, himmeä
} else {
  perfStr=perfData.pct+'%'; perfCol='#ff7a7a';        // luotettava, kirkas
}
label('PERF', W*0.84, yEy, Math.max(10,small*0.024*sScale), '#6a6a6a','center');
drawNumber(perfStr, W*0.84, yVal, small*0.062*sScale, perfCol, false);
```

---

## 5. Suorituskyky — miksi "vain pyynnöstä" on oikea valinta

`_computePolar(log)` käy koko lokin läpi kaksi kertaa (sileytyspassi + adaptiivinen
kernel). Nykyisellä testidatalla (639 pistettä) tämä kestää < 5ms — ei ongelma
käynnistyksessä. 60Hz piirtosilmukassa sama laskenta 60×/s = 300ms/s CPU-kuormaa,
mikä on täysin sopimaton. Pyynnöstä-malli on ainoa järkevä valinta.

Kun käyttäjä kerää uutta dataa purjehduksen aikana, "Update polar" -nappi
asetusvalikossa päivittää laskelman. Ei inkrementaalista päivitystä — yksinkertaisin
toteuttaa, toimii nykyisillä datamäärillä.

---

## 6. Mitä EI tehdä tässä vaiheessa

- Ei SOG-ristivalidointia (lykätty edelleen)
- Ei windAge-painotusta P90-laskennassa (vaihe 3)
- Ei confidence-laskentaa sessiotasolla (vaihe 3)
- Ei polaarinäyttöä käyttäjälle (ei erillistä visualisointia — PERF%-luku riittää)
- Ei PERF_MASS-kynnysten lukitsemista — jätetään nimettynä säätövakiona

## Reunaehdot

- polar-dev-v1-branch, surgical str_replace, base64-logo koskematon
- JS-syntaksi validoitu `node --check` ennen palautusta
- Kommentit suomeksi, UI englanniksi
- Versio v0.31 → v0.32, CHANGELOG päivitetään

---

## 7. Aloitusoverlayyn polaaridatan tilannekatsaus

Kun käyttäjä avaa sovelluksen, overlay näyttää jo GPS-selityksen. Lisätään sen
alle dynaaminen rivi joka lukee raakalogista pisteiden määrän ja kertoo
polaaritiedon statuksen. Tämä kommunikoi käyttäjälle kolme asiaa:
1. Sovellus muistaa veneen suorituskyvyn yli istuntojen
2. PERF on odottamassa — ei rikki, vaan oppii
3. Mitä enemmän purjehtii, sitä tarkempi PERF

### HTML-lisäys overlayhin (GPS-selityksen jälkeen)

```html
<p class="sub" id="polarStatus" style="font-size:11px;color:#555;margin-top:-8px;"></p>
```

### JS-lisäys — kutsutaan sivun latautuessa ennen overlayhin siirtymistä

```js
(function updatePolarStatus(){
  try{
    const log = JSON.parse(localStorage.getItem('tackcall_polar_log_v1')||'[]');
    const el = $("polarStatus");
    if(!el) return;
    if(log.length > 20){
      el.textContent = log.length + ' data points stored · PERF will show when ready';
    } else if(log.length > 0){
      el.textContent = 'Building performance data · keep sailing to enable PERF';
    } else {
      el.textContent = 'PERF learns your boat\'s speed as you sail · starts empty';
    }
  }catch(e){}
})();
```

### Kolme tilaa

| Pisteitä | Teksti |
|---|---|
| > 20 | "580 data points stored · PERF will show when ready" |
| 1–20 | "Building performance data · keep sailing to enable PERF" |
| 0 | "PERF learns your boat's speed as you sail · starts empty" |

Kynnys 20 on ensimmäinen arvaus — sama idea kuin PERF_MASS-kynnykset, säädettävissä.
Pisteiden määrä kertoo käyttäjälle konkreettisesti että data on olemassa ja
kasvaa — arvo jota kilpailupurjehtija arvostaa.

## Päätökset — kaikki lukittu

1. **PERF_MASS_DIM=3 / PERF_MASS_BRIGHT=8** — hyväksytty lähtöarvoiksi.
   Säädettävissä kenttädatan perusteella (O5).

2. **"Update polar" -napin paikka** — asetusvalikossa "Reset means" -napin
   yläpuolella. Hyväksytty.

3. **"Update polar" -napin teksti** — näyttää "No polar data yet" kun loki on
   tyhjä tai liian pieni. Vaihtuu "Update polar":ksi kun dataa on riittävästi.
   Palauteviesti painettaessa: "✓ N pts" tai "No polar data yet" jos edelleen
   liian vähän.
