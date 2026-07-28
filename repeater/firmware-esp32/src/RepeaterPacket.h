// RepeaterPacket.h
// Tackcall repeater — BLE-paketin jasennin (valmistusohje §4).
//
// PUHDAS C++: ei Arduino- eika laitteistoriippuvuuksia, jotta tama kaantyy
// ja testataan host-koneella (pio test -e native) ilman ESP32:ta.
//
// Protokolla (kiintea 10 tavua, little-endian) — identtinen selainsimulaattorin
// ja Android-kuoren kanssa. Tavut TAYTYY tasmata bitilleen kaikilla kolmella.
//
//   [0] MAGIC  = 0x7C
//   [1] FLAGS  : bitti0=means_valid, bitti1=gps_valid,
//                bitit2-3 = tila-enum (0=groove,1=lift,2=header,3=tack),
//                bitit4-7 varattu
//   [2-3] HDG  uint16 LE, 0xFFFF = ei dataa
//   [4] SHIFT  int8, +lift / -header, kyllastys ±90
//   [5] PERF   uint8, 0xFF = ei dataa
//   [6] VMG    uint8, solmut ×10, 0xFF = ei dataa
//   [7] SOG    uint8, solmut ×10, 0xFF = ei dataa
//   [8] SEQ    uint8
//   [9] CRC8   Dallas/Maxim (poly 0x8C heijastettu), tavut 0..8

#ifndef REPEATER_PACKET_H
#define REPEATER_PACKET_H

#include <stdint.h>
#include <stddef.h>

namespace tackcall {

static const uint8_t REPEATER_MAGIC = 0x7C;
static const size_t  REPEATER_LEN   = 10;

// Tila-enum (FLAGS bitit 2-3). Sama jarjestys kuin sim/Android.
enum class State : uint8_t {
  Groove = 0,
  Lift   = 1,
  Header = 2,
  Tack   = 3
};

// Dekoodattu paketti. null-arvot ilmaistaan *_valid = false -lipuilla,
// jotta nayton on helppo piirtaa "--" puuttuvalle datalle (§4.3).
struct RepeaterData {
  bool  magicOk   = false;   // MAGIC-tavu oikein?
  bool  crcOk     = false;   // CRC8 tasmaa?
  bool  meansValid= false;   // FLAGS bitti0
  bool  gpsValid  = false;   // FLAGS bitti1
  State state     = State::Groove;

  bool  hdgValid  = false;   // false jos 0xFFFF
  int   hdg       = 0;       // asteet 0..359

  int   shift     = 0;       // asteet, etumerkillinen (-90..90)

  bool  perfValid = false;   // false jos 0xFF
  int   perf      = 0;       // prosenttia

  bool  vmgValid  = false;   // false jos 0xFF
  float vmg       = 0.0f;    // solmut

  bool  sogValid  = false;   // false jos 0xFF
  float sog       = 0.0f;    // solmut

  uint8_t seq     = 0;

  // Onko paketti kokonaisuudessaan luotettava (MAGIC + CRC)?
  bool valid() const { return magicOk && crcOk; }
};

// CRC-8 (Dallas/Maxim), poly 0x31 heijastettuna = 0x8C. Sama kuin sim/Android.
uint8_t crc8(const uint8_t* data, size_t len);

// Dekoodaa 10 tavua. Palauttaa RepeaterData; tarkista .valid() ennen kayttoa.
// Jos len != 10, palautetaan magicOk=false, crcOk=false.
RepeaterData decode(const uint8_t* bytes, size_t len);

// Enkoodaa RepeaterData 10 tavuun (paaosin testeja ja host-varmennusta varten;
// laite itse vain dekoodaa). Kirjoittaa REPEATER_LEN tavua out-puskuriin.
// *_valid=false -> vastaava sentineli (0xFF / 0xFFFF). Palauttaa kirjoitetut tavut.
size_t encode(const RepeaterData& d, uint8_t* out, size_t outCap);

} // namespace tackcall

#endif // REPEATER_PACKET_H
