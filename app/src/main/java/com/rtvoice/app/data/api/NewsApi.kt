package com.rtvvoice.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Endpoints of the RT mobile API, reconstructed from the original app's
 * Retrofit interfaces (`com.rt.mobile.english.data.articles.*Service`).
 *
 * Paths are relative to the per-edition base URL, see [com.rtvvoice.app.data.Edition].
 */
interface NewsApi {

    @GET("news/latest")
    suspend fun latest(): Message<List<Article>>

    @GET("news/latest/{id}")
    suspend fun article(@Path("id") id: String): Message<Article>

    /** Raw HTML document of the article, rendered by the app in a WebView. */
    @GET("news/latest/{id}/html")
    suspend fun articleHtml(@Path("id") id: String): String

    @GET("news/{section}")
    suspend fun section(@Path("section") section: String): Message<List<Article>>

    /** The section catalog ("Latest", "World News", ...). */
    @GET("news")
    suspend fun sections(): Message<List<Section>>

    @GET("news/breaking")
    suspend fun breaking(): Message<Breaking>

    @GET("shows")
    suspend fun shows(): Message<List<Show>>

    @GET("shows/{id}/episodes")
    suspend fun episodes(@Path("id") showId: String): Message<List<ShowEpisode>>

    @GET("video")
    suspend fun liveChannels(): Message<List<LiveChannel>>
}
