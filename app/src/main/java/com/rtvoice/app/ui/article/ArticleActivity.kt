package com.rtvvoice.app.ui.article

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.api.NewsRepository
import com.rtvvoice.app.data.db.BookmarksDb
import com.rtvvoice.app.databinding.ActivityArticleBinding
import com.rtvvoice.app.tts.ArticleReader
import com.rtvvoice.app.util.ArticleTextExtractor
import kotlinx.coroutines.launch

/**
 * Article detail screen. Renders the HTML document served by the API in a
 * WebView (same approach as the original app) and offers a voice reader that
 * speaks the extracted plain text paragraph by paragraph.
 *
 * If the article is bookmarked, its HTML is stored locally, so both reading
 * and listening keep working offline.
 */
class ArticleActivity : AppCompatActivity(), ArticleReader.Listener {

    private lateinit var binding: ActivityArticleBinding
    private lateinit var reader: ArticleReader
    private lateinit var bookmarks: BookmarksDb

    private val articleId: String by lazy { intent.getStringExtra(EXTRA_ID) ?: "" }
    private val articleTitle: String by lazy { intent.getStringExtra(EXTRA_TITLE).orEmpty() }

    private var html: String? = null
    private var loadedEdition: Edition? = null

    private val speeds = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var speedIndex = 1

    private val currentEdition: Edition
        get() = (application as RTVoiceApp).currentEdition()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = null
        }

        bookmarks = BookmarksDb.get(this)
        val savedRate = (application as RTVoiceApp).prefs.speechRate
        speedIndex = speeds.indexOfFirst { it == savedRate }.takeIf { it >= 0 } ?: 1

        reader = ArticleReader(this, this)

        binding.btnSpeed.text = formatSpeed(speeds[speedIndex])
        binding.btnSpeed.setOnClickListener { cycleSpeed() }
        binding.btnPlay.setOnClickListener {
            if (reader.currentState == ArticleReader.State.IDLE) startReader() else reader.toggle()
        }
        binding.btnPrev.setOnClickListener { reader.previousParagraph() }
        binding.btnNext.setOnClickListener { reader.nextParagraph() }
        binding.btnStop.setOnClickListener { reader.stop() }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "http" || uri.scheme == "https") {
                    // Keep API-relative document loads inside the WebView, open the rest outside.
                    if (uri.host?.endsWith(currentEdition.host.removePrefix("https://").removeSuffix("/").substringBefore('/')) == true) {
                        return false
                    }
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    return true
                }
                return true
            }
        }

        binding.refresh.setOnRefreshListener { loadArticle() }
        loadArticle()
    }

    private fun loadArticle() {
        binding.progress.visibility = View.VISIBLE
        binding.error.visibility = View.GONE
        lifecycleScope.launch {
            val result = runCatching { NewsRepository.articleHtml(articleId) }
            val fetched = result.getOrNull()
            if (fetched != null && fetched.isNotBlank()) {
                html = fetched
                loadedEdition = currentEdition
                renderHtml(fetched)
            } else {
                // Offline fallback: a bookmarked article keeps its stored HTML.
                val stored = bookmarks.getHtml(articleId)
                if (stored != null) {
                    html = stored
                    renderHtml(stored)
                } else {
                    binding.progress.visibility = View.GONE
                    binding.error.visibility = View.VISIBLE
                    binding.readerBar.visibility = View.GONE
                }
            }
            binding.refresh.isRefreshing = false
            invalidateOptionsMenu()
        }
    }

    private fun renderHtml(body: String) {
        binding.progress.visibility = View.GONE
        binding.error.visibility = View.GONE
        val base = currentEdition.host + "news/latest/$articleId/html/"
        binding.webView.loadDataWithBaseURL(base, body, "text/html", "utf-8", null)
        binding.readerBar.visibility = View.VISIBLE
    }

    // Voice reader

    private fun startReader() {
        val body = html
        if (body.isNullOrBlank()) {
            Toast.makeText(this, R.string.reader_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }
        val text = ArticleTextExtractor.extract(body)
        if (text == null) {
            Toast.makeText(this, R.string.reader_no_text, Toast.LENGTH_SHORT).show()
            return
        }
        binding.readerBar.visibility = View.VISIBLE
        reader.start(text, loadedEdition?.voiceTag ?: currentEdition.voiceTag, speeds[speedIndex])
    }

    override fun onReaderStateChange(state: ArticleReader.State, paragraphIndex: Int, paragraphCount: Int) {
        binding.btnPlay.setImageResource(
            when (state) {
                ArticleReader.State.PLAYING -> R.drawable.ic_pause
                else -> R.drawable.ic_play
            }
        )
        binding.btnPlay.contentDescription = getString(
            if (state == ArticleReader.State.PLAYING) R.string.reader_pause else R.string.reader_play
        )
        binding.progressLabel.visibility = if (paragraphCount == 0) View.GONE else View.VISIBLE
        binding.progressLabel.text =
            if (paragraphCount == 0) "" else getString(R.string.reader_progress, paragraphIndex + 1, paragraphCount)
    }

    override fun onReaderError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun cycleSpeed() {
        speedIndex = (speedIndex + 1) % speeds.size
        val speed = speeds[speedIndex]
        (application as RTVoiceApp).prefs.speechRate = speed
        reader.setRate(speed)
        binding.btnSpeed.text = formatSpeed(speed)
    }

    private fun formatSpeed(speed: Float): String =
        if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "$speed×"

    // Bookmarks

    private fun isBookmarked(): Boolean = bookmarks.contains(articleId)

    private fun toggleBookmark() {
        if (isBookmarked()) {
            bookmarks.remove(articleId)
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
            invalidateOptionsMenu()
        } else {
            lifecycleScope.launch {
                val article = runCatching { NewsRepository.article(articleId) }.getOrNull()
                bookmarks.add(
                    article = article ?: com.rtvvoice.app.data.api.Article(
                        id = articleId, title = articleTitle,
                    ),
                    html = html.orEmpty(),
                    editionCode = currentEdition.code,
                )
                Toast.makeText(this@ArticleActivity, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                invalidateOptionsMenu()
            }
        }
    }

    // Menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_article, menu)
        val item = menu.findItem(R.id.action_bookmark)
        item.setIcon(if (isBookmarked()) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        item.setTitle(if (isBookmarked()) R.string.bookmark_remove else R.string.bookmark_add)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        R.id.action_bookmark -> {
            toggleBookmark()
            true
        }
        R.id.action_share -> {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, articleTitle)
                putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(EXTRA_URL) ?: articleTitle)
            }
            startActivity(Intent.createChooser(share, getString(R.string.share_article)))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        reader.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"

        fun start(context: Context, article: com.rtvvoice.app.data.api.Article) {
            context.startActivity(
                Intent(context, ArticleActivity::class.java)
                    .putExtra(EXTRA_ID, article.id)
                    .putExtra(EXTRA_TITLE, article.title)
                    .putExtra(EXTRA_URL, article.url)
            )
        }
    }
}
