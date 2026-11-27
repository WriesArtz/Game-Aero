package app.gamenative.ui.screen.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gamenative.R
import app.gamenative.data.LibraryItem
import app.gamenative.ui.theme.AeroStyleState
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlin.math.sin

private const val TWO_PI = 6.2831855f

/**
 * The "true Frutiger Aero" cover: a jewel case of blue translucent plastic, a white spine on the left with the AeroGame
 * logo, the name and a disc, and the art on its face under bubbles, a rolling water line, glints and a holographic
 * sticker. Drawn twice per tile, the second flipped as the reflection; only the current cover moves.
 */
@Composable
internal fun CaseCover(
    item: LibraryItem,
    imageUrl: String,
    w: Dp,
    h: Dp,
    current: Boolean,
    focused: Boolean,
    style: AeroStyleState,
    fx: AeroFx?,
    realistic: Boolean = false,
) {
    val corner = minOf(w, h) * 0.05f
    val shape = RoundedCornerShape(corner)
    val bezel = (minOf(w, h) * (if (realistic) 0.042f else 0.03f) * style.f(style.frameWidth)).coerceIn(1.5.dp, 14.dp)
    val frameAlpha = style.f(style.frameAlpha)
    val light = style.f(style.frameLight)
    val plastic = style.frameTint ?: Color(0xFF1E8CF0)
    val fxLevel = style.f(style.coverFx)
    val motion = style.f(style.coverMotion)
    val live = fx != null && current && motion > 0f
    val spineW = w * 0.115f
    val nameText: @Composable () -> Unit = {
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
        )
    }
    Box(
        modifier = Modifier
            .width(w)
            .height(h)
            .clip(shape)
            // the blue plastic of the case: lit at the top left, deeper at the bottom right
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(plastic, Color.White, (0.4f * light).coerceIn(0f, 1f)).copy(alpha = frameAlpha),
                        plastic.copy(alpha = frameAlpha),
                        lerp(plastic, Color.Black, 0.3f).copy(alpha = frameAlpha),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    if (focused) 3.dp else 1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = (0.9f * light * frameAlpha).coerceIn(0f, 1f)),
                            plastic.copy(alpha = frameAlpha),
                        ),
                    ),
                ),
                shape,
            )
            .drawWithContent {
                drawContent()
                // light on the plastic: a glare across the top left and a bright line down the left edge
                val glare = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.6f, 0f)
                    lineTo(0f, size.height * 0.5f)
                    close()
                }
                drawPath(
                    glare,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = (0.3f * fxLevel).coerceIn(0f, 1f)), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.35f, size.height * 0.3f),
                    ),
                )
                if (realistic) drawPlasticDetail(plastic, bezel.toPx(), corner.toPx(), fxLevel)
                drawRoundRect(
                    color = Color.White.copy(alpha = (0.35f * fxLevel).coerceIn(0f, 1f)),
                    topLeft = Offset(bezel.toPx() * 0.25f, corner.toPx()),
                    size = Size(bezel.toPx() * 0.4f, size.height - corner.toPx() * 2f),
                    cornerRadius = CornerRadius(bezel.toPx()),
                )
                if (live) {
                    drawRimEffect(
                        mode = style.fxMode,
                        color = style.fxTint,
                        fx = fx!!,
                        level = motion.coerceAtMost(1.5f),
                        radius = corner.toPx(),
                        inset = bezel.toPx() * 0.5f + 1.dp.toPx(),
                        thickness = (bezel.toPx() + 1.5.dp.toPx()).coerceAtLeast(2.dp.toPx()),
                    )
                }
            }
            .padding(bezel),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // realistic: the thickness of the case, seen on its near side as a slab of deep clear plastic
            if (realistic) {
                Box(
                    modifier = Modifier
                        .width(w * 0.055f)
                        .fillMaxHeight()
                        .drawWithContent {
                            val deep = lerp(plastic, Color.Black, 0.55f)
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(deep.copy(alpha = 0.95f), lerp(plastic, Color.White, 0.25f).copy(alpha = 0.9f), plastic.copy(alpha = 0.8f)),
                                ),
                            )
                            // light caught on the outer corner and a second, softer reflection inside the plastic
                            drawRect(Color.White.copy(alpha = 0.7f), Offset(size.width * 0.08f, 0f), Size(size.width * 0.1f, size.height))
                            drawRect(Color.White.copy(alpha = 0.22f), Offset(size.width * 0.55f, 0f), Size(size.width * 0.12f, size.height))
                            // thin ridges, like the moulded ribs along the side of a jewel case
                            val ribs = 14
                            for (i in 1 until ribs) {
                                val y = size.height * i / ribs
                                drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            }
                        },
                )
            }
            // the spine: logo at the top, the name running up it, a disc at the bottom
            Box(
                modifier = Modifier
                    .width(spineW)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(corner * 0.5f))
                    .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFE4EDF4)))),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = spineW * 0.12f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Image(
                        painter = painterResource(R.drawable.aerogame_logo),
                        contentDescription = null,
                        modifier = Modifier.size(spineW * 0.92f),
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "AeroGame",
                            color = Color(0xFF10151A),
                            fontSize = (spineW.value * 0.78f).sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .graphicsLayer { rotationZ = -90f },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(spineW * 0.8f)
                            .drawWithContent { drawDisc(center, size.minDimension / 2f, true) },
                    )
                }
            }
            // the face: the art under the effects of the glass
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = bezel * 0.5f)
                    .clip(RoundedCornerShape(corner * 0.5f))
                    .drawWithContent {
                        drawContent()
                        // under the clear lid the printed sheet is a little shaded at its edges
                        if (realistic) drawLidShade()
                        drawFaceDecor(if (live) fx else null, fxLevel, motion)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl.isNotEmpty()) {
                    CoilImage(
                        imageModel = { imageUrl },
                        modifier = Modifier.fillMaxSize().alpha(style.f(style.coverArt)),
                        imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = item.name),
                        loading = {},
                        failure = { nameText() },
                    )
                } else {
                    // no art: the empty tray of the case, with its hub, and the name over it
                    Box(modifier = Modifier.fillMaxSize().drawWithContent { drawTray(plastic) })
                    nameText()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (live) drawSurfaceEffect(style.fxMode, style.fxTint, fx!!, motion.coerceAtMost(1.5f))
                        },
                )
            }
        }
    }
}

