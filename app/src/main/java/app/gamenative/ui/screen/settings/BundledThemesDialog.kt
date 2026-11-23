package app.gamenative.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.theme.BundledTheme
import app.gamenative.theme.BundledThemes
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.MenuColorScope
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.ThemedAlertDialog

/** Lists the themes that ship with the app; picking one installs it and turns it on. */
@Composable
fun BundledThemesDialog(onPick: (BundledTheme) -> Unit, onDismiss: () -> Unit) {
    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(R.string.themes_bundled_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.themes_bundled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    BundledThemes.all.forEach { theme ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusOutline(RoundedCornerShape(12.dp))
                                .clickable { onPick(theme) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(text = theme.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = theme.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
