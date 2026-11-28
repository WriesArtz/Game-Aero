package app.gamenative.cast

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import timber.log.Timber

/** H.264 encoder that exposes a Surface for MediaProjection's virtual display. */
internal class AeroVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val bitrate: Int,
    private val fps: Int = 30,
    private val keyFrameIntervalSeconds: Int = 2,
) {
    interface Sink {
        fun onFormat(sps: ByteArray, pps: ByteArray)
        fun onSample(nalUnits: List<ByteArray>, ptsUs: Long, isKeyFrame: Boolean)
    }

    lateinit var inputSurface: Surface
        private set

    private var codec: MediaCodec? = null
    @Volatile private var running = false
    private var drainThread: Thread? = null
    private var keyFrameThread: Thread? = null

    fun start(sink: Sink) {
        check(!running) { "Encoder is already running" }
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameIntervalSeconds)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
            setInteger(
                MediaFormat.KEY_LEVEL,
                when {
                    width >= 1920 && height >= 1080 -> MediaCodecInfo.CodecProfileLevel.AVCLevel4
                    else -> MediaCodecInfo.CodecProfileLevel.AVCLevel31
                },
            )
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1_000_000L / fps)
        }

        val created = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        try {
            created.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = created.createInputSurface()
            created.start()
            codec = created
            running = true
            drainThread = thread(name = "aero-h264-drain", isDaemon = true) { drainOutput(sink) }
            keyFrameThread = thread(name = "aero-h264-keyframes", isDaemon = true) {
                while (running) {
                    try {
                        Thread.sleep(keyFrameIntervalSeconds * 1000L)
                    } catch (_: InterruptedException) {
                        break
                    }
                    if (running) {
                        runCatching {
                            codec?.setParameters(Bundle().apply {
                                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                            })
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            runCatching { if (::inputSurface.isInitialized) inputSurface.release() }
            runCatching { created.release() }
            throw error
        }
    }

    private fun drainOutput(sink: Sink) {
        val activeCodec = codec ?: return
        val info = MediaCodec.BufferInfo()
        while (running) {
            val index = try {
                activeCodec.dequeueOutputBuffer(info, 10_000)
            } catch (error: Throwable) {
                if (running) Timber.w(error, "Aero screen encoder stopped while draining")
                return
            }
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = activeCodec.outputFormat
                    val sps = outputFormat.getByteBuffer("csd-0")?.toByteArray()?.stripStartCode()
                    val pps = outputFormat.getByteBuffer("csd-1")?.toByteArray()?.stripStartCode()
                    if (sps != null && pps != null) sink.onFormat(sps, pps)
                    else Timber.w("H.264 encoder did not expose SPS/PPS in csd-0/csd-1")
                }
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                index >= 0 -> {
                    try {
                        val buffer = activeCodec.getOutputBuffer(index)
                        if (buffer != null && info.size > 0 &&
                            (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val sample = ByteArray(info.size)
                            buffer.get(sample)
                            val nals = splitAnnexB(sample)
                            if (nals.isNotEmpty()) {
                                val isKey = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                                sink.onSample(nals, info.presentationTimeUs, isKey)
                            }
                        }
                    } catch (error: Throwable) {
                        Timber.w(error, "Unable to process an H.264 output sample")
                    } finally {
                        activeCodec.releaseOutputBuffer(index, false)
                    }
                }
            }
        }
    }

    fun stop() {
        if (!running && codec == null) return
        running = false
        keyFrameThread?.interrupt()
        runCatching { drainThread?.join(700) }
        runCatching { keyFrameThread?.join(300) }
        codec?.let { active ->
            runCatching { active.signalEndOfInputStream() }
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        codec = null
        runCatching { if (::inputSurface.isInitialized) inputSurface.release() }
        drainThread = null
        keyFrameThread = null
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val duplicate = duplicate()
        return ByteArray(duplicate.remaining()).also(duplicate::get)
    }

    private fun ByteArray.stripStartCode(): ByteArray = when {
        size >= 4 && this[0] == 0.toByte() && this[1] == 0.toByte() && this[2] == 0.toByte() && this[3] == 1.toByte() ->
            copyOfRange(4, size)
        size >= 3 && this[0] == 0.toByte() && this[1] == 0.toByte() && this[2] == 1.toByte() ->
            copyOfRange(3, size)
        else -> this
    }

    private fun splitAnnexB(bytes: ByteArray): List<ByteArray> {
        val starts = ArrayList<Pair<Int, Int>>()
        var index = 0
        while (index + 3 <= bytes.size) {
            val four = index + 4 <= bytes.size &&
                bytes[index] == 0.toByte() && bytes[index + 1] == 0.toByte() &&
                bytes[index + 2] == 0.toByte() && bytes[index + 3] == 1.toByte()
            val three = bytes[index] == 0.toByte() && bytes[index + 1] == 0.toByte() &&
                bytes[index + 2] == 1.toByte()
            when {
                four -> { starts += index to 4; index += 4 }
                three -> { starts += index to 3; index += 3 }
                else -> index++
            }
        }
        if (starts.isEmpty()) return listOf(bytes).filter { it.isNotEmpty() }
        return starts.mapIndexedNotNull { i, (start, prefix) ->
            val begin = start + prefix
            val end = if (i + 1 < starts.size) starts[i + 1].first else bytes.size
            if (begin < end) bytes.copyOfRange(begin, end) else null
        }
    }
}
