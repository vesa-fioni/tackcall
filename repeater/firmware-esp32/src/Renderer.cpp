// Renderer.cpp — ks. Renderer.h
// Kaannetty selainsimulaattorin (tackcall-repeater-sim.html) render()-logiikasta.
// Koordinaatit, segmenttikartta, 5x7-fontti ja gauge-porrastus ovat identtiset
// sim:n kanssa. Muutokset tanne synkassa sim/mockup-puolen kanssa.

#include "Renderer.h"

namespace tackcall {

// ---- 7-segmenttikartta (sim SEG) ----
// Bitit: top,tl,tr,mid,bl,br,bot
enum { S_TOP=1, S_TL=2, S_TR=4, S_MID=8, S_BL=16, S_BR=32, S_BOT=64 };
static int segMask(char ch) {
  switch (ch) {
    case '0': return S_TOP|S_TL|S_TR|S_BL|S_BR|S_BOT;
    case '1': return S_TR|S_BR;
    case '2': return S_TOP|S_TR|S_MID|S_BL|S_BOT;
    case '3': return S_TOP|S_TR|S_MID|S_BR|S_BOT;
    case '4': return S_TL|S_TR|S_MID|S_BR;
    case '5': return S_TOP|S_TL|S_MID|S_BR|S_BOT;
    case '6': return S_TOP|S_TL|S_MID|S_BL|S_BR|S_BOT;
    case '7': return S_TOP|S_TR|S_BR;
    case '8': return S_TOP|S_TL|S_TR|S_MID|S_BL|S_BR|S_BOT;
    case '9': return S_TOP|S_TL|S_TR|S_MID|S_BR|S_BOT;
    default:  return -1; // ei numero
  }
}

static void drawDigit(FrameBuffer& fb, int x, int y, int w, int h, char ch, int t) {
  int s = segMask(ch);
  if (s < 0) return;
  int my = y + (h >> 1);
  if (s & S_TOP) fb.rect(x+t, y, x+w-t, y+t);
  if (s & S_BOT) fb.rect(x+t, y+h-t, x+w-t, y+h);
  if (s & S_MID) fb.rect(x+t, my-(t>>1), x+w-t, my+(t>>1)+(t%2));
  if (s & S_TL)  fb.rect(x, y+t, x+t, my);
  if (s & S_TR)  fb.rect(x+w-t, y+t, x+w, my);
  if (s & S_BL)  fb.rect(x, my, x+t, y+h-t);
  if (s & S_BR)  fb.rect(x+w-t, my, x+w, y+h-t);
}

// Piirra numeromerkkijono. Tukee numeroita, '.', '-', ' ' (kuten sim drawNumber).
static int drawNumber(FrameBuffer& fb, int x, int y, int dw, int dh, int t, int gap, const char* text) {
  int cx = x;
  for (const char* p = text; *p; ++p) {
    char ch = *p;
    if (ch == '.') { int sz = t+1; fb.rect(cx, y+dh-sz, cx+sz, y+dh); cx += sz+gap; }
    else if (segMask(ch) >= 0) { drawDigit(fb, cx, y, dw, dh, ch, t); cx += dw+gap; }
    else if (ch == '-') { int my = y+(dh>>1); fb.rect(cx, my-(t>>1), cx+dw, my+(t>>1)); cx += dw+gap; }
    else if (ch == ' ') { cx += dw+gap; }
  }
  return cx;
}

// ---- 5x7-otsikkofontti (sim FONT5x7), vain tarvittavat kirjaimet ----
// Jokainen kirjain 7 rivia, kukin 5 bittia (bitti 4 = vasen).
struct Glyph { char c; uint8_t rows[7]; };
static const Glyph FONT[] = {
  {'H', {0b10001,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001}},
  {'D', {0b11110,0b10001,0b10001,0b10001,0b10001,0b10001,0b11110}},
  {'G', {0b01111,0b10000,0b10000,0b10111,0b10001,0b10001,0b01111}},
  {'R', {0b11110,0b10001,0b10001,0b11110,0b10100,0b10010,0b10001}},
  {'P', {0b11110,0b10001,0b10001,0b11110,0b10000,0b10000,0b10000}},
  {'E', {0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b11111}},
  {'F', {0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b10000}},
  {'V', {0b10001,0b10001,0b10001,0b10001,0b10001,0b01010,0b00100}},
  {'M', {0b10001,0b11011,0b10101,0b10101,0b10001,0b10001,0b10001}},
  {'S', {0b01111,0b10000,0b10000,0b01110,0b00001,0b00001,0b11110}},
  {'O', {0b01110,0b10001,0b10001,0b10001,0b10001,0b10001,0b01110}},
  {'L', {0b10000,0b10000,0b10000,0b10000,0b10000,0b10000,0b11111}},
  {'I', {0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b11111}},
  {'T', {0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b00100}},
  {'A', {0b01110,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001}},
  {'C', {0b01111,0b10000,0b10000,0b10000,0b10000,0b10000,0b01111}},
  {'K', {0b10001,0b10010,0b10100,0b11000,0b10100,0b10010,0b10001}},
};
static const Glyph* glyph(char c) {
  for (unsigned i = 0; i < sizeof(FONT)/sizeof(FONT[0]); ++i)
    if (FONT[i].c == c) return &FONT[i];
  return nullptr;
}
static void drawLabel(FrameBuffer& fb, int x, int y, const char* text) {
  int cx = x;
  for (const char* p = text; *p; ++p) {
    const Glyph* g = glyph(*p);
    if (g) {
      for (int r = 0; r < 7; ++r)
        for (int c = 0; c < 5; ++c)
          if ((g->rows[r] >> (4 - c)) & 1) fb.px(cx + c, y + r);
    }
    cx += 6;
  }
}

// ---- gauge (sim drawGauge) ----
static void drawGauge(FrameBuffer& fb, bool valid, int shift) {
  const int gL=58, gR=134, gcx=(gL+gR)>>1, gbot=62, gmax=10, scale=20, segw=2, gap=1;
  const int half=(gR-gL)>>1, n=half/(segw+gap);
  fb.rect(gcx, gbot-gmax+1, gcx, gbot); // keskiviiva
  if (!valid) return;
  double fr = (double)shift / scale;
  if (fr > 1) fr = 1; if (fr < -1) fr = -1;
  int active = (int)(fr < 0 ? -fr : fr) ; // alustus
  // sama pyoristys kuin sim: Math.round(abs(frac)*n)
  double af = (fr < 0 ? -fr : fr) * n;
  active = (int)(af + 0.5);
  int sign = (fr >= 0) ? 1 : -1;
  for (int i = 0; i < n; ++i) {
    if (i >= active) continue;
    // segH = max(2, round((i+1)/n*gmax/2)*2)
    double hh = ((double)(i+1)/n) * gmax / 2.0;
    int hr = (int)(hh + 0.5) * 2;
    int h = hr < 2 ? 2 : hr;
    int y0 = gbot - h + 1;
    int x0 = (sign >= 0) ? (gcx+1 + i*(segw+gap))
                         : (gcx-1 - i*(segw+gap) - (segw-1));
    fb.rect(x0, y0, x0+segw-1, gbot);
  }
}

// ---- "--" sentinelit (sim drawDash / drawDashCentered) ----
static void drawDash(FrameBuffer& fb, int x, int y, int dw, int dh, int t) {
  int my = y + (dh >> 1);
  fb.rect(x, my-(t>>1), x+dw, my+(t>>1));
  fb.rect(x+dw+3, my-(t>>1), x+2*dw+3, my+(t>>1));
}
static void drawDashCentered(FrameBuffer& fb, int x, int y, int dw, int /*dh*/, int /*t*/) {
  int my = y + 15;
  fb.rect(x+8, my-1, x+8+dw, my+1);
  fb.rect(x+8+dw+5, my-1, x+8+2*dw+5, my+1);
}

// Pieni int->merkkijono apuri (ei stdio-riippuvuutta laitteella)
static void intToStr(int v, char* out) {
  char tmp[12]; int n = 0; bool neg = v < 0; unsigned u = neg ? -(unsigned)v : v;
  if (u == 0) tmp[n++] = '0';
  while (u) { tmp[n++] = '0' + (u % 10); u /= 10; }
  int k = 0; if (neg) out[k++] = '-';
  while (n) out[k++] = tmp[--n];
  out[k] = 0;
}
// yksi desimaali (esim 1.1) — vmg/sog
static void oneDecimal(float v, char* out) {
  int whole = (int)v;
  int dec = (int)((v - whole) * 10.0f + 0.5f);
  if (dec >= 10) { whole += 1; dec -= 10; }
  char w[12]; intToStr(whole, w);
  int k = 0; for (char* p = w; *p; ++p) out[k++] = *p;
  out[k++] = '.'; out[k++] = '0' + dec; out[k] = 0;
}

static const char* STATE_LABEL[4] = { "", "LIFT", "HDR", "TACK" }; // groove=tyhja

void renderTo(FrameBuffer& fb, const RepeaterData& d) {
  fb.clear();

  // HDG-otsikko keskella aina
  drawLabel(fb, 64+18, 0, "HDG");
  // HDG-luku (tai --)
  if (!d.hdgValid) {
    drawDashCentered(fb, 64, 13, 16, 31, 3);
  } else {
    char s[8]; intToStr(d.hdg, s);
    // padStart(3,' ') — oikealle tasattu 3 merkkia
    char padded[8]; int len = 0; for (char* p=s; *p; ++p) len++;
    int k = 0; for (int i = 0; i < 3-len; ++i) padded[k++] = ' ';
    for (char* p=s; *p; ++p) padded[k++] = *p; padded[k]=0;
    drawNumber(fb, 64, 13, 16, 31, 3, 5, padded);
  }

  // shift-kentta (ylavasen): otsikko tilan mukaan
  const char* lbl = "";
  if (d.meansValid) lbl = STATE_LABEL[(int)d.state & 3];
  drawLabel(fb, 2, 0, lbl);
  if (!d.meansValid) {
    drawDash(fb, 2, 13, 8, 14, 2);
  } else {
    char s[8]; intToStr(d.shift, s); // intToStr hoitaa etumerkin
    drawNumber(fb, 2, 13, 8, 14, 2, 3, s);
  }

  // PERF (ylaoikea)
  drawLabel(fb, 158, 0, "PERF");
  if (!d.perfValid) drawDash(fb, 158, 13, 8, 14, 2);
  else { char s[8]; intToStr(d.perf, s); drawNumber(fb, 158, 13, 8, 14, 2, 3, s); }

  // VMG (alavasen)
  drawLabel(fb, 2, 34, "VMG");
  if (!d.vmgValid) drawDash(fb, 2, 47, 8, 14, 2);
  else { char s[8]; oneDecimal(d.vmg, s); drawNumber(fb, 2, 47, 8, 14, 2, 3, s); }

  // SOG (alaoikea)
  drawLabel(fb, 158, 34, "SOG");
  if (!d.sogValid) drawDash(fb, 158, 47, 8, 14, 2);
  else { char s[8]; oneDecimal(d.sog, s); drawNumber(fb, 158, 47, 8, 14, 2, 3, s); }

  // gauge
  drawGauge(fb, d.meansValid, d.shift);
}

} // namespace tackcall