/** A disc seen from the front: silver with a sweep of light and a hole; on a black plate when [plate] is set. */
private fun DrawScope.drawDisc(c: Offset, r: Float, plate: Boolean) {
    if (plate) drawCircle(Color(0xFF0B0E12), r, c)
    val d = if (plate) r * 0.82f else r
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(Color(0xFFF4F8FB), Color(0xFF9FB4C4), Color(0xFFFFFFFF), Color(0xFF7F95A6), Color(0xFFF4F8FB)),
            center = c,
        ),
        radius = d,
        center = c,
    )
    drawCircle(Color(0x66FFFFFF), d * 0.5f, c, style = Stroke(width = d * 0.05f))
    drawCircle(Color(0xFF0B0E12), d * 0.2f, c)
    drawCircle(Color.White, d * 0.1f, c)
}

/**
 * What sits over the art on the face of the case, like the print of an AeroGame disc: bubbles, glints, a rolling
 * water line at the foot, a disc badge at the lower left and a holographic sticker at the lower right. Moves only
 * when [fx] is given.
 */
private fun DrawScope.drawFaceDecor(fx: AeroFx?, level: Float, motion: Float) {
    val w = size.width
    val h = size.height
    val phase = if (fx != null) fx.spin.value * (TWO_PI / 360f) else 0f
    val bob = if (fx != null) sin(fx.drift.value * TWO_PI * 2f) else 0f
    val a = level.coerceIn(0f, 1.5f)

    // water rolling along the bottom
    val base = h * 0.86f
    val amp = h * 0.018f
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
            listOf(Color(0xCCCFF6FF).copy(alpha = (0.8f * a).coerceIn(0f, 1f)), Color(0x883AB8FF).copy(alpha = (0.5f * a).coerceIn(0f, 1f))),
            startY = base - amp,
            endY = h,
        ),
    )
    // a splash of water curling up the left side and across the foot, as printed on the AeroGame discs
    val splash = Path().apply {
        moveTo(w * 0.1f, h * 0.04f)
        cubicTo(w * 0.34f, h * 0.2f, w * -0.02f, h * 0.42f, w * 0.12f, h * 0.64f)
        cubicTo(w * 0.2f, h * 0.8f, w * 0.38f, h * 0.9f, w * 0.7f, h * 0.9f)
    }
    drawPath(splash, Color.White.copy(alpha = (0.2f * a).coerceIn(0f, 1f)), style = Stroke(width = minOf(w, h) * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawPath(splash, Color(0xFFCFF3FF).copy(alpha = (0.45f * a).coerceIn(0f, 1f)), style = Stroke(width = minOf(w, h) * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawPath(splash, Color.White.copy(alpha = (0.85f * a).coerceIn(0f, 1f)), style = Stroke(width = minOf(w, h) * 0.008f + 1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // droplets thrown off the splash
    listOf(Offset(0.3f, 0.12f) to 0.014f, Offset(0.06f, 0.3f) to 0.01f, Offset(0.2f, 0.5f) to 0.012f, Offset(0.1f, 0.78f) to 0.016f, Offset(0.42f, 0.88f) to 0.012f)
        .forEach { (p, r) ->
            drawCircle(Color.White.copy(alpha = (0.55f * a).coerceIn(0f, 1f)), r * minOf(w, h), Offset(p.x * w, p.y * h))
        }
    // bubbles over the art
    val bubbles = listOf(
        Triple(0.14f, 0.26f, 0.07f), Triple(0.86f, 0.2f, 0.085f), Triple(0.8f, 0.62f, 0.06f),
        Triple(0.2f, 0.6f, 0.045f), Triple(0.92f, 0.4f, 0.032f), Triple(0.5f, 0.1f, 0.03f),
    )
    bubbles.forEachIndexed { i, (bx, by, br) ->
        val dy = bob * h * 0.012f * (if (i % 2 == 0) 1f else -1f)
        drawBubble(Offset(bx * w, by * h + dy), br * minOf(w, h), Color(0xFFCFF3FF), a)
    }
    // glints of light
    val stars = listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.08f), Offset(0.55f, 0.5f), Offset(0.12f, 0.8f), Offset(0.62f, 0.82f))
    stars.forEachIndexed { i, p ->
        val tw = if (fx != null) (sin(phase * 3f + i * 1.7f) * 0.5f + 0.5f) else 0.7f
        drawStar(Offset(p.x * w, p.y * h), minOf(w, h) * (0.03f + 0.012f * (i % 3)), Color.White.copy(alpha = (tw * a).coerceIn(0f, 1f)), 1.5f * density)
    }
    // the disc badge at the lower left
    val badge = minOf(w, h) * 0.17f
    val bx = w * 0.04f
    val by = h * 0.86f - badge * 0.2f
    drawRoundRect(Color(0xFF0B0E12), Offset(bx, by), Size(badge, badge), CornerRadius(badge * 0.12f))
    drawDisc(Offset(bx + badge / 2f, by + badge / 2f), badge * 0.38f, false)
    // the holographic sticker at the lower right, its colors sliding across it
    val sw = w * 0.36f
    val sh = h * 0.15f
    val sx = w * 0.6f
    val sy = h * 0.74f
    val shift = if (fx != null) sin(phase) * sw * 0.5f else 0f
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFFFFC2E8), Color(0xFF9FE8FF), Color(0xFFC9FFD8), Color(0xFFFFF2A6), Color(0xFFD4B8FF)),
            start = Offset(sx - sw * 0.4f + shift, sy),
            end = Offset(sx + sw * 0.9f + shift, sy + sh),
            tileMode = TileMode.Mirror,
        ),
        topLeft = Offset(sx, sy),
        size = Size(sw, sh),
        cornerRadius = CornerRadius(sh * 0.25f),
        alpha = 0.88f,
    )
    drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(sx, sy), Size(sw, sh), CornerRadius(sh * 0.25f), style = Stroke(width = 1.2f * density))
    drawBubble(Offset(sx + sw * 0.25f, sy + sh * 0.45f), sh * 0.22f, Color.White, 0.9f)
    drawBubble(Offset(sx + sw * 0.6f, sy + sh * 0.35f), sh * 0.16f, Color.White, 0.9f)
    drawBubble(Offset(sx + sw * 0.82f, sy + sh * 0.6f), sh * 0.12f, Color.White, 0.9f)
}

