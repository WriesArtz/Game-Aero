package app.gamenative.utils

import android.content.Context
import app.gamenative.PrefManager
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Square cover art for the glass frames of the home screen, from SteamGridDB (a free community database of game art;
 * the person adds their own API key). Steam's own covers are tall, so they crop badly into a square frame.
 *
 * The answers, found or not, are kept in a file so each game is asked about once. Without a key nothing is asked and
 * the caller falls back to the cover the store gives.
 */
object CoverArt {
    private const val API = "https://www.steamgriddb.com/api/v2"
    private const val DIMENSIONS = "1024x1024,512x512"

    private val gate = Semaphore(3)
    private val cache = HashMap<String, String>()
    private var loaded = false
    private var file: File? = null

    val enabled: Boolean get() = PrefManager.steamGridDbKey.isNotBlank()

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val f = File(context.applicationContext.filesDir, "cover_art.json").also { file = it }
        if (!f.isFile) return
        runCatching {
            val o = JSONObject(f.readText())
            o.keys().forEach { cache[it] = o.getString(it) }
        }.onFailure { Timber.w(it, "cover_art.json unreadable") }
    }

    @Synchronized
    private fun remember(appId: String, url: String) {
        cache[appId] = url
        runCatching { file?.writeText(JSONObject(cache as Map<*, *>).toString()) }
    }

    /** Forgets what was found, so every game is asked about again, for example after a new key. */
    @Synchronized
    fun clear(context: Context) {
        ensureLoaded(context)
        cache.clear()
        runCatching { file?.delete() }
    }

    /** The square cover's address for [item], or null while there is no key, no art or no network. */
    suspend fun squareUrl(context: Context, item: LibraryItem): String? {
        val key = PrefManager.steamGridDbKey.trim()
        if (key.isEmpty() || item.isRecTeaser) return null
        ensureLoaded(context)
        synchronized(this) { cache[item.appId] }?.let { return it.ifEmpty { null } }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                try {
                    val url = if (item.gameSource == GameSource.STEAM && item.gameId > 0) {
                        firstGrid("$API/grids/steam/${item.gameId}", key)
                    } else {
                        val search = "$API/search/autocomplete".toHttpUrl().newBuilder().addPathSegment(item.name).build()
                        val id = firstId(search.toString(), key)
                        id?.let { firstGrid("$API/grids/game/$it", key) }
                    }
                    // a game with no art is remembered as such, but a network failure is not
                    remember(item.appId, url ?: "")
                    url
                } catch (e: Exception) {
                    Timber.d(e, "SteamGridDB lookup failed for ${item.name}")
                    null
                }
            }
        }
    }

    /** The address of a game's logo from SteamGridDB, for when the store has none; null without a key or a logo. */
    suspend fun logoUrl(context: Context, item: LibraryItem): String? {
        val key = PrefManager.steamGridDbKey.trim()
        if (key.isEmpty() || item.isRecTeaser) return null
        ensureLoaded(context)
        val cacheKey = "logo_${item.appId}"
        synchronized(this) { cache[cacheKey] }?.let { return it.ifEmpty { null } }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                try {
                    val url = if (item.gameSource == GameSource.STEAM && item.gameId > 0) {
                        firstLogo("$API/logos/steam/${item.gameId}", key)
                    } else {
                        val search = "$API/search/autocomplete".toHttpUrl().newBuilder().addPathSegment(item.name).build()
                        firstId(search.toString(), key)?.let { firstLogo("$API/logos/game/$it", key) }
                    }
                    remember(cacheKey, url ?: "")
                    url
                } catch (e: Exception) {
                    Timber.d(e, "SteamGridDB logo lookup failed for ${item.name}")
                    null
                }
            }
        }
    }

    private fun firstLogo(base: String, key: String): String? {
        val url = base.toHttpUrl().newBuilder().addQueryParameter("types", "static").build().toString()
        val data = get(url, key).optJSONArray("data") ?: return null
        return if (data.length() > 0) data.getJSONObject(0).optString("url").ifEmpty { null } else null
    }

    private fun get(url: String, key: String): JSONObject {
        val request = Request.Builder().url(url).header("Authorization", "Bearer $key").build()
        Net.http.newCall(request).execute().use { response ->
            if (response.code == 404) return JSONObject().put("data", org.json.JSONArray())
            if (!response.isSuccessful) throw java.io.IOException("SteamGridDB answered ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun firstGrid(base: String, key: String): String? {
        val url = base.toHttpUrl().newBuilder()
            .addQueryParameter("dimensions", DIMENSIONS)
            .addQueryParameter("types", "static")
            .addQueryParameter("nsfw", "false")
            .addQueryParameter("humor", "false")
            .build().toString()
        val data = get(url, key).optJSONArray("data") ?: return null
        return if (data.length() > 0) data.getJSONObject(0).optString("url").ifEmpty { null } else null
    }

    private fun firstId(url: String, key: String): Int? {
        val data = get(url, key).optJSONArray("data") ?: return null
        return if (data.length() > 0) data.getJSONObject(0).optInt("id").takeIf { it > 0 } else null
    }
}
