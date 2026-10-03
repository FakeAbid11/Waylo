package com.waylo.app.data.progression

import androidx.room.withTransaction
import com.waylo.app.core.common.LevelCalculator
import com.waylo.app.core.common.StreakCalculator
import com.waylo.app.core.common.StreakResult
import com.waylo.app.core.common.XpCalculator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.local.XpAwardEntity
import com.waylo.app.domain.model.AwardOutcome
import com.waylo.app.domain.model.ProgressionResult
import com.waylo.app.domain.model.WalkingState
import java.time.ZoneId

class AwardActivityXp(
    private val database: WayloDatabase,
    private val now: () -> Long,
    private val zone: ZoneId,
) {

    suspend operator fun invoke(activityId: Long): ProgressionResult = database.withTransaction {
        val session = database.walkingDao().sessionById(activityId)
        if (session == null || session.state != WalkingState.Completed.name) {
            return@withTransaction buildResult(AwardOutcome.NotPersisted, activityId, 0)
        }

        val existing = database.progressionDao().awardForActivity(activityId)
        if (existing != null) {
            return@withTransaction buildResult(AwardOutcome.AlreadyAwarded, activityId, existing.xp)
        }

        val xp = XpCalculator.xpFor(session.distanceMeters)
        if (xp <= 0) {
            return@withTransaction buildResult(AwardOutcome.NotQualifying, activityId, 0)
        }

        val atMillis = now()
        val inserted = database.progressionDao().insertAward(
            XpAwardEntity(activityId = activityId, xp = xp, awardedAt = atMillis),
        )
        if (inserted == -1L) {
            val raced = database.progressionDao().awardForActivity(activityId)
            return@withTransaction buildResult(
                AwardOutcome.AlreadyAwarded,
                activityId,
                raced?.xp ?: 0,
            )
        }

        database.recalculateProgression(atMillis, zone)
        buildAwardedResult(activityId, xp)
    }

    private suspend fun buildAwardedResult(activityId: Long, xp: Int): ProgressionResult {
        val total = database.progressionDao().totalXp()
        val streak = liveStreak()
        return ProgressionResult(
            outcome = AwardOutcome.Awarded,
            activityId = activityId,
            xpAwarded = xp,
            totalXp = total,
            levelBefore = LevelCalculator.levelForTotalXp(total - xp),
            levelAfter = LevelCalculator.levelForTotalXp(total),
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
        )
    }

    private suspend fun buildResult(
        outcome: AwardOutcome,
        activityId: Long,
        xpAwarded: Int,
    ): ProgressionResult {
        val total = database.progressionDao().totalXp()
        val level = LevelCalculator.levelForTotalXp(total)
        val streak = liveStreak()
        return ProgressionResult(
            outcome = outcome,
            activityId = activityId,
            xpAwarded = xpAwarded,
            totalXp = total,
            levelBefore = level,
            levelAfter = level,
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
        )
    }

    private suspend fun liveStreak(): StreakResult {
        val days = database.progressionDao().qualifyingAwardMillis()
            .map { WayloDateFormatter.localDate(it, zone) }
            .toSet()
        return StreakCalculator.calculate(days, WayloDateFormatter.localDate(now(), zone))
    }
}