/**
 * What makes the case look photographed: the thick wall of the plastic on its right edge, the clips of the tray
 * showing at the top and the bottom, a line where the front cover hinges, the plastic darkening towards its edges and
 * a second, thin streak of light.
 */
private fun DrawScope.drawPlasticDetail(plastic: Color, bezel: Float, corner: Float, level: Float) {
    val w = size.width
    val h = size.height
    val deep = lerp(plastic, Color.Black, 0.45f)
    // the right wall of the case, seen as a thick strip of darker plastic with a bright edge
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, deep.copy(alpha = 0.9f)), startX = w - bezel * 4.2f, endX = w),
        topLeft = Offset(w - bezel * 4.2f, corner),
        size = Size(bezel * 4.2f, h - corner * 2f),
    )
    // the far side of the case falls into shade, the near side stays lit
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f * level.coerceAtMost(1.5f)))))
    // a bright bevel along the top edge, where the lid meets the wall
    drawLine(Color.White.copy(alpha = 0.6f), Offset(corner, bezel * 0.35f), Offset(w - corner, bezel * 0.35f), strokeWidth = bezel * 0.2f)
    drawLine(Color.White.copy(alpha = 0.55f * level.coerceAtMost(1f)), Offset(w - bezel * 0.5f, corner), Offset(w - bezel * 0.5f, h - corner), strokeWidth = bezel * 0.18f)
    // the plastic gets deeper in blue towards the top and bottom edges
    drawRect(Brush.verticalGradient(listOf(deep.copy(alpha = 0.55f), Color.Transparent), endY = bezel * 2.2f))
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, deep.copy(alpha = 0.55f)), startY = h - bezel * 2.2f, endY = h),
    )
    // the tray's clips: half-rings at the top and at the bottom, in clear plastic
    val clip = bezel * 2.6f
    val xs = listOf(0.3f, 0.78f)
    for (x in xs) {
        val cx = w * x
        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - clip, bezel * 0.9f - clip * 0.2f),
            size = Size(clip * 2f, clip * 1.4f),
            style = Stroke(width = bezel * 0.16f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - clip, h - bezel * 0.9f - clip * 1.2f),
            size = Size(clip * 2f, clip * 1.4f),
            style = Stroke(width = bezel * 0.16f),
        )
    }
    // the hinge between the spine and the front cover
    drawLine(Color.White.copy(alpha = 0.35f), Offset(w * 0.135f, corner), Offset(w * 0.135f, h - corner), strokeWidth = bezel * 0.14f)
    // a thin second streak of light, parallel to the first
    val streak = Path().apply {
        moveTo(w * 0.62f, 0f)
        lineTo(w * 0.7f, 0f)
        lineTo(w * 0.1f, h * 0.62f)
        lineTo(w * 0.05f, h * 0.62f)
        close()
    }
    drawPath(streak, Color.White.copy(alpha = (0.14f * level).coerceIn(0f, 1f)))
}

