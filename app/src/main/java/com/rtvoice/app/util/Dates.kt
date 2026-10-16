package com.rtvvoice.app.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Parses the feed timestamp format used by the mobile API
 * (e.g. `20261016T040836`, UTC) and renders relative labels.
 */
object Dates {

    private val API_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)

    fun parse(time: String?): Long {
        if (time.isNullOrBlank()) return 0L
        return try {
            LocalDateTime.parse(time, API_FORMAT)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    /** "just now", "5 min ago", "3 h ago", "2 d ago" or an empty string. */
    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        if (millis <= 0L) return ""
        val diff = now - millis
        if (diff < 0) return ""
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 60 * 24 -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
            minutes < 60 * 24 * 30 -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
            else -> ""
        }
    }
}
