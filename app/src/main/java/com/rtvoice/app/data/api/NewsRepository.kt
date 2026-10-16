package com.rtvvoice.app.data.api

import com.rtvvoice.app.data.Edition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Single source of API access. Recreates the Retrofit service whenever the
 * selected edition changes and emits on [editionChanges] so cached view models
 * can invalidate themselves.
 */
object NewsRepository {

    @Volatile
    private var edition: Edition = Edition.ENGLISH

    @Volatile
    private var api: NewsApi = ApiFactory.createApi(edition)

    private val _editionChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val editionChanges: SharedFlow<Unit> = _editionChanges

    fun currentEdition(): Edition = edition

    fun switchEdition(newEdition: Edition) {
        if (newEdition.code == edition.code) return
        synchronized(this) {
            edition = newEdition
            api = ApiFactory.createApi(newEdition)
        }
        _editionChanges.tryEmit(Unit)
    }

    suspend fun sections(): List<Section> = api.sections().data

    suspend fun latest(): List<Article> = api.latest().data

    suspend fun section(id: String): List<Article> = api.section(id).data

    suspend fun articleHtml(id: String): String = api.articleHtml(id)

    suspend fun article(id: String): Article = api.article(id).data

    suspend fun shows(): List<Show> = api.shows().data

    suspend fun episodes(showId: String): List<ShowEpisode> = api.episodes(showId).data

    suspend fun liveChannels(): List<LiveChannel> = api.liveChannels().data
}
