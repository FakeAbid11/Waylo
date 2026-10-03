package com.waylo.app.data.achievement

import androidx.room.withTransaction
import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.core.common.AchievementEvaluator
import com.waylo.app.core.common.LevelCalculator
import com.waylo.app.core.common.StreakCalculator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.data.local.AchievementEntity
import com.waylo.app.data.local.CompletedTotalsRow
import com.waylo.app.data.local.ProgressionEntity
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementSnapshot
import com.waylo.app.domain.model.AchievementSummary
import com.waylo.app.domain.model.AchievementUnlock
import com.waylo.app.domain.model.AchievementUnlockedEvent
import java.time.ZoneId
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AchievementRepositoryImpl(
    private val database: WayloDatabase,
    private val now: () -> Long,
    private val zone: ZoneId,
    override val definitions: List<AchievementDefinition> = AchievementCatalog.definitions,
) : AchievementRepository {

    private val _unlockEvent = MutableStateFlow<AchievementUnlockedEvent?>(null)

    override val unlockEvent: StateFlow<AchievementUnlockedEvent?> = _unlockEvent.asStateFlow()

    /**
     * One combined observation over five aggregate queries — never one query per
     * achievement. Unlock rows are the persisted truth; ids satisfied right now
     * are unioned in so progress that changed since the last reconciliation
     * still displays correctly (deletions can only *remove* ids from the union
     * side, and persisted unlocks are never revoked).
     */
    override val snapshot: Flow<AchievementSnapshot> = combine(
        database.achievementDao().observeUnlocks(),
        database.progressionDao().observeProgressionById(ProgressionEntity.SINGLE_ROW_ID),
        database.progressionDao().observeQualifyingAwardMillis(),
        database.progressionDao().observeCompletedTotals(),
        database.progressionDao().observeCompletedWalkCount(),
    ) { unlocks, progression, awardMillis, totals, walkCount ->
        val context = buildContext(
            totalXp = progression?.totalXp ?: 0,
            qualifiedAwardMillis = awardMillis,
            totals = totals,
            walkCount = walkCount,
        )
        val unlockDates = unlocks.associate { it.achievementId to it.unlockedAtMillis }
        val satisfied = AchievementEvaluator.evaluate(context, definitions).toSet()
        AchievementSnapshot(
            context = context,
            unlockedIds = unlockDates.keys + satisfied,
            unlockedAtById = unlockDates,
        )
    }

    override val summary: Flow<AchievementSummary> = snapshot
        .map { value -> value.summary(definitions) }
        .distinctUntilChanged()

    override suspend fun reconcile(activityId: Long?): List<String> {
        val context = currentContext()
        val satisfied = AchievementEvaluator.evaluate(context, definitions)
        if (satisfied.isEmpty()) return emptyList()

        val at = now()
        val newlyUnlocked: List<AchievementUnlock> = database.withTransaction {
            val dao = database.achievementDao()
            satisfied.mapNotNull { id ->
                val inserted = dao.insertUnlock(
                    AchievementEntity(achievementId = id, unlockedAtMillis = at),
                )
                if (inserted == -1L) null else AchievementUnlock(id = id, unlockedAtMillis = at)
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            _unlockEvent.value = AchievementUnlockedEvent(
                activityId = activityId,
                unlocks = newlyUnlocked,
                unlockedAtMillis = at,
            )
        }
        return newlyUnlocked.map { it.id }
    }

    override fun consumeUnlockEvent() {
        _unlockEvent.value = null
    }

    private suspend fun currentContext(): AchievementContext {
        val dao = database.progressionDao()
        return buildContext(
            totalXp = dao.totalXp(),
            qualifiedAwardMillis = dao.qualifyingAwardMillis(),
            totals = dao.completedTotals(),
            walkCount = dao.completedWalkCount(),
        )
    }

    private fun buildContext(
        totalXp: Int,
        qualifiedAwardMillis: List<Long>,
        totals: CompletedTotalsRow,
        walkCount: Int,
    ): AchievementContext {
        val qualifiedDays = qualifiedAwardMillis
            .map { WayloDateFormatter.localDate(it, zone) }
            .toSet()
        val streak = StreakCalculator.calculate(
            qualifiedDays,
            WayloDateFormatter.localDate(now(), zone),
        )
        return AchievementContext(
            completedWalkCount = walkCount,
            totalDistanceMeters = totals.distanceMeters.roundToLong(),
            totalXp = totalXp.toLong(),
            level = LevelCalculator.levelForTotalXp(totalXp),
            currentStreakDays = streak.currentStreakDays,
            longestStreakDays = streak.longestStreakDays,
            totalSteps = totals.steps,
            totalActiveSeconds = totals.activeMillis / 1_000L,
        )
    }
}

internal fun AchievementSnapshot.summary(
    definitions: List<AchievementDefinition>,
): AchievementSummary = AchievementSummary(
    unlockedCount = definitions.count { it.id in unlockedIds },
    totalCount = definitions.size,
)
