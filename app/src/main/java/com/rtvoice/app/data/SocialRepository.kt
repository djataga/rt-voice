package com.rtvvoice.app.data

import com.rtvvoice.app.util.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches and caches RT RSS feeds for the Social tab. Feeds are small and
 * change slowly; a short in-memory cache keeps tab switches instant without
 * hammering the server.
 */
object SocialRepository {

    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, Pair<Long, List<RssItem>>>()

    suspend fun feed(url: String): List<RssItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val cached = cache[url]
            if (cached != null && now - cached.first < CACHE_TTL_MS) {
                return@withContext cached.second
            }
        }
        val xml = client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "RTVoice/1.0 (Android)")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Feed HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        val items = RssParser.parse(xml)
        if (items.isNotEmpty()) {
            synchronized(cache) { cache[url] = now to items }
        }
        items
    }

    /** Drops cached feeds (edition switch). */
    fun clear() {
        synchronized(cache) { cache.clear() }
    }
}
