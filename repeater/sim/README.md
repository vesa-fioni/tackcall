# sim — selainsimulaattorit

Todistavat repeaterin **ohjelmistoketjun ilman rautaa**: protokolla (objekti→10 tavua→
kentät), tila-logiikka ja 192×64-renderöinti. Renderöinti vastaa pikselintarkasti
laitteen layoutia (valmistusohje §2).

## Sisältö
- `tackcall-repeater-sim.html` — standalone: säätimet + laitenäyttö + tavunäkymä.
  Avaa selaimessa, ei riipu muusta.
- `tackcall-repeater-live.html` — upottaa mittari.html:n iframeen ja renderöi
  laitenäytön elävästä sovellusdatasta (postMessage-silta).

## Huom
`tackcall-repeater-live.html` lataa sovelluksen osoitteesta `https://tackcall.app/mittari.html`,
joten se toimii tästä kansiosta suoraan (ei vaadi mittari.html:ää samassa hakemistossa).
Testattu: protokolla 32/32 + päästä-päähän 11/11 (ks. valmistusohje §13).
