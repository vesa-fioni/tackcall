# Tackcall — muutosloki

Versiointi: jokaisella julkaistulla tiedostolla on oma `APP_VERSION`-vakio,
joka näkyy sovelluksessa ja toimii GitHub Pages -cachebustingin apuna.
Kaksi näkymää jakavat saman taktiikkamoottorin (kulmamatematiikka,
keskisuunta, `evalShift`, anturit). Moottoria muutettaessa **molemmat
tiedostot päivitetään yhtä aikaa**, kunnes/ellei se eriytetään omaksi
`engine.js`-tiedostoksi.

## Tiedostot
- `index.html` — klassinen näkymä (kompassiruusu + oskillaatiograafi)
- `mittari.html` — uusi LED-mittarinäkymä (vaaka-asento), luonnoksen sivu 2

---

## mittari.html

### mittari v0.26 — 2026-06-28
- Lisätty MIT-lisenssikommentti tiedoston alkuun (DOCTYPE-rivin jälkeen):
  Copyright (c) 2026 Vesa Lindqvist, Contributors: Joonas Sandholm.
  Varmistaa että tekijänoikeus- ja lisenssimaininta kulkee mukana myös
  yksittäisenä tiedostona jaettaessa. Ei vaikuta käyttöliittymään.

### mittari v0.25 — 2026-06-28
- Lisätty aloitusruutuun "How to use →" -nappi ja sitä vastaava käyttöohje-sheet
  (kuten klassisen version "Näin käytät"), käännettynä englanniksi ja sovitettuna
  mustaan/LED-teemaan: 3 askelta, palkin värilegenda (LIFT/HEADER/TACK/GROOVE),
  kompassi, äänet ja vinkki.
- Korjattu "Classic view" -nappi: poistettu inline-tyylit ja laajennettu
  .overlay-nappityyli koskemaan myös linkkiä, joten se näyttää nyt samalta kuin
  muut napit.
- Korjattu napien päällekkäisyys: napeille lisätty box-sizing:border-box ja
  linkki renderöityy nyt lohkona (display:block), joten napit pinoutuvat siististi.

### mittari v0.24 — 2026-06-27
- Poistettu "Meter"-otsikko aloitusruudusta. Aloitusruudussa on nyt pelkkä
  Tackcall-logo ja kuvausteksti.

### mittari v0.23 — 2026-06-27
- Lisätty aloitusruutuun sama Tackcall-logo kuin klassisessa näkymässä
  (sama base64-PNG kuin index.html:ssä). Korvaa aiemman "Tackcall"-tekstin;
  "Meter"-otsikko säilyy logon alla. Logo skaalautuu (max 240×96).

### mittari v0.22 — 2026-06-27
- Korjattu: myötätuulella palkisto vilkutti himmeästi turkoosia TACK-tilaa,
  koska shiftiarvio laski ison poikkeaman luovikeskisuuntaan nähden.
  Myötätuulella lift/header/tack-tila on nyt poistettu kokonaan — näkyy vain
  himmeä staattinen asteikko, ei vilkkua, ja poikkeamalukema on "--".

### mittari v0.21 — 2026-06-27
- Demo tehty uudelleen opettelua palvelevaksi ja rauhallisemmaksi:
  - Opetteluvaihe: vene seuraa käyttäjän valitsemaa halssia (STB/BB)
    luovissa, ja käyttäjä asettaa upwind STB/BB -keskisuunnat itse — juuri
    se ainoa taito, joka pitää oppia. Keskisuuntia ei enää aseteta valmiiksi.
  - Esittelyvaihe (käynnistyy kun molemmat keskisuunnat on asetettu): hidas
    rata, joka näyttää myös myötätuulen himmennyksen. Hitaampi kuin v0.20
    (n. 18 s/vaihe, loivat käännökset, lievempi heilahtelu).
  - "Reset means" palauttaa opetteluvaiheeseen.

### mittari v0.20 — 2026-06-27
- Demo kulkee nyt myös myötätuuleen: rata kiertää neljä tilaa ~10 s kukin
  (UPWIND STB → UPWIND BB → DOWNWIND BB → DOWNWIND STB) pehmein käännöksin.
  Demo asettaa keskisuunnat automaattisesti, joten tuuli tunnetaan heti ja
  myötätuulen himmennys + indikaattori näkyvät ilman vesillä testaamista.

### mittari v0.19 — 2026-06-27
- Myötätuuli rauhoitettu tässä versiossa: DOWNWIND STB/BB -tilassa palkisto
  ja vasemman/keskilohkon lukemat (poikkeama, tila, MEAN) himmennetään
  voimakkaasti, ja äänimerkit vaimennetaan kokonaan. Kirkkaina pidetään vain
  iso kompassilukema ja oikean kulman tila (UPWIND/DOWNWIND + STB/BB).
  Aiemmin sovellus piippaili myös myötätuulella, mikä häiritsi vesillä.
  Palkisto tuodaan myötätuulelle takaisin vasta jos käyttäjäpalaute pyytää.

