// RepeaterPacket.cpp — ks. RepeaterPacket.h
// Peilaa selainsimulaattorin (tackcall-repeater-sim.html) encode/decode/crc8-
// logiikkaa bitilleen. Muutokset tanne on tehtava synkassa sim/Android-puolen kanssa.

#include "RepeaterPacket.h"

namespace tackcall {

uint8_t crc8(const uint8_t* data, size_t len) {
  uint8_t crc = 0;
  for (size_t i = 0; i < len; ++i) {
    crc ^= data[i];
    for (int b = 0; b < 8; ++b) {
      crc = (crc & 1) ? static_cast<uint8_t>((crc >> 1) ^ 0x8C)
                      : static_cast<uint8_t>(crc >> 1);
    }
  }
  return crc;
}

// Kyllastys apuri
static int clampInt(int v, int lo, int hi) {
  if (v < lo) return lo;
  if (v > hi) return hi;
  return v;
}

RepeaterData decode(const uint8_t* bytes, size_t len) {
  RepeaterData d;
  if (bytes == nullptr || len != REPEATER_LEN) {
    return d; // magicOk=false, crcOk=false
  }

  d.magicOk = (bytes[0] == REPEATER_MAGIC);
  d.crcOk   = (crc8(bytes, 9) == bytes[9]);

  const uint8_t flags = bytes[1];
  d.meansValid = (flags & 0x01) != 0;
  d.gpsValid   = (flags & 0x02) != 0;
  d.state      = static_cast<State>((flags >> 2) & 0x03);

  const uint16_t hdgRaw = static_cast<uint16_t>(bytes[2] | (bytes[3] << 8));
  if (hdgRaw == 0xFFFF) {
    d.hdgValid = false;
    d.hdg = 0;
  } else {
    d.hdgValid = true;
    d.hdg = static_cast<int>(hdgRaw);
  }

  // SHIFT int8 (kahden komplementti)
  d.shift = static_cast<int>(static_cast<int8_t>(bytes[4]));

  if (bytes[5] == 0xFF) { d.perfValid = false; d.perf = 0; }
  else                  { d.perfValid = true;  d.perf = static_cast<int>(bytes[5]); }

  if (bytes[6] == 0xFF) { d.vmgValid = false; d.vmg = 0.0f; }
  else                  { d.vmgValid = true;  d.vmg = bytes[6] / 10.0f; }

  if (bytes[7] == 0xFF) { d.sogValid = false; d.sog = 0.0f; }
  else                  { d.sogValid = true;  d.sog = bytes[7] / 10.0f; }

  d.seq = bytes[8];
  return d;
}

size_t encode(const RepeaterData& d, uint8_t* out, size_t outCap) {
  if (out == nullptr || outCap < REPEATER_LEN) return 0;

  out[0] = REPEATER_MAGIC;

  uint8_t flags = 0;
  if (d.meansValid) flags |= 0x01;
  if (d.gpsValid)   flags |= 0x02;
  flags |= static_cast<uint8_t>((static_cast<uint8_t>(d.state) & 0x03) << 2);
  out[1] = flags;

  if (!d.hdgValid) { out[2] = 0xFF; out[3] = 0xFF; }
  else {
    uint16_t h = static_cast<uint16_t>(d.hdg & 0xFFFF);
    out[2] = static_cast<uint8_t>(h & 0xFF);
    out[3] = static_cast<uint8_t>((h >> 8) & 0xFF);
  }

  int sh = clampInt(d.shift, -90, 90);
  out[4] = static_cast<uint8_t>(static_cast<int8_t>(sh));

  out[5] = d.perfValid ? static_cast<uint8_t>(clampInt(d.perf, 0, 254)) : 0xFF;
  out[6] = d.vmgValid ? static_cast<uint8_t>(clampInt(static_cast<int>(d.vmg * 10.0f + 0.5f), 0, 254)) : 0xFF;
  out[7] = d.sogValid ? static_cast<uint8_t>(clampInt(static_cast<int>(d.sog * 10.0f + 0.5f), 0, 254)) : 0xFF;

  out[8] = d.seq;
  out[9] = crc8(out, 9);
  return REPEATER_LEN;
}

} // namespace tackcall
