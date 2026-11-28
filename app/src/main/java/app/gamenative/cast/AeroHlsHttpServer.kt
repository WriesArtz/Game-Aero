package app.gamenative.cast

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.thread

/**
 * Token-gated, local-network HTTP origin for the live HLS playlist and MPEG-TS segments.
 * It deliberately binds only to the device's LAN interfaces and never uses an internet relay.
 */
internal class AeroHlsHttpServer(
    private val segmenter: AeroHlsSegmenter,
    private val resolution: AeroScreenStreamSettings.Resolution,
    private val width: Int,
    private val height: Int,
    private val hasAudio: Boolean = false,
) {
    private val token = UUID.randomUUID().toString().replace("-", "")
    private var socket: ServerSocket? = null
    private var boundAddress: String? = null
    private var pool: ExecutorService? = null
    @Volatile private var running = false

    val streamUrl: String
        get() {
            val address = requireNotNull(boundAddress) { "The local stream server has not started." }
            val port = requireNotNull(socket).localPort
            return "http://$address:$port/aero/$token/master.m3u8"
        }

    fun start() {
        check(!running)
        val address = requireNotNull(lanIpv4()) { "No local IPv4 address. Connect the phone and TV to the same Wi-Fi network." }
        val server = ServerSocket()
        server.reuseAddress = true
        server.bind(InetSocketAddress(address, 0), 12)
        boundAddress = address
        socket = server
        val workers = Executors.newFixedThreadPool(6) { task -> Thread(task, "aero-hls-client").apply { isDaemon = true } }
        pool = workers
        running = true
        thread(name = "aero-hls-http", isDaemon = true) {
            while (running) {
                try {
                    val client = server.accept()
                    try {
                        workers.execute { serve(client) }
                    } catch (_: RejectedExecutionException) {
                        runCatching { client.close() }
                    }
                } catch (error: Throwable) {
                    if (running) android.util.Log.w("AeroHlsServer", "HTTP accept failed", error)
                }
            }
        }
    }

    private fun serve(client: Socket) {
        client.use { connection ->
            runCatching {
                connection.soTimeout = 5_000
                connection.tcpNoDelay = true
                connection.sendBufferSize = 256 * 1024
                val reader = BufferedReader(InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(' ')
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore('?')
                while (true) {
                    val header = reader.readLine() ?: break
                    if (header.isEmpty()) break
                }

                if (method == "OPTIONS") {
                    respond(connection, 204, "No Content", "text/plain", ByteArray(0), headOnly = true)
                    return
                }
                if (method != "GET" && method != "HEAD") {
                    respond(connection, 405, "Method Not Allowed", "text/plain", "Method not allowed".toByteArray(), method == "HEAD")
                    return
                }

                val base = "/aero/$token/"
                if (!path.startsWith(base)) {
                    respond(connection, 404, "Not Found", "text/plain", ByteArray(0), method == "HEAD")
                    return
                }
                val resource = path.removePrefix(base)
                when {
                    resource == "master.m3u8" -> {
                        val macroblocksPerSecond = ((width + 15) / 16) * ((height + 15) / 16) * 30
                        val level = if (macroblocksPerSecond > 108_000) "28" else "1F"
                        val playlist = buildString {
                            append("#EXTM3U\n")
                            append("#EXT-X-VERSION:3\n")
                            append("#EXT-X-STREAM-INF:BANDWIDTH=${resolution.bitrate},CODECS=\"avc1.42E0$level${if (hasAudio) ",mp4a.40.2" else ""}\",RESOLUTION=${width}x$height\n")
                            append("media.m3u8\n")
                        }
                        respond(connection, 200, "OK", "application/x-mpegURL", playlist.toByteArray(), method == "HEAD")
                    }
                    resource == "media.m3u8" -> {
                        respond(connection, 200, "OK", "application/x-mpegURL", segmenter.playlist().toByteArray(), method == "HEAD")
                    }
                    resource.startsWith("seg-") && resource.endsWith(".ts") -> {
                        val sequence = resource.removePrefix("seg-").removeSuffix(".ts").toIntOrNull()
                        val bytes = sequence?.let(segmenter::segment)
                        if (bytes == null) {
                            respond(connection, 404, "Not Found", "text/plain", ByteArray(0), method == "HEAD")
                        } else {
                            respond(connection, 200, "OK", "video/mp2t", bytes, method == "HEAD")
                        }
                    }
                    else -> respond(connection, 404, "Not Found", "text/plain", ByteArray(0), method == "HEAD")
                }
            }.onFailure { android.util.Log.w("AeroHlsServer", "Request failed", it) }
        }
    }

    private fun respond(
        client: Socket,
        status: Int,
        reason: String,
        contentType: String,
        body: ByteArray,
        headOnly: Boolean,
    ) {
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: Range, Accept, Content-Type\r\n")
            append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
            append("Connection: close\r\n\r\n")
        }
        val output = client.getOutputStream()
        output.write(headers.toByteArray(StandardCharsets.US_ASCII))
        if (!headOnly && body.isNotEmpty()) output.write(body)
        output.flush()
    }

    private fun lanIpv4(): String? {
        val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }.getOrDefault(emptyList())
        return interfaces
            .filter { network ->
                runCatching { network.isUp && !network.isLoopback }.getOrDefault(false)
            }
            .sortedBy { network ->
                when {
                    network.name.startsWith("wlan", true) -> 0
                    network.name.startsWith("eth", true) -> 1
                    else -> 2
                }
            }
            .asSequence()
            .flatMap { network -> Collections.list(network.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }

    fun stop() {
        running = false
        runCatching { pool?.shutdownNow() }
        pool = null
        runCatching { socket?.close() }
        socket = null
        boundAddress = null
    }
}
