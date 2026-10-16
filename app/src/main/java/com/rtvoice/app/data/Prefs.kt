package com.rtvvoice.app.data

import android.content.Context
import android.content.SharedPreferences

/** Small persisted state: selected edition and reader speech rate. */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("rt_voice_prefs", Context.MODE_PRIVATE)

    var edition: Edition
        get() = Edition.byCode(sp.getString(KEY_EDITION, Edition.ENGLISH.code)!!)
        set(value) = sp.edit().putString(KEY_EDITION, value.code).apply()

    var speechRate: Float
        get() = sp.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = sp.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    private companion object {
        const val KEY_EDITION = "edition"
        const val KEY_SPEECH_RATE = "speech_rate"
    }
}
