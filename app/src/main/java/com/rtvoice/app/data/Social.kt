package com.rtvvoice.app.data

/**
 * Social section data.
 *
 * X and Facebook offer no free programmatic read access (X is pay-per-use,
 * Facebook requires a page-admin token and banned RT globally in 2024), so
 * the accounts open as official pages in the in-app browser, while the actual
 * post feed comes from RT's public RSS mirrors — verified live in October
 * 2026. RSS item links map back to the mobile API by article id, so posts
 * open natively with the voice reader.
 */
data class SocialAccount(
    val name: String,
    val url: String,
)

data class SocialFeed(
    val id: String,
    val title: String,
    val url: String,
)

data class RssItem(
    val title: String,
    val link: String,
    val publishedAt: Long,
    val thumbnail: String?,
    val summary: String?,
)

object SocialCatalog {

    /** RT's global accounts. Telegram and VK open in their installed apps
     *  when available (t.me web previews are unreliable from some networks);
     *  the others are opened via LiveWebActivity. */
    val ACCOUNTS: List<SocialAccount> = listOf(
        SocialAccount("Telegram", "https://t.me/rtnews"),
        SocialAccount("X", "https://x.com/RT_com"),
        SocialAccount("Rumble", "https://rumble.com/c/RTNews"),
        SocialAccount("Odysee", "https://odysee.com/@RT"),
        SocialAccount("VK", "https://vk.com/rtnews"),
        SocialAccount("Facebook", "https://www.facebook.com/RTnews"),
    )

    fun feedsFor(edition: Edition): List<SocialFeed> = when (edition.code) {
        "fr" -> listOf(SocialFeed("all", "À la une", "https://rtenfrancais.tv/rss/"))
        "de" -> listOf(SocialFeed("all", "Aktuell", "https://de.rt.com/rss/"))
        "es" -> listOf(SocialFeed("all", "Noticias", "https://actualidad-rt.com/feeds/all.rss"))
        "ar" -> listOf(SocialFeed("all", "News", "https://arabic.rt.com/feeds/all.rss"))
        "ru" -> listOf(SocialFeed("all", "Новости", "https://russiatoday.ru/rss/"))
        else -> listOf(
            SocialFeed("news", "News", "https://www.rt.com/rss/news/"),
            SocialFeed("world", "World", "https://www.rt.com/rss/russia/"),
            SocialFeed("business", "Business", "https://www.rt.com/rss/business/"),
            SocialFeed("oped", "Op-ed", "https://www.rt.com/rss/op-ed/"),
        )
    }
}
