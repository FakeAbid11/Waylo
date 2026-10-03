package com.waylo.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.core.common.AchievementEvaluator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.data.achievement.AchievementRepository
import com.waylo.app.domain.model.AchievementCategory
import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementRequirement
import com.waylo.app.domain.model.AchievementSnapshot
import com.waylo.app.domain.model.AchievementUnlockedEvent
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One achievement rendered by the Achievements screen. */
data class AchievementCardUi(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val unlocked: Boolean,
    val unlockedAtMillis: Long?,
    val statusLabel: String,
    val progressText: String?,
    val progressFraction: Float,
    val dateLabel: String?,
    val accessibilityText: String,
)

/** One-shot unlock presentation, e.g. "3 achievements unlocked!". */
data class AchievementUnlockBannerUi(val titles: List<String>) {
    val count: Int
        get() = titles.size

    val isMultiple: Boolean
        get() = titles.size > 1
}

data class AchievementUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val unlockedCount: Int = 0,
    val totalCount: Int = AchievementCatalog.definitions.size,
    val filter: AchievementCategory? = null,
    val achievements: List<AchievementCardUi> = emptyList(),
    val unlockBanner: AchievementUnlockBannerUi? = null,
) {
    val allLocked: Boolean
        get() = !loading && !error && unlockedCount == 0

    val allUnlocked: Boolean
        get() = !loading && !error && totalCount > 0 && unlockedCount == totalCount

    val filterIsEmpty: Boolean
        get() = !loading && !error && filter != null && achievements.isEmpty()
}

/**
 * Loads one [AchievementSnapshot] from the repository (no polling, no per-
 * achievement queries), maps it to display models and owns filter + one-shot
 * unlock banner state.
 */
