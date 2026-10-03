package com.waylo.app.core.common

import kotlin.math.roundToInt

object XpCalculator {

    const val MIN_QUALIFYING_DISTANCE_METERS = 100.0
    const val XP_PER_KILOMETER = 100

    fun xpFor(distanceMeters: Double): Int {
        if (!distanceMeters.isFinite() || distanceMeters < MIN_QUALIFYING_DISTANCE_METERS) return 0
        val xp = (distanceMeters / 1_000.0 * XP_PER_KILOMETER).roundToInt()
        return xp.coerceAtLeast(0)
    }
}
