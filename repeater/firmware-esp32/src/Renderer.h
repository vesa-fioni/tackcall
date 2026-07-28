// Renderer.h
// Tackcall repeater — nayton piirtologiikka (valmistusohje §2).
//
// Piirtaa RepeaterData:n FrameBufferiin. PUHDAS C++ (ei U8g2:ta), jotta
// host-testi voi verrata tulosta selainsimulaattoriin. Laitteella sama
// FrameBuffer siirretaan U8g2:lle (u8g2.drawXBM tms.) — se on ohut liima
// joka tehdaan main.cpp:ssa, ei tassa.
//
// Layout (192×64), peilaa selainsimulaattoria bitilleen:
//   HDG   iso 7-segm keskella (~31px)
//   shift ylavasen: otsikko LIFT/HDR/TACK (tyhja groove/nomean) + etumerkillinen luku
//   PERF  ylaoikea, VMG alavasen, SOG alaoikea (pienet segm ~14px)
//   gauge HDG:n alla, kaksisuuntainen keskelta
//   puuttuva data -> "--"

#ifndef RENDERER_H
#define RENDERER_H

#include "FrameBuffer.h"
#include "RepeaterPacket.h"

namespace tackcall {

// Piirra dekoodattu paketti puskuriin. Tyhjentaa puskurin ensin.
void renderTo(FrameBuffer& fb, const RepeaterData& d);

} // namespace tackcall

#endif // RENDERER_H
