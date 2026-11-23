package app.gamenative.ui.theme

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.gamenative.PrefManager
import app.gamenative.R
import java.io.File
import java.io.IOException
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Animated wallpapers the app can draw behind its main screens. */
// new modes go at the end: the stored choice is the position in this list
enum class WallpaperMode { OFF, BUBBLES, AURORA, VIDEO, WATER }

data class WallpaperState(
    val mode: WallpaperMode = WallpaperMode.OFF,
    /** Absolute path of the video used by [WallpaperMode.VIDEO]; empty until one is picked. */
    val videoPath: String = "",
) {
    val active: Boolean get() = mode != WallpaperMode.OFF
}

/**
 * The wallpaper choice. It lives outside the theme system on purpose: a theme only describes colors, shapes and
 * still images, while a wallpaper moves, so the two can be combined freely (any theme over any wallpaper).
 */
object WallpaperSettings {
    /** Biggest video accepted; the file is copied into the app so it keeps working if the original moves. */
    private const val MAX_VIDEO_BYTES = 200L * 1024 * 1024

    private val _state = MutableStateFlow(read())
    val state: StateFlow<WallpaperState> = _state.asStateFlow()

    private fun read(): WallpaperState = try {
        val mode = WallpaperMode.entries.getOrNull(PrefManager.wallpaperMode) ?: WallpaperMode.OFF
        WallpaperState(mode, PrefManager.wallpaperVideoPath)
    } catch (e: Throwable) {
        // previews and tests run without the preference store
        WallpaperState()
    }

    fun setMode(mode: WallpaperMode) {
        PrefManager.wallpaperMode = mode.ordinal
        _state.value = _state.value.copy(mode = mode)
    }

    /**
     * Makes a video that ships inside the app (under assets/) the video wallpaper and turns that mode on. The file
     * is copied out of the assets once, because the player needs a real path.
     */
    suspend fun useBundled(context: Context, assetPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "wallpapers")
            dir.mkdirs()
            val file = File(dir, "bundled-" + assetPath.substringAfterLast('/'))
            if (!file.isFile || file.length() == 0L) {
                context.assets.open(assetPath).use { src -> file.outputStream().use { out -> src.copyTo(out) } }
            }
            val previous = _state.value.videoPath
            PrefManager.wallpaperVideoPath = file.absolutePath
            PrefManager.wallpaperMode = WallpaperMode.VIDEO.ordinal
            _state.value = WallpaperState(WallpaperMode.VIDEO, file.absolutePath)
            // a picked video is replaced; a bundled one stays, it is only a copy of an asset
            if (previous.isNotEmpty() && previous != file.absolutePath && !File(previous).name.startsWith("bundled-")) {
                File(previous).delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Bundled wallpaper $assetPath could not be used")
            Result.failure(e)
        }
    }

    /** Copies the video at [uri] into the app, makes it the video wallpaper and turns that mode on. */
    suspend fun importVideo(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "wallpapers")
        var target: File? = null
        try {
            dir.mkdirs()
            val ext = when (context.contentResolver.getType(uri)) {
                "video/webm" -> "webm"
                "video/x-matroska" -> "mkv"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                else -> "mp4"
            }
            val file = File(dir, "wallpaper-${System.currentTimeMillis()}.$ext")
            target = file
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read the selected video")
            input.use { src ->
                file.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = src.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_VIDEO_BYTES) throw IOException("The video is larger than 200 MB")
                        out.write(buffer, 0, n)
                    }
                }
            }
            val previous = _state.value.videoPath
            PrefManager.wallpaperVideoPath = file.absolutePath
            PrefManager.wallpaperMode = WallpaperMode.VIDEO.ordinal
            _state.value = WallpaperState(WallpaperMode.VIDEO, file.absolutePath)
            if (previous.isNotEmpty() && previous != file.absolutePath) File(previous).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Wallpaper video import failed")
            target?.delete()
            Result.failure(e)
        }
    }
}

/** Frame time in seconds since the wallpaper appeared, refreshed about 30 times a second; still when animations are off. */
@Composable
private fun rememberWallpaperSeconds(): State<Float> {
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    return produceState(0f, animate) {
        if (!animate) return@produceState
        var origin = -1L
        var last = 0L
        while (isActive) {
            withFrameNanos { t ->
                if (origin < 0L) origin = t
                if (t - last >= FRAME_NANOS) {
                    last = t
                    value = (t - origin) / 1_000_000_000f
                }
            }
        }
    }
}

private const val FRAME_NANOS = 30_000_000L

