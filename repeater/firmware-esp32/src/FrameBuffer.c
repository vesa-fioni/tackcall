// FrameBuffer.h
// Tackcall repeater — abstrakti 192×64 monokromi pikselipuskuri.
//
// PUHDAS C++: ei U8g2- eika laiteriippuvuuksia. Renderer piirtaa tahan,
// host-testi tarkistaa sisallon, ja laitteella puskuri siirretaan U8g2:lle.
//
// Bittijarjestys: yksi bitti per pikseli. 1 = muste (musta piste nakyy),
// 0 = tausta. Tama vastaa selainsimulaattorin logiikkaa (sim: 0=muste,
// mutta tassa kaannetty jotta "aseta piste" = bitti 1, mika on luontevampi).
// Pakkaus: rivi kerrallaan, 8 pikselia per tavu (MSB vasemmalla).

#ifndef FRAME_BUFFER_H
#define FRAME_BUFFER_H

#include <stdint.h>
#include <string.h>

namespace tackcall {

static const int FB_W = 192;
static const int FB_H = 64;
static const int FB_STRIDE = FB_W / 8;          // 24 tavua per rivi
static const int FB_BYTES = FB_STRIDE * FB_H;   // 1536 tavua

class FrameBuffer {
public:
  FrameBuffer() { clear(); }

  void clear() { memset(buf_, 0, sizeof(buf_)); }

  // Aseta piste (muste). Rajojen ulkopuoliset jatetaan huomiotta.
  void px(int x, int y) {
    if (x < 0 || x >= FB_W || y < 0 || y >= FB_H) return;
    buf_[y * FB_STRIDE + (x >> 3)] |= static_cast<uint8_t>(0x80 >> (x & 7));
  }

  // Onko piste asetettu? (testeja varten)
  bool get(int x, int y) const {
    if (x < 0 || x >= FB_W || y < 0 || y >= FB_H) return false;
    return (buf_[y * FB_STRIDE + (x >> 3)] >> (7 - (x & 7))) & 1;
  }

  // Tayta suorakaide (molemmat paatepisteet mukaan lukien), kuten sim rect().
  void rect(int x0, int y0, int x1, int y1) {
    for (int y = y0; y <= y1; ++y)
      for (int x = x0; x <= x1; ++x)
        px(x, y);
  }

  // Asetettujen pikselien lkm (testeja varten: nopea "ei tyhja" -tarkistus)
  int count() const {
    int c = 0;
    for (int i = 0; i < FB_BYTES; ++i) {
      uint8_t b = buf_[i];
      while (b) { c += b & 1; b >>= 1; }
    }
    return c;
  }

  const uint8_t* data() const { return buf_; }
  uint8_t* data() { return buf_; }

private:
  uint8_t buf_[FB_BYTES];
};

} // namespace tackcall

#endif // FRAME_BUFFER_H
