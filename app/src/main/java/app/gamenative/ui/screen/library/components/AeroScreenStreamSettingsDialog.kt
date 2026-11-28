package app.gamenative.ui.screen.library.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
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
import app.gamenative.R
import app.gamenative.cast.AeroScreenStreamSettings

@Composable
internal fun AeroScreenStreamSettingsDialog(
    context: Context,
    onDismiss: () -> Unit,
) {
    var resolution by remember { mutableStateOf(AeroScreenStreamSettings.resolution(context)) }
    var ratio by remember { mutableStateOf(AeroScreenStreamSettings.aspectRatio(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.aero_cast_settings_title)) },
        text = {
            Column {
                Text(context.getString(R.string.aero_cast_resolution))
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
                                AeroScreenStreamSettings.AspectRatio.RATIO_4_3 -> "4:3"
                                AeroScreenStreamSettings.AspectRatio.RATIO_21_9 -> "21:9"
                            },
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Text(
                    text = context.getString(R.string.aero_cast_settings_pending),
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AeroScreenStreamSettings.setResolution(context, resolution)
                AeroScreenStreamSettings.setAspectRatio(context, ratio)
                onDismiss()
            }) { Text(context.getString(R.string.aero_cast_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(context.getString(R.string.aero_cast_cancel)) }
        },
    )
}