/** The shade a clear lid leaves along the edges of the sheet under it: darker at the top and left, light at the foot. */
private fun DrawScope.drawLidShade() {
    val edge = minOf(size.width, size.height) * 0.05f
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent), endY = edge * 1.6f))
    drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), endX = edge))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f)), startY = size.height - edge, endY = size.height))
}

/** The empty tray of a jewel case seen through its clear front: a deep blue dish with a ring and a hub of spokes. */
private fun DrawScope.drawTray(plastic: Color) {
    val c = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = minOf(size.width, size.height) * 0.46f
    drawRect(lerp(plastic, Color.Black, 0.35f).copy(alpha = 0.85f))
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), center = c, radius = r), r, c)
    drawCircle(Color.White.copy(alpha = 0.35f), r, c, style = Stroke(width = r * 0.03f))
    drawCircle(Color.White.copy(alpha = 0.22f), r * 0.94f, c, style = Stroke(width = r * 0.015f))
    val hub = r * 0.22f
    drawCircle(Color.White.copy(alpha = 0.35f), hub, c, style = Stroke(width = r * 0.03f))
    for (i in 0 until 8) {
        val a = i * 0.7853982f
        drawLine(
            Color.White.copy(alpha = 0.4f),
            Offset(c.x + kotlin.math.cos(a) * hub * 0.55f, c.y + kotlin.math.sin(a) * hub * 0.55f),
            Offset(c.x + kotlin.math.cos(a) * hub * 1.05f, c.y + kotlin.math.sin(a) * hub * 1.05f),
            strokeWidth = r * 0.025f,
        )
    }
}
