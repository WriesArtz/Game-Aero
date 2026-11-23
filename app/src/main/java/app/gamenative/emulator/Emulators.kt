package app.gamenative.emulator

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/** The emulators the library knows how to start straight into a game, without opening their own menus. */
enum class EmulatorKind(val label: String, val packages: List<String>) {
    /** RetroArch: starts `RetroActivityFuture` with the ROM, the core and the config as extras. */
    RETROARCH("RetroArch", listOf("com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32")),

    /** Eden (and the Yuzu forks that share its activity): opened with the game's document URI. */
    EDEN("Eden", listOf("dev.eden.eden_emulator")),

    /** Any other emulator that opens a game from a plain VIEW intent with the file's URI. */
    OTHER("Otro", emptyList()),
}

/** A console or system the person can pick: the RetroArch core to use, the file extensions and the cover set. */
class SystemPreset(
    val id: String,
    val label: String,
    val kind: EmulatorKind,
    val core: String,
    val extensions: String,
    /** Folder name of the libretro thumbnail server, for covers; empty for none. */
    val thumbs: String,
)

object SystemPresets {
    val all: List<SystemPreset> = listOf(
        SystemPreset("nes", "Nintendo NES", EmulatorKind.RETROARCH, "fceumm_libretro_android.so", "nes,fds,unf,zip,7z", "Nintendo - Nintendo Entertainment System"),
        SystemPreset("snes", "Super Nintendo", EmulatorKind.RETROARCH, "snes9x_libretro_android.so", "sfc,smc,swc,fig,zip,7z", "Nintendo - Super Nintendo Entertainment System"),
        SystemPreset("n64", "Nintendo 64", EmulatorKind.RETROARCH, "mupen64plus_next_gles3_libretro_android.so", "n64,z64,v64,zip,7z", "Nintendo - Nintendo 64"),
        SystemPreset("gbc", "Game Boy / Color", EmulatorKind.RETROARCH, "gambatte_libretro_android.so", "gb,gbc,zip,7z", "Nintendo - Game Boy Color"),
        SystemPreset("gba", "Game Boy Advance", EmulatorKind.RETROARCH, "mgba_libretro_android.so", "gba,zip,7z", "Nintendo - Game Boy Advance"),
        SystemPreset("nds", "Nintendo DS", EmulatorKind.RETROARCH, "melonds_libretro_android.so", "nds,zip,7z", "Nintendo - Nintendo DS"),
        SystemPreset("md", "Sega Mega Drive / Genesis", EmulatorKind.RETROARCH, "genesis_plus_gx_libretro_android.so", "md,gen,smd,bin,zip,7z", "Sega - Mega Drive - Genesis"),
        SystemPreset("dc", "Sega Dreamcast", EmulatorKind.RETROARCH, "flycast_libretro_android.so", "chd,cdi,gdi,cue", "Sega - Dreamcast"),
        SystemPreset("psx", "PlayStation", EmulatorKind.RETROARCH, "pcsx_rearmed_libretro_android.so", "chd,cue,pbp,iso,m3u", "Sony - PlayStation"),
        SystemPreset("psp", "PlayStation Portable", EmulatorKind.RETROARCH, "ppsspp_libretro_android.so", "iso,cso,pbp", "Sony - PlayStation Portable"),
        SystemPreset("pce", "PC Engine", EmulatorKind.RETROARCH, "mednafen_pce_fast_libretro_android.so", "pce,chd,cue,zip", "NEC - PC Engine - TurboGrafx 16"),
        SystemPreset("arcade", "Arcade (FBNeo)", EmulatorKind.RETROARCH, "fbneo_libretro_android.so", "zip,7z", "FBNeo - Arcade Games"),
        SystemPreset("retro", "RetroArch (other)", EmulatorKind.RETROARCH, "", "zip,7z,chd,cue,iso", ""),
        SystemPreset("switch", "Nintendo Switch", EmulatorKind.EDEN, "", "nsp,xci", "Nintendo - Nintendo Switch"),
        SystemPreset("other", "Other", EmulatorKind.OTHER, "", "", ""),
    )

    fun byId(id: String): SystemPreset? = all.firstOrNull { it.id == id }
    fun forKind(kind: EmulatorKind): List<SystemPreset> = all.filter { it.kind == kind }
}

/** One configured emulator: which app, which system, where its games are and how to start them. */
data class EmulatorEntry(
    val id: String,
    val kind: EmulatorKind,
    val name: String,
    val packageName: String = "",
    val systemId: String = "",
    /** RetroArch core file name, or an absolute path to one. */
    val core: String = "",
    /** Comma separated, without dots. */
    val extensions: String = "",
    /** Tree URI of the games folder, picked with the system folder picker. */
    val folderUri: String = "",
    /** RetroArch's retroarch.cfg, when it is not in the default place. */
    val configPath: String = "",
) {
    val label: String
        get() = SystemPresets.byId(systemId)?.let { if (kind == EmulatorKind.RETROARCH) "$name · ${it.label}" else name } ?: name
}