### mittari v0.18 — 2026-06-27
- Oikean yläkulman indikaattori näyttää nyt purjehdustilan: yläotsikko
  UPWIND/DOWNWIND ja arvona STB/BB. Tila päätellään tuulesta, joka saadaan
  kahdesta asetetusta luovikeskisuunnasta (estWind): tuuli oikealla = STB,
  vasemmalla = BB; keula kohti tuulta = UPWIND, poispäin = DOWNWIND. Ennen
  kuin molemmat keskisuunnat on asetettu, näytetään valittu halssi (TACK).
- HUOM: shiftipalkisto laskee yhä luovikeskisuuntiin nähden (mielekäs
  upwind). Downwind-shiftien seuranta on erillinen, isompi askel.

### mittari v0.17 — 2026-06-27
- Mittarinäkymän käyttöliittymä käännetty kokonaan englanniksi (aloitusruutu,
  asetukset, kankaan tekstit, virheilmoitukset, kääntökehotus).
- Keskisuunta-nappiin lisätty "UPWIND" selventämään toimintoa:
  "SET UPWIND STB MEAN" / "SET UPWIND BB MEAN" (ilman halssia "SET UPWIND MEAN").
  Erottaa luovikeskisuunnan mahdollisesta tulevasta lenssikeskisuunnasta.

### mittari v0.16 — 2026-06-27
- "ASETA KESKISUUNTA" -napin teksti on nyt halssikohtainen: se kertoo minkä
  halssin keskisuunnan painallus asettaa ("ASETA STB-KESKISUUNTA" /
  "ASETA BB-KESKISUUNTA"). Ilman valittua halssia teksti on yleinen
  "ASETA KESKISUUNTA".

### mittari v0.15 — 2026-06-27
- Korjattu STB/BB-nappien korostus: se päivittyy nyt joka ruudulla suoraan
  halssin tilasta, joten se pysyy synkassa yläkulman HALSSI-tiedon kanssa
  myös automaattisen halssinvaihdon (autoSelectTack) jälkeen. Lisäksi napin
  painalluksessa halssi asetetaan ennen korostuksen päivitystä. Aiemmin
  napin korostus saattoi jäädä näyttämään väärää halssia.

### mittari v0.14 — 2026-06-27
- Palkiston puoli kääntyy nyt halssin mukaan reaalimaailmaa vastaavaksi:
  oikea = suunta kasvaa, vasen = suunta pienenee. Styyrpuurilla lift (vihreä)
  on oikealla, paapuurilla vasemmalla — eli vihreä osoittaa aina siihen
  suuntaan, johon keula kääntyy noustessa. Värit seuraavat semantiikkaa
  (vihreä = lift, kiihtyvä = header, turkoosi = tack); poikkeamaluku
  ennallaan (− = header).

### mittari v0.13 — 2026-06-27
- Palkiston väritys tehty epäsymmetriseksi tactical-logiikan mukaan:
  lift-puoli (oikea) pysyy vihreänä ("menee hyvin, jatka"), header-puoli
  (vasen) säilyttää kiihtyvän varoituksen vihreä → keltainen → oranssi →
  punainen, ja tack-kynnyksen yli turkoosi. Aiemmin myös iso lift muuttui
  punaiseksi, mikä oli harhaanjohtavaa. Poikkeamaluku noudattaa samaa
  logiikkaa.

### mittari v0.12 — 2026-06-27
- Kompassin tila näkyy nyt kankaalla mittaritilassa: "ODOTETAAN KOMPASSIA…"
  ja jos dataa ei tule ~2,5 s kuluessa, selkeä "EI KOMPASSIDATAA" -ohje
  (avaa Safarissa https-osoitteesta ja salli Liike & suunta). Aiemmin jos
  kompassidataa ei saatu, mittari näytti tyhjältä, koska virheilmoitus jäi
  jo suljetun aloitusruudun sisään piiloon.
- Orientaation tunnistus käyttää nyt ensisijaisesti matchMediaa (luotettava
  myös sovellusten sisäisissä selaimissa), varalla viewportin mitat.
- HUOM: anturien käyttöönottokoodi on identtinen index.html:n kanssa, joten
  jos kompassi ei toimi, syy on yleensä katselukontekstissa (sovelluksen
  sisäinen selain ei anna liike-/kompassidataa). Avaa varsinaisessa
  Safarissa.

### mittari v0.11 — 2026-06-27
- Palkiston korkeusprofiili muutettu lineaarisesta kaarevaksi (paraabeli):
  matala keskellä, kasvu kiihtyy reunoja kohti. Reunapalkkien ylärajaa
  korotettu, jolloin aiemmin tyhjäksi jäänyt kulmatila tulee käyttöön ja
  iso shifti erottuu paremmin. Keskipalkit pysyvät matalina, joten ne eivät
  osu suuntanumeroon.

### mittari v0.10 — 2026-06-27
- Tack-tilassa poikkeamaluku muuttuu nyt turkoosiksi, kuten palkistokin.
  Aiemmin luku jäi punaiseksi (suuruuden mukaan) vaikka palkit vaihtuivat
  turkoosiksi. Iso suuntanumero pysyy edelleen punaisena (identiteettiväri).

