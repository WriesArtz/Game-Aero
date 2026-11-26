package app.gamenative.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.AeroStyle
import app.gamenative.ui.theme.MenuColorScope
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.ThemedAlertDialog
import kotlin.math.roundToInt

/** Colors offered for the glass tint, the text and the selection. */
private val AeroSwatches: List<Int> = listOf(
    0xFFFFFFFF, 0xFF8FF5EA, 0xFFB6FF5A, 0xFF7FB8FF, 0xFFC8A8FF, 0xFFFF9EC8, 0xFFFFB86B, 0xFF0B1D2A,
).map { it.toInt() }

/**
 * Looks of the dashboard glass: how clear, glossy and outlined it is, its tint, the text and selection colors, and
 * the size of the whole UI. Changes apply at once and are kept.
 */
@Composable
fun AeroStyleDialog(onDismiss: () -> Unit) {
    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(R.string.aero_style_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    AeroStyleControls()
                }
            },
            dismissButton = {
                RingTextButton(onClick = { AeroStyle.reset() }) { Text(stringResource(R.string.aero_style_reset)) }
            },
            confirmButton = {
                RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            },
        )
    }
}

/** The sliders and swatches of the dashboard glass; used by the dialog and by the Style tab of the themes. */
@Composable
internal fun AeroStyleControls(modifier: Modifier = Modifier) {
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
            Text(
                text = stringResource(R.string.aero_style_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            StyleSlider(
                label = stringResource(R.string.aero_style_opacity),
                value = style.opacity,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(opacity = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.aero_style_gloss),
                value = style.gloss,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(gloss = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.aero_style_outline),
                value = style.outline,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(outline = v) } },
            )
            SwatchRow(
                label = stringResource(R.string.aero_style_tint),
                selected = style.tint,
                onPick = { c -> AeroStyle.update { it.copy(tint = c) } },
            )
            SwatchRow(
                label = stringResource(R.string.aero_style_text),
                selected = style.text,
                onPick = { c -> AeroStyle.update { it.copy(text = c) } },
            )
            SwatchRow(
                label = stringResource(R.string.aero_style_selection),
                selected = style.selection,
                onPick = { c -> AeroStyle.update { it.copy(selection = c) } },
            )
            Text(
                text = stringResource(R.string.cover_style_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 10.dp),
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_art),
                value = style.coverArt,
                range = 20..100,
                onChange = { v -> AeroStyle.update { it.copy(coverArt = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_fx),
                value = style.coverFx,
                range = 0..250,
                onChange = { v -> AeroStyle.update { it.copy(coverFx = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_frame_alpha),
                value = style.frameAlpha,
                range = 0..100,
                onChange = { v -> AeroStyle.update { it.copy(frameAlpha = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_frame_width),
                value = style.frameWidth,
                range = 30..300,
                onChange = { v -> AeroStyle.update { it.copy(frameWidth = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_frame_light),
                value = style.frameLight,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(frameLight = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_glow),
                value = style.coverGlow,
                range = 0..250,
                onChange = { v -> AeroStyle.update { it.copy(coverGlow = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_reflection),
                value = style.coverReflection,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(coverReflection = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.cover_style_motion),
                value = style.coverMotion,
                range = 0..200,
                onChange = { v -> AeroStyle.update { it.copy(coverMotion = v) } },
            )
            StyleSlider(
                label = stringResource(R.string.aero_style_scale),
                value = style.uiScale,
                range = 70..130,
                step = 5,
                onChange = { v -> AeroStyle.update { it.copy(uiScale = v) } },
            )
    }
}

@Composable
private fun StyleSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    step: Int = 5,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$value%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(((it / step).roundToInt() * step).coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / step - 1,
        )
    }
}

@Composable
private fun SwatchRow(label: String, selected: Int, onPick: (Int) -> Unit) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // automatic: whatever the theme or the design asks for
            Swatch(
                color = MaterialTheme.colorScheme.surfaceVariant,
                chosen = selected == 0,
                label = stringResource(R.string.aero_style_auto),
                onClick = { onPick(0) },
            )
            AeroSwatches.forEach { argb ->
                Swatch(color = Color(argb), chosen = selected == argb, label = null, onClick = { onPick(argb) })
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, chosen: Boolean, label: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (chosen) 3.dp else 1.dp,
                color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
