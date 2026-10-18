package com.rtvvoice.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Resolves a Rumble live embed (`rumble.com/embed/<id>/`) to its direct HLS
 * playlist. The embed page is served without Cloudflare challenges (unlike
 * the embedJS JSON API) and carries the stream URL in its player config, so
 * a plain fetch plus regex is enough — no WebView needed.
 */
object RumbleResolver {

    private val M3U8 = Regex("""https://[^"']+?\.m3u8[^"']*""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun resolveLiveStream(embedUrl: String): String? = withContext(Dispatchers.IO) {
        val html = client.newCall(
            Request.Builder()
                .url(embedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36",
                )
                .header("Referer", "https://rt-english.com/")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        } ?: return@withContext null

        // The config JSON escapes slashes: "https:\/\/rumble.com\/live-hls\..."
        val unescaped = html.replace("\\/", "/")
        M3U8.find(unescaped)?.value
    }
}
