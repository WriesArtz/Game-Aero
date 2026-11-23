package app.gamenative.ui.theme

import androidx.annotation.StringRes
import app.gamenative.R
import app.gamenative.theme.BundledThemes
import app.gamenative.theme.ThemeManager

/**
 * A whole look for the app in one choice: the theme (colors, glass, layout of the shelf), which dashboard is drawn
 * (glass covers or flat Metro tiles), the style of the glass and the animated wallpaper.
 */
class UiLook(
    val id: String,
    val name: String,
    @StringRes val hint: Int,
    /** Name of the bundled theme this look installs and turns on. */
    val themeName: String,
    val wallpaper: WallpaperMode,
    val style: AeroStyleState,
)

object UiLooks {
    private fun argb(value: Long): Int = value.toInt()

    val all: List<UiLook> = listOf(
        UiLook(
            id = "frutiger",
            name = "Frutiger Aero",
            hint = R.string.look_frutiger_hint,
            themeName = "Frutiger Aero",
            wallpaper = WallpaperMode.WATER,
            style = AeroStyleState(homeStyle = HOME_AERO),
        ),
        UiLook(
            id = "crystal",
            name = "Liquid Crystal",
            hint = R.string.look_crystal_hint,
            themeName = "Liquid Crystal",
            wallpaper = WallpaperMode.AURORA,
            style = AeroStyleState(opacity = 60, gloss = 140, outline = 150, homeStyle = HOME_AERO),
        ),
        UiLook(
            id = "abyssal",
            name = "Frutiger Abyssal",
            hint = R.string.look_abyssal_hint,
            themeName = "Frutiger Abyssal",
            wallpaper = WallpaperMode.BUBBLES,
            style = AeroStyleState(
                opacity = 80,
                gloss = 90,
                outline = 120,
                tint = argb(0xFF8FF5EA),
                selection = argb(0xFF8FF5EA),
                homeStyle = HOME_AERO,
            ),
        ),
        UiLook(
            id = "metro",
            name = "Xbox 360 Metro",
            hint = R.string.look_metro_hint,
            themeName = "Xbox 360 Metro",
            wallpaper = WallpaperMode.OFF,
            style = AeroStyleState(selection = argb(0xFF107C10), homeStyle = HOME_METRO),
        ),
        UiLook(
            id = "xboxaero",
            name = "Xbox Aero",
            hint = R.string.look_xboxaero_hint,
            themeName = "Xbox Aero",
            wallpaper = WallpaperMode.WATER,
            style = AeroStyleState(tint = argb(0xFFB6FF5A), selection = argb(0xFFB6FF5A), homeStyle = HOME_AERO),
        ),
    )

    /** Installs and enables the look's theme, then sets the dashboard, the glass and the wallpaper. */
    suspend fun apply(look: UiLook): Result<Unit> {
        val theme = BundledThemes.all.firstOrNull { it.name == look.themeName }
            ?: return Result.failure(IllegalStateException("bundled theme ${look.themeName} is missing"))
        val installed = ThemeManager.installBundled(theme)
        // the person's UI scale stays as it is: it depends on the screen, not on the look
        AeroStyle.update { look.style.copy(uiScale = it.uiScale) }
        app.gamenative.PrefManager.aeroHome = true
        WallpaperSettings.setMode(look.wallpaper)
        return installed.map { }
    }
}
