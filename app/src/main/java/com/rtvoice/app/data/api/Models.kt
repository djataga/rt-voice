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

/** A 24/7 live channel from the `video` endpoint, HLS streams. */
data class LiveChannel(
    val id: String,
    val title: String,
    val url: String? = null,
    val schedule: String? = null,
    val video: Map<String, List<LiveStream>>? = null,
) {
    /** Best-effort: highest-bitrate HLS stream, falling back to the bare url. */
    val hlsUrl: String?
        get() = video?.get("hls")
            ?.maxByOrNull { it.bitrate?.toIntOrNull() ?: 0 }
            ?.url
            ?: url
}

data class LiveStream(
    val url: String? = null,
    val bitrate: String? = null,
    val qualityTitle: String? = null,
)
