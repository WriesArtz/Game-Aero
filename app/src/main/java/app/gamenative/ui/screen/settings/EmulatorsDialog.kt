package app.gamenative.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.emulator.EmulatorEntry
import app.gamenative.emulator.EmulatorKind
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.emulator.SystemPresets
import app.gamenative.service.SteamService
import app.gamenative.ui.component.RingTextButton
import app.gamenative.ui.theme.MenuColorScope
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.ThemedAlertDialog
import java.util.UUID

/** The emulators whose games show in the library: add one, change it, take it away or look for games again. */
@Composable
fun EmulatorsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffectEnsureLoaded()
    val entries by EmulatorLibrary.entries.collectAsStateWithLifecycle()
    val games by EmulatorLibrary.games.collectAsStateWithLifecycle()
    val scanning by EmulatorLibrary.scanning.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EmulatorEntry?>(null) }
    var adding by remember { mutableStateOf(false) }

    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(R.string.emulators_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.emulators_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    if (entries.isEmpty()) {
                        Text(text = stringResource(R.string.emulators_none), style = MaterialTheme.typography.bodyMedium)
                    }
                    entries.forEach { entry ->
                        val count = games.count { it.entryId == entry.id }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusOutline(RoundedCornerShape(12.dp))
                                .selectable(selected = false, role = Role.Button, onClick = { editing = entry })
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(text = entry.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (scanning) {
                                    stringResource(R.string.emulators_scanning)
                                } else {
                                    stringResource(R.string.emulators_games_count, count)
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RingTextButton(
                            onClick = { adding = true },
                            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
                        ) { Text(stringResource(R.string.emulators_add)) }
                        if (entries.isNotEmpty()) {
                            RingTextButton(
                                onClick = { EmulatorLibrary.rescan(context) },
                                enabled = !scanning,
                                modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
                            ) { Text(stringResource(R.string.emulators_rescan)) }
                        }
                    }
                }
            },
            confirmButton = {
                RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            },
        )
    }

    if (adding) {
        EmulatorEditDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = {
                EmulatorLibrary.save(context, it)
                adding = false
            },
            onDelete = null,
        )
    }
    editing?.let { entry ->
        EmulatorEditDialog(
            initial = entry,
            onDismiss = { editing = null },
            onSave = {
                EmulatorLibrary.save(context, it)
                editing = null
            },
            onDelete = {
                EmulatorLibrary.remove(context, entry.id)
                editing = null
            },
        )
    }
}

@Composable
private fun LaunchedEffectEnsureLoaded() {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(context) }
}

@Composable
private fun EmulatorEditDialog(
    initial: EmulatorEntry?,
    onDismiss: () -> Unit,
    onSave: (EmulatorEntry) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(initial?.kind ?: EmulatorKind.RETROARCH) }
    var systemId by remember { mutableStateOf(initial?.systemId ?: SystemPresets.forKind(EmulatorKind.RETROARCH).first().id) }
    var name by remember { mutableStateOf(initial?.name ?: EmulatorKind.RETROARCH.label) }
    var pkg by remember { mutableStateOf(initial?.packageName ?: EmulatorLibrary.installedPackage(context, EmulatorKind.RETROARCH)) }
    var core by remember { mutableStateOf(initial?.core ?: SystemPresets.forKind(EmulatorKind.RETROARCH).first().core) }
    var extensions by remember { mutableStateOf(initial?.extensions ?: SystemPresets.forKind(EmulatorKind.RETROARCH).first().extensions) }
    var config by remember { mutableStateOf(initial?.configPath ?: "") }
    var folder by remember { mutableStateOf(initial?.folderUri ?: "") }
    var systemMenu by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        SteamService.isImporting = false
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            folder = uri.toString()
        }
    }

    fun applyPreset(id: String) {
        systemId = id
        val preset = SystemPresets.byId(id) ?: return
        core = preset.core
        extensions = preset.extensions
    }

    fun pickKind(next: EmulatorKind) {
        kind = next
        name = next.label
        pkg = EmulatorLibrary.installedPackage(context, next)
        applyPreset(SystemPresets.forKind(next).first().id)
    }

    MenuColorScope {
        ThemedAlertDialog(
            onDismissRequest = onDismiss,
            containerColor = PluviaTheme.dialogContainerColor,
            title = { Text(stringResource(if (initial == null) R.string.emulators_add else R.string.emulators_edit)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (initial == null) {
                        EmulatorKind.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusOutline(RoundedCornerShape(12.dp))
                                    .selectable(selected = kind == option, role = Role.RadioButton, onClick = { pickKind(option) })
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = kind == option, onClick = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Box {
                        RingTextButton(
                            onClick = { systemMenu = true },
                            modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
                        ) {
                            Text(
                                stringResource(
                                    R.string.emulators_system,
                                    SystemPresets.byId(systemId)?.label ?: stringResource(R.string.emulators_system_none),
                                ),
                            )
                        }
                        DropdownMenu(expanded = systemMenu, onDismissRequest = { systemMenu = false }) {
                            SystemPresets.forKind(kind).forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.label) },
                                    onClick = {
                                        applyPreset(preset.id)
                                        systemMenu = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.emulators_field_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = pkg,
                        onValueChange = { pkg = it.trim() },
                        label = { Text(stringResource(R.string.emulators_field_package)) },
                        supportingText = { Text(stringResource(R.string.emulators_field_package_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (kind == EmulatorKind.RETROARCH) {
                        OutlinedTextField(
                            value = core,
                            onValueChange = { core = it.trim() },
                            label = { Text(stringResource(R.string.emulators_field_core)) },
                            supportingText = { Text(stringResource(R.string.emulators_field_core_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = config,
                            onValueChange = { config = it.trim() },
                            label = { Text(stringResource(R.string.emulators_field_config)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = extensions,
                        onValueChange = { extensions = it.replace(" ", "").lowercase() },
                        label = { Text(stringResource(R.string.emulators_field_extensions)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = if (folder.isEmpty()) {
                            stringResource(R.string.emulators_folder_none)
                        } else {
                            Uri.decode(Uri.parse(folder).lastPathSegment ?: folder)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    RingTextButton(
                        onClick = {
                            SteamService.isImporting = true
                            folderLauncher.launch(null)
                        },
                        modifier = Modifier.focusOutline(RoundedCornerShape(12.dp), ring = false),
                    ) { Text(stringResource(R.string.emulators_choose_folder)) }
                }
            },
            dismissButton = {
                if (onDelete != null) {
                    RingTextButton(onClick = onDelete) { Text(stringResource(R.string.emulators_remove)) }
                } else {
                    RingTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            },
            confirmButton = {
                RingTextButton(
                    enabled = folder.isNotEmpty() && name.isNotBlank(),
                    onClick = {
                        onSave(
                            EmulatorEntry(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                kind = kind,
                                name = name.trim(),
                                packageName = pkg,
                                systemId = systemId,
                                core = core,
                                extensions = extensions,
                                folderUri = folder,
                                configPath = config,
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.save)) }
            },
        )
    }
}
