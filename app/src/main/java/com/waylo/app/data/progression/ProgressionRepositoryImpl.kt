package com.waylo.app.data.progression

import androidx.room.withTransaction
import com.waylo.app.core.common.LevelCalculator
import com.waylo.app.core.common.StreakCalculator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.data.local.CompletedTotalsRow
import com.waylo.app.data.local.ProgressionEntity
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.domain.model.ProgressionAwardEvent
import com.waylo.app.domain.model.ProgressionResult
import com.waylo.app.domain.model.AwardOutcome
import com.waylo.app.domain.model.UserProgress
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class ProgressionRepositoryImpl(
    private val database: WayloDatabase,
    private val now: () -> Long,
    private val zone: ZoneId,
) : ProgressionRepository {

    private val awardActivityXpUseCase = AwardActivityXp(database, now, zone)

    private val _awardEvent = MutableStateFlow<ProgressionAwardEvent?>(null)

    override val awardEvent: StateFlow<ProgressionAwardEvent?> = _awardEvent.asStateFlow()

    override val progress: Flow<UserProgress> = combine(
        database.progressionDao().observeProgressionById(ProgressionEntity.SINGLE_ROW_ID),
        database.progressionDao().observeQualifyingAwardMillis(),
        database.progressionDao().observeCompletedTotals(),
    ) { progression, awardMillis, totals ->
        buildProgress(progression, awardMillis, totals)
    }

    override suspend fun awardActivityXp(activityId: Long): ProgressionResult {
        val result = awardActivityXpUseCase(activityId)
        if (result.outcome == AwardOutcome.Awarded && result.xpAwarded > 0) {
            _awardEvent.value = ProgressionAwardEvent(
                activityId = result.activityId,
                xpAwarded = result.xpAwarded,
                totalXp = result.totalXp,
                levelBefore = result.levelBefore,
                levelAfter = result.levelAfter,
            )
        }
        return result
    }

    override suspend fun recoverAwardIfNeeded(activityId: Long, sessionUpdatedMillis: Long) {
        val eraStart = database.progressionDao()
            .progressionById(ProgressionEntity.SINGLE_ROW_ID)
            ?.createdAtMillis
            ?: Long.MIN_VALUE
        if (sessionUpdatedMillis < eraStart) return
        awardActivityXp(activityId)
    }

    override fun observeAwardXp(activityId: Long): Flow<Int?> =
        database.progressionDao().observeAwardXp(activityId)

    override suspend fun deleteActivityCascade(
        activityId: Long,
        deleteWalkData: suspend () -> Boolean,
    ): Boolean = database.withTransaction {
        val removed = deleteWalkData()
        if (removed) {
            database.progressionDao().deleteAwardForActivity(activityId)
            database.recalculateProgression(now(), zone)
        }
        removed
    }

    override fun consumeAwardEvent() {
        _awardEvent.value = null
    }

    private fun buildProgress(
        progression: ProgressionEntity?,
        awardMillis: List<Long>,
        totals: CompletedTotalsRow,
    ): UserProgress {
        val totalXp = progression?.totalXp ?: 0
        val levelState = LevelCalculator.stateFor(totalXp)
        val qualifiedDays = awardMillis
            .map { WayloDateFormatter.localDate(it, zone) }
            .toSet()
        val streak = StreakCalculator.calculate(
            qualifiedDays,
            WayloDateFormatter.localDate(now(), zone),
        )
        return UserProgress(
            level = levelState.level,
            totalXp = totalXp,
            xp = levelState.xpIntoCurrentLevel,
            xpToNextLevel = levelState.nextLevelXp - levelState.currentLevelXp,
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
            totalSteps = totals.steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            totalDistanceKm = totals.distanceMeters / 1_000.0,
            totalWalkingMinutes = (totals.activeMillis / 60_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
        )
    }
}

internal suspend fun WayloDatabase.recalculateProgression(atMillis: Long, zone: ZoneId) {
    val dao = progressionDao()
    val total = dao.totalXp()
    val qualifiedDays = dao.qualifyingAwardMillis()
        .map { WayloDateFormatter.localDate(it, zone) }
        .toSet()
    val streak = StreakCalculator.calculate(
        qualifiedDays,
        WayloDateFormatter.localDate(atMillis, zone),
    )
    val existing = dao.progressionById(ProgressionEntity.SINGLE_ROW_ID)
    dao.upsertProgression(
        ProgressionEntity(
            id = ProgressionEntity.SINGLE_ROW_ID,
            totalXp = total,
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
            updatedAtMillis = atMillis,
            createdAtMillis = existing?.createdAtMillis ?: atMillis,
        ),
    )
}
