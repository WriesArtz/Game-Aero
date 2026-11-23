package app.gamenative.theme

/** A theme folder shipped inside the app and installed from the theme manager: file name to file text. */
class BundledTheme(
    val name: String,
    val description: String,
    val files: Map<String, String>,
) {
    val themeJson: String get() = files.getValue("theme.json")
    val themeCss: String get() = files.getValue("theme.css")
}

/**
 * The themes that ship with the app, all in the glass look of Frutiger Aero (translucent blurred panels, bright rims,
 * glossy highlights) with the Big Picture shelf of covers. Each one has a Mode option with three looks: Clear (almost
 * transparent glass over the background), Light and Dark. The shape of a theme lives in theme.css and the colors of
 * each mode in mode-clear.css, mode-light.css and mode-dark.css, which the Mode option loads after it. Once installed
 * they are ordinary theme folders, edited, reordered and removed like any other theme.
 */
object BundledThemes {

    val all: List<BundledTheme> by lazy { palettes.map { it.toTheme() } }

    private val MODES = listOf("Clear", "Light", "Dark")

    /** The colors of one mode of one theme. */
    private class Look(
        /** Page color; its brightness is what makes the app draw light or dark. */
        val bg: String,
        val sky: String,
        val surface: String,
        val surfaceVariant: String,
        /** r,g,b of the glass body and of its bright rim. */
        val tint: String,
        val rim: String,
        val cardAlpha: Double,
        val rimAlpha: Double,
        val text: String,
        val muted: String,
        val subtitle: String,
        val textGlow: String,
        val focus: String,
        val onFocus: String,
        val onAccent: String,
        /** How much the page darkens the focused game's artwork behind the shelf. */
        val heroOverlay: Double,
        /** Replaces the plain glass of menus and dialogs, to tint them; empty keeps the plain glass. */
        val menuTint: String = "",
        /** Extra `:root` lines, for the glass strength of the Clear mode. */
        val extra: String = "",
    )

    /** Almost transparent white glass over a colorful background, with white text. */
    private fun clear(
        bg: String,
        sky: String,
        surface: String,
        surfaceVariant: String,
        focus: String,
        onFocus: String,
        onAccent: String,
        menuTint: String = "",
    ) = Look(
        bg = bg, sky = sky, surface = surface, surfaceVariant = surfaceVariant,
        tint = "255, 255, 255", rim = "255, 255, 255", cardAlpha = 0.08, rimAlpha = 0.75,
        text = "#ffffff", muted = "#f0f5ff", subtitle = "#e3eaff", textGlow = "8px rgba(0, 20, 60, 0.5)",
        focus = focus, onFocus = onFocus, onAccent = onAccent, heroOverlay = 0.25, menuTint = menuTint,
        extra = "  --gn-panel-opacity: 0.2;\n  --gn-menu-opacity: 0.38;\n  --gn-blur: 10px;\n  --gn-menu-blur: 12px;\n  --gn-header-blur: 10px;\n  --gn-footer-blur: 10px;\n",
    )

    /** Pale milky glass over a bright background, with dark text. */
    private fun light(
        bg: String,
        sky: String,
        surface: String,
        surfaceVariant: String,
        ink: String,
        muted: String,
        subtitle: String,
        focus: String,
        onFocus: String,
        onAccent: String,
        menuTint: String = "",
    ) = Look(
        bg = bg, sky = sky, surface = surface, surfaceVariant = surfaceVariant,
        tint = "255, 255, 255", rim = "255, 255, 255", cardAlpha = 0.6, rimAlpha = 0.9,
        text = ink, muted = muted, subtitle = subtitle, textGlow = "0px rgba(255, 255, 255, 0)",
        focus = focus, onFocus = onFocus, onAccent = onAccent, heroOverlay = 0.08, menuTint = menuTint,
    )

    /** Tinted glass over a dark background, with light text. */
    private fun dark(
        bg: String,
        sky: String,
        surface: String,
        surfaceVariant: String,
        tint: String,
        rim: String,
        text: String,
        muted: String,
        subtitle: String,
        textGlow: String,
        focus: String,
        onFocus: String,
        onAccent: String,
        menuTint: String = "",
    ) = Look(
        bg = bg, sky = sky, surface = surface, surfaceVariant = surfaceVariant,
        tint = tint, rim = rim, cardAlpha = 0.16, rimAlpha = 0.45,
        text = text, muted = muted, subtitle = subtitle, textGlow = textGlow,
        focus = focus, onFocus = onFocus, onAccent = onAccent, heroOverlay = 0.45, menuTint = menuTint,
    )

