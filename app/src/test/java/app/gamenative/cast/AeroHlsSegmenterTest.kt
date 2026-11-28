package app.gamenative.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AeroHlsSegmenterTest {
    @Test
    fun emitsMpegTransportStreamWithProgramTablesAndH264Video() {
        val segmenter = newSegmenterWithTwoSegments()
        val firstSegment = segmenter.segment(0)

        assertNotNull(firstSegment)
        val bytes = requireNotNull(firstSegment)
        assertTrue(bytes.isNotEmpty())
        assertEquals("MPEG-TS packets must be 188 bytes", 0, bytes.size % 188)

        val packets = bytes.asList().chunked(188)
        assertTrue(packets.size >= 3)
        assertTrue(packets.all { it.size == 188 && (it[0].toInt() and 0xFF) == 0x47 })
        assertEquals(0x0000, pid(packets[0]))
        assertEquals(0x1000, pid(packets[1]))
        assertEquals(0x0100, pid(packets[2]))
    }

    @Test
    fun publishesRollingLivePlaylistWithRelativeSegmentPaths() {
        val segmenter = newSegmenterWithTwoSegments()
        val playlist = segmenter.playlist()

        assertTrue(playlist.startsWith("#EXTM3U\n"))
        assertTrue(playlist.contains("#EXT-X-MEDIA-SEQUENCE:0"))
        assertTrue(playlist.contains("#EXTINF:"))
        assertTrue(playlist.contains("seg-0.ts"))
        assertFalse(playlist.contains("#EXT-X-ENDLIST"))
    }

    @Test
    fun segmentDurationRunsUntilTheNextKeyFrame() {
        val playlist = newSegmenterWithTwoSegments().playlist()

        assertTrue(playlist.contains("#EXTINF:2.100,"))
    }

    private fun newSegmenterWithTwoSegments(): AeroHlsSegmenter {
        val segmenter = AeroHlsSegmenter(targetDurationSeconds = 2.0, windowSize = 5)
        segmenter.onFormat(
            sps = byteArrayOf(0x67, 0x42, 0x00, 0x1F),
            pps = byteArrayOf(0x68, 0xCE.toByte(), 0x06, 0xE2.toByte()),
        )
        val keyFrame = byteArrayOf(0x65, 0x88.toByte())
        val deltaFrame = byteArrayOf(0x41, 0x9A.toByte())

        segmenter.onSample(listOf(keyFrame), 0L, true)
        segmenter.onSample(listOf(deltaFrame), 1_000_000L, false)
        segmenter.onSample(listOf(keyFrame), 2_100_000L, true)
        segmenter.onSample(listOf(deltaFrame), 3_100_000L, false)
        segmenter.onSample(listOf(keyFrame), 4_200_000L, true)
        return segmenter
    }

    private fun pid(packet: List<Byte>): Int =
        ((packet[1].toInt() and 0x1F) shl 8) or (packet[2].toInt() and 0xFF)
}
