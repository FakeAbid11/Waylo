package com.waylo.app.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

object WayloFormat {

    fun count(value: Int): String = count(value.toLong())

    fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    fun distance(kilometers: Double): String {
        if (!kilometers.isFinite() || kilometers < 0.0) return "0 $DISTANCE_UNIT"
        val totalMeters = round(kilometers * METERS_PER_KILOMETER).toLong()
        return if (totalMeters < METERS_PER_KILOMETER) {
            "${count(totalMeters)} $SHORT_DISTANCE_UNIT"
        } else {
            val roundedKm = round(kilometers * HUNDREDTHS) / HUNDREDTHS
            String.format(Locale.getDefault(), KM_PATTERN, roundedKm)
        }
    }

    /** Pace as minutes per kilometer, e.g. 6:30 /km. Null or invalid values show a dash. */
    fun pace(secondsPerKilometer: Double?): String {
        if (secondsPerKilometer == null ||
            !secondsPerKilometer.isFinite() ||
            secondsPerKilometer <= 0.0
        ) {
            return DASH
        }
        val totalSeconds = round(secondsPerKilometer).toLong().coerceAtLeast(1L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.getDefault(), PACE_PATTERN, minutes, seconds)
    }

    /** Speed as kilometers per hour, e.g. 4.8 km/h. Null or invalid values show a dash. */
    fun speed(metersPerSecond: Double?): String {
        if (metersPerSecond == null || !metersPerSecond.isFinite() || metersPerSecond < 0.0) {
            return DASH
        }
        return String.format(
            Locale.getDefault(),
            SPEED_PATTERN,
            metersPerSecond * METERS_PER_SECOND_TO_KM_H,
        )
    }

    /** Estimated calories as whole kilocalories; null shows a dash. */
    fun calories(kilocalories: Double?): String {
        if (kilocalories == null || !kilocalories.isFinite() || kilocalories < 0.0) return DASH
        return count(round(kilocalories).toLong())
    }

    /** Step counts; unavailable shows a dash (never a fake zero). */
    fun steps(value: Long?): String {
        if (value == null || value < 0L) return DASH
        return count(value)
    }

    fun minutes(value: Int): String = "${count(value)} $TIME_UNIT"

    fun duration(milliseconds: Long): String {
        val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L
        return String.format(Locale.getDefault(), DURATION_PATTERN, hours, minutes, seconds)
    }

    private const val HUNDREDTHS = 100.0
    private const val METERS_PER_KILOMETER = 1_000L
    private const val METERS_PER_SECOND_TO_KM_H = 3.6
    private const val DURATION_PATTERN = "%02d:%02d:%02d"
    private const val KM_PATTERN = "%.2f km"
    private const val PACE_PATTERN = "%d:%02d /km"
    private const val SPEED_PATTERN = "%.1f km/h"
    private const val DISTANCE_UNIT = "km"
    private const val SHORT_DISTANCE_UNIT = "m"
    private const val TIME_UNIT = "min"
    const val DASH = "—"
}