    /** What stays the same in every mode of a theme, and the three looks. */
    private class Palette(
        val name: String,
        val description: String,
        /** The mode a freshly installed theme starts in: Clear, Light or Dark. */
        val defaultMode: String,
        /** The accent in the color picker, and the three stops of the glossy accent gradient. */
        val accent: String,
        val accentTop: String,
        val accentMid: String,
        val accentBottom: String,
        val toggleOn: String,
        /** r,g,b of the glow around the bars. */
        val glow: String,
        val radius: Int,
        val cardRadius: Int,
        val menuRadius: Int,
        val focusGlow: Int,
        val heroOpacity: Double,
        val heroBlur: Int,
        val reflection: Double,
        val reflectionFocused: Double,
        val columns: Int,
        val cardScale: Double,
        val focusedScale: Double,
        val clear: Look,
        val light: Look,
        val dark: Look,
        /** Pure, flat glass like iOS 26: no animated sheen, no colored glow, no gloss blobs, thin bright edges. */
        val clean: Boolean = false,
        /** Console-dashboard layout: a wide banner first, then the covers low on the screen, with the game's title centered above. */
        val dash: Boolean = false,
    ) {
        fun toTheme() = BundledTheme(
            name = name,
            description = description,
            files = mapOf(
                "theme.json" to json(),
                "theme.css" to structureCss(),
                "mode-clear.css" to colorsCss("Clear", clear),
                "mode-light.css" to colorsCss("Light", light),
                "mode-dark.css" to colorsCss("Dark", dark),
            ),
        )

        private fun json(): String = """
{
  "name": "$name",
  "author": "GameNative",
  "version": "v2.0",
  "description": "$description",
  "manifest_version": 9,
  "target": "System-Wide",
  "inject": {
    "theme.css": ["All"]
  },
  "patches": {
    "Mode": {
      "type": "dropdown",
      "default": "$defaultMode",
      "values": {
        "Clear": {
          "mode-clear.css": ["All"]
        },
        "Light": {
          "mode-light.css": ["All"]
        },
        "Dark": {
          "mode-dark.css": ["All"]
        }
      }
    },
    "Transparency": {
      "type": "dropdown",
      "default": "Mode default",
      "values": {
        "Mode default": {},
        "Crystal": {
          "--gn-panel-opacity": ["0.12", "All"],
          "--gn-menu-opacity": ["0.25", "All"],
          "--gn-header-fill": ["linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.021))", "All"],
          "--gn-footer-fill": ["linear-gradient(0deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.021))", "All"]
        },
        "Clear": {
          "--gn-panel-opacity": ["0.22", "All"],
          "--gn-menu-opacity": ["0.4", "All"],
          "--gn-header-fill": ["linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.042))", "All"],
          "--gn-footer-fill": ["linear-gradient(0deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.042))", "All"]
        },
        "Medium": {
          "--gn-panel-opacity": ["0.45", "All"],
          "--gn-menu-opacity": ["0.65", "All"],
          "--gn-header-fill": ["linear-gradient(180deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.07))", "All"],
          "--gn-footer-fill": ["linear-gradient(0deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.07))", "All"]
        },
        "Frosted": {
          "--gn-panel-opacity": ["0.7", "All"],
          "--gn-menu-opacity": ["0.85", "All"],
          "--gn-header-fill": ["linear-gradient(180deg, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.112))", "All"],
          "--gn-footer-fill": ["linear-gradient(0deg, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.112))", "All"]
        }
      }
    },
    "Blur": {
      "type": "dropdown",
      "default": "Mode default",
      "values": {
        "Mode default": {},
        "None": {
          "--gn-blur": ["0px", "All"],
          "--gn-menu-blur": ["2px", "All"],
          "--gn-header-blur": ["0px", "All"],
          "--gn-footer-blur": ["0px", "All"]
        },
        "Low": {
          "--gn-blur": ["6px", "All"],
          "--gn-menu-blur": ["8px", "All"],
          "--gn-header-blur": ["6px", "All"],
          "--gn-footer-blur": ["6px", "All"]
        },
        "Medium": {
          "--gn-blur": ["14px", "All"],
          "--gn-menu-blur": ["16px", "All"],
          "--gn-header-blur": ["14px", "All"],
          "--gn-footer-blur": ["14px", "All"]
        },
        "High": {
          "--gn-blur": ["24px", "All"],
          "--gn-menu-blur": ["26px", "All"],
          "--gn-header-blur": ["24px", "All"],
          "--gn-footer-blur": ["24px", "All"]
        },
        "Max": {
          "--gn-blur": ["36px", "All"],
          "--gn-menu-blur": ["38px", "All"],
          "--gn-header-blur": ["36px", "All"],
          "--gn-footer-blur": ["36px", "All"]
        }
      }
    },
    "Accent": {
      "type": "none",
      "default": "Custom",
      "values": {
        "Custom": {}
      },
      "components": [
        {
          "name": "Accent color",
          "type": "color-picker",
          "on": "Custom",
          "default": "$accent",
          "css_variable": "gn-primary",
          "tabs": ["All"]
        }
      ]
    }
  }
}
""".trimStart()

        /** The shape of the theme; every color is set by the mode file loaded after it. */
        private fun structureCss(): String {
            val panelSlide = if (clean) "" else "  --gn-panel-slide: 24s;\n"
            val fillSlide = if (clean) "" else "  --gn-fill-slide: 14s;\n"
            val underline = if (clean) {
                "  --gn-focus-underline-height: 0px;\n"
            } else {
                "  --gn-focus-underline: linear-gradient(90deg, $accentTop, $accentBottom, $accentTop);\n" +
                    "  --gn-focus-underline-height: 3px;\n  --gn-focus-underline-slide: 5s;\n"
            }
            val barGlow = if (clean) "0px rgba(255, 255, 255, 0)" else "14px rgba($glow, 0.5)"
            return """
/* $name for GameNative: shape, accent and layout. Colors come from the Mode option (mode-*.css), glass strength from the Glass option. */
:root {
  --gn-corner-radius: ${radius}px;
  --gn-card-corner-radius: ${cardRadius}px;
  --gn-icon-corner-radius: ${(cardRadius * 2 / 3).coerceAtLeast(4)}px;
$panelSlide
  --gn-heading-weight: 600;
  --gn-letter-spacing: 0.01em;
  --gn-title-size: 1.1;

  --gn-accent-gradient: linear-gradient(180deg, $accentTop 0%, $accentMid 55%, $accentBottom 100%);
$fillSlide  --gn-toggle-on: $toggleOn;
  --gn-thumb-on: #ffffff;

  --gn-focus-ring: ${if (clean) 2 else 3}px;
  --gn-focus-glow: ${if (clean) 8 else focusGlow}px;
$underline
  --gn-header-glow: $barGlow;
  --gn-header-blur: ${if (clean) 14 else 10}px;
  --gn-footer-glow: $barGlow;
  --gn-footer-blur: ${if (clean) 14 else 10}px;

  --gn-menu-radius: ${menuRadius}px;
  --gn-menu-glow: ${if (clean) 0 else 18}px;

  --gn-hero-opacity: $heroOpacity;
  --gn-hero-blur: ${heroBlur}px;
  --gn-hero-saturation: 1;
  --gn-hero-fade: 0.25;

  --gn-library-layout: row;
  --gn-shelf-wide-first: 1;
${if (dash) "  --gn-shelf-row-anchor: 0.58;\n  --gn-shelf-title-offset: -10px;\n" else ""}  --gn-card-scale: $cardScale;
  --gn-card-focused-scale: $focusedScale;
  --gn-title-align: ${if (dash) "center" else "start"};
  --gn-grid-columns: $columns;

  --gn-cover-reflection: $reflection;
  --gn-cover-reflection-focused: $reflectionFocused;
  --gn-cover-reflection-fade: 0.5;
  --gn-cover-reflection-blur: 2px;
  --gn-cover-reflection-gap: 3px;
}
""".trimStart()
        }

        private fun colorsCss(mode: String, look: Look): String {
            val tint = look.tint
            val rim = look.rim
            val gloss = if (clean) {
                "linear-gradient(180deg, rgba($rim, 0.16), rgba($tint, 0.05) 35%, rgba($tint, 0.03))"
            } else {
                "radial-gradient(ellipse at 50% 0%, rgba($rim, 0.4), transparent 70%), linear-gradient(180deg, rgba($tint, 0.22), rgba($tint, 0.05))"
            }
            val menuBody = look.menuTint.ifEmpty { "linear-gradient(180deg, rgba($tint, 0.26), rgba($tint, 0.06))" }
            val menuGloss = if (clean) {
                "linear-gradient(180deg, rgba($rim, 0.2), rgba($tint, 0.08) 40%, rgba($tint, 0.05))"
            } else {
                "radial-gradient(ellipse at 50% 0%, rgba($rim, 0.45), transparent 70%), $menuBody"
            }
            val outlineAlpha = if (clean) minOf(look.rimAlpha, 0.5) else look.rimAlpha
            val textGlow = if (clean) "0px rgba(0, 0, 0, 0)" else look.textGlow
            val buttonFill = if (clean) {
                "linear-gradient(180deg, rgba($rim, 0.3), rgba($rim, 0.14))"
            } else {
                "linear-gradient(180deg, rgba($rim, 0.5), rgba($rim, 0.12) 55%, rgba($tint, 0.22))"
            }
            val barUp = if (clean) "linear-gradient(180deg, rgba($rim, 0.14), rgba($tint, 0.05))" else "linear-gradient(180deg, rgba($rim, 0.38), rgba($tint, 0.06))"
            val barDown = if (clean) "linear-gradient(0deg, rgba($rim, 0.14), rgba($tint, 0.05))" else "linear-gradient(0deg, rgba($rim, 0.38), rgba($tint, 0.06))"
            val edge = if (clean) 0.4 else 0.55
            val menuEdge = if (clean) 0.5 else 0.65
            val ringAlpha = if (clean) 0.6 else 0.95
            return """
/* $name, $mode mode: colors. */
:root {
  --gn-background: ${look.bg};
  --gn-background-gradient: ${look.sky};

  --gn-surface: ${look.surface};
  --gn-surface-variant: ${look.surfaceVariant};
  --gn-card: rgba($tint, ${look.cardAlpha});
  --gn-outline: rgba($rim, $outlineAlpha);
  --gn-panel-fill: $gloss;

  --gn-on-primary: ${look.onAccent};
  --gn-text: ${look.text};
  --gn-text-muted: ${look.muted};
  --gn-heading: ${look.text};
  --gn-subtitle: ${look.subtitle};
  --gn-text-glow: $textGlow;

  --gn-toggle-off: rgba($tint, 0.3);
  --gn-thumb-off: #eef6fb;
  --gn-slider-track: rgba($tint, 0.3);
  --gn-slider-thumb: #ffffff;

  --gn-button: rgba($tint, 0.24);
  --gn-on-button: ${look.text};
  --gn-button-fill: $buttonFill;
  --gn-button-focus: ${look.focus};
  --gn-on-button-focus: ${look.onFocus};
  --gn-button-underline: ${if (clean) 0 else 2}px rgba($rim, 0.8);
  --gn-tab: ${look.focus};
  --gn-on-tab: ${look.onFocus};

  --gn-focus: ${look.focus};
  --gn-focus-ring-color: rgba($rim, $ringAlpha);
  --gn-focus-fill: rgba($tint, 0.3);
  --gn-on-focus-fill: ${look.text};

  --gn-bar: rgba($tint, 0.14);
  --gn-header: rgba($tint, 0.14);
  --gn-header-fill: $barUp;
  --gn-header-border: 1px rgba($rim, $edge);
  --gn-header-text: ${look.text};
  --gn-footer: rgba($tint, 0.14);
  --gn-footer-fill: $barDown;
  --gn-footer-border: 1px rgba($rim, $edge);
  --gn-footer-text: ${look.text};

  --gn-menu: ${look.surface};
  --gn-on-menu: ${look.text};
  --gn-menu-fill: $menuGloss;
  --gn-menu-border: 1px rgba($rim, $menuEdge);
  --gn-menu-focus: rgba($tint, 0.34);
  --gn-on-menu-focus: ${look.text};

  --gn-hero-overlay: ${look.heroOverlay};
${look.extra}}
""".trimStart()
        }
    }

