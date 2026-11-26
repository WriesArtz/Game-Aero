package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import app.gamenative.ui.theme.FX_BUBBLES
import app.gamenative.ui.theme.FX_IRIDESCENT
import app.gamenative.ui.theme.FX_SABER
import app.gamenative.ui.theme.FX_SHINE
import app.gamenative.ui.theme.FX_WATER
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dashboard's running animations, shared by every cover so that one clock drives them all. The values are read
 * only while drawing, so a tick redraws the current cover instead of recomposing the screen.
 */
internal class AeroFx(val pulse: State<Float>, val shine: State<Float>, val spin: State<Float>, val drift: State<Float>)

@Composable
internal fun rememberAeroFx(): AeroFx {
    val clock = rememberInfiniteTransition(label = "aeroFx")
    val pulse = clock.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val shine = clock.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "shine")
    val spin = clock.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "spin")
    val drift = clock.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "drift")
    return remember(pulse, shine, spin, drift) { AeroFx(pulse, shine, spin, drift) }
}

private const val TWO_PI = 6.2831855f

/** A four-point star at [c], [r] long, as in a glint of light. */
internal fun DrawScope.drawStar(c: Offset, r: Float, color: Color, width: Float) {
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - r * 0.45f, c.y - r * 0.45f), Offset(c.x + r * 0.45f, c.y + r * 0.45f), strokeWidth = width * 0.6f, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - r * 0.45f, c.y + r * 0.45f), Offset(c.x + r * 0.45f, c.y - r * 0.45f), strokeWidth = width * 0.6f, cap = StrokeCap.Round)
}

/** A bubble: a faint body, a bright rim and a highlight at its upper left. */
internal fun DrawScope.drawBubble(c: Offset, r: Float, color: Color, alpha: Float) {
    val a = alpha.coerceIn(0f, 1f)
    drawCircle(color.copy(alpha = 0.12f * a), r, c)
    drawCircle(color.copy(alpha = 0.7f * a), r, c, style = Stroke(width = (r * 0.12f).coerceAtLeast(1f)))
    drawCircle(Color.White.copy(alpha = 0.85f * a), r * 0.2f, Offset(c.x - r * 0.35f, c.y - r * 0.38f))
}

/**
 * Four-point stars that twinkle around the current cover, at its sides; for the shine and iridescent effects.
 */
internal fun DrawScope.drawSparkles(color: Color?, fx: AeroFx, level: Float, pad: Float) {
    val tw = fx.spin.value * (TWO_PI / 360f)
    val c = color ?: Color.White
    for (i in 0 until 7) {
        val ang = i * 0.8975979f + tw * 0.25f
        val rx = size.width * 0.5f + pad
        val ry = size.height * 0.5f + pad
        val p = Offset(size.width * 0.5f + cos(ang) * rx, size.height * 0.5f + sin(ang) * ry)
        val alpha = ((sin(tw * 3f + i * 1.3f) * 0.5f + 0.5f) * level).coerceIn(0f, 1f)
        drawStar(p, pad * (0.25f + 0.1f * (i % 3)), c.copy(alpha = alpha), pad * 0.12f)
    }
}

/**
 * The moving effect inside the art of the current cover: a band of light (shine), ripples and a rolling water line
 * (water) or bubbles rising (bubbles). The light saber and the iridescent effects live on the rim instead.
 */
