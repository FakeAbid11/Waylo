package com.waylo.app.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

object WayloFormat {

    fun count(value: Int): String = count(value.toLong())

    fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    fun distance(kilometers: Double): String {
        val rounded = round(kilometers * TENTHS) / TENTHS
        val text = if (rounded == rounded.toLong().toDouble()) {
            count(rounded.toInt())
        } else {
            String.format(Locale.getDefault(), DECIMAL_PATTERN, rounded)
        }
        return "$text $DISTANCE_UNIT"
    }

    fun minutes(value: Int): String = "${count(value)} $TIME_UNIT"

    fun duration(milliseconds: Long): String {
        val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L
        return String.format(Locale.getDefault(), DURATION_PATTERN, hours, minutes, seconds)
    }

    private const val TENTHS = 10.0
    private const val DURATION_PATTERN = "%02d:%02d:%02d"
    private const val DECIMAL_PATTERN = "%.1f"
    private const val DISTANCE_UNIT = "km"
    private const val TIME_UNIT = "min"
}
