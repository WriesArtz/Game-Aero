package app.gamenative.ui.screen.library.components

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import app.gamenative.ui.util.SnackbarManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.gamenative.R
import app.gamenative.cast.AeroScreenStreamService
import app.gamenative.cast.AeroScreenStreamSettings
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

/**
 * Owns everything about casting the screen: the capture permission flow, the service broadcasts, the Cast
 * session listener and the settings dialog. It lives apart from the home screen so the cast button can sit in the
 * top bar and the stream keeps being handled on any tab.
 */
@Composable
fun AeroCastController(showDialog: Boolean, onDismiss: () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    var isScreenStreaming by remember { mutableStateOf(false) }
    val capturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val projectionData = result.data
        if (result.resultCode == Activity.RESULT_OK && projectionData != null) {
            val connected = runCatching {
                CastContext.getSharedInstance(appContext).sessionManager.currentCastSession?.isConnected == true
            }.getOrDefault(false)
            if (connected) {
                val serviceIntent = Intent(appContext, AeroScreenStreamService::class.java)
                    .putExtra(AeroScreenStreamService.EXTRA_RESULT_CODE, result.resultCode)
                    .putExtra(AeroScreenStreamService.EXTRA_PROJECTION_DATA, projectionData)
                runCatching { ContextCompat.startForegroundService(appContext, serviceIntent) }
                    .onFailure { SnackbarManager.show(it.message ?: appContext.getString(R.string.aero_cast_stream_error_generic)) }
            } else {
                SnackbarManager.show(appContext.getString(R.string.aero_cast_connect_first))
            }
        } else {
            SnackbarManager.show(appContext.getString(R.string.aero_cast_permission_cancelled))
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // granted or not, the stream still starts; without it the service falls back to video only
        val manager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        capturePermissionLauncher.launch(manager.createScreenCaptureIntent())
    }

    DisposableEffect(appContext) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AeroScreenStreamService.ACTION_STREAM_READY -> {
                        val url = intent.getStringExtra(AeroScreenStreamService.EXTRA_STREAM_URL) ?: return
                        val remoteClient = runCatching {
                            CastContext.getSharedInstance(appContext).sessionManager.currentCastSession?.remoteMediaClient
                        }.getOrNull()
                        if (remoteClient == null) {
                            appContext.stopService(Intent(appContext, AeroScreenStreamService::class.java))
                            SnackbarManager.show(appContext.getString(R.string.aero_cast_connect_first))
                        } else {
                            val media = MediaInfo.Builder(url)
                                .setContentType("application/x-mpegURL")
                                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                                .build()
                            isScreenStreaming = true
                            SnackbarManager.show(appContext.getString(R.string.aero_cast_streaming))
                            remoteClient.load(MediaLoadRequestData.Builder().setMediaInfo(media).build())
                                .setResultCallback { loadResult ->
                                    if (!loadResult.status.isSuccess) {
                                        isScreenStreaming = false
                                        appContext.stopService(Intent(appContext, AeroScreenStreamService::class.java))
                                        SnackbarManager.show(appContext.getString(R.string.aero_cast_receiver_rejected))
                                    }
                                }
                        }
                    }
                    AeroScreenStreamService.ACTION_STREAM_ERROR -> {
                        isScreenStreaming = false
                        val message = intent.getStringExtra(AeroScreenStreamService.EXTRA_ERROR_MESSAGE)
                            ?: appContext.getString(R.string.aero_cast_stream_error_generic)
                        SnackbarManager.show(message)
                    }
                    AeroScreenStreamService.ACTION_STREAM_NOTICE -> {
                        val message = intent.getStringExtra(AeroScreenStreamService.EXTRA_ERROR_MESSAGE) ?: return
                        SnackbarManager.show(message)
                    }
                    AeroScreenStreamService.ACTION_STREAM_STOPPED -> isScreenStreaming = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AeroScreenStreamService.ACTION_STREAM_READY)
            addAction(AeroScreenStreamService.ACTION_STREAM_ERROR)
            addAction(AeroScreenStreamService.ACTION_STREAM_STOPPED)
            addAction(AeroScreenStreamService.ACTION_STREAM_NOTICE)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val castContext = runCatching { CastContext.getSharedInstance(appContext) }.getOrNull()
        val sessionListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) = Unit
            override fun onSessionStarted(session: CastSession, sessionId: String) = Unit
            override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
            override fun onSessionEnding(session: CastSession) = Unit
            override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
            override fun onSessionEnded(session: CastSession, error: Int) {
                appContext.stopService(Intent(appContext, AeroScreenStreamService::class.java))
            }
            override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = Unit
            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                appContext.stopService(Intent(appContext, AeroScreenStreamService::class.java))
            }
        }
        castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
        onDispose {
            runCatching { appContext.unregisterReceiver(receiver) }
            castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }

    val startScreenStreaming: () -> Unit = {
        val connected = runCatching {
            CastContext.getSharedInstance(appContext).sessionManager.currentCastSession?.isConnected == true
        }.getOrDefault(false)
        if (!connected) {
            SnackbarManager.show(appContext.getString(R.string.aero_cast_connect_first))
        } else {
            val needsAudioPermission = AeroScreenStreamSettings.audioEnabled(appContext) &&
                ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.RECORD_AUDIO) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (needsAudioPermission) {
                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } else {
                val manager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                capturePermissionLauncher.launch(manager.createScreenCaptureIntent())
            }
        }
    }
    val stopScreenStreaming: () -> Unit = {
        runCatching {
            CastContext.getSharedInstance(appContext)
                .sessionManager.currentCastSession?.remoteMediaClient?.stop()
        }
        appContext.stopService(Intent(appContext, AeroScreenStreamService::class.java))
    }


    if (showDialog) {
        AeroScreenStreamSettingsDialog(
            context = appContext,
            onDismiss = onDismiss,
            onStartStreaming = { startScreenStreaming() },
            onStopStreaming = { stopScreenStreaming() },
            isStreaming = isScreenStreaming,
        )
    }
}