internal fun DrawScope.drawSurfaceEffect(mode: Int, color: Color?, fx: AeroFx, level: Float) {
    val w = size.width
    val h = size.height
    when (mode) {
        FX_SHINE -> {
            val t = fx.shine.value
            val band = w * 0.32f
            val skew = h * 0.35f
            val x = (-0.5f + 2f * t) * w
            val sweep = Path().apply {
                moveTo(x + skew, 0f)
                lineTo(x + skew + band, 0f)
                lineTo(x + band, h)
                lineTo(x, h)
                close()
            }
            val c = color ?: Color.White
            drawPath(
                path = sweep,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, c.copy(alpha = (0.5f * level).coerceIn(0f, 1f)), Color.Transparent),
                    startX = x,
                    endX = x + band + skew,
                ),
                blendMode = BlendMode.Screen,
            )
        }
        FX_WATER -> {
            val c = color ?: Color(0xFF8FE8FF)
            val phase = fx.spin.value * (TWO_PI / 360f)
            // ripples spreading from the lower middle of the cover
            for (i in 0..1) {
                val t = (fx.drift.value * 4f + i * 0.5f) % 1f
                drawCircle(
                    color = c.copy(alpha = ((1f - t) * 0.55f * level).coerceIn(0f, 1f)),
                    radius = t * maxOf(w, h) * 0.7f,
                    center = Offset(w * 0.5f, h * 0.7f),
                    style = Stroke(width = 2f + 3f * (1f - t)),
                )
            }
            // a rolling water line at the foot of the cover
            val base = h * 0.82f
            val amp = h * 0.025f
            val wave = Path().apply {
                moveTo(0f, base)
                var x = 0f
                while (x <= w) {
                    lineTo(x, base + sin(x / w * TWO_PI * 1.5f + phase) * amp)
                    x += w / 24f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                wave,
                Brush.verticalGradient(
                    listOf(c.copy(alpha = (0.45f * level).coerceIn(0f, 1f)), c.copy(alpha = (0.2f * level).coerceIn(0f, 1f))),
                    startY = base - amp,
                    endY = h,
                ),
                blendMode = BlendMode.Screen,
            )
        }
        FX_BUBBLES -> {
            val c = color ?: Color.White
            for (i in 0 until 8) {
                val seed = i * 0.6180339f
                val k = 2 + i % 3
                val y = h * (1.1f - ((fx.drift.value * k + seed) % 1f) * 1.2f)
                val x = (((seed * 5.7f) % 1f) * 0.8f + 0.1f) * w + sin(fx.drift.value * TWO_PI * k + i) * w * 0.03f
                drawBubble(Offset(x, y), minOf(w, h) * (0.04f + 0.015f * (i % 3)), c, level.coerceAtMost(1.4f))
            }
        }
    }
}

/**
 * The moving effect on the rim of the current cover: a light saber, a blade of light that runs round the edge with
 * a glow behind it, or an iridescent ring whose colors travel. Other modes draw nothing here.
 */
internal fun DrawScope.drawRimEffect(mode: Int, color: Color?, fx: AeroFx, level: Float, radius: Float, inset: Float, thickness: Float) {
    val w = size.width
    val h = size.height
    when (mode) {
        FX_IRIDESCENT -> {
            val corner = CornerRadius(radius)
            val ring = Path().apply {
                fillType = PathFillType.EvenOdd
                addRoundRect(RoundRect(0f, 0f, w, h, corner))
                addRoundRect(RoundRect(thickness, thickness, w - thickness, h - thickness, CornerRadius((radius - thickness).coerceAtLeast(0f))))
            }
            clipPath(ring) {
                rotate(fx.spin.value) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF5FE6FF), Color.White, Color(0xFFFF9CE8), Color(0xFFFFE08A), Color(0xFF5FE6FF)),
                        ),
                        radius = maxOf(w, h),
                        alpha = level.coerceIn(0f, 1f),
                    )
                }
            }
        }
        FX_SABER -> {
            val c = color ?: Color(0xFF5FE6FF)
            val path = Path().apply {
                addRoundRect(RoundRect(inset, inset, w - inset, h - inset, CornerRadius((radius - inset).coerceAtLeast(0f))))
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val total = measure.length
            if (total <= 0f) return
            // the whole edge glows faintly, like a blade at rest
            drawPath(path, c.copy(alpha = (0.35f * level).coerceIn(0f, 1f)), style = Stroke(width = thickness))
            // a long bright blade runs round it
            val len = total * 0.3f
            val start = fx.shine.value * total
            val pieces = if (start + len <= total) {
                listOf(start to start + len)
            } else {
                listOf(start to total, 0f to (start + len - total))
            }
            val widths = listOf(thickness * 4.5f to 0.22f, thickness * 2.6f to 0.5f, thickness * 1.2f to 1f)
            for ((from, to) in pieces) {
                val seg = Path()
                measure.getSegment(from, to, seg, true)
                widths.forEachIndexed { idx, (stroke, a) ->
                    val tone = if (idx == widths.lastIndex) Color.White else c
                    drawPath(seg, tone.copy(alpha = (a * level).coerceIn(0f, 1f)), style = Stroke(width = stroke, cap = StrokeCap.Round))
                }
            }
        }
    }
}
