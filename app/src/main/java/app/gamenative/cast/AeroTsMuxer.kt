package app.gamenative.cast

import java.io.ByteArrayOutputStream

/**
 * Small video-only MPEG-TS muxer for H.264 Annex-B samples.
 * PSI tables and a PCR are emitted at each IDR so every HLS segment can start independently.
 */
internal class AeroTsMuxer {
    private val output = ByteArrayOutputStream(64 * 1024)
    private var videoContinuity = 0
    private var pmtContinuity = 0
    private var patContinuity = 0
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null

    fun setSpsPps(sps: ByteArray, pps: ByteArray) {
        this.sps = sps
        this.pps = pps
    }

    fun writeKeyFrame(nals: List<ByteArray>, ptsUs: Long) {
        writePat()
        writePmt()
        writeVideoPes(withParameterSets(nals), ptsUs, randomAccess = true)
    }

    fun writeFrame(nals: List<ByteArray>, ptsUs: Long, isKeyFrame: Boolean) {
        if (isKeyFrame) {
            writePat()
            writePmt()
        }
        writeVideoPes(if (isKeyFrame) withParameterSets(nals) else withAud(nals), ptsUs, randomAccess = isKeyFrame)
    }

    fun drain(): ByteArray {
        val result = output.toByteArray()
        output.reset()
        return result
    }

    private fun withParameterSets(nals: List<ByteArray>): List<ByteArray> = buildList {
        add(AUD)
        sps?.let(::add)
        pps?.let(::add)
        nals.filterNot { isAud(it) || isParameterSet(it) }.forEach(::add)
    }

    private fun withAud(nals: List<ByteArray>): List<ByteArray> = buildList {
        add(AUD)
        nals.filterNot(::isAud).forEach(::add)
    }

    private fun isAud(nal: ByteArray) = nal.isNotEmpty() && (nal[0].toInt() and 0x1F) == 9

    private fun isParameterSet(nal: ByteArray): Boolean {
        if (nal.isEmpty()) return false
        val type = nal[0].toInt() and 0x1F
        return type == 7 || type == 8
    }

    private fun writePat() {
        val section = ByteArrayOutputStream().apply {
            write(0x00)
            writeShort(0xB00D)
            writeShort(0x0001)
            write(0xC1)
            write(0x00)
            write(0x00)
            writeShort(0x0001)
            writeShort(0xE000 or PMT_PID)
        }.toByteArray()
        writeSection(PAT_PID, appendCrc(section), patContinuity)
        patContinuity = (patContinuity + 1) and 0x0F
    }

    private fun writePmt() {
        val section = ByteArrayOutputStream().apply {
            write(0x02)
            writeShort(0xB012) // section_length = 18: fixed body + one H.264 stream + CRC
            writeShort(0x0001)
            write(0xC1)
            write(0x00)
            write(0x00)
            writeShort(0xE000 or VIDEO_PID)
            writeShort(0xF000)
            write(0x1B) // H.264
            writeShort(0xE000 or VIDEO_PID)
            writeShort(0xF000)
        }.toByteArray()
        writeSection(PMT_PID, appendCrc(section), pmtContinuity)
        pmtContinuity = (pmtContinuity + 1) and 0x0F
    }

    private fun writeSection(pid: Int, section: ByteArray, continuity: Int) {
        val packet = ByteArray(188) { 0xFF.toByte() }
        packet[0] = 0x47
        packet[1] = (0x40 or ((pid shr 8) and 0x1F)).toByte()
        packet[2] = (pid and 0xFF).toByte()
        packet[3] = (0x10 or continuity).toByte()
        packet[4] = 0x00
        System.arraycopy(section, 0, packet, 5, minOf(section.size, 183))
        output.write(packet)
    }

    private fun writeVideoPes(nals: List<ByteArray>, ptsUs: Long, randomAccess: Boolean) {
        val elementary = ByteArrayOutputStream()
        nals.forEach { nal ->
            elementary.write(0x00)
            elementary.write(0x00)
            elementary.write(0x00)
            elementary.write(0x01)
            elementary.write(nal)
        }

        val pts90k = (ptsUs * 9L) / 100L
        val pes = ByteArrayOutputStream()
        pes.write(0x00); pes.write(0x00); pes.write(0x01)
        pes.write(0xE0)
        pes.write(0x00); pes.write(0x00) // unbounded video PES
        pes.write(0x80); pes.write(0x80); pes.write(0x05)
        writePts(pes, pts90k)
        pes.write(elementary.toByteArray())
        writePesPackets(pes.toByteArray(), pts90k, randomAccess)
    }

