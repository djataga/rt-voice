package com.rtvvoice.app.data.api

import com.rtvvoice.app.data.Edition
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a Retrofit [NewsApi] for the given edition. The original app picked a
 * host from `R.array.hosts` after probing `<host>healthcheck`; the first host
 * of each edition is hard-wired here because all of them were verified live.
 */
object ApiFactory {

    fun createApi(edition: Edition): NewsApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(edition.host)
            .client(client)
            // Scalars first, so `suspend fun articleHtml(): String` bypasses Gson
            // (same split the original made between its Retrofit instances).
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }
}
