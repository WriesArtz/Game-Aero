package app.gamenative.ui.screen.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.MenuColorScope
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.ThemedAlertDialog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A color wheel: hue around, saturation outwards, a slider for brightness and a hex code, with a preview. [initial] is
 * ARGB (0 starts from a soft cyan); [onPick] gets an opaque ARGB color.
 */
@Composable
internal fun ColorWheelDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(if (initial == 0) 0xFF5FE6FF.toInt() else initial, out)
        out
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    var hex by remember { mutableStateOf("") }
    var hexEdited by remember { mutableStateOf(false) }
    // the hex field follows the wheel until the person types in it
    val shownHex = if (hexEdited) hex else String.format("%06X", argb and 0xFFFFFF)

    fun pick(p: Offset, radius: Float, center: Offset) {
        val dx = p.x - center.x
        val dy = p.y - center.y
        val deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        hue = (deg + 360f) % 360f
        sat = min(sqrt(dx * dx + dy * dy) / radius, 1f)
        hexEdited = false
    }

    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(R.string.color_wheel_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { p ->
                                    pick(p, size.width / 2f, Offset(size.width / 2f, size.height / 2f))
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    pick(change.position, size.width / 2f, Offset(size.width / 2f, size.height / 2f))
                                }
                            },
                    ) {
                        val r = size.width / 2f
                        val c = Offset(r, r)
                        // hue around the wheel, then white fading in towards the middle for saturation
                        drawCircle(
                            brush = Brush.sweepGradient(
                                (0..6).map { Color.hsv((it * 60f) % 360f, 1f, 1f) },
                                center = c,
                            ),
                            radius = r,
                            center = c,
                        )
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center = c, radius = r),
                            radius = r,
                            center = c,
                        )
                        // the brightness chosen darkens the whole wheel, so what is shown is what will be picked
                        drawCircle(Color.Black.copy(alpha = 1f - value), r, c)
                        val rad = Math.toRadians(hue.toDouble())
                        val dot = Offset(c.x + cos(rad).toFloat() * sat * r, c.y + sin(rad).toFloat() * sat * r)
                        drawCircle(Color.White, 9.dp.toPx(), dot, style = Stroke(width = 3.dp.toPx()))
                        drawCircle(Color(argb), 6.dp.toPx(), dot)
                    }
                    Text(text = stringResource(R.string.color_wheel_brightness), style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            hexEdited = false
                        },
                        valueRange = 0f..1f,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                        OutlinedTextField(
                            value = shownHex,
                            onValueChange = { text ->
                                val clean = text.trimStart('#').filter { it.isLetterOrDigit() }.take(6).uppercase()
                                hex = clean
                                hexEdited = true
                                if (clean.length == 6 && clean.all { it in "0123456789ABCDEF" }) {
                                    val out = FloatArray(3)
                                    android.graphics.Color.colorToHSV(0xFF000000.toInt() or clean.toInt(16), out)
                                    hue = out[0]
                                    sat = out[1]
                                    value = out[2]
                                }
                            },
                            label = { Text(stringResource(R.string.color_wheel_hex)) },
                            prefix = { Text("#") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            },
            dismissButton = {
                RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                RingTextButton(onClick = { onPick(argb or 0xFF000000.toInt()) }) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}
