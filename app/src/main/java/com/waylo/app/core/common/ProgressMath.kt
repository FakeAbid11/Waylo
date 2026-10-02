package com.waylo.app.core.common

import kotlin.math.roundToInt

internal fun ratioToFraction(value: Float, total: Float): Float {
    if (total <= 0f) return 0f
    return (value / total).coerceIn(MIN_FRACTION, MAX_FRACTION)
}

internal fun fractionToPercent(fraction: Float): Int {
    return (fraction.coerceIn(MIN_FRACTION, MAX_FRACTION) * PERCENT_MAX).roundToInt()
}

private const val MIN_FRACTION = 0f
private const val MAX_FRACTION = 1f
private const val PERCENT_MAX = 100
