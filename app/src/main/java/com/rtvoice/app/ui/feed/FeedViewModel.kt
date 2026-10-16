package com.rtvvoice.app.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.api.Article
import com.rtvvoice.app.data.api.NewsRepository
import com.rtvvoice.app.data.api.Section
import kotlinx.coroutines.launch

/** Loads the section catalog and per-section article lists. */
class FeedViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs: com.rtvvoice.app.data.Prefs
        get() = (getApplication() as RTVoiceApp).prefs

    private val _sections = MutableLiveData<Result<List<Section>>>()
    val sections: LiveData<Result<List<Section>>> = _sections

    private val _editionChanged = MutableLiveData<Unit>()
    val editionChanged: LiveData<Unit> = _editionChanged

    private val feedResults = mutableMapOf<String, MutableLiveData<Result<List<Article>>>>()

    init {
        NewsRepository.switchEdition(prefs.edition)
    }

    fun articlesFor(sectionId: String): LiveData<Result<List<Article>>> =
        feedResults.getOrPut(sectionId) { MutableLiveData() }

    fun loadSections() {
        viewModelScope.launch {
            val result = runCatching {
                val list = NewsRepository.sections()
                // The API's catalog has no "Latest" entry; the original app prepended it.
                if (list.none { it.id == "latest" }) {
                    listOf(Section("latest", "Latest")) + list
                } else list
            }
            _sections.postValue(result)
        }
    }

    fun loadArticles(sectionId: String) {
        val live: MutableLiveData<Result<List<Article>>> =
            feedResults.getOrPut(sectionId) { MutableLiveData() }
        viewModelScope.launch {
            val result = runCatching {
                if (sectionId == "latest") NewsRepository.latest()
                else NewsRepository.section(sectionId)
            }
            live.postValue(result)
        }
    }

    fun selectEdition(edition: Edition) {
        prefs.edition = edition
        NewsRepository.switchEdition(edition)
        feedResults.clear()
        _editionChanged.value = Unit
        loadSections()
    }
}
