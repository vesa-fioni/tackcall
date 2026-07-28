// test_renderer.cpp
// Host-testi Rendererille. Vertaa C++:n tuottaman FrameBufferin
// selainsimulaattorista generoituihin referenssipuskureihin (ref_frames.h)
// TAVU TAVULTA. Jos tama menee lapi, C++-renderointi on pikselintarkasti
// sama kuin sim — eli laitteella nakyva kuva vastaa hyvaksyttya mockupia.

#include <unity.h>
#include "Renderer.h"
#include "ref_frames.h"

using namespace tackcall;

void setUp(void) {}
void tearDown(void) {}

// Vertaa fb sim-referenssiin tavu tavulta.
static void assertMatches(const FrameBuffer& fb, const uint8_t* ref) {
  const uint8_t* got = fb.data();
  for (int i = 0; i < FB_BYTES; ++i) {
    if (got[i] != ref[i]) {
      char msg[64];
      snprintf(msg, sizeof(msg), "byte %d: got 0x%02X ref 0x%02X", i, got[i], ref[i]);
      TEST_FAIL_MESSAGE(msg);
      return;
    }
  }
}

static RepeaterData mk(bool means, bool gps, State st, bool hdgV, int hdg,
                       int shift, bool pV, int perf, bool vV, float vmg, bool sV, float sog) {
  RepeaterData d;
  d.magicOk=true; d.crcOk=true;
  d.meansValid=means; d.gpsValid=gps; d.state=st;
  d.hdgValid=hdgV; d.hdg=hdg; d.shift=shift;
  d.perfValid=pV; d.perf=perf;
  d.vmgValid=vV; d.vmg=vmg;
  d.sogValid=sV; d.sog=sog;
  return d;
}

void test_render_header(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,true,State::Header,true,335,-17,true,87,true,1.1f,true,2.0f));
  assertMatches(fb, REF_header);
}
void test_render_lift(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,true,State::Lift,true,10,6,true,95,true,3.2f,true,4.1f));
  assertMatches(fb, REF_lift);
}
void test_render_tack(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,true,State::Tack,true,270,33,true,80,true,2.0f,true,5.5f));
  assertMatches(fb, REF_tack);
}
void test_render_groove(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,true,State::Groove,true,90,2,true,99,true,0.5f,true,1.0f));
  assertMatches(fb, REF_groove);
}
void test_render_nomean(void) {
  FrameBuffer fb;
  renderTo(fb, mk(false,true,State::Groove,true,200,0,true,50,true,3.3f,true,4.4f));
  assertMatches(fb, REF_nomean);
}
void test_render_gpsoff(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,false,State::Lift,true,50,8,false,0,false,0,false,0));
  assertMatches(fb, REF_gpsoff);
}

// Jarkevyystarkistus: puskuri ei ole tyhja eika taynna
void test_render_sane(void) {
  FrameBuffer fb;
  renderTo(fb, mk(true,true,State::Header,true,335,-17,true,87,true,1.1f,true,2.0f));
  int c = fb.count();
  TEST_ASSERT_TRUE(c > 100);          // jotain piirretty
  TEST_ASSERT_TRUE(c < FB_W*FB_H/2);  // ei kaikki mustana
}

int main(int, char**) {
  UNITY_BEGIN();
  RUN_TEST(test_render_header);
  RUN_TEST(test_render_lift);
  RUN_TEST(test_render_tack);
  RUN_TEST(test_render_groove);
  RUN_TEST(test_render_nomean);
  RUN_TEST(test_render_gpsoff);
  RUN_TEST(test_render_sane);
  return UNITY_END();
}
