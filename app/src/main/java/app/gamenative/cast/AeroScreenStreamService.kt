package app.gamenative.cast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import app.gamenative.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import timber.log.Timber

/**
 * Owns the user-authorized screen projection and HLS stream lifecycle.
 * The projection token is used once, for one virtual display, then discarded.
 */
class AeroScreenStreamService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: AeroVideoEncoder? = null
    private var segmenter: AeroHlsSegmenter? = null
    private var httpServer: AeroHlsHttpServer? = null
    private val stopping = AtomicBoolean(false)

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Timber.i("Aero screen projection was stopped by Android")
            stopStreaming(stopProjection = false)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopStreaming()
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val projectionData = getProjectionData(intent)
        if (resultCode == 0 || projectionData == null) {
            broadcastError(getString(R.string.aero_cast_stream_error_permission))
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val activeProjection = manager.getMediaProjection(resultCode, projectionData)
                ?: error("Android did not create a screen projection")
            projection = activeProjection
            activeProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            startPipeline(activeProjection)
        } catch (error: Throwable) {
            Timber.e(error, "Unable to start Aero screen stream")
            broadcastError(error.message ?: getString(R.string.aero_cast_stream_error_generic))
            stopStreaming()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getProjectionData(intent: Intent?): Intent? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }
    }

    private fun startPipeline(activeProjection: MediaProjection) {
        val resolution = AeroScreenStreamSettings.resolution(this)
        val aspect = AeroScreenStreamSettings.aspectRatio(this)
        val size = AeroScreenStreamSettings.outputSize(this, resolution, aspect)
        val densityDpi = resources.displayMetrics.densityDpi.coerceAtLeast(DisplayMetrics.DENSITY_DEFAULT)

        val liveSegmenter = AeroHlsSegmenter(targetDurationSeconds = 2.0, windowSize = 5)
        segmenter = liveSegmenter
        val server = AeroHlsHttpServer(liveSegmenter, resolution, size.width, size.height)
        httpServer = server
        server.start()
        val url = server.streamUrl

        val liveEncoder = AeroVideoEncoder(size.width, size.height, resolution.bitrate)
        encoder = liveEncoder
        liveEncoder.start(liveSegmenter)

        val display = activeProjection.createVirtualDisplay(
            "Game-Aero screen stream",
            size.width,
            size.height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            liveEncoder.inputSurface,
            null,
            Handler(Looper.getMainLooper()),
        ) ?: error("Android could not create the video capture display")

        virtualDisplay = display

        thread(name = "aero-stream-ready", isDaemon = true) {
            val deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS
            while (!stopping.get() && System.currentTimeMillis() < deadline) {
                if (liveSegmenter.readySegmentCount() > 0) {
                    broadcastReady(url)
                    return@thread
                }
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                    return@thread
                }
            }
            if (!stopping.get()) {
                broadcastError(getString(R.string.aero_cast_stream_error_timeout))
                stopSelf()
            }
        }
        Timber.i("Aero stream started: ${size.width}x${size.height} at $url")
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AeroScreenStreamService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle(getString(R.string.aero_cast_notification_title))
            .setContentText(getString(R.string.aero_cast_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.aero_cast_stop), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.aero_cast_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun broadcastReady(url: String) {
        sendBroadcast(
            Intent(ACTION_STREAM_READY)
                .setPackage(packageName)
                .putExtra(EXTRA_STREAM_URL, url),
        )
    }

    private fun broadcastError(message: String) {
        sendBroadcast(
            Intent(ACTION_STREAM_ERROR)
                .setPackage(packageName)
                .putExtra(EXTRA_ERROR_MESSAGE, message),
        )
    }

    private fun stopStreaming(stopProjection: Boolean = true) {
        if (!stopping.compareAndSet(false, true)) return
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { encoder?.stop() }
        encoder = null
        runCatching { httpServer?.stop() }
        httpServer = null
        segmenter = null

        val activeProjection = projection
        projection = null
        if (activeProjection != null) {
            runCatching { activeProjection.unregisterCallback(projectionCallback) }
            if (stopProjection) runCatching { activeProjection.stop() }
        }
        sendBroadcast(Intent(ACTION_STREAM_STOPPED).setPackage(packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STREAM_READY = "app.gamenative.cast.STREAM_READY"
        const val ACTION_STREAM_ERROR = "app.gamenative.cast.STREAM_ERROR"
        const val ACTION_STREAM_STOPPED = "app.gamenative.cast.STREAM_STOPPED"
        const val ACTION_STOP = "app.gamenative.cast.STOP"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_PROJECTION_DATA = "projection_data"

        private const val CHANNEL_ID = "aero_screen_stream"
        private const val NOTIFICATION_ID = 4872
        private const val STARTUP_TIMEOUT_MS = 15_000L
    }
}
