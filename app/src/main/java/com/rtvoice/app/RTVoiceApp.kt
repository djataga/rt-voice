package com.rtvvoice.app

import android.app.Application
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.Prefs

class RTVoiceApp : Application() {

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    /** Edition selected the last time the app was used. */
    fun currentEdition(): Edition = prefs.edition
}
