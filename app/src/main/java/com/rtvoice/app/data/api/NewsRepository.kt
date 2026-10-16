package com.rtvvoice.app.data.api

import com.rtvvoice.app.data.Edition

/**
 * Single source of API access. Recreates the Retrofit service whenever the
 * selected edition changes.
 */
object NewsRepository {

    @Volatile
    private var edition: Edition = Edition.ENGLISH

    @Volatile
    private var api: NewsApi = ApiFactory.createApi(edition)

    fun currentEdition(): Edition = edition

    fun switchEdition(newEdition: Edition) {
        if (newEdition.code == edition.code) return
        synchronized(this) {
            edition = newEdition
            api = ApiFactory.createApi(newEdition)
        }
    }

    suspend fun sections(): List<Section> = api.sections().data

    suspend fun latest(): List<Article> = api.latest().data

    suspend fun section(id: String): List<Article> = api.section(id).data

    suspend fun articleHtml(id: String): String = api.articleHtml(id)

    suspend fun article(id: String): Article = api.article(id).data
}
