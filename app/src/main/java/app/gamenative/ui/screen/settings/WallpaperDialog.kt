package app.gamenative.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.service.SteamService
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.MenuColorScope
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.ThemedAlertDialog
import app.gamenative.ui.theme.WallpaperMode
import app.gamenative.ui.theme.WallpaperSettings
import kotlinx.coroutines.launch

/**
 * Picks the animated wallpaper drawn behind the main screens: off, the Frutiger Aero bubbles, a slow aurora, or a
 * looping video from the device. The wallpaper is independent of the theme, so any theme can be put over it.
 */
@Composable
fun WallpaperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by WallpaperSettings.state.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        SteamService.isImporting = false
        if (uri != null) {
            busy = true
            error = null
            scope.launch {
                WallpaperSettings.importVideo(context, uri).onFailure {
                    error = context.getString(R.string.wallpaper_video_failed, it.message ?: it.javaClass.simpleName)
                }
                busy = false
            }
        }
    }

    fun pickVideo() {
        SteamService.isImporting = true
        videoLauncher.launch(arrayOf("video/*", "image/gif", "image/webp"))
    }

    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(R.string.wallpaper_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.wallpaper_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    WallpaperOption(
                        label = stringResource(R.string.wallpaper_off),
                        selected = state.mode == WallpaperMode.OFF,
                        onSelect = { WallpaperSettings.setMode(WallpaperMode.OFF) },
                    )
                    WallpaperOption(
                        label = stringResource(R.string.wallpaper_bubbles),
                        selected = state.mode == WallpaperMode.BUBBLES,
                        onSelect = { WallpaperSettings.setMode(WallpaperMode.BUBBLES) },
                    )
                    WallpaperOption(
                        label = stringResource(R.string.wallpaper_water),
                        selected = state.mode == WallpaperMode.WATER,
                        onSelect = { WallpaperSettings.setMode(WallpaperMode.WATER) },
                    )
                    WallpaperOption(
                        label = stringResource(R.string.wallpaper_aurora),
                        selected = state.mode == WallpaperMode.AURORA,
                        onSelect = { WallpaperSettings.setMode(WallpaperMode.AURORA) },
                    )
                    WallpaperOption(
                        label = stringResource(R.string.wallpaper_video),
                        selected = state.mode == WallpaperMode.VIDEO,
                        onSelect = {
                            // a first choice of video needs a file before it can play
                            if (state.videoPath.isEmpty()) pickVideo() else WallpaperSettings.setMode(WallpaperMode.VIDEO)
                        },
                    )
                    if (state.mode == WallpaperMode.VIDEO || state.videoPath.isNotEmpty()) {
                        RingTextButton(
                            onClick = { pickVideo() },
                            enabled = !busy,
                            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
                        ) {
                            Text(
                                stringResource(
                                    if (state.videoPath.isEmpty()) R.string.wallpaper_choose_video else R.string.wallpaper_change_video,
                                ),
                            )
                        }
                    }
                    if (state.mode == WallpaperMode.VIDEO || busy) {
                        Text(
                            text = stringResource(if (busy) R.string.working else R.string.wallpaper_video_battery),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            },
        )
    }
}

@Composable
private fun WallpaperOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusOutline(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