/** The animated wallpaper the person picked, filling its parent; draws nothing when it is off. */
@Composable
fun AnimatedWallpaper(modifier: Modifier = Modifier) {
    val state by WallpaperSettings.state.collectAsStateWithLifecycle()
    when (state.mode) {
        WallpaperMode.OFF -> Unit
        WallpaperMode.BUBBLES -> BubblesWallpaper(modifier)
        WallpaperMode.AURORA -> AuroraWallpaper(modifier)
        WallpaperMode.WATER -> WaterWallpaper(modifier)
        WallpaperMode.VIDEO ->
            if (state.videoPath.isNotEmpty() && File(state.videoPath).isFile) {
                val ext = state.videoPath.substringAfterLast('.', "").lowercase()
                if ((ext == "gif" || ext == "webp") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageWallpaper(state.videoPath, modifier)
                } else if (ext == "gif" || ext == "webp") {
                    // animated images need Android 9; older versions get the bubbles
                    BubblesWallpaper(modifier)
                } else {
                    VideoWallpaper(state.videoPath, modifier)
                }
            } else {
                BubblesWallpaper(modifier)
            }
    }
}

private class Bubble(
    /** Horizontal position, 0..1 of the width. */
    val x: Float,
    /** Passes of the screen per second. */
    val speed: Float,
    val phase: Float,
    /** Radius as a share of the shorter screen side. */
    val radius: Float,
    /** Sideways sway as a share of the width. */
    val sway: Float,
)

private val Bubbles: List<Bubble> = run {
    val random = java.util.Random(7L)
    List(18) {
        Bubble(
            x = random.nextFloat(),
            speed = 0.010f + random.nextFloat() * 0.030f,
            phase = random.nextFloat(),
            radius = 0.018f + random.nextFloat() * 0.075f,
            sway = 0.008f + random.nextFloat() * 0.030f,
        )
    }
}

private const val TWO_PI = 6.2831855f

/** The classic Frutiger Aero scene: a bright sky fading to fresh green, soft light rays and rising glass bubbles. */
@Composable
private fun BubblesWallpaper(modifier: Modifier = Modifier) {
    val seconds = rememberWallpaperSeconds()
    val ray = remember { Path() }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = seconds.value
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0F6FC0), Color(0xFF2E9BE0), Color(0xFF63CBEF), Color(0xFFA7EBD0)),
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x66FFFFFF), Color.Transparent),
                center = Offset(w * 0.18f, -h * 0.08f),
                radius = w * 0.75f,
            ),
        )
        // light rays from the top left, breathing slowly
        val rayAlpha = 0.07f + 0.035f * sin(t * 0.4f)
        val rayBrush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = rayAlpha), Color.Transparent),
        )
        for (i in 0..2) {
            val x0 = w * (0.05f + i * 0.17f)
            ray.reset()
            ray.moveTo(x0, 0f)
            ray.lineTo(x0 + w * 0.10f, 0f)
            ray.lineTo(x0 + w * 0.42f, h)
            ray.lineTo(x0 + w * 0.22f, h)
            ray.close()
            drawPath(ray, rayBrush)
        }
        // green glow of the hills at the bottom
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x5539D98A)),
                startY = h * 0.55f,
                endY = h,
            ),
        )
        for (b in Bubbles) {
            val progress = (b.phase + t * b.speed) % 1f
            val cy = h * (1.12f - progress * 1.3f)
            val cx = w * (b.x + sin(t * 0.35f + b.phase * TWO_PI) * b.sway)
            drawBubble(Offset(cx, cy), size.minDimension * b.radius)
        }
    }
}

/** Clear teal water: slow ripples spreading from the middle, shafts of light from above and rising glass bubbles. */
@Composable
private fun WaterWallpaper(modifier: Modifier = Modifier) {
    val seconds = rememberWallpaperSeconds()
    val ray = remember { Path() }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = seconds.value
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0B6F97), Color(0xFF1596B4), Color(0xFF0E7AA0), Color(0xFF0A5C80)),
            ),
        )
        val middle = Offset(w * 0.5f, h * 0.42f)
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x66CFFBFF), Color.Transparent),
                center = middle,
                radius = size.minDimension * 0.85f,
            ),
        )
        // shafts of light from the surface, breathing slowly
        val rayAlpha = 0.08f + 0.04f * sin(t * 0.35f)
        val rayBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = rayAlpha), Color.Transparent))
        for (i in 0..3) {
            val x0 = w * (0.02f + i * 0.26f) + sin(t * 0.2f + i) * w * 0.02f
            ray.reset()
            ray.moveTo(x0, 0f)
            ray.lineTo(x0 + w * 0.09f, 0f)
            ray.lineTo(x0 + w * 0.30f, h)
            ray.lineTo(x0 + w * 0.12f, h)
            ray.close()
            drawPath(ray, rayBrush)
        }
        // ripples
        for (i in 0..3) {
            val p = (t * 0.07f + i / 4f) % 1f
            val r = size.minDimension * (0.12f + 0.62f * p)
            val a = (1f - p) * 0.38f
            drawCircle(Color.White.copy(alpha = a), r, middle, style = Stroke(width = 2f + 6f * (1f - p)))
            drawCircle(Color(0xFFBFF6FF).copy(alpha = a * 0.5f), r * 0.94f, middle, style = Stroke(width = 1.5f))
        }
        for (b in Bubbles) {
            val progress = (b.phase + t * b.speed) % 1f
            val cy = h * (1.12f - progress * 1.3f)
            val cx = w * (b.x + sin(t * 0.35f + b.phase * TWO_PI) * b.sway)
            drawBubble(Offset(cx, cy), size.minDimension * b.radius)
        }
    }
}

