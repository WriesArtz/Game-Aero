package app.gamenative.cast

import java.util.LinkedHashMap
import kotlin.math.ceil

/**
 * Adapted for Game-Aero's video-only stream from the HLS segmenting approach in
 * ddagunts/ScreenCast (Apache-2.0). Keeps a short rolling window in memory.
 */
internal class AeroHlsSegmenter(
    private val targetDurationSeconds: Double = 2.0,
    private val windowSize: Int = 5,
) : AeroVideoEncoder.Sink {
    private data class Segment(val bytes: ByteArray, val durationSeconds: Double)

    private val lock = Any()
    private val muxer = AeroTsMuxer()
    private val segments = LinkedHashMap<Int, Segment>()
    private var nextSequence = 0
    private var currentStartUs = -1L
    private var currentEndUs = -1L
    private var ptsBaseUs = -1L
    @Volatile private var hasFormat = false

    override fun onFormat(sps: ByteArray, pps: ByteArray) {
        synchronized(lock) {
            muxer.setSpsPps(sps, pps)
            hasFormat = true
        }
    }

    override fun onSample(nalUnits: List<ByteArray>, ptsUs: Long, isKeyFrame: Boolean) {
        synchronized(lock) {
            if (!hasFormat || nalUnits.isEmpty()) return
            if (ptsBaseUs < 0) ptsBaseUs = ptsUs
            val pts = (ptsUs - ptsBaseUs).coerceAtLeast(0L)

            if (isKeyFrame && currentStartUs >= 0 &&
                (pts - currentStartUs) / 1_000_000.0 >= targetDurationSeconds
            ) {
                flushSegmentLocked()
            }

            if (currentStartUs < 0) {
                if (!isKeyFrame) return
                currentStartUs = pts
                muxer.writeKeyFrame(nalUnits, pts)
            } else {
                muxer.writeFrame(nalUnits, pts, isKeyFrame)
            }
            currentEndUs = pts
        }
    }

    private fun flushSegmentLocked() {
        val bytes = muxer.drain()
        val duration = ((currentEndUs - currentStartUs).coerceAtLeast(1L)) / 1_000_000.0
        currentStartUs = -1L
        if (bytes.isEmpty()) return
        segments[nextSequence++] = Segment(bytes, duration)
        while (segments.size > windowSize * 2) {
            val oldest = segments.keys.firstOrNull() ?: break
            segments.remove(oldest)
        }
        (lock as java.lang.Object).notifyAll()
    }

    fun awaitFirstSegment(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        synchronized(lock) {
            while (segments.isEmpty()) {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0L) return false
                (lock as java.lang.Object).wait(remaining.coerceAtMost(250L))
            }
            return true
        }
    }

    fun segment(sequence: Int): ByteArray? = synchronized(lock) { segments[sequence]?.bytes }

    fun playlist(): String = synchronized(lock) {
        val visible = segments.entries.toList().takeLast(windowSize)
        val maxDuration = visible.maxOfOrNull { it.value.durationSeconds } ?: targetDurationSeconds
        val mediaSequence = visible.firstOrNull()?.key ?: nextSequence
        buildString {
            append("#EXTM3U\n")
            append("#EXT-X-VERSION:3\n")
            append("#EXT-X-TARGETDURATION:${ceil(maxDuration).toInt().coerceAtLeast(1)}\n")
            append("#EXT-X-MEDIA-SEQUENCE:$mediaSequence\n")
            visible.forEach { (sequence, segment) ->
                append("#EXTINF:${"%.3f".format(java.util.Locale.US, segment.durationSeconds)},\n")
                append("seg-$sequence.ts\n")
            }
        }
    }
}
