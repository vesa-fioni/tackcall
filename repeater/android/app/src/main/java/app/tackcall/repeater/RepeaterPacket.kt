package app.tackcall.repeater

/**
 * Tackcall Repeater -BLE-pakettiprotokolla (Android-kuoren valmistusohje v4, §4.2).
 *
 * Kiinteä 10 tavua, little-endian, CRC8 Dallas/Maxim (poly 0x8C heijastettu) tavujen
 * 0..8 päällä. Puhdas Kotlin-objekti; ei Android-riippuvuuksia, ei BLE-kutsuja —
 * tämä luokka osaa vain muuntaa itsensä tavuiksi. BLE-lähetys on eri kerroksen vastuu.
 *
 * ESP32-firmware on protokollan toinen pää (host-testattu, 18 testiä PlatformIO:lla).
 * Tämä tiedosto ei muuta protokollaa millään tavalla — vain peilaa sen.
 */
data class RepeaterPacket(
    val meansValid: Boolean,
    val gpsValid: Boolean,
    val state: RepeaterState,
    /** Aste 0..359, tai null = ei dataa (koodataan 0xFFFF:ksi). */
    val heading: Int?,
    /** Aste, + = lift / − = header. Kyllästetään ±90:een (protokollan mukaista, ei virhe). */
    val shift: Int,
    /** 0..100 tyypillisesti, tai null = ei dataa (koodataan 0xFF:ksi). */
    val perf: Int?,
    /** Solmua, tai null = ei dataa (koodataan 0xFF:ksi). */
    val vmg: Double?,
    /** Solmua, tai null = ei dataa (koodataan 0xFF:ksi). */
    val sog: Double?,
    /** 0..255, kiertyy ympäri kutsujan vastuulla. */
    val seq: Int,
) {

    /**
     * Pakkaa paketin 10 tavuksi. Viimeinen tavu on CRC8 tavujen 0..8 päältä.
     */
    fun toBytes(): ByteArray {
        val out = ByteArray(PACKET_SIZE)

        out[0] = MAGIC.toByte()
        out[1] = encodeFlags()

        val hdgRaw = encodeHeading(heading)
        out[2] = (hdgRaw and 0xFF).toByte()
        out[3] = ((hdgRaw ushr 8) and 0xFF).toByte()

        out[4] = shift.coerceIn(-SHIFT_SATURATION, SHIFT_SATURATION).toByte()

        out[5] = encodeByteOrNoData(perf)
        out[6] = encodeTenthsOrNoData(vmg)
        out[7] = encodeTenthsOrNoData(sog)

        out[8] = (seq and 0xFF).toByte()
        out[9] = crc8(out, 0, 9)

        return out
    }

    private fun encodeFlags(): Byte {
        var flags = 0
        if (meansValid) flags = flags or FLAG_MEANS_VALID
        if (gpsValid) flags = flags or FLAG_GPS_VALID
        flags = flags or (state.code shl STATE_FIELD_SHIFT)
        return flags.toByte()
    }

    companion object {
        const val MAGIC: Int = 0x7C
        const val PACKET_SIZE: Int = 10
        const val NO_DATA_U8: Int = 0xFF
        const val NO_DATA_U16: Int = 0xFFFF
        const val SHIFT_SATURATION: Int = 90

        /**
         * Ylin sallittu koodattava arvo perf/vmg/sog-tavuille. 0xFF (255) on varattu
         * "ei dataa" -sentinelliksi, joten oikeat arvot kyllästetään 254:ään asti ettei
         * kelvollinen lukema koskaan sekoitu ei-dataan. Tämä on saman kyllästysperiaatteen
         * laajennus kuin shiftin ±90 (protokolla ei erikseen mainitse tätä — ks. CHANGELOG/
         * kommentti kutsupaikassa, avoinna vahvistettavaksi).
         */
        private const val MAX_ENCODABLE_U8: Int = 254

        private const val FLAG_MEANS_VALID: Int = 0x01
        private const val FLAG_GPS_VALID: Int = 0x02
        private const val STATE_FIELD_SHIFT: Int = 2

        private fun encodeHeading(heading: Int?): Int {
            if (heading == null) return NO_DATA_U16
            // Normalisoi 0..359:ään puolustavasti; ylävirran pitäisi jo antaa norm(heading).
            val normalized = ((heading % 360) + 360) % 360
            return normalized
        }

        private fun encodeByteOrNoData(value: Int?): Byte {
            if (value == null) return NO_DATA_U8.toByte()
            return value.coerceIn(0, MAX_ENCODABLE_U8).toByte()
        }

        private fun encodeTenthsOrNoData(value: Double?): Byte {
            if (value == null) return NO_DATA_U8.toByte()
            val tenths = Math.round(value * 10.0).toInt()
            return tenths.coerceIn(0, MAX_ENCODABLE_U8).toByte()
        }

        /**
         * CRC8 Dallas/Maxim, heijastettu polynomi 0x8C. [data] tavuväli
         * [offset, offset+length) lasketaan mukaan.
         */
        internal fun crc8(data: ByteArray, offset: Int, length: Int): Byte {
            var crc = 0
            for (i in offset until offset + length) {
                crc = crc xor (data[i].toInt() and 0xFF)
                repeat(8) {
                    crc = if (crc and 1 != 0) {
                        (crc ushr 1) xor 0x8C
                    } else {
                        crc ushr 1
                    }
                }
            }
            return (crc and 0xFF).toByte()
        }
    }
}

/**
 * Pakettitilan enum, koodattuna FLAGS-tavun bittien 2-3 arvoksi (§4.2).
 */
enum class RepeaterState(val code: Int) {
    GROOVE(0),
    LIFT(1),
    HEADER(2),
    TACK(3),
}
