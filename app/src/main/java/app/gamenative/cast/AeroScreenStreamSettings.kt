package app.gamenative.cast

import android.content.Context

/**
 * User preferences for the future in-app screen-streaming pipeline.
 * Values are persisted locally and are deliberately independent of Cast session state.
 */
object AeroScreenStreamSettings {
    private const val PREFS = "aero_screen_stream"
    private const val KEY_RESOLUTION = "resolution"
    private const val KEY_ASPECT = "aspect"

    enum class Resolution(val height: Int) {
        P480(480), P720(720), P1080(1080)
    }

    enum class AspectRatio(val label: String) {
        ORIGINAL("original"), RATIO_16_9("16:9"), RATIO_4_3("4:3"), RATIO_21_9("21:9")
    }

    fun resolution(context: Context): Resolution =
        Resolution.entries.firstOrNull { it.name == prefs(context).getString(KEY_RESOLUTION, Resolution.P720.name) }
            ?: Resolution.P720

    fun aspectRatio(context: Context): AspectRatio =
        AspectRatio.entries.firstOrNull { it.name == prefs(context).getString(KEY_ASPECT, AspectRatio.ORIGINAL.name) }
            ?: AspectRatio.ORIGINAL

    fun setResolution(context: Context, value: Resolution) {
        prefs(context).edit().putString(KEY_RESOLUTION, value.name).apply()
    }

    fun setAspectRatio(context: Context, value: AspectRatio) {
        prefs(context).edit().putString(KEY_ASPECT, value.name).apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