/** One game found in an emulator's folder. */
data class EmulatorGame(
    val entryId: String,
    val title: String,
    /** Document URI of the file. */
    val uri: String,
    /** File system path of the file, when it can be worked out, which RetroArch needs. */
    val path: String,
    /** Cover on the libretro thumbnail server, or empty. */
    val coverUrl: String,
    /** Name of the emulator and system it belongs to. */
    val groupLabel: String,
)

object EmulatorLibrary {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<EmulatorEntry>>(emptyList())
    private val _games = MutableStateFlow<List<EmulatorGame>>(emptyList())
    private val _scanning = MutableStateFlow(false)
    private var loaded = false

    val entries: StateFlow<List<EmulatorEntry>> = _entries.asStateFlow()
    val games: StateFlow<List<EmulatorGame>> = _games.asStateFlow()
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private fun file(context: Context) = File(context.applicationContext.filesDir, "emulators.json")

    /** Reads the saved emulators once and looks for their games in the background. */
    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val f = file(context)
        if (f.exists()) {
            runCatching { _entries.value = parse(f.readText()) }.onFailure { Timber.w(it, "emulators.json unreadable") }
        }
        if (_entries.value.isNotEmpty()) rescan(context)
    }

    fun save(context: Context, entry: EmulatorEntry) {
        ensureLoaded(context)
        val list = _entries.value.toMutableList()
        val at = list.indexOfFirst { it.id == entry.id }
        if (at >= 0) list[at] = entry else list.add(entry)
        write(context, list)
        rescan(context)
    }

    fun remove(context: Context, id: String) {
        ensureLoaded(context)
        write(context, _entries.value.filterNot { it.id == id })
        rescan(context)
    }

    private fun write(context: Context, list: List<EmulatorEntry>) {
        _entries.value = list
        runCatching { file(context).writeText(serialize(list)) }.onFailure { Timber.w(it, "emulators.json not saved") }
    }

    fun rescan(context: Context) {
        val app = context.applicationContext
        scope.launch {
            _scanning.value = true
            try {
                _games.value = _entries.value.flatMap { entry -> runCatching { scan(app, entry) }.getOrElse { emptyList() } }
            } finally {
                _scanning.value = false
            }
        }
    }

    /** Name of the first of the emulator's known apps that is installed, or empty. */
    fun installedPackage(context: Context, kind: EmulatorKind): String =
        kind.packages.firstOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        } ?: ""

    private const val MAX_DEPTH = 5
    private const val MAX_GAMES = 5000

    private fun scan(context: Context, entry: EmulatorEntry): List<EmulatorGame> {
        if (entry.folderUri.isBlank()) return emptyList()
        val root = DocumentFile.fromTreeUri(context, Uri.parse(entry.folderUri)) ?: return emptyList()
        val wanted = entry.extensions.split(',').map { it.trim().trimStart('.').lowercase() }.filter { it.isNotEmpty() }.toSet()
        val thumbs = SystemPresets.byId(entry.systemId)?.thumbs.orEmpty()
        val found = ArrayList<EmulatorGame>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (found.size >= MAX_GAMES) return
            for (child in dir.listFiles()) {
                if (found.size >= MAX_GAMES) return
                if (child.isDirectory) {
                    if (depth < MAX_DEPTH) walk(child, depth + 1)
                    continue
                }
                val name = child.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (wanted.isNotEmpty() && ext !in wanted) continue
                val base = name.substringBeforeLast('.')
                found += EmulatorGame(
                    entryId = entry.id,
                    title = cleanTitle(base),
                    uri = child.uri.toString(),
                    path = pathOf(child.uri),
                    coverUrl = if (thumbs.isEmpty()) "" else thumbnailUrl(thumbs, base),
                    groupLabel = entry.label,
                )
            }
        }
        walk(root, 0)
        return found.sortedBy { it.title.lowercase() }
    }

    /** "Super Mario World (USA) [!]" becomes "Super Mario World". */
    fun cleanTitle(base: String): String {
        val cleaned = base.replace(Regex("\\s*[(\\[][^)\\]]*[)\\]]"), "").trim()
        return cleaned.ifEmpty { base }
    }

    /** The libretro thumbnail server names covers after the file, with these characters turned into underscores. */
    fun thumbnailUrl(system: String, base: String): String {
        val safe = base.replace(Regex("[&*/:`<>?\\\\|\"]"), "_")
        return "https://thumbnails.libretro.com/${Uri.encode(system)}/Named_Boxarts/${Uri.encode(safe)}.png"
    }

    /** The path in the file system of a document, from its id: `primary:Roms/a.sfc` is `/storage/emulated/0/Roms/a.sfc`. */
    fun pathOf(documentUri: Uri): String {
        val id = runCatching { DocumentsContract.getDocumentId(documentUri) }.getOrNull() ?: return ""
        val volume = id.substringBefore(':', "")
        val rest = id.substringAfter(':', "")
        if (volume.isEmpty()) return ""
        return if (volume.equals("primary", ignoreCase = true)) "/storage/emulated/0/$rest" else "/storage/$volume/$rest"
    }

    private fun serialize(list: List<EmulatorEntry>): String {
        val array = JSONArray()
        for (e in list) {
            array.put(
                JSONObject()
                    .put("id", e.id).put("kind", e.kind.name).put("name", e.name).put("package", e.packageName)
                    .put("system", e.systemId).put("core", e.core).put("extensions", e.extensions)
                    .put("folder", e.folderUri).put("config", e.configPath),
            )
        }
        return array.toString(2)
    }

    private fun parse(text: String): List<EmulatorEntry> {
        val array = JSONArray(text)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val kind = runCatching { EmulatorKind.valueOf(o.optString("kind")) }.getOrNull() ?: return@mapNotNull null
            val id = o.optString("id")
            if (id.isEmpty()) return@mapNotNull null
            EmulatorEntry(
                id = id,
                kind = kind,
                name = o.optString("name"),
                packageName = o.optString("package"),
                systemId = o.optString("system"),
                core = o.optString("core"),
                extensions = o.optString("extensions"),
                folderUri = o.optString("folder"),
                configPath = o.optString("config"),
            )
        }
    }
}

