package com.rtvvoice.app.tts

import android.content.Context
import com.rtvvoice.app.service.ReaderService
import com.rtvvoice.app.util.ArticleTextExtractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped owner of the [ArticleReader] so spoken playback survives leaving
 * the article screen. Exposes the playback status as a [StateFlow] consumed by
 * the article screen and by [ReaderService] (notification, media session).
 */
object ReaderManager {

    data class Status(
        val state: ArticleReader.State = ArticleReader.State.IDLE,
        val paragraphIndex: Int = 0,
        val paragraphCount: Int = 0,
        val title: String = "",
        val rate: Float = 1.0f,
    ) {
        val isActive: Boolean get() = state != ArticleReader.State.IDLE
    }

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors

    private var reader: ArticleReader? = null

    val currentState: ArticleReader.State
        get() = _status.value.state

    /**
     * Starts (or replaces) playback. The caller should also start
     * [ReaderService] so the foreground notification keeps the process alive.
     */
    fun start(
        context: Context,
        text: ArticleTextExtractor.ArticleText,
        languageTag: String,
        speechRate: Float,
        title: String,
    ) {
        val engine = reader ?: ArticleReader(context.applicationContext, listener).also {
            reader = it
        }
        _status.value = _status.value.copy(title = title, rate = speechRate)
        engine.start(text, languageTag, speechRate)
        ReaderService.start(context)
    }

    fun toggle() = reader?.toggle()

    fun pause() = reader?.pause()

    fun resume() = reader?.resume()

    fun nextParagraph() = reader?.nextParagraph()

    fun previousParagraph() = reader?.previousParagraph()

    fun setRate(rate: Float) {
        _status.value = _status.value.copy(rate = rate)
        reader?.setRate(rate)
    }

    fun stop() = reader?.stop()

    private fun publish(state: ArticleReader.State, paragraphIndex: Int, paragraphCount: Int) {
        _status.value = Status(
            state = state,
            paragraphIndex = paragraphIndex,
            paragraphCount = paragraphCount,
            title = _status.value.title,
            rate = _status.value.rate,
        )
    }

    private val listener = object : ArticleReader.Listener {
        override fun onReaderStateChange(state: ArticleReader.State, paragraphIndex: Int, paragraphCount: Int) {
            publish(state, paragraphIndex, paragraphCount)
        }

        override fun onReaderError(message: String) {
            _errors.tryEmit(message)
        }
    }
}
