package com.rtvvoice.app.data

/**
 * Live channel catalog.
 *
 * The original app's `video` API endpoint still returns the old
 * `rt-glb.rttv.com` host, which changed and no longer plays everywhere — the
 * working sources below were recovered (October 2026) from the live pages of
 * the RT websites: rt-english.com/on-air (plus rumble/odysee variants),
 * en.rtdoc.tv/live, rtenfrancais.tv/en-direct, de.rt.com/livetv,
 * actualidad.rt.com/en_vivo and arabic.rt.com/live.
 *
 * HLS sources play in the in-app player (media3); the site players
 * (Rumble / Odysee / VK) are opened in an in-app WebView.
 *
 * Note: the *.rttv.com CDN resolves IPv6-only; on networks without IPv6 only
 * the web players work.
 */
enum class LiveSourceKind { HLS, WEB }

data class LiveSource(
    val label: String,
    val url: String,
    val kind: LiveSourceKind,
)

data class LiveChannel(
    val id: String,
    val title: String,
    val sources: List<LiveSource>,
) {
    val hlsSource: LiveSource? get() = sources.firstOrNull { it.kind == LiveSourceKind.HLS }
}

object LiveCatalog {

    private val rtNewsEn = LiveChannel(
        id = "rt_news_en",
        title = "RT News",
        sources = listOf(
            LiveSource("App player", "https://rt-glb.rttv.com/dvr/rtnews/playlist.m3u8", LiveSourceKind.HLS),
            LiveSource("Website player", "https://rt-english.com/on-air/", LiveSourceKind.WEB),
            LiveSource("Rumble", "https://rt-english.com/on-air/rumble/", LiveSourceKind.WEB),
            LiveSource("Odysee", "https://rt-english.com/on-air/odysee/", LiveSourceKind.WEB),
        ),
    )

    private val rtd = LiveChannel(
        id = "rtd",
        title = "RTD",
        sources = listOf(
            LiveSource("App player", "https://rt-rtd.rttv.com/dvr/rtdoc/playlist.m3u8", LiveSourceKind.HLS),
            LiveSource("Website player", "https://en.rtdoc.tv/live", LiveSourceKind.WEB),
        ),
    )

    private val rtIndia = LiveChannel(
        id = "rt_india",
        title = "RT India",
        sources = listOf(
            LiveSource("App player", "https://rt-india.rttv.com/dvr/rtindia/playlist.m3u8", LiveSourceKind.HLS),
            LiveSource("Website player", "https://rt-english.com/on-air/rt-india-air/", LiveSourceKind.WEB),
        ),
    )

    fun forEdition(edition: Edition): List<LiveChannel> = when (edition.code) {
        "en" -> listOf(rtNewsEn, rtd, rtIndia)
        "fr" -> listOf(
            LiveChannel(
                id = "rt_france",
                title = "RT France",
                sources = listOf(
                    LiveSource("Website player", "https://rtenfrancais.tv/en-direct/", LiveSourceKind.WEB),
                    LiveSource("Odysee", "https://odysee.com/@RTFRANCE", LiveSourceKind.WEB),
                ),
            ),
        )
        "de" -> listOf(
            LiveChannel(
                id = "rt_de",
                title = "RT DE",
                sources = listOf(
                    LiveSource("App player", "https://rt-ger.rttv.com/dvr/rtdeutsch/playlist.m3u8", LiveSourceKind.HLS),
                    LiveSource("Website player", "https://de.rt.com/livetv/", LiveSourceKind.WEB),
                ),
            ),
        )
        "es" -> listOf(
            LiveChannel(
                id = "rt_es",
                title = "RT en Español",
                sources = listOf(
                    LiveSource("App player", "https://rt-esp.rttv.com/dvr/rtesp/playlist.m3u8", LiveSourceKind.HLS),
                    LiveSource("Website player", "https://actualidad.rt.com/en_vivo", LiveSourceKind.WEB),
                ),
            ),
        )
        "ar" -> listOf(
            LiveChannel(
                id = "rt_ar",
                title = "RT Arabic",
                sources = listOf(
                    LiveSource("App player", "https://rt-arb.rttv.com/dvr/rtarab/playlist.m3u8", LiveSourceKind.HLS),
                    LiveSource("Website player", "https://arabic.rt.com/live/", LiveSourceKind.WEB),
                ),
            ),
        )
        // The Russian-language site no longer carries a live TV page.
        "ru" -> emptyList()
        else -> listOf(rtNewsEn, rtd, rtIndia)
    }
}