    private fun writePesPackets(pes: ByteArray, pts90k: Long, randomAccess: Boolean) {
        var offset = 0
        var first = true
        while (offset < pes.size) {
            val remaining = pes.size - offset
            val needsPcr = first
            val minimumAdaptation = if (needsPcr) 7 else 0
            val take = minOf(remaining, if (needsPcr) 176 else 184)
            val adaptationLength = if (needsPcr || take < 184) 183 - take else 0
            val hasAdaptation = needsPcr || take < 184

            val packet = ByteArray(188) { 0xFF.toByte() }
            packet[0] = 0x47
            packet[1] = ((if (first) 0x40 else 0x00) or ((VIDEO_PID shr 8) and 0x1F)).toByte()
            packet[2] = (VIDEO_PID and 0xFF).toByte()
            packet[3] = ((if (hasAdaptation) 0x30 else 0x10) or videoContinuity).toByte()
            videoContinuity = (videoContinuity + 1) and 0x0F

            var payloadOffset = 4
            if (hasAdaptation) {
                packet[payloadOffset++] = adaptationLength.toByte()
                if (adaptationLength > 0) {
                    packet[payloadOffset++] = ((if (randomAccess && first) 0x40 else 0x00) or
                        (if (needsPcr) 0x10 else 0x00)).toByte()
                    if (needsPcr) {
                        val pcr = encodePcr(pts90k)
                        System.arraycopy(pcr, 0, packet, payloadOffset, pcr.size)
                        payloadOffset += pcr.size
                    }
                    while (payloadOffset < 5 + adaptationLength) packet[payloadOffset++] = 0xFF.toByte()
                }
            }
            val payloadSpace = 188 - payloadOffset
            val bytesToCopy = minOf(payloadSpace, remaining)
            System.arraycopy(pes, offset, packet, payloadOffset, bytesToCopy)
            offset += bytesToCopy
            output.write(packet)
            first = false
        }
    }

    private fun encodePcr(pts90k: Long): ByteArray {
        val base = pts90k and 0x1FFFFFFFFL
        return byteArrayOf(
            (base shr 25).toByte(),
            (base shr 17).toByte(),
            (base shr 9).toByte(),
            (base shr 1).toByte(),
            ((((base and 1L).toInt() shl 7) or 0x7E)).toByte(),
            0x00,
        )
    }

    private fun writePts(out: ByteArrayOutputStream, pts90k: Long) {
        val p = pts90k and 0x1FFFFFFFFL
        out.write(0x21 or (((p shr 30).toInt() and 0x07) shl 1))
        out.write((p shr 22).toInt() and 0xFF)
        out.write((((p shr 15).toInt() and 0x7F) shl 1) or 0x01)
        out.write((p shr 7).toInt() and 0xFF)
        out.write(((p.toInt() and 0x7F) shl 1) or 0x01)
    }

    private fun appendCrc(section: ByteArray): ByteArray {
        val crc = mpegCrc32(section)
        return section + byteArrayOf(
            (crc shr 24).toByte(),
            (crc shr 16).toByte(),
            (crc shr 8).toByte(),
            crc.toByte(),
        )
    }

    private fun mpegCrc32(bytes: ByteArray): Int {
        var crc = -1
        for (byte in bytes) {
            val index = ((crc ushr 24) xor (byte.toInt() and 0xFF)) and 0xFF
            crc = (crc shl 8) xor CRC_TABLE[index]
        }
        return crc
    }

    private fun ByteArrayOutputStream.writeShort(value: Int) {
        write((value shr 8) and 0xFF)
        write(value and 0xFF)
    }

    companion object {
        private const val PAT_PID = 0x0000
        private const val PMT_PID = 0x1000
        private const val VIDEO_PID = 0x0100
        private val AUD = byteArrayOf(0x09, 0xF0.toByte())
        private val CRC_TABLE = IntArray(256).also { table ->
            val polynomial = 0x04C11DB7
            for (i in 0 until 256) {
                var crc = i shl 24
                repeat(8) {
                    crc = if ((crc and Int.MIN_VALUE) != 0) (crc shl 1) xor polynomial else crc shl 1
                }
                table[i] = crc
            }
        }
    }
}
