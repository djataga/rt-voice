package com.rtvvoice.app.ui.article

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.api.NewsRepository
import com.rtvvoice.app.data.db.BookmarksDb
import com.rtvvoice.app.databinding.ActivityArticleBinding
import com.rtvvoice.app.tts.ArticleReader
import com.rtvvoice.app.tts.ReaderManager
import com.rtvvoice.app.util.ArticleTextExtractor
import kotlinx.coroutines.launch

/**
 * Article detail screen. Renders the HTML document served by the API in a
 * WebView (same approach as the original app) and offers a voice reader that
 * speaks the extracted plain text paragraph by paragraph — continuing in the
 * background via [com.rtvvoice.app.service.ReaderService] once started.
 *
 * If the article is bookmarked, its HTML is stored locally, so both reading
 * and listening keep working offline.
 */
class ArticleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleBinding
    private lateinit var bookmarks: BookmarksDb

    private val articleId: String by lazy { intent.getStringExtra(EXTRA_ID) ?: "" }
    private val articleTitle: String by lazy { intent.getStringExtra(EXTRA_TITLE).orEmpty() }

    private var html: String? = null
    private var loadedEdition: Edition? = null

    private val speeds = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var speedIndex = 1

    private val currentEdition: Edition
        get() = (application as RTVoiceApp).currentEdition()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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

        binding.btnSpeed.text = formatSpeed(speeds[speedIndex])
        binding.btnSpeed.setOnClickListener { cycleSpeed() }
        binding.btnPlay.setOnClickListener {
            if (ReaderManager.currentState == ArticleReader.State.IDLE) startReader()
            else ReaderManager.toggle()
        }
        binding.btnPrev.setOnClickListener { ReaderManager.previousParagraph() }
        binding.btnNext.setOnClickListener { ReaderManager.nextParagraph() }
        binding.btnStop.setOnClickListener { ReaderManager.stop() }

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReaderManager.status.collect { renderReaderState(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ReaderManager.errors.collect { message ->
                    Toast.makeText(this@ArticleActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
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
        maybeRequestNotificationPermission()
        ReaderManager.start(
            context = this,
            text = text,
            languageTag = loadedEdition?.voiceTag ?: currentEdition.voiceTag,
            speechRate = speeds[speedIndex],
            title = articleTitle,
        )
        binding.readerBar.visibility = View.VISIBLE
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun renderReaderState(status: ReaderManager.Status) {
        val playing = status.state == ArticleReader.State.PLAYING
        binding.btnPlay.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        binding.btnPlay.contentDescription = getString(
            if (playing) R.string.reader_pause else R.string.reader_play
        )
        binding.btnSpeed.text = formatSpeed(status.rate)
        val showProgress = status.isActive && status.paragraphCount > 0
        binding.progressLabel.visibility = if (showProgress) View.VISIBLE else View.GONE
        binding.progressLabel.text =
            if (showProgress) getString(R.string.reader_progress, status.paragraphIndex + 1, status.paragraphCount)
            else ""
    }

    private fun cycleSpeed() {
        speedIndex = (speedIndex + 1) % speeds.size
        val speed = speeds[speedIndex]
        (application as RTVoiceApp).prefs.speechRate = speed
        ReaderManager.setRate(speed)
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
