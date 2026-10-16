package com.rtvvoice.app.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Plain-text extraction from the article HTML served by the mobile API.
 *
 * The HTML ships with stable semantic anchors (verified against live responses):
 * `#title` for the headline, `.app-mobile__subtitle` for the lead paragraph and
 * `#content` for the body. Anything else (script, style, nav) is dropped.
 */
object ArticleTextExtractor {

    data class ArticleText(
        val title: String,
        val subtitle: String?,
        val paragraphs: List<String>,
    ) {
        val paragraphCount: Int get() = paragraphs.size
    }

    fun extract(html: String): ArticleText? {
        if (html.isBlank()) return null
        val doc: Document = Jsoup.parse(html)

        val title = doc.selectFirst("#title")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: doc.title()
        if (title.isBlank()) return null

        val subtitle = doc.selectFirst(".app-mobile__subtitle")?.text()?.takeIf { it.isNotBlank() }

        val body = doc.selectFirst("#content")
            ?: doc.selectFirst(".app-mobile__text")
            ?: doc.body()

        val paragraphs = body?.select("p")
            .orEmpty()
            .map { it.text().trim() }
            .filter { it.length > 1 }

        if (paragraphs.isEmpty()) return null
        return ArticleText(title, subtitle, paragraphs)
    }
}