private fun DrawScope.drawBubble(center: Offset, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0x00FFFFFF),
            0.72f to Color(0x14FFFFFF),
            1f to Color(0x55FFFFFF),
            center = center,
            radius = r,
        ),
        radius = r,
        center = center,
    )
    drawCircle(Color(0x66FFFFFF), r, center, style = Stroke(width = max(r * 0.04f, 1f)))
    // curved reflection of the sky on the lower right rim
    drawArc(
        color = Color(0x44BFFFFF),
        startAngle = 15f,
        sweepAngle = 65f,
        useCenter = false,
        topLeft = Offset(center.x - r * 0.82f, center.y - r * 0.82f),
        size = Size(r * 1.64f, r * 1.64f),
        style = Stroke(width = max(r * 0.07f, 1f)),
    )
    // glossy highlight on the upper left
    rotate(degrees = -35f, pivot = center) {
        drawOval(
            color = Color(0x99FFFFFF),
            topLeft = Offset(center.x - r * 0.62f, center.y - r * 0.66f),
            size = Size(r * 0.5f, r * 0.26f),
        )
    }
}

private class Aurora(val color: Color, val fx: Float, val fy: Float, val phase: Float)

private val AuroraBlobs = listOf(
    Aurora(Color(0xFF2EE6C8), 0.11f, 0.08f, 0.0f),
    Aurora(Color(0xFF3D8BFF), 0.07f, 0.12f, 1.9f),
    Aurora(Color(0xFF8A6BFF), 0.09f, 0.06f, 3.7f),
    Aurora(Color(0xFF42D9F5), 0.05f, 0.10f, 5.1f),
)

/** Deep water with slow glowing light and a glass sheen sweeping across it. */
@Composable
private fun AuroraWallpaper(modifier: Modifier = Modifier) {
    val seconds = rememberWallpaperSeconds()
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = seconds.value
        val w = size.width
        val h = size.height
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF04152E), Color(0xFF083A5E), Color(0xFF0B5C7A)),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        val radius = max(w, h) * 0.55f
        for (blob in AuroraBlobs) {
            val center = Offset(
                w * (0.5f + 0.38f * sin(t * blob.fx * TWO_PI + blob.phase)),
                h * (0.5f + 0.34f * cos(t * blob.fy * TWO_PI + blob.phase * 1.3f)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color.copy(alpha = 0.5f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        // sheen: a faint diagonal band of light drifting from left to right
        val sx = (((t * 0.035f) % 1.6f) - 0.3f) * w
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x16FFFFFF), Color.Transparent),
                start = Offset(sx - w * 0.3f, 0f),
                end = Offset(sx + w * 0.3f, h * 0.45f),
            ),
        )
    }
}

/** A looping GIF or animated WebP of the person's choosing, cropped to fill the screen. */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun ImageWallpaper(path: String, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawable by produceState<Drawable?>(null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path)))
            } catch (e: Exception) {
                Timber.w(e, "Animated wallpaper cannot be decoded")
                null
            }
        }
    }
    val current = drawable
    if (current == null) {
        BubblesWallpaper(modifier)
        return
    }
    DisposableEffect(current, lifecycleOwner) {
        val animated = current as? AnimatedImageDrawable
        animated?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        if (ValueAnimator.areAnimatorsEnabled()) animated?.start()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> animated?.stop()
                Lifecycle.Event.ON_RESUME -> if (ValueAnimator.areAnimatorsEnabled()) animated?.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            animated?.stop()
        }
    }
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageDrawable(current) },
        modifier = modifier.fillMaxSize(),
    )
}

/** A muted, looping video of the person's choosing; falls back to the bubbles scene if it cannot be played. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoWallpaper(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var failed by remember(path) { mutableStateOf(false) }

    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.w(error, "Video wallpaper cannot be played")
                failed = true
            }
        }
        player.addListener(listener)
        // a wallpaper nobody can see should not keep decoding
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            player.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    if (failed) {
        BubblesWallpaper(modifier)
    } else {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.wallpaper_player, null) as PlayerView).also { it.player = player }
            },
            update = { it.player = player },
            modifier = modifier.fillMaxSize(),
        )
    }
}
