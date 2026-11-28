package app.gamenative.ui.screen.library.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouter
import app.gamenative.R
import app.gamenative.cast.AeroScreenStreamSettings
import com.google.android.gms.cast.framework.CastContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
internal fun AeroScreenStreamSettingsDialog(
    context: Context,
    onDismiss: () -> Unit,
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit,
    isStreaming: Boolean,
) {
    var resolution by remember { mutableStateOf(AeroScreenStreamSettings.resolution(context)) }
    var ratio by remember { mutableStateOf(AeroScreenStreamSettings.aspectRatio(context)) }
    var audio by remember { mutableStateOf(AeroScreenStreamSettings.audioEnabled(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.aero_cast_settings_title)) },
        text = {
            Column {
                Text(context.getString(R.string.aero_cast_connect_tv))
                CastRoutePicker(context)
                Text(context.getString(R.string.aero_cast_resolution), modifier = Modifier.padding(top = 10.dp))
                AeroScreenStreamSettings.Resolution.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = resolution == option, onClick = { resolution = option })
                        Text(
                            text = when (option) {
                                AeroScreenStreamSettings.Resolution.P480 -> context.getString(R.string.aero_cast_480p)
                                AeroScreenStreamSettings.Resolution.P720 -> context.getString(R.string.aero_cast_720p)
                                AeroScreenStreamSettings.Resolution.P1080 -> context.getString(R.string.aero_cast_1080p)
                            },
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Text(context.getString(R.string.aero_cast_aspect), modifier = Modifier.padding(top = 12.dp))
                AeroScreenStreamSettings.AspectRatio.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = ratio == option, onClick = { ratio = option })
                        Text(
                            text = when (option) {
                                AeroScreenStreamSettings.AspectRatio.ORIGINAL -> context.getString(R.string.aero_cast_aspect_original)
                                AeroScreenStreamSettings.AspectRatio.RATIO_16_9 -> "16:9"
                                AeroScreenStreamSettings.AspectRatio.RATIO_21_9 -> "21:9"
                            },
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(context.getString(R.string.aero_cast_audio), modifier = Modifier.weight(1f))
                    Switch(checked = audio, onCheckedChange = { audio = it })
                }
                Text(
                    text = if (isStreaming) {
                        context.getString(R.string.aero_cast_streaming)
                    } else {
                        context.getString(R.string.aero_cast_capture_notice)
                    },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            if (isStreaming) {
                TextButton(onClick = onStopStreaming) { Text(context.getString(R.string.aero_cast_stop)) }
            } else {
                TextButton(onClick = {
                    AeroScreenStreamSettings.setResolution(context, resolution)
                    AeroScreenStreamSettings.setAspectRatio(context, ratio)
                    AeroScreenStreamSettings.setAudioEnabled(context, audio)
                    onStartStreaming()
                }) { Text(context.getString(R.string.aero_cast_start)) }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!isStreaming) {
                    AeroScreenStreamSettings.setResolution(context, resolution)
                    AeroScreenStreamSettings.setAspectRatio(context, ratio)
                    AeroScreenStreamSettings.setAudioEnabled(context, audio)
                }
                onDismiss()
            }) { Text(context.getString(R.string.aero_cast_close)) }
        },
    )
}

/**
 * Lists the Chromecasts on the network and connects on tap. Google's own MediaRouteButton needs an AppCompat theme and a
 * FragmentActivity, which this app's activity is not, so the picker is drawn here and talks to MediaRouter directly.
 */
@Composable
private fun CastRoutePicker(context: Context) {
    val castContext = remember { runCatching { CastContext.getSharedInstance(context) }.getOrNull() }
    val router = remember { runCatching { MediaRouter.getInstance(context) }.getOrNull() }
    val selector = castContext?.mergedSelector
    var routes by remember { mutableStateOf(emptyList<MediaRouter.RouteInfo>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(selector, router) {
        if (selector == null || router == null) return@DisposableEffect onDispose {}
        fun refresh() {
            routes = router.routes.filter { it.matchesSelector(selector) }
            selectedId = router.selectedRoute.id
        }
        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
        }
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        refresh()
        onDispose { router.removeCallback(callback) }
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
        if (routes.isEmpty()) {
            Text(
                context.getString(R.string.aero_cast_searching),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        routes.forEach { route ->
            val connected = route.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (connected) {
                            runCatching { castContext?.sessionManager?.endCurrentSession(true) }
                        } else {
                            runCatching { router?.selectRoute(route) }
                        }
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = connected, onClick = null)
                Text(route.name, modifier = Modifier.padding(start = 8.dp).weight(1f))
            }
        }
    }
}
