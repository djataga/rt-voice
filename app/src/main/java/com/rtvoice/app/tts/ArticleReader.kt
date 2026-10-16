package com.rtvvoice.app.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.rtvvoice.app.util.ArticleTextExtractor
import java.util.Locale

/**
 * Reads an article aloud with the system text-to-speech engine.
 *
 * The article is spoken paragraph by paragraph, which makes pause/resume and
 * paragraph skipping trivial: pausing stops the engine and remembering the
 * paragraph index; resuming re-speaks the current paragraph from its start.
 *
 * All callbacks are delivered on the main thread.
 */
class ArticleReader(
    context: Context,
    private val listener: Listener,
) : UtteranceProgressListener() {

    interface Listener {
        fun onReaderStateChange(state: State, paragraphIndex: Int, paragraphCount: Int)
        fun onReaderError(message: String)
    }

    enum class State { IDLE, PLAYING, PAUSED }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingStart: (() -> Unit)? = null

    private var paragraphs: List<String> = emptyList()
    private var index = 0
    private var state = State.IDLE
    private var rate = 1.0f
    private var locale: Locale = Locale.US
    private var languageAvailable = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) {
                listener.onReaderError("Text-to-speech engine is not available on this device")
                return@TextToSpeech
            }
            tts?.setOnUtteranceProgressListener(this)
            languageAvailable = tts?.setLanguage(locale)?.let {
                it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED
            } == true
            pendingStart?.invoke()
            pendingStart = null
        }
    }

    val currentState: State get() = state
    val currentRate: Float get() = rate
    val paragraphCount: Int get() = paragraphs.size

    fun setRate(newRate: Float) {
        rate = newRate
        if (state == State.PLAYING) speakCurrent(flush = true)
    }

    /** Starts (or restarts) reading the given text in the given language. */
    fun start(text: ArticleTextExtractor.ArticleText, languageTag: String, speechRate: Float) {
        rate = speechRate
        locale = Locale.forLanguageTag(languageTag)
        val items = buildList {
            add(text.title)
            text.subtitle?.let { add(it) }
            addAll(text.paragraphs)
        }
        if (items.isEmpty()) {
            listener.onReaderError("Nothing to read in this article")
            return
        }
        paragraphs = items
        index = 0
        if (!ready) {
            state = State.PLAYING
            pendingStart = { beginSpeaking() }
            // Initialisation may already have completed between calls.
            if (ready) { pendingStart = null; beginSpeaking() }
            emitState()
            return
        }
        state = State.PLAYING
        beginSpeaking()
        emitState()
    }

    fun toggle() = when (state) {
        State.PLAYING -> pause()
        State.PAUSED -> resume()
        State.IDLE -> Unit
    }

    fun pause() {
        if (state != State.PLAYING) return
        tts?.stop()
        state = State.PAUSED
        abandonFocus()
        emitState()
    }

    fun resume() {
        if (state != State.PAUSED) return
        state = State.PLAYING
        if (ready) speakCurrent(flush = true)
        emitState()
    }

    fun nextParagraph() = skip(+1)

    fun previousParagraph() = skip(-1)

    private fun skip(delta: Int) {
        if (paragraphs.isEmpty()) return
        val target = (index + delta).coerceIn(0, paragraphs.lastIndex)
        index = target
        state = State.PLAYING
        if (ready) speakCurrent(flush = true)
        emitState()
    }

    fun stop() {
        tts?.stop()
        index = 0
        paragraphs = emptyList()
        state = State.IDLE
        abandonFocus()
        emitState()
    }

    fun shutdown() {
        abandonFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        state = State.IDLE
        paragraphs = emptyList()
    }

    private fun beginSpeaking() {
        if (!requestFocus()) {
            state = State.IDLE
            listener.onReaderError("Audio focus is not available")
            emitState()
            return
        }
        if (!languageAvailable) {
            tts?.setLanguage(locale)?.let { result ->
                languageAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        speakCurrent(flush = true)
        emitState()
    }

    private fun speakCurrent(flush: Boolean) {
        val engine = tts ?: return
        val text = paragraphs.getOrNull(index) ?: run {
            state = State.IDLE
            abandonFocus()
            emitState()
            return
        }
        engine.setSpeechRate(rate)
        engine.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "para_$index")
    }

    private fun emitState() {
        listener.onReaderStateChange(state, index, paragraphs.size)
    }

    override fun onStart(utteranceId: String?) = Unit

    override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit

    override fun onDone(utteranceId: String?) {
        if (state != State.PLAYING) return
        if (index < paragraphs.lastIndex) {
            index++
            speakCurrent(flush = false)
            emitState()
        } else {
            state = State.IDLE
            index = 0
            abandonFocus()
            emitState()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) {
        if (state == State.IDLE) return
        state = State.IDLE
        abandonFocus()
        listener.onReaderError("The speech engine stopped unexpectedly")
        emitState()
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?, errorCode: Int) {
        onError(utteranceId)
    }

    private fun requestFocus(): Boolean {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS) pause()
            }
            .build()
            .also { focusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
}