/** Starts a game in its emulator, straight into the game. */
object EmulatorLauncher {
    sealed interface Result {
        data object Started : Result
        data class Failed(val reason: Reason) : Result
    }

    enum class Reason { NOT_INSTALLED, NO_PATH, NO_CORE, NO_ENTRY }

    fun launch(context: Context, game: EmulatorGame): Result {
        val entry = EmulatorLibrary.entries.value.firstOrNull { it.id == game.entryId }
            ?: return Result.Failed(Reason.NO_ENTRY)
        val app = context.applicationContext
        val pkg = entry.packageName.ifBlank { EmulatorLibrary.installedPackage(app, entry.kind) }
        if (pkg.isBlank()) return Result.Failed(Reason.NOT_INSTALLED)
        return try {
            when (entry.kind) {
                EmulatorKind.RETROARCH -> startRetroArch(app, pkg, entry, game)
                EmulatorKind.EDEN -> startEden(app, pkg, game)
                EmulatorKind.OTHER -> startView(app, pkg, game)
            }
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "emulator ${entry.name} ($pkg) cannot be started")
            Result.Failed(Reason.NOT_INSTALLED)
        } catch (e: SecurityException) {
            Timber.w(e, "emulator ${entry.name} ($pkg) refused the game")
            Result.Failed(Reason.NOT_INSTALLED)
        }
    }

    private fun startRetroArch(context: Context, pkg: String, entry: EmulatorEntry, game: EmulatorGame): Result {
        if (game.path.isBlank()) return Result.Failed(Reason.NO_PATH)
        if (entry.core.isBlank()) return Result.Failed(Reason.NO_CORE)
        val core = if (entry.core.startsWith("/")) entry.core else "/data/data/$pkg/cores/${entry.core}"
        val config = entry.configPath.ifBlank { "/storage/emulated/0/Android/data/$pkg/files/retroarch.cfg" }
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(pkg, "com.retroarch.browser.retroactivity.RetroActivityFuture")
            .putExtra("ROM", game.path)
            .putExtra("LIBRETRO", core)
            .putExtra("CONFIGFILE", config)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
        return Result.Started
    }

    /** Eden opens a game from the intent that an NFC tag would send, with the document as data; VIEW is the fallback. */
    private fun startEden(context: Context, pkg: String, game: EmulatorGame): Result {
        val data = Uri.parse(game.uri)
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        val first = Intent("android.nfc.action.TECH_DISCOVERED").setPackage(pkg).setData(data).addFlags(flags)
        return try {
            context.startActivity(first)
            Result.Started
        } catch (e: ActivityNotFoundException) {
            startView(context, pkg, game)
        }
    }

    private fun startView(context: Context, pkg: String, game: EmulatorGame): Result {
        val intent = Intent(Intent.ACTION_VIEW)
            .setPackage(pkg)
            .setData(Uri.parse(game.uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        return Result.Started
    }
}
