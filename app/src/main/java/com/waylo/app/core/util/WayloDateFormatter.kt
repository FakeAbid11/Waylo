package com.waylo.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date and time labels for the activity history and finished-activity screens.
 * Every function takes an explicit zone (and "now" where relative wording is needed)
 * so day boundaries are decided in the user's local time, never in raw UTC.
 */
object WayloDateFormatter {

    fun localDate(timestampMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()

    /** "Today", "Yesterday" or "Sep 28, 2026" for any other local day. */
    fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(ABBREVIATED_DATE)
    }

    /** "September 28, 2026". */
    fun fullDate(day: LocalDate): String = day.format(FULL_DATE)

    /** "6:42 PM". */
    fun timeOfDay(timestampMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(timestampMillis)
            .atZone(zone)
            .toLocalTime()
            .format(TIME_OF_DAY)

    /** "Today · 6:42 PM" - the history card timestamp line. */
    fun historyTimestamp(timestampMillis: Long, nowMillis: Long, zone: ZoneId): String =
        "${dayLabel(localDate(timestampMillis, zone), localDate(nowMillis, zone))} · " +
            timeOfDay(timestampMillis, zone)

    private val ABBREVIATED_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    private val FULL_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

    private val TIME_OF_DAY: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
}