class AchievementViewModel(
    private val repository: AchievementRepository,
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope = stateScope ?: viewModelScope

    private val snapshot = MutableStateFlow<AchievementSnapshot?>(null)
    private val failed = MutableStateFlow(false)
    private val filter = MutableStateFlow<AchievementCategory?>(null)
    private var observeJob: Job? = null

    init {
        observeSnapshot()
    }

    val uiState: StateFlow<AchievementUiState> = combine(
        snapshot,
        failed,
        filter,
        repository.unlockEvent,
    ) { current, hasFailed, selected, event ->
        buildState(current, hasFailed, selected, event)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AchievementUiState(),
    )

    fun retry() {
        failed.value = false
        observeSnapshot()
    }

    fun setFilter(category: AchievementCategory?) {
        filter.value = category
    }

    fun dismissUnlockBanner() {
        repository.consumeUnlockEvent()
    }

    private fun observeSnapshot() {
        observeJob?.cancel()
        observeJob = scope.launch {
            try {
                repository.snapshot.collect { value -> snapshot.value = value }
            } catch (expected: Throwable) {
                failed.value = true
            }
        }
    }

    private fun buildState(
        current: AchievementSnapshot?,
        hasFailed: Boolean,
        selected: AchievementCategory?,
        event: AchievementUnlockedEvent?,
    ): AchievementUiState {
        val definitions = repository.definitions
        if (hasFailed) {
            return AchievementUiState(
                loading = false,
                error = true,
                totalCount = definitions.size,
                filter = selected,
            )
        }
        if (current == null) {
            // Nothing observed yet: show the loading state, never guessed cards.
            return AchievementUiState(
                loading = true,
                totalCount = definitions.size,
                filter = selected,
            )
        }
        val context = current.context
        val unlockedIds = current.unlockedIds
        val unlockDates = current.unlockedAtById
        val cards = definitions.map { definition ->
            buildCard(
                definition = definition,
                context = context,
                unlockedIds = unlockedIds,
                unlockDates = unlockDates,
                zone = zoneId(),
            )
        }
        return AchievementUiState(
            loading = false,
            error = false,
            unlockedCount = cards.count { it.unlocked },
            totalCount = definitions.size,
            filter = selected,
            achievements = if (selected == null) {
                cards
            } else {
                cards.filter { it.category == selected }
            },
            unlockBanner = event?.toBanner(definitions),
        )
    }

    private fun buildCard(
        definition: AchievementDefinition,
        context: AchievementContext,
        unlockedIds: Set<String>,
        unlockDates: Map<String, Long>,
        zone: ZoneId,
    ): AchievementCardUi {
        val current = AchievementEvaluator.currentValue(definition.requirement, context)
        val target = AchievementEvaluator.requiredValue(definition.requirement)
        val unlocked = definition.id in unlockedIds
        val unlockedAt = unlockDates[definition.id]
        val dateLabel = unlockedAt?.let {
            WayloDateFormatter.fullDate(WayloDateFormatter.localDate(it, zone))
        }
        return AchievementCardUi(
            id = definition.id,
            title = definition.title,
            description = definition.description,
            category = definition.category,
            unlocked = unlocked,
            unlockedAtMillis = unlockedAt,
            statusLabel = if (unlocked) "Unlocked" else "Locked",
            progressText = if (unlocked) {
                null
            } else {
                progressText(definition.requirement, current, target)
            },
            progressFraction = AchievementEvaluator.progressFraction(definition.requirement, context),
            dateLabel = dateLabel,
            accessibilityText = accessibilityText(
                definition = definition,
                unlocked = unlocked,
                current = current,
                target = target,
                dateLabel = dateLabel,
            ),
        )
    }

    private fun AchievementUnlockedEvent.toBanner(
        definitions: List<AchievementDefinition>,
    ): AchievementUnlockBannerUi? {
        val titles = unlocks.mapNotNull { unlock ->
            definitions.firstOrNull { it.id == unlock.id }?.title
        }
        return titles.takeIf { it.isNotEmpty() }?.let { AchievementUnlockBannerUi(it) }
    }

    companion object {
        /** Visual progress label, e.g. "1.24 km / 10.00 km" or "3 / 10 walks". */
        internal fun progressText(
            requirement: AchievementRequirement,
            current: Long,
            target: Long,
        ): String = when (requirement) {
            is AchievementRequirement.CompletedWalks ->
                "${WayloFormat.count(current)} / ${WayloFormat.count(target)} walks"

            is AchievementRequirement.DistanceMeters ->
                "${WayloFormat.distance(current / 1_000.0)} / " +
                    WayloFormat.distance(target / 1_000.0)

            is AchievementRequirement.TotalXp ->
                "${WayloFormat.count(current)} / ${WayloFormat.count(target)} XP"

            is AchievementRequirement.Level ->
                "Level ${WayloFormat.count(current)} / ${WayloFormat.count(target)}"

            is AchievementRequirement.StreakDays ->
                "${WayloFormat.count(current)} / ${WayloFormat.count(target)} days"

            is AchievementRequirement.Steps ->
                "${WayloFormat.count(current)} / ${WayloFormat.count(target)} steps"

            is AchievementRequirement.ActiveDurationSeconds ->
                "${WayloFormat.count(current / 60L)} / ${WayloFormat.count(target / 60L)} min"
        }

        /**
         * Spoken progress for screen readers, in full words: "Progress:
         * 1.24 kilometers of 10 kilometers." Icons and bars never carry meaning
         * alone.
         */
        internal fun spokenProgress(
            requirement: AchievementRequirement,
            current: Long,
            target: Long,
        ): String = when (requirement) {
            is AchievementRequirement.CompletedWalks ->
                "Progress: ${WayloFormat.count(current)} of ${WayloFormat.count(target)} walks."

            is AchievementRequirement.DistanceMeters ->
                "Progress: ${spokenDistance(current)} of ${spokenDistance(target)}."

            is AchievementRequirement.TotalXp ->
                "Progress: ${WayloFormat.count(current)} of ${WayloFormat.count(target)} XP."

            is AchievementRequirement.Level ->
                "Progress: level ${WayloFormat.count(current)} of ${WayloFormat.count(target)}."

            is AchievementRequirement.StreakDays ->
                "Progress: ${WayloFormat.count(current)} of ${WayloFormat.count(target)} days."

            is AchievementRequirement.Steps ->
                "Progress: ${WayloFormat.count(current)} of ${WayloFormat.count(target)} steps."

            is AchievementRequirement.ActiveDurationSeconds ->
                "Progress: ${WayloFormat.count(current / 60L)} of " +
                    "${WayloFormat.count(target / 60L)} minutes."
        }

        internal fun spokenDistance(meters: Long): String {
            if (meters < METERS_PER_KILOMETER) return "${WayloFormat.count(meters)} meters"
            val kilometers = meters / 1_000.0
            val rounded = (kilometers * HUNDREDTHS).roundToInt() / HUNDREDTHS
            return if (rounded == rounded.toLong().toDouble()) {
                "${WayloFormat.count(rounded.toLong())} kilometers"
            } else {
                String.format(Locale.getDefault(), KILOMETER_PATTERN, rounded)
            }
        }

        private fun accessibilityText(
            definition: AchievementDefinition,
            unlocked: Boolean,
            current: Long,
            target: Long,
            dateLabel: String?,
        ): String = if (unlocked) {
            if (dateLabel != null) {
                "${definition.description} Completed on $dateLabel."
            } else {
                "${definition.description} Completed."
            }
        } else {
            "${definition.description} Locked. " +
                spokenProgress(definition.requirement, current, target)
        }

        private const val METERS_PER_KILOMETER = 1_000L
        private const val HUNDREDTHS = 100.0
        private const val KILOMETER_PATTERN = "%.2f kilometers"

        fun factory(
            repository: AchievementRepository,
            zoneId: () -> ZoneId = { ZoneId.systemDefault() },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AchievementViewModel(repository = repository, zoneId = zoneId)
            }
        }
    }
}
