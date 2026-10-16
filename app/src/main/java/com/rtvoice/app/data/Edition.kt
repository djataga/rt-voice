package com.rtvvoice.app.data

import android.content.Context
import java.util.Locale

/**
 * A language edition of the news service.
 *
 * Hosts were extracted from the original RT News APK (localized "hosts"
 * string arrays) and verified live in October 2026. The RU/FR hosts answer
 * with HTTP redirects to their current domains; OkHttp follows them transparently.
 */
data class Edition(
    val code: String,
    val label: String,
    val host: String,
    val locale: Locale,
) {
    /** BCP-47 tag used to pick a matching TTS voice. */
    val voiceTag: String get() = locale.toLanguageTag()

    companion object {
        val ENGLISH = Edition("en", "English", "https://eng.mobileapiru.com/rtmobile/", Locale.US)
        val FRENCH = Edition("fr", "Français", "https://fra.mobileapiru.com/rtmobile/v3/", Locale.FRENCH)
        val GERMAN = Edition("de", "Deutsch", "https://ger.mobileapiru.com/rtmobile/", Locale.GERMANY)
        val SPANISH = Edition("es", "Español", "https://eps.mobileapiru.com/mobiledata/v3/", Locale("es", "ES"))
        val ARABIC = Edition("ar", "العربية", "https://arb.mobileapiru.com/rtmobile3/", Locale("ar"))
        val RUSSIAN = Edition("ru", "Русский", "https://rus.mobileapiru.com/api/mobile/", Locale("ru", "RU"))

        val ALL = listOf(ENGLISH, FRENCH, GERMAN, SPANISH, ARABIC, RUSSIAN)

        fun byCode(code: String): Edition = ALL.firstOrNull { it.code == code } ?: ENGLISH
    }
}
