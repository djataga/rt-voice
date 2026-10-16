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
