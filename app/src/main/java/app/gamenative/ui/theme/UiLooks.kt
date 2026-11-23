package app.gamenative.ui.theme

import android.content.Context
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
    /** A video under assets/ that plays as the wallpaper of this look; when null, [wallpaper] is used. */
    val wallpaperAsset: String? = null,
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
        UiLook(
            id = "ramune",
            name = "Ramune",
            hint = R.string.look_ramune_hint,
            themeName = "Ramune",
            wallpaper = WallpaperMode.WATER,
            style = AeroStyleState(tint = argb(0xFFE6F9FF), selection = argb(0xFFE8352B), homeStyle = HOME_AERO),
            wallpaperAsset = "wallpapers/ramune.mp4",
        ),
        UiLook(
            id = "aquaplaza",
            name = "Aqua Plaza",
            hint = R.string.look_aquaplaza_hint,
            themeName = "Aqua Plaza",
            wallpaper = WallpaperMode.OFF,
            style = AeroStyleState(opacity = 90, gloss = 110, selection = argb(0xFF2F86E8), homeStyle = HOME_AERO),
            wallpaperAsset = "wallpapers/aquaplaza.mp4",
        ),
        UiLook(
            id = "aquarium",
            name = "Aquarium",
            hint = R.string.look_aquarium_hint,
            themeName = "Aquarium",
            wallpaper = WallpaperMode.BUBBLES,
            style = AeroStyleState(selection = argb(0xFFFF8A24), homeStyle = HOME_AERO),
            wallpaperAsset = "wallpapers/aquarium.mp4",
        ),
        UiLook(
            id = "goldfish",
            name = "Goldfish Y2K",
            hint = R.string.look_goldfish_hint,
            themeName = "Goldfish Y2K",
            wallpaper = WallpaperMode.AURORA,
            style = AeroStyleState(opacity = 70, gloss = 150, outline = 130, tint = argb(0xFFFFE6D6), selection = argb(0xFFFF8B5C), homeStyle = HOME_AERO),
            wallpaperAsset = "wallpapers/goldfish.mp4",
        ),
        UiLook(
            id = "hibiscus",
            name = "Hibiscus",
            hint = R.string.look_hibiscus_hint,
            themeName = "Hibiscus",
            wallpaper = WallpaperMode.WATER,
            style = AeroStyleState(tint = argb(0xFFFFE0EF), selection = argb(0xFFFF4F9A), homeStyle = HOME_AERO),
            wallpaperAsset = "wallpapers/hibiscus.mp4",
        ),
    )

    /** Installs and enables the look's theme, then sets the dashboard, the glass and the wallpaper. */
    suspend fun apply(context: Context, look: UiLook): Result<Unit> {
        val theme = BundledThemes.all.firstOrNull { it.name == look.themeName }
            ?: return Result.failure(IllegalStateException("bundled theme ${look.themeName} is missing"))
        val installed = ThemeManager.installBundled(theme)
        // the person's UI scale stays as it is: it depends on the screen, not on the look
        AeroStyle.update { look.style.copy(uiScale = it.uiScale) }
        app.gamenative.PrefManager.aeroHome = true
        val asset = look.wallpaperAsset
        if (asset == null || WallpaperSettings.useBundled(context, asset).isFailure) {
            // no bundled video, or it could not be copied: the live wallpaper of the look instead
            WallpaperSettings.setMode(look.wallpaper)
        }
        return installed.map { }
    }
}
