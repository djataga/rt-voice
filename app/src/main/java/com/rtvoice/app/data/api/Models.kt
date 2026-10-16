package com.rtvvoice.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * Envelope used by every endpoint of the RT mobile API:
 * `{"data": ...}`. Same contract as the original app's `Message<T>`.
 */
data class Message<T>(val data: T)

data class Section(val id: String, val title: String)

data class Breaking(
    val id: String? = null,
    val title: String? = null,
    val url: String? = null,
)

data class Article(
    val id: String,
    val title: String,
    val summary: String? = null,
    val image: String? = null,
    @SerializedName("imageTitle") val imageCaption: String? = null,
    val section: String? = null,
    @SerializedName("sectionTitle") val sectionTitle: String? = null,
    val time: String? = null,
    val url: String? = null,
    val type: String? = null,
) {
    val hasWebUrl: Boolean get() = !url.isNullOrEmpty()
}

/** A program from the `shows` catalog. */
data class Show(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    val archive: Boolean? = null,
    val text: String? = null,
)

/** One playable episode of a show (`shows/{id}/episodes`). */
data class ShowEpisode(
    val id: String,
    val title: String,
    val summary: String? = null,
    val image: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    val time: String? = null,
    val show: String? = null,
    val video: EpisodeVideo? = null,
) {
    val playableUrl: String? get() = video?.url?.takeIf { it.isNotBlank() }
}

data class EpisodeVideo(
    val url: String? = null,
    val size: String? = null,
    @SerializedName("youtube_id") val youtubeId: String? = null,
)
