// test_repeater_packet.cpp
// Host-testit RepeaterPacketille (pio test -e native). Ei laitetta.
// Sama kattavuus kuin selainsimulaattorin protokollatesti (32 PASS).
// Varmistaa etta ESP32 tulkitsee tavut identtisesti sim/Android-puolen kanssa.

#include <unity.h>
#include "RepeaterPacket.h"
#include <string.h>

using namespace tackcall;

void setUp(void) {}
void tearDown(void) {}

// Apuri: rakenna RepeaterData ja aja round-trip encode->decode
static RepeaterData roundtrip(const RepeaterData& in) {
  uint8_t buf[REPEATER_LEN];
  size_t n = encode(in, buf, sizeof(buf));
  TEST_ASSERT_EQUAL_UINT32(REPEATER_LEN, n);   // T9: pituus aina 10
  return decode(buf, sizeof(buf));
}

// ---- T1: header-tila, taysi data ----
void test_header_full(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Header;
  in.hdgValid = true; in.hdg = 335; in.shift = -17;
  in.perfValid = true; in.perf = 87;
  in.vmgValid = true; in.vmg = 1.1f;
  in.sogValid = true; in.sog = 2.0f;
  RepeaterData d = roundtrip(in);
  TEST_ASSERT_TRUE(d.magicOk);
  TEST_ASSERT_TRUE(d.crcOk);
  TEST_ASSERT_EQUAL_INT(335, d.hdg);
  TEST_ASSERT_TRUE(d.hdgValid);
  TEST_ASSERT_EQUAL_INT(-17, d.shift);
  TEST_ASSERT_EQUAL_INT(87, d.perf);
  TEST_ASSERT_TRUE(d.vmgValid);
  TEST_ASSERT_FLOAT_WITHIN(0.001f, 1.1f, d.vmg);
  TEST_ASSERT_FLOAT_WITHIN(0.001f, 2.0f, d.sog);
  TEST_ASSERT_EQUAL_INT((int)State::Header, (int)d.state);
  TEST_ASSERT_TRUE(d.meansValid);
  TEST_ASSERT_TRUE(d.gpsValid);
}

// ---- T2: GPS pois -> perf/vmg/sog sentinelit ----
void test_gps_off_sentinels(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = false; in.state = State::Lift;
  in.hdgValid = true; in.hdg = 10; in.shift = 6;
  in.perfValid = false; in.vmgValid = false; in.sogValid = false;
  RepeaterData d = roundtrip(in);
  TEST_ASSERT_FALSE(d.perfValid);
  TEST_ASSERT_FALSE(d.vmgValid);
  TEST_ASSERT_FALSE(d.sogValid);
  TEST_ASSERT_FALSE(d.gpsValid);
  TEST_ASSERT_EQUAL_INT((int)State::Lift, (int)d.state);
  TEST_ASSERT_EQUAL_INT(6, d.shift);
}

// ---- T3: nomean (means false), HDG voi silti nakya ----
void test_nomean_hdg_visible(void) {
  RepeaterData in;
  in.meansValid = false; in.gpsValid = true; in.state = State::Groove;
  in.hdgValid = true; in.hdg = 200;
  in.perfValid = true; in.perf = 50;
  in.vmgValid = true; in.vmg = 3.3f;
  in.sogValid = true; in.sog = 4.4f;
  RepeaterData d = roundtrip(in);
  TEST_ASSERT_FALSE(d.meansValid);
  TEST_ASSERT_EQUAL_INT(200, d.hdg);
  TEST_ASSERT_EQUAL_INT((int)State::Groove, (int)d.state);
}

// ---- T4: HDG null -> 0xFFFF sentinel ----
void test_hdg_null(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Tack;
  in.hdgValid = false; in.shift = 35;
  in.perfValid = true; in.perf = 100;
  in.vmgValid = true; in.vmg = 5.0f;
  in.sogValid = true; in.sog = 5.0f;
  RepeaterData d = roundtrip(in);
  TEST_ASSERT_FALSE(d.hdgValid);
  TEST_ASSERT_EQUAL_INT((int)State::Tack, (int)d.state);
}

// ---- T5: SHIFT-etumerkki kahden komplementtina (-90..90) ----
void test_shift_signs(void) {
  const int vals[] = { -90, -45, -1, 0, 1, 45, 90 };
  for (int i = 0; i < 7; ++i) {
    RepeaterData in;
    in.meansValid = true; in.gpsValid = true; in.state = State::Header;
    in.hdgValid = true; in.hdg = 0; in.shift = vals[i];
    in.perfValid = true; in.perf = 0;
    in.vmgValid = true; in.vmg = 0.0f;
    in.sogValid = true; in.sog = 0.0f;
    RepeaterData d = roundtrip(in);
    TEST_ASSERT_EQUAL_INT(vals[i], d.shift);
  }
}

// ---- T6: CRC havaitsee bittivirheen ----
void test_crc_detects_corruption(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Lift;
  in.hdgValid = true; in.hdg = 100; in.shift = 5;
  in.perfValid = true; in.perf = 90;
  in.vmgValid = true; in.vmg = 1.0f;
  in.sogValid = true; in.sog = 1.0f;
  uint8_t buf[REPEATER_LEN];
  encode(in, buf, sizeof(buf));
  buf[4] ^= 0x01; // korruptoi SHIFT
  RepeaterData d = decode(buf, sizeof(buf));
  TEST_ASSERT_FALSE(d.crcOk);
}

