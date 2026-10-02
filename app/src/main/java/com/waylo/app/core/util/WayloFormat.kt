package com.waylo.app.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

object WayloFormat {

    fun count(value: Int): String = NumberFormat.getIntegerInstance().format(value.toLong())

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

    private const val TENTHS = 10.0
    private const val DECIMAL_PATTERN = "%.1f"
    private const val DISTANCE_UNIT = "km"
    private const val TIME_UNIT = "min"
}
