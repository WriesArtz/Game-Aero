package app.gamenative.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.gamenative.PrefManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which dashboard the home draws: glass covers (Frutiger Aero) or flat Metro tiles. */
const val HOME_AERO = 0
const val HOME_METRO = 1

/**
 * What the person chose for the dashboard glass and the size of the UI. Percentages are 100 for the stock look;
 * colors are ARGB ints where 0 means "automatic" (white glass, the theme's text, the theme's selection).
 */
data class AeroStyleState(
    val opacity: Int = 100,
    val gloss: Int = 100,
    val outline: Int = 100,
    val tint: Int = 0,
    val text: Int = 0,
    val selection: Int = 0,
    val uiScale: Int = 100,
    /** 0 = glass covers (Frutiger Aero), 1 = flat Metro tiles. */
    val homeStyle: Int = 0,
    // the glass covers of the dashboard, in percent (100 = as designed)
    /** How opaque the art of a cover is; less lets the glass show through. */
    val coverArt: Int = 100,
    /** Strength of the effects over the art: cyan light, gloss and inner glow. */
    val coverFx: Int = 100,
    /** How opaque the frame of a cover is. */
    val frameAlpha: Int = 100,
    /** Thickness of the frame. */
    val frameWidth: Int = 100,
    /** How bright the frame is. */
    val frameLight: Int = 100,
    /** Strength of the halo around the covers. */
    val coverGlow: Int = 100,
    /** Strength of the mirror image under the covers. */
    val coverReflection: Int = 100,
    /** Strength of the moving effects: rim, light sweep, pulse, sparkles and bubbles; 0 turns them off. */
    val coverMotion: Int = 100,
) {
    /** [percent] as a factor, 1 at 100. */
    fun f(percent: Int): Float = percent / 100f

    val metro: Boolean get() = homeStyle == HOME_METRO

    val tintColor: Color get() = if (tint == 0) Color.White else Color(tint)

    private fun a(base: Float, percent: Int) = (base * percent / 100f).coerceIn(0f, 1f)

    /** The body of a glass piece: a bright top edge, a clear middle and a soft base, tinted. */
    fun fill(): Brush = Brush.verticalGradient(
        listOf(
            tintColor.copy(alpha = a(0.26f, gloss)),
            tintColor.copy(alpha = a(0.08f, opacity)),
            tintColor.copy(alpha = a(0.14f, opacity)),
        ),
    )

    /** The thin rim of a glass piece. */
    fun rim(): Brush = Brush.verticalGradient(
        listOf(tintColor.copy(alpha = a(0.75f, outline)), tintColor.copy(alpha = a(0.2f, outline))),
    )

    /** The chosen text color, keeping the transparency the design asks for; [default] when automatic. */
    fun textColor(default: Color): Color = if (text == 0) default else Color(text).copy(alpha = default.alpha)

    val selectionColor: Color? get() = if (selection == 0) null else Color(selection)

    /** Dark or light text that reads on the chosen selection color. */
    fun onSelection(default: Color): Color {
        val s = selectionColor ?: return default
        return if (s.luminance() > 0.5f) Color(0xFF06222B) else Color.White
    }
}

object AeroStyle {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AeroStyleState> = _state.asStateFlow()

    private fun load() = AeroStyleState(
        opacity = PrefManager.aeroOpacity,
        gloss = PrefManager.aeroGloss,
        outline = PrefManager.aeroOutline,
        tint = PrefManager.aeroTint,
        text = PrefManager.aeroText,
        selection = PrefManager.aeroSelection,
        uiScale = PrefManager.uiScalePercent,
        homeStyle = PrefManager.homeStyle,
        coverArt = PrefManager.coverArt,
        coverFx = PrefManager.coverFx,
        frameAlpha = PrefManager.frameAlpha,
        frameWidth = PrefManager.frameWidth,
        frameLight = PrefManager.frameLight,
        coverGlow = PrefManager.coverGlow,
        coverReflection = PrefManager.coverReflection,
        coverMotion = PrefManager.coverMotion,
    )

    fun update(transform: (AeroStyleState) -> AeroStyleState) {
        val next = transform(_state.value)
        PrefManager.aeroOpacity = next.opacity
        PrefManager.aeroGloss = next.gloss
        PrefManager.aeroOutline = next.outline
        PrefManager.aeroTint = next.tint
        PrefManager.aeroText = next.text
        PrefManager.aeroSelection = next.selection
        PrefManager.uiScalePercent = next.uiScale
        PrefManager.homeStyle = next.homeStyle
        PrefManager.coverArt = next.coverArt
        PrefManager.coverFx = next.coverFx
        PrefManager.frameAlpha = next.frameAlpha
        PrefManager.frameWidth = next.frameWidth
        PrefManager.frameLight = next.frameLight
        PrefManager.coverGlow = next.coverGlow
        PrefManager.coverReflection = next.coverReflection
        PrefManager.coverMotion = next.coverMotion
        _state.value = next
    }

    fun reset() = update { AeroStyleState(homeStyle = it.homeStyle) }
}
