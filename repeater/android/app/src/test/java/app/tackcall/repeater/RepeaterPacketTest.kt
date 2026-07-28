package app.tackcall.repeater

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM-yksikkötestit RepeaterPacket.kt:lle. Ei Android-riippuvuuksia, ajettavissa
 * heti ilman laitetta (Android-kuoren valmistusohje v4, §9 askel 1).
 *
 * Testivektorit ovat suoraan valmistusohjeen §4.2:sta ja on varmennettu erikseen
 * bitilleen (CRC8 + koko tavukartta) Python-referenssitoteutuksella ennen tätä
 * tiedostoa. Sama data täytyy täsmätä ESP32-firmwaren host-testien kanssa.
 */
class RepeaterPacketTest {

    private fun hexBytes(hex: String): ByteArray =
        hex.trim().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `header taysi - means plus gps plus header, hdg335, shift-17`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = true,
            state = RepeaterState.HEADER,
            heading = 335,
            shift = -17,
            perf = 87,
            vmg = 1.1,
            sog = 2.0,
            seq = 1,
        )
        assertArrayEquals(hexBytes("7C 0B 4F 01 EF 57 0B 14 01 0C"), packet.toBytes())
    }

    @Test
    fun `lift plus GPS - hdg10, shift plus6`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = true,
            state = RepeaterState.LIFT,
            heading = 10,
            shift = 6,
            perf = 95,
            vmg = 3.2,
            sog = 4.1,
            seq = 2,
        )
        assertArrayEquals(hexBytes("7C 07 0A 00 06 5F 20 29 02 2C"), packet.toBytes())
    }

    @Test
    fun `tack - hdg270, shift plus33`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = true,
            state = RepeaterState.TACK,
            heading = 270,
            shift = 33,
            perf = 80,
            vmg = 2.0,
            sog = 5.5,
            seq = 3,
        )
        assertArrayEquals(hexBytes("7C 0F 0E 01 21 50 14 37 03 D8"), packet.toBytes())
    }

    @Test
    fun `groove - hdg90, shift plus2`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = true,
            state = RepeaterState.GROOVE,
            heading = 90,
            shift = 2,
            perf = 99,
            vmg = 0.5,
            sog = 1.0,
            seq = 4,
        )
        assertArrayEquals(hexBytes("7C 03 5A 00 02 63 05 0A 04 A8"), packet.toBytes())
    }

    @Test
    fun `nomean - means pois, gps paalla, hdg200`() {
        val packet = RepeaterPacket(
            meansValid = false,
            gpsValid = true,
            state = RepeaterState.GROOVE,
            heading = 200,
            shift = 0,
            perf = 50,
            vmg = 3.3,
            sog = 4.4,
            seq = 5,
        )
        assertArrayEquals(hexBytes("7C 02 C8 00 00 32 21 2C 05 16"), packet.toBytes())
    }

    @Test
    fun `GPS pois - means paalla, gps pois, perf vmg sog ei dataa`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = false,
            state = RepeaterState.LIFT,
            heading = 50,
            shift = 8,
            perf = null,
            vmg = null,
            sog = null,
            seq = 6,
        )
        assertArrayEquals(hexBytes("7C 05 32 00 08 FF FF FF 06 91"), packet.toBytes())
    }

    // --- Reunatapaukset, ei suoraan valmistusohjeen testivektoreista ---

    @Test
    fun `heading null koodataan 0xFFFF sentinelliksi`() {
        val packet = RepeaterPacket(
            meansValid = true,
            gpsValid = false,
            state = RepeaterState.GROOVE,
            heading = null,
            shift = 0,
            perf = null,
            vmg = null,
            sog = null,
            seq = 0,
        )
        val bytes = packet.toBytes()
        assertEquals(0xFF, bytes[2].toInt() and 0xFF)
        assertEquals(0xFF, bytes[3].toInt() and 0xFF)
    }

    @Test
    fun `shift kyllastyy plus-miinus 90 asteeseen`() {
        val over = RepeaterPacket(
            meansValid = true, gpsValid = true, state = RepeaterState.GROOVE,
            heading = 0, shift = 150, perf = null, vmg = null, sog = null, seq = 0,
        ).toBytes()
        val under = RepeaterPacket(
            meansValid = true, gpsValid = true, state = RepeaterState.GROOVE,
            heading = 0, shift = -150, perf = null, vmg = null, sog = null, seq = 0,
        ).toBytes()
        assertEquals(90, over[4].toInt())
        assertEquals(-90, under[4].toInt())
    }

    @Test
    fun `paketti on aina 10 tavua pituudeltaan`() {
        val packet = RepeaterPacket(
            meansValid = false, gpsValid = false, state = RepeaterState.TACK,
            heading = null, shift = 0, perf = null, vmg = null, sog = null, seq = 255,
        )
        assertEquals(10, packet.toBytes().size)
    }

    @Test
    fun `magic-tavu on aina 0x7C`() {
        val packet = RepeaterPacket(
            meansValid = true, gpsValid = true, state = RepeaterState.LIFT,
            heading = 0, shift = 0, perf = 0, vmg = 0.0, sog = 0.0, seq = 0,
        )
        assertEquals(0x7C, packet.toBytes()[0].toInt() and 0xFF)
    }
}
