package app.gamenative.cast

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import kotlin.concurrent.thread
import timber.log.Timber

/**
 * Captures Game-Aero's own playback (menu music, game audio) through the user-approved projection,
 * encodes it as AAC-LC 48 kHz stereo and hands the raw AAC frames to [Sink].
 * Only this app's audio is captured, never other apps'.
 */
internal class AeroAudioEncoder(private val bitrate: Int = 128_000) {
    interface Sink {
        fun onAudio(aacFrame: ByteArray, ptsUs: Long)
    }

    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var feedThread: Thread? = null
    private var drainThread: Thread? = null

    /** Returns false when the device cannot capture playback; the stream then simply stays video-only. */
    @SuppressLint("MissingPermission")
    fun start(projection: MediaProjection, sink: Sink): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || running) return false
        var audioRecord: AudioRecord? = null
        var audioCodec: MediaCodec? = null
        try {
            val capture = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUid(Process.myUid())
                .build()
            val pcm = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            )
            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(capture)
                .setAudioFormat(pcm)
                .setBufferSizeInBytes((minBuffer * 2).coerceAtLeast(CHUNK_BYTES * 4))
                .build()
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                return false
            }

            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, CHUNK_BYTES * 2)
            }
            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioCodec.start()
            audioRecord.startRecording()
        } catch (error: Throwable) {
            Timber.w(error, "Aero audio capture is unavailable; streaming video only")
            runCatching { audioRecord?.release() }
            runCatching { audioCodec?.release() }
            return false
        }

        record = audioRecord
        codec = audioCodec
        running = true
        val activeRecord = audioRecord
        val activeCodec = audioCodec
        feedThread = thread(name = "aero-audio-feed", isDaemon = true) { feed(activeRecord, activeCodec) }
        drainThread = thread(name = "aero-audio-drain", isDaemon = true) { drain(activeCodec, sink) }
        return true
    }

    /** PCM -> encoder. Presentation time counts samples from a monotonic start, so it stays on the video clock without jitter. */
    private fun feed(source: AudioRecord, encoder: MediaCodec) {
        val chunk = ByteArray(CHUNK_BYTES)
        var baseUs = -1L
        var frames = 0L
        while (running) {
            val read = source.read(chunk, 0, chunk.size)
            if (read <= 0) {
                if (read < 0) break
                continue
            }
            if (baseUs < 0) baseUs = System.nanoTime() / 1_000L - read / BYTES_PER_FRAME * 1_000_000L / SAMPLE_RATE
            try {
                var offset = 0
                while (offset < read && running) {
                    val index = encoder.dequeueInputBuffer(10_000)
                    if (index < 0) continue
                    val buffer = encoder.getInputBuffer(index) ?: continue
                    buffer.clear()
                    val take = minOf(buffer.remaining(), read - offset)
                    buffer.put(chunk, offset, take)
                    val ptsUs = baseUs + frames * 1_000_000L / SAMPLE_RATE
                    encoder.queueInputBuffer(index, 0, take, ptsUs, 0)
                    offset += take
                    frames += take / BYTES_PER_FRAME
                }
            } catch (error: Throwable) {
                if (running) Timber.w(error, "Aero audio encoder stopped while feeding")
                return
            }
        }
    }

    private fun drain(encoder: MediaCodec, sink: Sink) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            val index = try {
                encoder.dequeueOutputBuffer(info, 10_000)
            } catch (error: Throwable) {
                if (running) Timber.w(error, "Aero audio encoder stopped while draining")
                return
            }
            if (index < 0) continue
            try {
                val buffer = encoder.getOutputBuffer(index)
                if (buffer != null && info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    val frame = ByteArray(info.size)
                    buffer.get(frame)
                    sink.onAudio(frame, info.presentationTimeUs)
                }
            } catch (error: Throwable) {
                Timber.w(error, "Unable to process an AAC output frame")
            } finally {
                runCatching { encoder.releaseOutputBuffer(index, false) }
            }
        }
    }

    fun stop() {
        if (!running && codec == null) return
        running = false
        runCatching { record?.stop() }
        runCatching { feedThread?.join(300) }
        runCatching { drainThread?.join(300) }
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { record?.release() }
        codec = null
        record = null
        feedThread = null
        drainThread = null
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        private const val BYTES_PER_FRAME = 4 // 16-bit stereo
        private const val CHUNK_BYTES = 4096
    }
}
