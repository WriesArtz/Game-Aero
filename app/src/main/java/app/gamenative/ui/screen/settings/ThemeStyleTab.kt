package app.gamenative.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.PrefManager
import app.gamenative.R
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.AeroStyle
import app.gamenative.ui.theme.HOME_AERO
import app.gamenative.ui.theme.HOME_METRO
import app.gamenative.ui.theme.UiLook
import app.gamenative.ui.theme.UiLooks
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The Style tab of the themes: whole looks in one choice, which dashboard is drawn, the safe zone of the screen,
 * the glass, the size of the UI, the wallpaper and the emulators. These were in the interface settings before.
 */
@Composable
internal fun ThemeStyleTab(onMessage: (String) -> Unit, onWallpaper: () -> Unit) {
    val scope = rememberCoroutineScope()
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    var busyLook by remember { mutableStateOf<String?>(null) }
    var classic by remember { mutableStateOf(!PrefManager.aeroHome) }
    var safeZone by remember { mutableFloatStateOf(PrefManager.safeZonePercent.toFloat()) }
    var emulatorsOpen by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.look_apply_failed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionTitle(stringResource(R.string.style_looks_title))
        Text(
            text = stringResource(R.string.style_looks_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        UiLooks.all.forEach { look: UiLook ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusOutline(RoundedCornerShape(12.dp))
                    .selectable(
                        selected = busyLook == look.id,
                        role = Role.Button,
                        enabled = busyLook == null,
                        onClick = {
                            busyLook = look.id
                            scope.launch {
                                UiLooks.apply(look)
                                    .onSuccess {
                                        classic = false
                                        safeZone = PrefManager.safeZonePercent.toFloat()
                                    }
                                    .onFailure { onMessage(failedText) }
                                busyLook = null
                            }
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Text(text = look.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (busyLook == look.id) stringResource(R.string.working) else stringResource(look.hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionTitle(stringResource(R.string.style_home_title))
        HomeOption(
            label = stringResource(R.string.style_home_aero),
            selected = !classic && style.homeStyle == HOME_AERO,
            onSelect = {
                classic = false
                PrefManager.aeroHome = true
                AeroStyle.update { it.copy(homeStyle = HOME_AERO) }
            },
        )
        HomeOption(
            label = stringResource(R.string.style_home_metro),
            selected = !classic && style.homeStyle == HOME_METRO,
            onSelect = {
                classic = false
                PrefManager.aeroHome = true
                AeroStyle.update { it.copy(homeStyle = HOME_METRO) }
            },
        )
        HomeOption(
            label = stringResource(R.string.style_home_classic),
            selected = classic,
            onSelect = {
                classic = true
                PrefManager.aeroHome = false
            },
        )

        SectionTitle(stringResource(R.string.settings_interface_safe_zone, safeZone.toInt()))
        Text(
            text = stringResource(R.string.settings_interface_safe_zone_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = safeZone,
            onValueChange = { safeZone = it.roundToInt().toFloat() },
            onValueChangeFinished = { PrefManager.safeZonePercent = safeZone.toInt() },
            valueRange = 0f..15f,
            steps = 14,
        )

        SectionTitle(stringResource(R.string.aero_style_title))
        AeroStyleControls()
        RingTextButton(
            onClick = { AeroStyle.reset() },
            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
        ) { Text(stringResource(R.string.aero_style_reset)) }

        SectionTitle(stringResource(R.string.wallpaper_title))
        RingTextButton(
            onClick = onWallpaper,
            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
        ) { Text(stringResource(R.string.style_wallpaper_button)) }

        SectionTitle(stringResource(R.string.emulators_title))
        Text(
            text = stringResource(R.string.emulators_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        RingTextButton(
            onClick = { emulatorsOpen = true },
            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
        ) { Text(stringResource(R.string.emulators_manage)) }
    }

    if (emulatorsOpen) EmulatorsDialog(onDismiss = { emulatorsOpen = false })
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
private fun HomeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
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