### mittari v0.9 — 2026-06-27
- Poikkeamaluku (tila-sanan alla) värjätään nyt skaalan värityksen mukaan
  poikkeaman suuruuden perusteella (vihreä → keltainen → oranssi → punainen),
  samalla logiikalla kuin palkisto. Tila-sana (LIFT/HEADER/TACK/GROOVE)
  säilyttää oman merkitysvärinsä.

### mittari v0.8 — 2026-06-27
- Tack-kynnyksen oletusarvo muutettu 5 → 20 (= mittarin skaalan maksimi).
  Lähtötilassa kynnys on siis löysimmillään, ja käyttäjä voi itse kiristää
  sitä tiukemmaksi asetuksista.

### mittari v0.7 — 2026-06-27
- Ylärivin asettelu tasattu: poikkeama+tila (vasen), MEAN (keski) ja
  HALSSI/SOG (oikea) ovat nyt samassa ruudukossa — pieni otsikko ylhäällä ja
  arvo sen alla, kaikki samalla korkeudella. Aiemmin vasen poikkeamalohko
  istui muita alempana.

### mittari v0.6 — 2026-06-27
- Kääntökehotus ei enää peitä aloitusruutua: landscape-vaatimus aktivoituu
  vasta kun käyttäjä on siirtynyt mittaritilaan (anturit/demo). Näin
  käyttäjä ehtii valita aloitusruudulta klassisen näkymän tai mittarin myös
  pystyasennossa. v0.5:n luotettava orientaation havainnointi säilyy.

### mittari v0.5 — 2026-06-27
- Korjattu orientaation käsittely: mittari toimii vain vaaka-asennossa ja
  pystyssä näkyy aina "Käännä laite vaaka-asentoon" -kehotus, myös
  aloitusruudun ja asetusten päällä. Kääntövihje nostettu ylimmäksi
  kerrokseksi (z-index 60). Orientaatio tarkistetaan nyt viewportin
  mitoista usean tapahtuman (resize, orientationchange) ja varmuusajastimen
  kautta, joten paluu vaaka→pysty havaitaan luotettavasti (aiemmin vihje ei
  palannut kertaalleen vaakaan siirtymisen jälkeen).

### mittari v0.4 — 2026-06-27
- Palkisto muutettu kaaresta tasapohjaiseksi pylväsmittariksi: kaikkien
  palkkien alareuna samalla alalinjalla, palkit nousevat pystysuoraan ja
  vain yläpää vaihtelee (matalin keskellä, korkein reunoilla). Ei enää
  kaaren taipumaa. Asteikkonumerot yhteisen alalinjan alla.

### mittari v0.3 — 2026-06-27
- Värikoodattu palkisto muutettu luonnoksen mukaiseksi: matalin keskellä
  (groove) ja kasvaa molemmin puolin mitä kauemmas keskisuunnasta mennään.
  Parantaa luettavuutta kirkkaassa auringonvalossa ja hyödyntää ruudun
  reunatilan. Asteikkonumerot seuraavat kunkin palkin alapäätä.

### mittari v0.2 — 2026-06-27
- Lisätty näkyvä **ASETA KESKISUUNTA** -nappi alas keskelle (STB/BB-riville),
  koska pelkkä numeron napautus oli liian piilossa. Numeron napautus toimii
  edelleen bonuksena. Onnistuessa nappi välähtää vihreänä.
- Kaaripalkki sovitettu nappirivin yläpuolelle, ettei se jää napin alle.

### mittari v0.1 — 2026-06-27
- Uusi vaihtoehtoinen käyttöliittymä: seitsenosainen LED-mittari, joka
  näkyy kauas ja vaaka-asennossa.
- Iso nykysuunta keskellä, lukittu keskisuunta pienellä ylhäällä
  (napauta = aseta keskisuunta nykysuuntaan), etumerkillinen poikkeama ja
  tila (LIFT / GROOVE / HEADER / TACK) vasemmalla.
- Kaareva värikoodattu oskillaatiopalkki (asteikko säädettävissä, oletus ±20°).
  TACK-kynnyksen ylitys → cyan + vilkku; asteikon ylitys → reuna pegaa.
- Oikeassa yläkulmassa halssi tai GPS-nopeus (SOG).
- Käyttää **muuttumattomana** index.html:n moottoria: pehmennys, keskisuunta
  per halssi, `evalShift`, automaattinen halssinvalinta, merkkiäänet, demo,
  GPS, wake lock.
- **Ei sisällä** automaattista COG-lukitusta vendan jälkeen (siirretty) eikä
  pre-start-näkymää (luonnoksen sivu 3 = oma uusi ominaisuus).
- Linkit klassiseen näkymään aloitusruudulta ja asetuksista.

---

## index.html

### v11 — jäädytetty
- Klassinen näkymä ennallaan. Ei muutoksia tässä julkaisussa.
