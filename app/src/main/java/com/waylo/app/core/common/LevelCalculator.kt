package com.waylo.app.core.common

data class LevelState(
    val level: Int,
    val totalXp: Int,
    val currentLevelXp: Int,
    val nextLevelXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpRemaining: Int,
    val progressFraction: Float,
)

object LevelCalculator {

    const val STARTING_LEVEL = 1
    const val BASE_XP_PER_LEVEL = 100
    const val MAX_LEVEL = 401

    fun requiredXpForLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(STARTING_LEVEL)
        val required = BASE_XP_PER_LEVEL.toLong() * safeLevel * safeLevel
        return required.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun totalXpForLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(STARTING_LEVEL) - 1L
        val squareSum = safeLevel * (safeLevel + 1L) * (2L * safeLevel + 1L)
        val cumulative = BASE_XP_PER_LEVEL.toLong() * squareSum / 6L
        return cumulative.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun levelForTotalXp(totalXp: Int): Int {
        var level = STARTING_LEVEL
        while (level < MAX_LEVEL && totalXpForLevel(level + 1) <= totalXp) {
            level += 1
        }
        return level
    }

    fun stateFor(totalXp: Int): LevelState {
        val safeTotal = totalXp.coerceAtLeast(0)
        val level = levelForTotalXp(safeTotal)
        val currentLevelXp = totalXpForLevel(level)
        val nextLevelXp = totalXpForLevel(level + 1)
        val xpIntoCurrentLevel = (safeTotal - currentLevelXp).coerceAtLeast(0)
        val xpRemaining = (nextLevelXp - safeTotal).coerceAtLeast(0)
        val required = nextLevelXp - currentLevelXp
        val progressFraction = if (required <= 0) {
            0f
        } else {
            (xpIntoCurrentLevel.toFloat() / required.toFloat()).coerceIn(0f, 1f)
        }
        return LevelState(
            level = level,
            totalXp = safeTotal,
            currentLevelXp = currentLevelXp,
            nextLevelXp = nextLevelXp,
            xpIntoCurrentLevel = xpIntoCurrentLevel,
            xpRemaining = xpRemaining,
            progressFraction = progressFraction,
        )
    }
}