    private val palettes: List<Palette> = listOf(
        Palette(
            name = "Frutiger Aero",
            description = "Sky blue glass, glossy highlights and fresh green accents.",
            defaultMode = "Clear",
            accent = "#2bc6e8", accentTop = "#8af5d2", accentMid = "#2bc6e8", accentBottom = "#1a8fd8",
            toggleOn = "linear-gradient(90deg, #8af5d2, #2bc6e8)",
            glow = "120, 220, 255",
            radius = 20, cardRadius = 18, menuRadius = 24, focusGlow = 16,
            heroOpacity = 0.6, heroBlur = 8, reflection = 0.28, reflectionFocused = 0.45,
            columns = 5, cardScale = 1.1, focusedScale = 1.1,
            clear = clear(
                bg = "#1b8fdb",
                sky = "linear-gradient(180deg, #0f6fc0 0%, #2e9be0 38%, #63cbef 72%, #a7ebd0 100%)",
                surface = "#cfeeff", surfaceVariant = "#a9dcf7",
                focus = "#ffffff", onFocus = "#04324d", onAccent = "#04324d",
            ),
            light = light(
                bg = "#e8f6ff",
                sky = "linear-gradient(180deg, #f4fbff 0%, #d6efff 50%, #c8f0e0 100%)",
                surface = "#f2faff", surfaceVariant = "#d9eefb",
                ink = "#0d3a5c", muted = "#3a6a8c", subtitle = "#4f7f9f",
                focus = "#1a8fd8", onFocus = "#ffffff", onAccent = "#04324d",
            ),
            dark = dark(
                bg = "#06182c",
                sky = "linear-gradient(180deg, #031326 0%, #06304f 50%, #0a5a73 100%)",
                surface = "#0b2a45", surfaceVariant = "#123a5c",
                tint = "120, 190, 255", rim = "190, 230, 255",
                text = "#eaf6ff", muted = "#a9cde6", subtitle = "#8fbbd8", textGlow = "6px rgba(0, 20, 50, 0.8)",
                focus = "#9ff0ff", onFocus = "#04243a", onAccent = "#021a2b",
            ),
        ),
        // iOS 26 / macOS Tahoe: almost clear glass, strong blur, bright specular rim, rounded everything
        Palette(
            name = "Liquid Crystal",
            description = "Nearly clear liquid glass with strong blur and bright edges, in the spirit of iOS 26 and macOS Tahoe.",
            defaultMode = "Clear",
            accent = "#0a84ff", accentTop = "#7cc2ff", accentMid = "#2e97ff", accentBottom = "#0a6fe8",
            toggleOn = "linear-gradient(90deg, #5be37d, #34c759)",
            glow = "255, 255, 255",
            radius = 28, cardRadius = 26, menuRadius = 32, focusGlow = 22,
            heroOpacity = 0.55, heroBlur = 16, reflection = 0.0, reflectionFocused = 0.0,
            columns = 5, cardScale = 1.05, focusedScale = 1.08,
            clean = true,
            clear = clear(
                bg = "#8fa9e8",
                sky = "linear-gradient(160deg, #a9c8ff 0%, #bfb6ff 45%, #f3c4dc 100%)",
                surface = "#7f98e0", surfaceVariant = "#6f89d6",
                focus = "#ffffff", onFocus = "#1f2f66", onAccent = "#ffffff",
            ),
            light = light(
                bg = "#f3f4fa",
                sky = "linear-gradient(135deg, #f7f8ff 0%, #ece8ff 45%, #ffeef3 100%)",
                surface = "#ffffff", surfaceVariant = "#eceefa",
                ink = "#1c1c28", muted = "#5a5d72", subtitle = "#767990",
                focus = "#0a84ff", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#0d0f1a",
                sky = "linear-gradient(135deg, #12162b 0%, #231a45 50%, #3a1f45 100%)",
                surface = "#1a1d30", surfaceVariant = "#252944",
                tint = "255, 255, 255", rim = "255, 255, 255",
                text = "#f4f5ff", muted = "#b9bcd6", subtitle = "#9ea2c4", textGlow = "6px rgba(0, 0, 0, 0.5)",
                focus = "#ffffff", onFocus = "#1a1d30", onAccent = "#ffffff",
            ),
        ),
        // the Xbox 360 blades dashboard: pale grey-white with signal green and glossy edges
        Palette(
            name = "Xbox 360",
            description = "The pale blades dashboard: white and grey glass, signal green focus and glossy edges.",
            defaultMode = "Light",
            accent = "#7cc61e", accentTop = "#c4ee7c", accentMid = "#8fd22c", accentBottom = "#5faa12",
            toggleOn = "linear-gradient(90deg, #c4ee7c, #6fb81a)",
            glow = "150, 210, 60",
            radius = 8, cardRadius = 6, menuRadius = 10, focusGlow = 14,
            heroOpacity = 0.35, heroBlur = 6, reflection = 0.2, reflectionFocused = 0.3,
            columns = 4, cardScale = 1.1, focusedScale = 1.1,
            clear = clear(
                bg = "#7a8a90",
                sky = "radial-gradient(ellipse at 50% 100%, rgba(150, 220, 60, 0.7) 0%, rgba(150, 220, 60, 0) 55%), linear-gradient(180deg, #8da0a8 0%, #5d7078 55%, #3c4b52 100%)",
                surface = "#6c7f87", surfaceVariant = "#586a72",
                focus = "#b6ec5a", onFocus = "#16300a", onAccent = "#16300a",
            ),
            light = light(
                bg = "#e6eaec",
                sky = "radial-gradient(ellipse at 50% 100%, #b9e07a 0%, rgba(185, 224, 122, 0) 55%), linear-gradient(180deg, #ffffff 0%, #e9eef0 55%, #cfd6d9 100%)",
                surface = "#f4f7f8", surfaceVariant = "#dfe6e9",
                ink = "#2a3236", muted = "#586369", subtitle = "#6b777d",
                focus = "#8fd22c", onFocus = "#16300a", onAccent = "#16300a",
            ),
            dark = dark(
                bg = "#0e1214",
                sky = "radial-gradient(ellipse at 50% 100%, rgba(120, 200, 30, 0.35) 0%, rgba(120, 200, 30, 0) 55%), linear-gradient(180deg, #1b2226 0%, #0f1417 100%)",
                surface = "#192024", surfaceVariant = "#232c31",
                tint = "200, 215, 220", rim = "220, 240, 200",
                text = "#e8efe6", muted = "#a7b4ab", subtitle = "#8c9a91", textGlow = "0px rgba(0, 0, 0, 0)",
                focus = "#9be02f", onFocus = "#0e1a04", onAccent = "#10200a",
            ),
        ),
        // the 2011 Xbox 360 Metro dashboard: flat square tiles, black and white, one strong green and no gloss
        Palette(
            name = "Xbox 360 Metro",
            description = "The Metro dashboard: flat square tiles, black and white with one strong green, no gloss.",
            defaultMode = "Dark",
            accent = "#107c10", accentTop = "#2d9a2d", accentMid = "#107c10", accentBottom = "#0b5e0b",
            toggleOn = "linear-gradient(90deg, #107c10, #107c10)",
            glow = "16, 124, 16",
            radius = 2, cardRadius = 2, menuRadius = 2, focusGlow = 0,
            heroOpacity = 0.3, heroBlur = 0, reflection = 0.0, reflectionFocused = 0.0,
            columns = 5, cardScale = 1.0, focusedScale = 1.04,
            clean = true,
            clear = clear(
                bg = "#7a8a90",
                sky = "linear-gradient(180deg, #2b3a32 0%, #14201a 100%)",
                surface = "#6c7f87", surfaceVariant = "#586a72",
                focus = "#ffffff", onFocus = "#0b3d0b", onAccent = "#ffffff",
            ),
            light = light(
                bg = "#e6eaec",
                sky = "linear-gradient(180deg, #ffffff 0%, #e4e4e4 100%)",
                surface = "#f4f7f8", surfaceVariant = "#dfe6e9",
                ink = "#2a3236", muted = "#586369", subtitle = "#6b777d",
                focus = "#107c10", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#0a0a0a",
                sky = "linear-gradient(180deg, #1a1a1a 0%, #050505 100%)",
                surface = "#161616", surfaceVariant = "#222222",
                tint = "200, 215, 220", rim = "220, 240, 200",
                text = "#e8efe6", muted = "#a7b4ab", subtitle = "#8c9a91", textGlow = "0px rgba(0, 0, 0, 0)",
                focus = "#3fb13f", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
        ),
        // the deep end: bioluminescent teal over almost black water
        Palette(
            name = "Frutiger Abyssal",
            description = "Deep ocean glass with glowing bioluminescent teal.",
            defaultMode = "Dark",
            accent = "#2ee6c8", accentTop = "#7affea", accentMid = "#15c7b8", accentBottom = "#0a7f9c",
            toggleOn = "linear-gradient(90deg, #7affea, #15c7b8)",
            glow = "40, 230, 200",
            radius = 22, cardRadius = 20, menuRadius = 26, focusGlow = 26,
            heroOpacity = 0.45, heroBlur = 12, reflection = 0.3, reflectionFocused = 0.5,
            columns = 5, cardScale = 1.1, focusedScale = 1.12,
            clear = clear(
                bg = "#0a4f78",
                sky = "radial-gradient(ellipse at 50% 110%, rgba(21, 199, 184, 0.55) 0%, rgba(21, 199, 184, 0) 60%), linear-gradient(180deg, #0a4f78 0%, #0a7f9c 50%, #15b5b0 100%)",
                surface = "#1b7f9a", surfaceVariant = "#146d86",
                focus = "#c8fff5", onFocus = "#01302e", onAccent = "#01201f",
            ),
            light = light(
                bg = "#e4f8f8",
                sky = "linear-gradient(180deg, #f2fcfc 0%, #cdf1f1 55%, #aee4e6 100%)",
                surface = "#f1fcfc", surfaceVariant = "#d3f1f1",
                ink = "#0a3a46", muted = "#356a77", subtitle = "#4d8390",
                focus = "#0fb5a5", onFocus = "#ffffff", onAccent = "#01201f",
            ),
            dark = dark(
                bg = "#020d1c",
                sky = "radial-gradient(ellipse at 50% 110%, rgba(21, 199, 184, 0.35) 0%, rgba(21, 199, 184, 0) 60%), linear-gradient(180deg, #02101f 0%, #031d33 45%, #04293f 75%, #052f3a 100%)",
                surface = "#06243a", surfaceVariant = "#0b3550",
                tint = "40, 150, 190", rim = "110, 235, 235",
                text = "#d8fbff", muted = "#86c9d6", subtitle = "#6fb6c4", textGlow = "10px rgba(0, 200, 190, 0.45)",
                focus = "#7affea", onFocus = "#01302e", onAccent = "#01201f",
            ),
        ),
        // the Wii menu, rebuilt in glass: white, big rounded channels, strong gloss and a wet-floor reflection
        Palette(
            name = "Wii Recrystal",
            description = "The clean white Wii menu remade in glass, with rounded channels, strong gloss and deep reflections.",
            defaultMode = "Light",
            accent = "#2fa8ee", accentTop = "#8fd8ff", accentMid = "#43b6f2", accentBottom = "#1c8fd6",
            toggleOn = "linear-gradient(90deg, #8fd8ff, #1c8fd6)",
            glow = "90, 190, 240",
            radius = 26, cardRadius = 24, menuRadius = 30, focusGlow = 22,
            heroOpacity = 0.3, heroBlur = 8, reflection = 0.4, reflectionFocused = 0.6,
            columns = 4, cardScale = 1.1, focusedScale = 1.16,
            clear = clear(
                bg = "#5aaee6",
                sky = "linear-gradient(180deg, #8fd0f7 0%, #5aaee6 55%, #3b8fd0 100%)",
                surface = "#6fb8ea", surfaceVariant = "#4f9fd8",
                focus = "#ffffff", onFocus = "#1c6aa5", onAccent = "#ffffff",
            ),
            light = light(
                bg = "#eef6fc",
                sky = "linear-gradient(180deg, #ffffff 0%, #e6f3fc 55%, #cfe8f7 100%)",
                surface = "#f6fbff", surfaceVariant = "#dcedf9",
                ink = "#35566e", muted = "#648299", subtitle = "#7897ad",
                focus = "#43b6f2", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#0a1824",
                sky = "linear-gradient(180deg, #0e2638 0%, #0a1824 100%)",
                surface = "#122a3d", surfaceVariant = "#1a3a54",
                tint = "140, 200, 240", rim = "200, 235, 255",
                text = "#e8f4fc", muted = "#a8c8de", subtitle = "#8fb2ca", textGlow = "6px rgba(0, 10, 30, 0.7)",
                focus = "#6fcaff", onFocus = "#06202f", onAccent = "#ffffff",
            ),
        ),
        // crystal menus tinted in translucent color: rose, violet and aqua through the same pane
        Palette(
            name = "Crystal Stells",
            description = "Crystal menus tinted with translucent rose, violet and aqua.",
            defaultMode = "Dark",
            accent = "#b57bff", accentTop = "#ff9be0", accentMid = "#a98bff", accentBottom = "#4fd8ff",
            toggleOn = "linear-gradient(90deg, #ff9be0, #a98bff, #4fd8ff)",
            glow = "200, 140, 255",
            radius = 22, cardRadius = 20, menuRadius = 28, focusGlow = 24,
            heroOpacity = 0.5, heroBlur = 12, reflection = 0.3, reflectionFocused = 0.5,
            columns = 5, cardScale = 1.1, focusedScale = 1.12,
            clear = clear(
                bg = "#6a4fd8",
                sky = "linear-gradient(135deg, #7a5cf0 0%, #b05cd8 40%, #4f8cf0 75%, #3cc8d8 100%)",
                surface = "#7e62dc", surfaceVariant = "#6b4fc8",
                focus = "#ffffff", onFocus = "#33106b", onAccent = "#2a0f5e",
                menuTint = "linear-gradient(135deg, rgba(255, 120, 200, 0.22), rgba(130, 150, 255, 0.18) 50%, rgba(90, 235, 220, 0.2))",
            ),
            light = light(
                bg = "#f4eefc",
                sky = "linear-gradient(135deg, #fbeeff 0%, #e7e8ff 50%, #dcf6fb 100%)",
                surface = "#faf5ff", surfaceVariant = "#ebe0fb",
                ink = "#2e1f5e", muted = "#5e4d94", subtitle = "#7a6aae",
                focus = "#a97bff", onFocus = "#ffffff", onAccent = "#2a0f5e",
                menuTint = "linear-gradient(135deg, rgba(255, 150, 210, 0.4), rgba(150, 170, 255, 0.32) 50%, rgba(110, 230, 220, 0.34))",
            ),
            dark = dark(
                bg = "#1b0f4a",
                sky = "linear-gradient(135deg, #1b0f4a 0%, #3b1f7a 35%, #0f4c8a 70%, #0b8f9c 100%)",
                surface = "#2a1a66", surfaceVariant = "#3a2686",
                tint = "190, 150, 255", rim = "255, 235, 255",
                text = "#ffffff", muted = "#e6d8ff", subtitle = "#d3c0ff", textGlow = "8px rgba(60, 20, 130, 0.7)",
                focus = "#ffd6ff", onFocus = "#33106b", onAccent = "#2a0f5e",
                menuTint = "linear-gradient(135deg, rgba(255, 120, 200, 0.34), rgba(130, 150, 255, 0.28) 50%, rgba(90, 235, 220, 0.3))",
            ),
        ),
        // blueberry and lime jelly soda in a glass can in front of an aquarium: bubbly aqua blue, lime green, a Bliss hill
        Palette(
            name = "Fishes and soda",
            description = "Blueberry and lime jelly soda in an aquarium glow: fizzy blue glass, lime green accents and a grassy hill.",
            defaultMode = "Clear",
            accent = "#6fe02a", accentTop = "#d4ff78", accentMid = "#6fe02a", accentBottom = "#1fa84a",
            toggleOn = "linear-gradient(90deg, #d4ff78, #3fcf2a)",
            glow = "80, 200, 255",
            radius = 24, cardRadius = 22, menuRadius = 28, focusGlow = 22,
            heroOpacity = 0.55, heroBlur = 10, reflection = 0.35, reflectionFocused = 0.55,
            columns = 5, cardScale = 1.1, focusedScale = 1.12,
            clear = clear(
                bg = "#1572d6",
                sky = "radial-gradient(ellipse at 50% 118%, rgba(110, 224, 42, 0.85) 0%, rgba(110, 224, 42, 0) 48%), radial-gradient(ellipse at 85% 20%, rgba(120, 230, 255, 0.45) 0%, rgba(120, 230, 255, 0) 45%), linear-gradient(180deg, #0a4fb8 0%, #1a86e6 45%, #4cc3f2 78%, #8be86a 100%)",
                surface = "#2a8fe0", surfaceVariant = "#1e78c8",
                focus = "#d4ff78", onFocus = "#0b3a12", onAccent = "#0b3a12",
                menuTint = "linear-gradient(160deg, rgba(110, 224, 42, 0.2), rgba(255, 255, 255, 0.06) 45%, rgba(20, 70, 190, 0.28))",
            ),
            light = light(
                bg = "#e6f6ff",
                sky = "radial-gradient(ellipse at 50% 120%, #b9f08a 0%, rgba(185, 240, 138, 0) 50%), linear-gradient(180deg, #f4fcff 0%, #cdeeff 55%, #bdeedb 100%)",
                surface = "#f2fbff", surfaceVariant = "#d4eefa",
                ink = "#0b3550", muted = "#2f6a86", subtitle = "#4a85a2",
                focus = "#3fcf2a", onFocus = "#0b3a12", onAccent = "#0b3a12",
            ),
            dark = dark(
                bg = "#030f2e",
                sky = "radial-gradient(ellipse at 50% 120%, rgba(95, 210, 30, 0.38) 0%, rgba(95, 210, 30, 0) 52%), linear-gradient(180deg, #020a24 0%, #06235f 50%, #0a3d86 100%)",
                surface = "#08235a", surfaceVariant = "#0f3478",
                tint = "90, 170, 255", rim = "190, 255, 150",
                text = "#e8fbff", muted = "#a9d4f0", subtitle = "#8fc0e2", textGlow = "8px rgba(0, 40, 120, 0.7)",
                focus = "#b6ff5a", onFocus = "#0b3a12", onAccent = "#0b3a12",
                menuTint = "linear-gradient(160deg, rgba(110, 224, 42, 0.18), rgba(60, 130, 255, 0.22))",
            ),
        ),
        // Ramune soda bottles floating in bright turquoise water: halftone blue and white, red lettering, navy labels
        Palette(
            name = "Ramune",
            description = "Japanese ramune soda in sunlit turquoise water: bright cyan glass, red accents, navy labels and white halftone dots.",
            defaultMode = "Clear",
            accent = "#e8352b", accentTop = "#ff8d7c", accentMid = "#e8352b", accentBottom = "#a9170f",
            toggleOn = "linear-gradient(90deg, #ff8d7c, #e8352b)",
            glow = "60, 210, 255",
            radius = 22, cardRadius = 18, menuRadius = 26, focusGlow = 20,
            heroOpacity = 0.5, heroBlur = 8, reflection = 0.4, reflectionFocused = 0.6,
            columns = 5, cardScale = 1.08, focusedScale = 1.12,
            clear = clear(
                bg = "#0ab0ea",
                sky = "radial-gradient(circle at 20% 15%, rgba(255, 255, 255, 0.55) 0%, rgba(255, 255, 255, 0) 30%), radial-gradient(circle at 85% 70%, rgba(255, 255, 255, 0.35) 0%, rgba(255, 255, 255, 0) 35%), linear-gradient(160deg, #0092e0 0%, #19c4f4 50%, #7fe4ff 100%)",
                surface = "#25b9ee", surfaceVariant = "#0a9fdc",
                focus = "#ffffff", onFocus = "#14337a", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.06) 45%, rgba(20, 60, 160, 0.25))",
            ),
            light = light(
                bg = "#eefbff",
                sky = "radial-gradient(circle at 80% 10%, rgba(255, 150, 140, 0.25) 0%, rgba(255, 150, 140, 0) 40%), linear-gradient(180deg, #ffffff 0%, #d8f4ff 60%, #b6ebff 100%)",
                surface = "#f7fdff", surfaceVariant = "#d6f1fb",
                ink = "#14337a", muted = "#3a63a8", subtitle = "#5580c0",
                focus = "#e8352b", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#041a40",
                sky = "radial-gradient(circle at 80% 90%, rgba(232, 53, 43, 0.25) 0%, rgba(232, 53, 43, 0) 45%), linear-gradient(180deg, #02112e 0%, #063a7a 55%, #0a6cb0 100%)",
                surface = "#082a60", surfaceVariant = "#0f3d82",
                tint = "120, 210, 255", rim = "255, 255, 255",
                text = "#f2fbff", muted = "#b4daf2", subtitle = "#94c4e6", textGlow = "8px rgba(0, 40, 120, 0.7)",
                focus = "#ff6a5c", onFocus = "#ffffff", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(255, 90, 76, 0.16), rgba(40, 150, 255, 0.24))",
            ),
        ),
        // the pixel-art Aqua Plaza mall: Windows blue panels, glass roof, blue tile floor, green and red directory buttons
        Palette(
            name = "Aqua Plaza",
            description = "A 2000s web mall: Windows-blue directory panels, a glass atrium sky, glossy blue tile and green fountain accents.",
            defaultMode = "Light",
            accent = "#2f86e8", accentTop = "#9fcdff", accentMid = "#2f86e8", accentBottom = "#124fb0",
            toggleOn = "linear-gradient(90deg, #9fcdff, #2f86e8)",
            glow = "90, 170, 255",
            radius = 10, cardRadius = 8, menuRadius = 12, focusGlow = 16,
            heroOpacity = 0.45, heroBlur = 6, reflection = 0.3, reflectionFocused = 0.5,
            columns = 5, cardScale = 1.06, focusedScale = 1.1,
            clear = clear(
                bg = "#2a74d8",
                sky = "linear-gradient(180deg, #8fd0ff 0%, #4a9ff0 32%, #2a6fd0 62%, #1b4fa8 100%)",
                surface = "#3a86e6", surfaceVariant = "#2a70cc",
                focus = "#7be873", onFocus = "#06280a", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.05) 45%, rgba(10, 50, 150, 0.3))",
            ),
            light = light(
                bg = "#eaf3ff",
                sky = "linear-gradient(180deg, #cfe9ff 0%, #eaf3ff 35%, #d6e6fb 70%, #b8d2f5 100%)",
                surface = "#f6faff", surfaceVariant = "#dbe8fa",
                ink = "#0f2d66", muted = "#3a5f9e", subtitle = "#5a7fba",
                focus = "#2f86e8", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#061a4a",
                sky = "linear-gradient(180deg, #0a2a70 0%, #123f96 50%, #0a2a70 100%)",
                surface = "#0d2c6c", surfaceVariant = "#173f88",
                tint = "110, 170, 255", rim = "190, 225, 255",
                text = "#eef5ff", muted = "#b0c8ee", subtitle = "#90b0e0", textGlow = "6px rgba(0, 20, 90, 0.7)",
                focus = "#7be873", onFocus = "#06280a", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(123, 232, 115, 0.14), rgba(60, 130, 255, 0.24))",
            ),
        ),
        // a lit aquarium full of yellow, orange and white fish over green plants
        Palette(
            name = "Aquarium",
            description = "A glowing fish tank: teal water, lime plants, goldfish orange accents and little yellow fish.",
            defaultMode = "Clear",
            accent = "#ff8a24", accentTop = "#ffc982", accentMid = "#ff8a24", accentBottom = "#cf560a",
            toggleOn = "linear-gradient(90deg, #ffc982, #ff8a24)",
            glow = "90, 240, 210",
            radius = 22, cardRadius = 18, menuRadius = 26, focusGlow = 20,
            heroOpacity = 0.5, heroBlur = 10, reflection = 0.35, reflectionFocused = 0.55,
            columns = 5, cardScale = 1.08, focusedScale = 1.12,
            clear = clear(
                bg = "#12aeb4",
                sky = "radial-gradient(ellipse at 20% 100%, rgba(150, 240, 90, 0.7) 0%, rgba(150, 240, 90, 0) 50%), radial-gradient(ellipse at 80% 15%, rgba(255, 255, 255, 0.35) 0%, rgba(255, 255, 255, 0) 40%), linear-gradient(180deg, #0a8fc8 0%, #17c2c0 50%, #7fe6a0 100%)",
                surface = "#27c0c2", surfaceVariant = "#12a6ae",
                focus = "#ffd34a", onFocus = "#4a2300", onAccent = "#3a1a00",
                menuTint = "linear-gradient(160deg, rgba(255, 190, 90, 0.2), rgba(255, 255, 255, 0.06) 45%, rgba(0, 110, 140, 0.3))",
            ),
            light = light(
                bg = "#e6fbf4",
                sky = "radial-gradient(ellipse at 50% 120%, #b6f0a0 0%, rgba(182, 240, 160, 0) 55%), linear-gradient(180deg, #f2fffb 0%, #c8f3ea 60%, #a8e8d0 100%)",
                surface = "#f4fffb", surfaceVariant = "#d2f3e8",
                ink = "#0a3a40", muted = "#2f6b70", subtitle = "#4a8a8e",
                focus = "#ff8a24", onFocus = "#3a1a00", onAccent = "#3a1a00",
            ),
            dark = dark(
                bg = "#03242a",
                sky = "radial-gradient(ellipse at 30% 110%, rgba(120, 220, 60, 0.3) 0%, rgba(120, 220, 60, 0) 55%), linear-gradient(180deg, #021a24 0%, #06404a 55%, #0a6a6a 100%)",
                surface = "#08343c", surfaceVariant = "#0f4a54",
                tint = "100, 230, 210", rim = "200, 255, 200",
                text = "#effffb", muted = "#a8e2d8", subtitle = "#88c8c0", textGlow = "8px rgba(0, 60, 70, 0.7)",
                focus = "#ffb347", onFocus = "#3a1a00", onAccent = "#3a1a00",
                menuTint = "linear-gradient(160deg, rgba(255, 150, 50, 0.16), rgba(30, 200, 190, 0.2))",
            ),
        ),
        // goldfish in clear plastic: a see-through Game Boy, cassettes, bags of water and a rainbow sheen
        Palette(
            name = "Goldfish Y2K",
            description = "Translucent plastic and goldfish: a see-through handheld, iridescent cassettes, peach and pale blue with a rainbow sheen.",
            defaultMode = "Light",
            accent = "#ff8b5c", accentTop = "#ffd2b8", accentMid = "#ff8b5c", accentBottom = "#e0572a",
            toggleOn = "linear-gradient(90deg, #ffd2b8, #ff8b5c)",
            glow = "255, 200, 170",
            radius = 18, cardRadius = 14, menuRadius = 22, focusGlow = 16,
            heroOpacity = 0.4, heroBlur = 12, reflection = 0.45, reflectionFocused = 0.6,
            columns = 5, cardScale = 1.06, focusedScale = 1.1,
            clear = clear(
                bg = "#9cc8e6",
                sky = "linear-gradient(135deg, rgba(255, 214, 190, 0.7) 0%, rgba(214, 236, 255, 0.5) 35%, rgba(244, 214, 255, 0.5) 65%, rgba(255, 236, 200, 0.6) 100%), linear-gradient(180deg, #8ec2e8 0%, #b6d8ee 100%)",
                surface = "#b2d4ea", surfaceVariant = "#9ac4e0",
                focus = "#ff8b5c", onFocus = "#4a1d08", onAccent = "#4a1d08",
                menuTint = "linear-gradient(160deg, rgba(255, 190, 150, 0.22), rgba(255, 255, 255, 0.1) 45%, rgba(190, 150, 255, 0.16))",
            ),
            light = light(
                bg = "#f8f2ea",
                sky = "linear-gradient(150deg, #fdeede 0%, #e3f0fa 45%, #f6dff0 75%, #fff0d6 100%)",
                surface = "#fffaf4", surfaceVariant = "#efe4da",
                ink = "#4a3328", muted = "#7a5f50", subtitle = "#9a8070",
                focus = "#ff8b5c", onFocus = "#4a1d08", onAccent = "#4a1d08",
            ),
            dark = dark(
                bg = "#1d1a24",
                sky = "linear-gradient(150deg, #2a1f30 0%, #1c2a3e 50%, #30212a 100%)",
                surface = "#2a2430", surfaceVariant = "#383040",
                tint = "255, 200, 170", rim = "255, 225, 205",
                text = "#fff4ea", muted = "#dcc4b4", subtitle = "#bca494", textGlow = "6px rgba(60, 20, 10, 0.6)",
                focus = "#ffa07a", onFocus = "#3a1408", onAccent = "#3a1408",
                menuTint = "linear-gradient(160deg, rgba(255, 150, 110, 0.14), rgba(150, 190, 255, 0.16))",
            ),
        ),
        // hibiscus flowers, strawberries and sparkles over a bright tropical sea
        Palette(
            name = "Hibiscus",
            description = "A tropical sea: sky blue and turquoise glass, hot pink hibiscus accents, strawberry pastels and sparkles.",
            defaultMode = "Clear",
            accent = "#ff4f9a", accentTop = "#ffb0d4", accentMid = "#ff4f9a", accentBottom = "#cf2272",
            toggleOn = "linear-gradient(90deg, #ffb0d4, #ff4f9a)",
            glow = "255, 160, 205",
            radius = 26, cardRadius = 22, menuRadius = 30, focusGlow = 22,
            heroOpacity = 0.5, heroBlur = 8, reflection = 0.4, reflectionFocused = 0.6,
            columns = 5, cardScale = 1.08, focusedScale = 1.12,
            clear = clear(
                bg = "#1d9ad8",
                sky = "radial-gradient(circle at 78% 18%, rgba(255, 120, 180, 0.35) 0%, rgba(255, 120, 180, 0) 28%), radial-gradient(circle at 15% 85%, rgba(255, 170, 200, 0.3) 0%, rgba(255, 170, 200, 0) 30%), linear-gradient(180deg, #2b6fe0 0%, #6fb4f2 38%, #2fc4d8 56%, #0f9fbf 100%)",
                surface = "#2fb0e0", surfaceVariant = "#1c96cc",
                focus = "#ffe0ef", onFocus = "#6b0f3a", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(255, 150, 200, 0.22), rgba(255, 255, 255, 0.08) 45%, rgba(10, 100, 190, 0.26))",
            ),
            light = light(
                bg = "#e9f8ff",
                sky = "radial-gradient(circle at 85% 10%, rgba(255, 150, 200, 0.3) 0%, rgba(255, 150, 200, 0) 38%), linear-gradient(180deg, #d4ecff 0%, #bfeaf6 55%, #9fe0ee 100%)",
                surface = "#f6fdff", surfaceVariant = "#d4eef8",
                ink = "#0c3a5c", muted = "#336a8e", subtitle = "#5588a8",
                focus = "#ff4f9a", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#04243f",
                sky = "radial-gradient(circle at 80% 15%, rgba(255, 90, 160, 0.22) 0%, rgba(255, 90, 160, 0) 38%), linear-gradient(180deg, #03183a 0%, #06406e 55%, #0a7a98 100%)",
                surface = "#08365a", surfaceVariant = "#0f4a74",
                tint = "130, 220, 240", rim = "255, 210, 230",
                text = "#f2fcff", muted = "#b4e0ee", subtitle = "#94c8dc", textGlow = "8px rgba(0, 50, 90, 0.7)",
                focus = "#ff7ab8", onFocus = "#4a0828", onAccent = "#ffffff",
                menuTint = "linear-gradient(160deg, rgba(255, 90, 160, 0.16), rgba(40, 190, 220, 0.2))",
            ),
        ),
        // the water dashboard: round glass tiles on an aquatic wallpaper (put a water video or GIF behind it)
        Palette(
            name = "Xbox Aero",
            description = "Console-dashboard layout in aqua glass: a wide banner first, covers low on the screen, the title centered above and bubbly teal crystal. Best over a water video or GIF.",
            defaultMode = "Clear",
            accent = "#3fe0d0", accentTop = "#b8fff4", accentMid = "#3fe0d0", accentBottom = "#1a9fc0",
            toggleOn = "linear-gradient(90deg, #b8fff4, #29c6d8)",
            glow = "120, 230, 255",
            radius = 26, cardRadius = 20, menuRadius = 30, focusGlow = 18,
            heroOpacity = 0.7, heroBlur = 6, reflection = 0.25, reflectionFocused = 0.4,
            columns = 6, cardScale = 0.95, focusedScale = 1.3,
            dash = true,
            clear = clear(
                bg = "#0f6f8f",
                sky = "radial-gradient(ellipse at 50% 38%, rgba(190, 250, 255, 0.45) 0%, rgba(190, 250, 255, 0) 55%), linear-gradient(180deg, #0b7aa0 0%, #1596b4 50%, #0a5f86 100%)",
                surface = "#1a8aa8", surfaceVariant = "#137594",
                focus = "#d9fffb", onFocus = "#04343c", onAccent = "#04343c",
                menuTint = "linear-gradient(160deg, rgba(170, 245, 255, 0.22), rgba(255, 255, 255, 0.06) 45%, rgba(10, 90, 130, 0.3))",
            ),
            light = light(
                bg = "#e3fbff",
                sky = "radial-gradient(ellipse at 50% 40%, #ffffff 0%, rgba(255, 255, 255, 0) 60%), linear-gradient(180deg, #d4f6ff 0%, #a9e8f2 100%)",
                surface = "#f0fdff", surfaceVariant = "#cdf0f7",
                ink = "#06394a", muted = "#2b6c7e", subtitle = "#4a8a9c",
                focus = "#1fb8c8", onFocus = "#04343c", onAccent = "#04343c",
            ),
            dark = dark(
                bg = "#021a26",
                sky = "radial-gradient(ellipse at 50% 40%, rgba(60, 190, 210, 0.3) 0%, rgba(60, 190, 210, 0) 58%), linear-gradient(180deg, #021722 0%, #053a52 100%)",
                surface = "#06303f", surfaceVariant = "#0b4358",
                tint = "90, 210, 230", rim = "190, 255, 250",
                text = "#e6fdff", muted = "#a5d8e0", subtitle = "#86c0cc", textGlow = "6px rgba(0, 40, 60, 0.7)",
                focus = "#8ff5ea", onFocus = "#04343c", onAccent = "#04343c",
                menuTint = "linear-gradient(160deg, rgba(100, 230, 240, 0.18), rgba(30, 120, 170, 0.25))",
            ),
        ),
        // Ramune bottles under sunlit water: cyan glass, dotted white and navy labels, a red accent
        Palette(
            name = "Ramune",
            description = "Ramune bottles in sunlit water: bright cyan glass with caustic light, navy and white dotted labels and a red accent.",
            defaultMode = "Clear",
            accent = "#ec3a45", accentTop = "#ff8a90", accentMid = "#ec3a45", accentBottom = "#b5182a",
            toggleOn = "linear-gradient(90deg, #ff8a90, #e02a3a)",
            glow = "40, 200, 255",
            radius = 22, cardRadius = 18, menuRadius = 26, focusGlow = 20,
            heroOpacity = 0.55, heroBlur = 8, reflection = 0.45, reflectionFocused = 0.65,
            columns = 5, cardScale = 1.05, focusedScale = 1.14,
            clear = clear(
                bg = "#0e9fd8",
                sky = "radial-gradient(ellipse at 20% 15%, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0) 40%), radial-gradient(ellipse at 80% 70%, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 45%), linear-gradient(160deg, #35d4f2 0%, #0fa8e0 45%, #0765c0 100%)",
                surface = "#1fb4e6", surfaceVariant = "#1099d2",
                focus = "#ffffff", onFocus = "#0a2a6a", onAccent = "#0a2a6a",
                menuTint = "linear-gradient(160deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.06) 45%, rgba(10, 70, 170, 0.3))",
            ),
            light = light(
                bg = "#eaf9ff",
                sky = "radial-gradient(ellipse at 20% 10%, #ffffff 0%, rgba(255, 255, 255, 0) 50%), linear-gradient(160deg, #f4fdff 0%, #bfeaf8 100%)",
                surface = "#f5fcff", surfaceVariant = "#d2effa",
                ink = "#0b2f7a", muted = "#2a5a9e", subtitle = "#4a7ab8",
                focus = "#ec3a45", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#031c42",
                sky = "radial-gradient(ellipse at 25% 15%, rgba(60, 200, 240, 0.3) 0%, rgba(60, 200, 240, 0) 50%), linear-gradient(170deg, #02183a 0%, #064a8c 100%)",
                surface = "#06285a", surfaceVariant = "#0b3a78",
                tint = "90, 190, 255", rim = "200, 240, 255",
                text = "#eaf8ff", muted = "#a9d4f0", subtitle = "#8fbfe6", textGlow = "6px rgba(0, 30, 90, 0.7)",
                focus = "#ff7a84", onFocus = "#2a0408", onAccent = "#2a0408",
                menuTint = "linear-gradient(160deg, rgba(236, 58, 69, 0.16), rgba(40, 140, 255, 0.22))",
            ),
        ),
        // the Y2K mall atrium: cobalt kiosk panels, blue tiled floor, a skylight, gold and green list chips
        Palette(
            name = "Aqua Plaza",
            description = "Y2K mall atrium: cobalt kiosk panels with squarer corners, a tiled-floor sheen, a skylight glow and gold highlights.",
            defaultMode = "Dark",
            accent = "#ffc93a", accentTop = "#ffe58a", accentMid = "#ffc93a", accentBottom = "#d98e10",
            toggleOn = "linear-gradient(90deg, #ffe58a, #f0a818)",
            glow = "90, 170, 255",
            radius = 10, cardRadius = 8, menuRadius = 12, focusGlow = 14,
            heroOpacity = 0.5, heroBlur = 4, reflection = 0.3, reflectionFocused = 0.5,
            columns = 5, cardScale = 1.0, focusedScale = 1.1,
            clear = clear(
                bg = "#164fc0",
                sky = "radial-gradient(ellipse at 50% 0%, rgba(190, 235, 255, 0.55) 0%, rgba(190, 235, 255, 0) 50%), repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.06) 0px, rgba(255, 255, 255, 0.06) 2px, rgba(255, 255, 255, 0) 2px, rgba(255, 255, 255, 0) 48px), linear-gradient(180deg, #5bb8f0 0%, #2a78d8 40%, #123f9e 100%)",
                surface = "#2063cf", surfaceVariant = "#1a52b5",
                focus = "#ffe58a", onFocus = "#3a2600", onAccent = "#3a2600",
                menuTint = "linear-gradient(160deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.05) 45%, rgba(10, 40, 130, 0.35))",
            ),
            light = light(
                bg = "#e8f3ff",
                sky = "radial-gradient(ellipse at 50% 0%, #ffffff 0%, rgba(255, 255, 255, 0) 55%), linear-gradient(180deg, #f2f8ff 0%, #bcd8f6 100%)",
                surface = "#f4f9ff", surfaceVariant = "#d3e5fa",
                ink = "#0b2a66", muted = "#2c568f", subtitle = "#4a76b0",
                focus = "#2a78d8", onFocus = "#ffffff", onAccent = "#ffffff",
            ),
            dark = dark(
                bg = "#06163f",
                sky = "radial-gradient(ellipse at 50% 0%, rgba(120, 190, 255, 0.28) 0%, rgba(120, 190, 255, 0) 50%), linear-gradient(180deg, #0a2260 0%, #050f33 100%)",
                surface = "#0b2a70", surfaceVariant = "#123888",
                tint = "110, 170, 255", rim = "190, 225, 255",
                text = "#eaf3ff", muted = "#a9c6ee", subtitle = "#8aaede", textGlow = "4px rgba(0, 20, 80, 0.7)",
                focus = "#ffd45c", onFocus = "#3a2600", onAccent = "#3a2600",
                menuTint = "linear-gradient(160deg, rgba(255, 201, 58, 0.14), rgba(40, 100, 230, 0.25))",
            ),
        ),
        // the fish tank: teal and green water with caustics, goldfish orange accents
        Palette(
            name = "Aquarium",
            description = "Fish-tank glass: teal and algae-green water, bright caustic light and goldfish-orange accents.",
            defaultMode = "Clear",
            accent = "#ff8a2a", accentTop = "#ffc27a", accentMid = "#ff8a2a", accentBottom = "#d4560a",
            toggleOn = "linear-gradient(90deg, #ffc27a, #ff7a1a)",
            glow = "120, 255, 210",
            radius = 24, cardRadius = 20, menuRadius = 28, focusGlow = 20,
            heroOpacity = 0.55, heroBlur = 10, reflection = 0.4, reflectionFocused = 0.6,
            columns = 5, cardScale = 1.08, focusedScale = 1.12,
            clear = clear(
                bg = "#0c9fa8",
                sky = "radial-gradient(ellipse at 70% 10%, rgba(200, 255, 230, 0.5) 0%, rgba(200, 255, 230, 0) 45%), radial-gradient(ellipse at 15% 100%, rgba(110, 220, 110, 0.6) 0%, rgba(110, 220, 110, 0) 55%), linear-gradient(180deg, #19c4d8 0%, #0e9fb0 50%, #3fb98a 100%)",
                surface = "#16aab5", surfaceVariant = "#0e8f9c",
                focus = "#ffd9a8", onFocus = "#4a2200", onAccent = "#4a2200",
                menuTint = "linear-gradient(160deg, rgba(190, 255, 220, 0.22), rgba(255, 255, 255, 0.06) 45%, rgba(10, 110, 130, 0.3))",
            ),
            light = light(
                bg = "#e6fbf6",
                sky = "radial-gradient(ellipse at 70% 0%, #ffffff 0%, rgba(255, 255, 255, 0) 50%), linear-gradient(180deg, #f2fffb 0%, #b6ecdc 100%)",
                surface = "#f2fffb", surfaceVariant = "#cdf3e8",
                ink = "#08413f", muted = "#2b6f6a", subtitle = "#4a8f88",
                focus = "#ff8a2a", onFocus = "#4a2200", onAccent = "#4a2200",
            ),
            dark = dark(
                bg = "#02242b",
                sky = "radial-gradient(ellipse at 70% 5%, rgba(90, 230, 200, 0.28) 0%, rgba(90, 230, 200, 0) 50%), linear-gradient(180deg, #02262e 0%, #04464a 100%)",
                surface = "#06383e", surfaceVariant = "#0a4c52",
                tint = "90, 230, 200", rim = "190, 255, 235",
                text = "#e6fff8", muted = "#a5dccf", subtitle = "#86c4b8", textGlow = "6px rgba(0, 50, 50, 0.7)",
                focus = "#ffa95a", onFocus = "#3a1a00", onAccent = "#3a1a00",
                menuTint = "linear-gradient(160deg, rgba(255, 138, 42, 0.14), rgba(40, 190, 170, 0.22))",
            ),
        ),
        // clear plastic: a see-through Game Boy and cassette, pastel peach and ice blue, goldfish orange
        Palette(
            name = "Goldfish Y2K",
            description = "Clear-plastic electronics with goldfish inside: pastel peach and ice-blue translucent panels, chunky corners and an orange accent.",
            defaultMode = "Light",
            accent = "#ff9a4d", accentTop = "#ffd0a0", accentMid = "#ff9a4d", accentBottom = "#e0661a",
            toggleOn = "linear-gradient(90deg, #ffd0a0, #ff8a3a)",
            glow = "255, 190, 140",
            radius = 14, cardRadius = 12, menuRadius = 16, focusGlow = 14,
            heroOpacity = 0.45, heroBlur = 6, reflection = 0.3, reflectionFocused = 0.5,
            columns = 5, cardScale = 1.0, focusedScale = 1.1,
            clean = true,
            clear = clear(
                bg = "#8cc6e8",
                sky = "radial-gradient(ellipse at 20% 0%, rgba(255, 236, 214, 0.7) 0%, rgba(255, 236, 214, 0) 50%), linear-gradient(160deg, #cfe9f6 0%, #8ac4e6 55%, #f3c8a4 100%)",
                surface = "#a6d2ea", surfaceVariant = "#8cc2e0",
                focus = "#ffffff", onFocus = "#5a2a00", onAccent = "#5a2a00",
                menuTint = "linear-gradient(160deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.08) 45%, rgba(120, 180, 220, 0.25))",
            ),
            light = light(
                bg = "#f4f9fc",
                sky = "radial-gradient(ellipse at 20% 0%, #fff3e6 0%, rgba(255, 243, 230, 0) 50%), linear-gradient(160deg, #f7fbfe 0%, #cfe6f3 60%, #fbe2cc 100%)",
                surface = "#fbfdff", surfaceVariant = "#e0eef6",
                ink = "#26485e", muted = "#4f7488", subtitle = "#6b90a4",
                focus = "#ff9a4d", onFocus = "#4a2200", onAccent = "#4a2200",
            ),
            dark = dark(
                bg = "#12202e",
                sky = "radial-gradient(ellipse at 20% 0%, rgba(255, 170, 110, 0.2) 0%, rgba(255, 170, 110, 0) 50%), linear-gradient(160deg, #14283a 0%, #22405a 100%)",
                surface = "#1b3348", surfaceVariant = "#264560",
                tint = "150, 200, 235", rim = "255, 225, 200",
                text = "#f3f9fd", muted = "#b4cadb", subtitle = "#98b2c6", textGlow = "4px rgba(0, 20, 40, 0.6)",
                focus = "#ffb27a", onFocus = "#3a1a00", onAccent = "#3a1a00",
                menuTint = "linear-gradient(160deg, rgba(255, 154, 77, 0.14), rgba(120, 180, 230, 0.2))",
            ),
        ),
        // a tropical sea with hibiscus and strawberries: sky blue, turquoise water, hot pink accents and sparkles
        Palette(
            name = "Hibiscus Sea",
            description = "Tropical sea with hibiscus flowers: summer-sky blue over turquoise water, hot pink accents and pearly glass.",
            defaultMode = "Clear",
            accent = "#ff4fa0", accentTop = "#ff9ccb", accentMid = "#ff4fa0", accentBottom = "#d41a78",
            toggleOn = "linear-gradient(90deg, #ff9ccb, #f02a8a)",
            glow = "255, 150, 210",
            radius = 26, cardRadius = 22, menuRadius = 30, focusGlow = 22,
            heroOpacity = 0.5, heroBlur = 8, reflection = 0.4, reflectionFocused = 0.6,
            columns = 5, cardScale = 1.08, focusedScale = 1.14,
            clear = clear(
                bg = "#1a7fe0",
                sky = "radial-gradient(ellipse at 20% 12%, rgba(255, 255, 255, 0.55) 0%, rgba(255, 255, 255, 0) 38%), radial-gradient(ellipse at 80% 20%, rgba(255, 150, 210, 0.3) 0%, rgba(255, 150, 210, 0) 40%), linear-gradient(180deg, #2c7af0 0%, #4fb0f2 38%, #22c4cc 55%, #0aa8b8 100%)",
                surface = "#2a9ad8", surfaceVariant = "#1e86c4",
                focus = "#ffd0e8", onFocus = "#5a0a36", onAccent = "#5a0a36",
                menuTint = "linear-gradient(160deg, rgba(255, 220, 240, 0.24), rgba(255, 255, 255, 0.07) 45%, rgba(20, 110, 200, 0.28))",
            ),
            light = light(
                bg = "#eaf6ff",
                sky = "radial-gradient(ellipse at 20% 0%, #ffffff 0%, rgba(255, 255, 255, 0) 50%), linear-gradient(180deg, #f2f9ff 0%, #bfe6f2 60%, #ffdcec 100%)",
                surface = "#f5faff", surfaceVariant = "#d4e9f7",
                ink = "#0b3560", muted = "#2f6490", subtitle = "#4f84b0",
                focus = "#ff4fa0", onFocus = "#5a0a36", onAccent = "#5a0a36",
            ),
            dark = dark(
                bg = "#04163a",
                sky = "radial-gradient(ellipse at 80% 0%, rgba(255, 110, 190, 0.22) 0%, rgba(255, 110, 190, 0) 45%), linear-gradient(180deg, #051a48 0%, #07406a 55%, #055a6a 100%)",
                surface = "#082a5a", surfaceVariant = "#0e3b78",
                tint = "100, 190, 255", rim = "255, 210, 235",
                text = "#eef8ff", muted = "#acd2ee", subtitle = "#90bde0", textGlow = "6px rgba(0, 30, 90, 0.7)",
                focus = "#ff8cc4", onFocus = "#3a0a22", onAccent = "#3a0a22",
                menuTint = "linear-gradient(160deg, rgba(255, 79, 160, 0.16), rgba(40, 150, 240, 0.22))",
            ),
        ),
    )
}
