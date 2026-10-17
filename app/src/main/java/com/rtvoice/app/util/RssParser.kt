package com.rtvvoice.app.util

import com.rtvvoice.app.data.RssItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Minimal RSS 2.0 parser for the RT feeds. Titles, links and pubDates are
 * plain elements; descriptions carry CDATA HTML whose first <img> is the
 * thumbnail and whose trailing text ends with a boilerplate "Read Full
 * Article" link — both cleaned here.
 */
object RssParser {

    private val ARTICLE_ID = Regex("""/(\d{5,})(?:-[^/]*)?/?$""")

    fun parse(xml: String): List<RssItem> {
        if (xml.isBlank()) return emptyList()
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        return doc.select("item").mapNotNull { item ->
            val title = item.selectFirst("title")?.text()?.trim().orEmpty()
            val link = item.selectFirst("link")?.text()?.trim().orEmpty()
            if (title.isBlank() || link.isBlank()) return@mapNotNull null

            val descriptionHtml = item.selectFirst("description")?.text().orEmpty()
            val parsedDescription: Element? =
                if (descriptionHtml.isBlank()) null else Jsoup.parseBodyFragment(descriptionHtml)
            val thumbnail = parsedDescription?.selectFirst("img[src]")?.attr("src")
            val summary = parsedDescription?.text()
                ?.replace("Read Full Article at RT.com", "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            RssItem(
                title = title,
                link = link.substringBefore("?utm_"),
                publishedAt = parseDate(item.selectFirst("pubDate")?.text()),
                thumbnail = thumbnail,
                summary = summary,
            )
        }
    }

    /** Numeric article id from the link slug, e.g. /news/647324-trump-…/ → 647324. */
    fun articleId(link: String): String? =
        ARTICLE_ID.find(link.substringBefore("?"))?.groupValues?.get(1)

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            ZonedDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
