package app.gamenative.cast

import android.content.Context

/** Persisted settings for the local Game-Aero to Chromecast video stream. */
object AeroScreenStreamSettings {
    private const val PREFS = "aero_screen_stream"
    private const val KEY_RESOLUTION = "resolution"
    private const val KEY_ASPECT = "aspect"
    private const val KEY_AUDIO = "audio"

    enum class Resolution(val height: Int, val bitrate: Int) {
        P480(480, 1_500_000),
        P720(720, 3_000_000),
        P1080(1080, 6_000_000),
    }

    enum class AspectRatio(val label: String) {
        ORIGINAL("original"),
        RATIO_16_9("16:9"),
        RATIO_21_9("21:9"),
    }

    data class OutputSize(val width: Int, val height: Int)

    fun resolution(context: Context): Resolution =
        Resolution.entries.firstOrNull { it.name == prefs(context).getString(KEY_RESOLUTION, Resolution.P720.name) }
            ?: Resolution.P720

    fun aspectRatio(context: Context): AspectRatio =
        AspectRatio.entries.firstOrNull { it.name == prefs(context).getString(KEY_ASPECT, AspectRatio.ORIGINAL.name) }
            ?: AspectRatio.ORIGINAL

    /** Whether the stream carries Game-Aero's own audio (needs the microphone permission Android uses for capture). */
    fun audioEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUDIO, true)

    fun setAudioEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUDIO, value).apply()
    }

    fun setResolution(context: Context, value: Resolution) {
        prefs(context).edit().putString(KEY_RESOLUTION, value.name).apply()
    }

    fun setAspectRatio(context: Context, value: AspectRatio) {
        prefs(context).edit().putString(KEY_ASPECT, value.name).apply()
    }

    /**
     * Builds an even-sized landscape/portrait video surface. 16:9 remains standard HD;
     * 21:9 is bounded to the corresponding preset's 16:9 width so older Cast devices
     * are not asked to decode an unusually wide 2520-pixel frame.
     */
    fun outputSize(context: Context, resolution: Resolution, aspectRatio: AspectRatio): OutputSize {
        val metrics = context.resources.displayMetrics
        val sourceWidth = metrics.widthPixels.coerceAtLeast(1)
        val sourceHeight = metrics.heightPixels.coerceAtLeast(1)
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
        val ratio = when (aspectRatio) {
            AspectRatio.ORIGINAL -> sourceRatio
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.RATIO_21_9 -> 21f / 9f
        }.coerceIn(0.35f, 2.4f)

        val targetHeight = resolution.height
        val maxWidth = (targetHeight * 16f / 9f).toInt()
        var width = (targetHeight * ratio).toInt()
        var height = targetHeight
        if (width > maxWidth) {
            width = maxWidth
            height = (width / ratio).toInt()
        }
        width = (width.coerceAtLeast(2) / 2) * 2
        height = (height.coerceAtLeast(2) / 2) * 2
        return OutputSize(width, height)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
