package com.waylo.app.core.common

import java.time.LocalDate

data class StreakResult(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
)

object StreakCalculator {

    fun calculate(qualifiedDays: Set<LocalDate>, today: LocalDate): StreakResult {
        if (qualifiedDays.isEmpty()) return StreakResult(0, 0)

        val sorted = qualifiedDays.sorted()
        var longest = 1
        var run = 1
        for (index in 1 until sorted.size) {
            run = if (sorted[index] == sorted[index - 1].plusDays(1)) run + 1 else 1
            if (run > longest) longest = run
        }

        val anchor = when {
            today in qualifiedDays -> today
            today.minusDays(1) in qualifiedDays -> today.minusDays(1)
            else -> return StreakResult(0, longest)
        }

        var current = 1
        var cursor = anchor
        while (cursor.minusDays(1) in qualifiedDays) {
            cursor = cursor.minusDays(1)
            current += 1
        }
        return StreakResult(current, longest)
    }
}