// ---- T7: vaara MAGIC hylataan ----
void test_wrong_magic(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Lift;
  in.hdgValid = true; in.hdg = 100; in.shift = 5;
  in.perfValid = true; in.perf = 90;
  in.vmgValid = true; in.vmg = 1.0f;
  in.sogValid = true; in.sog = 1.0f;
  uint8_t buf[REPEATER_LEN];
  encode(in, buf, sizeof(buf));
  buf[0] = 0x00;
  buf[9] = crc8(buf, 9); // korjaa CRC jotta vain MAGIC on vaarin
  RepeaterData d = decode(buf, sizeof(buf));
  TEST_ASSERT_FALSE(d.magicOk);
  TEST_ASSERT_FALSE(d.valid());
}

// ---- T8: PERF > 100 sallittu (ei cappia 100:aan) ----
void test_perf_over_100(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Lift;
  in.hdgValid = true; in.hdg = 0; in.shift = 0;
  in.perfValid = true; in.perf = 115;
  in.vmgValid = true; in.vmg = 0.0f;
  in.sogValid = true; in.sog = 0.0f;
  RepeaterData d = roundtrip(in);
  TEST_ASSERT_EQUAL_INT(115, d.perf);
}

// ---- T10: vaara pituus hylataan ----
void test_wrong_length(void) {
  uint8_t buf[5] = {0x7C, 0, 0, 0, 0};
  RepeaterData d = decode(buf, 5);
  TEST_ASSERT_FALSE(d.magicOk);
  TEST_ASSERT_FALSE(d.crcOk);
  TEST_ASSERT_FALSE(d.valid());
}

// ---- T11: kaikki neljä tilaa round-trippaavat ----
void test_all_states(void) {
  State states[] = { State::Groove, State::Lift, State::Header, State::Tack };
  for (int i = 0; i < 4; ++i) {
    RepeaterData in;
    in.meansValid = true; in.gpsValid = true; in.state = states[i];
    in.hdgValid = true; in.hdg = 90; in.shift = 2;
    in.perfValid = true; in.perf = 95;
    in.vmgValid = true; in.vmg = 0.5f;
    in.sogValid = true; in.sog = 1.0f;
    RepeaterData d = roundtrip(in);
    TEST_ASSERT_EQUAL_INT((int)states[i], (int)d.state);
    TEST_ASSERT_TRUE(d.valid());
  }
}

// ---- T12: CRC8 tunnetulla vektorilla (ristiinvarmennus sim:n kanssa) ----
// header-paketti hdg=335, shift=-17, perf=87, vmg=11, sog=20, seq=1
// Tavut lasketaan tassa; verrataan etta CRC on determininen ja MAGIC/rakenne oikein.
void test_known_vector_bytes(void) {
  RepeaterData in;
  in.meansValid = true; in.gpsValid = true; in.state = State::Header;
  in.hdgValid = true; in.hdg = 335; in.shift = -17;
  in.perfValid = true; in.perf = 87;
  in.vmgValid = true; in.vmg = 1.1f;
  in.sogValid = true; in.sog = 2.0f;
  in.seq = 1;
  uint8_t buf[REPEATER_LEN];
  encode(in, buf, sizeof(buf));
  // MAGIC
  TEST_ASSERT_EQUAL_HEX8(0x7C, buf[0]);
  // FLAGS: means(1)+gps(2)+header(2<<2=8) = 0x0B
  TEST_ASSERT_EQUAL_HEX8(0x0B, buf[1]);
  // HDG 335 = 0x014F LE -> 0x4F, 0x01
  TEST_ASSERT_EQUAL_HEX8(0x4F, buf[2]);
  TEST_ASSERT_EQUAL_HEX8(0x01, buf[3]);
  // SHIFT -17 = 0xEF
  TEST_ASSERT_EQUAL_HEX8(0xEF, buf[4]);
  // PERF 87
  TEST_ASSERT_EQUAL_HEX8(87, buf[5]);
  // VMG 11, SOG 20
  TEST_ASSERT_EQUAL_HEX8(11, buf[6]);
  TEST_ASSERT_EQUAL_HEX8(20, buf[7]);
  // SEQ 1
  TEST_ASSERT_EQUAL_HEX8(1, buf[8]);
  // CRC determininen: dekoodaus hyvaksyy
  RepeaterData d = decode(buf, sizeof(buf));
  TEST_ASSERT_TRUE(d.valid());
}

int main(int, char**) {
  UNITY_BEGIN();
  RUN_TEST(test_header_full);
  RUN_TEST(test_gps_off_sentinels);
  RUN_TEST(test_nomean_hdg_visible);
  RUN_TEST(test_hdg_null);
  RUN_TEST(test_shift_signs);
  RUN_TEST(test_crc_detects_corruption);
  RUN_TEST(test_wrong_magic);
  RUN_TEST(test_perf_over_100);
  RUN_TEST(test_wrong_length);
  RUN_TEST(test_all_states);
  RUN_TEST(test_known_vector_bytes);
  return UNITY_END();
}
