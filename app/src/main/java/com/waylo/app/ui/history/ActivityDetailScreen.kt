package com.waylo.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.common.MascotContent
import com.waylo.app.core.common.MascotContext
import com.waylo.app.core.common.MascotResolver
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.data.map.WalkMapLoadState
import com.waylo.app.data.map.WalkMapState
import com.waylo.app.domain.model.ProgressionAwardEvent
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingSession
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WorkoutStatistics
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloStatCard
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloShapes
import com.waylo.app.ui.theme.WayloTheme
import com.waylo.app.ui.walk.WalkMap

@Composable
fun ActivityDetailRoute(
    activityId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as WayloApplication
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = ActivityDetailViewModel.factory(
            repository = application.walkingRepository,
            activityId = activityId,
            now = { System.currentTimeMillis() },
            networkStatus = application.mapNetworkStatus,
            weightKg = application.wayloPreferences.weightKg,
            progression = application.progressionRepository,
            achievements = application.achievementRepository,
            onDeleted = onDeleted,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ActivityDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRequestDelete = viewModel::delete,
        onMapStyleLoaded = viewModel::onMapStyleLoaded,
        onMapStyleFailed = viewModel::onMapStyleFailed,
        onMapPanGesture = viewModel::onMapPanGesture,
        onRetryMapStyle = viewModel::retryMapStyle,
        modifier = modifier,
    )
}

@Composable
fun ActivityDetailScreen(
    state: ActivityDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestDelete: () -> Unit,
    onMapStyleLoaded: () -> Unit,
    onMapStyleFailed: () -> Unit,
    onMapPanGesture: () -> Unit,
    onRetryMapStyle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = WayloDimens.screenHorizontalPadding,
                vertical = WayloDimens.screenVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Walk details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when (state) {
            ActivityDetailUiState.Loading -> CenteredMessage {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Loading walk details" },
                    color = WayloColors.Primary,
                )
            }

            ActivityDetailUiState.NotFound -> CenteredMessage {
                Text(
                    text = "Activity not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "This walk may have been deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                WayloPrimaryButton(
                    text = "Back to history",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ActivityDetailUiState.Error -> CenteredMessage {
                Text(
                    text = "Couldn't load this activity.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                WayloPrimaryButton(
                    text = "Try again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is ActivityDetailUiState.Loaded -> LoadedContent(
                state = state,
                onRequestDelete = { showDeleteDialog = true },
                onMapStyleLoaded = onMapStyleLoaded,
                onMapStyleFailed = onMapStyleFailed,
                onMapPanGesture = onMapPanGesture,
                onRetryMapStyle = onRetryMapStyle,
            )
        }
    }

    if (showDeleteDialog) {
        DeleteWalkDialog(
            onConfirm = {
                showDeleteDialog = false
                onRequestDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun LoadedContent(
    state: ActivityDetailUiState.Loaded,
    onRequestDelete: () -> Unit,
    onMapStyleLoaded: () -> Unit,
    onMapStyleFailed: () -> Unit,
    onMapPanGesture: () -> Unit,
    onRetryMapStyle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        WayloCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = state.endTimeLabel?.let { "${state.startTimeLabel} – $it" }
                        ?: state.startTimeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val mascotContext = MascotContext(
            justCompleted = true,
            xpAwarded = state.xpAwarded ?: 0,
            levelUp = state.levelUp != null,
            achievementCount = state.achievementUnlock?.count ?: 0,
        )
        val mascotDecision = MascotResolver.resolve(mascotContext)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            WayloMascot(
                state = mascotDecision.state,
                size = WayloDimens.smallMascotSize,
                decorative = true,
            )
            Text(
                text = MascotContent.message(mascotDecision, mascotContext),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.levelUp?.let { levelUp ->
            LevelUpBanner(levelUp = levelUp)
        }

        state.achievementUnlock?.let { unlock ->
            AchievementUnlockCard(unlock = unlock)
        }

        RouteMapCard(
            state = state,
            onStyleLoaded = onMapStyleLoaded,
            onStyleFailed = onMapStyleFailed,
            onPanGesture = onMapPanGesture,
            onRetryStyle = onRetryMapStyle,
        )

        StatsRow(
            firstValue = WayloFormat.distance(state.session.distanceMeters / 1_000.0),
            firstLabel = "Distance",
            secondValue = WayloFormat.duration(state.session.activeMillis),
            secondLabel = "Duration",
        )
        StatsRow(
            firstValue = WayloFormat.pace(state.statistics.averagePaceSecondsPerKm),
            firstLabel = "Avg pace",
            secondValue = WayloFormat.speed(state.statistics.averageSpeedMetersPerSecond),
            secondLabel = "Avg speed",
        )
        StatsRow(
            firstValue = WayloFormat.calories(state.statistics.estimatedCaloriesKcal),
            firstLabel = "Est. kcal",
            secondValue = WayloFormat.steps(state.statistics.walkSteps),
            secondLabel = "Steps",
        )
        XpEarnedCard(xpAwarded = state.xpAwarded)
        if (state.weightKg == null) {
            Text(
                text = "Add your weight in Profile to estimate calories.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = onRequestDelete,
            shape = WayloShapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(WayloDimens.primaryButtonHeight),
        ) {
            Text(
                text = "Delete walk",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RouteMapCard(
    state: ActivityDetailUiState.Loaded,
    onStyleLoaded: () -> Unit,
    onStyleFailed: () -> Unit,
    onPanGesture: () -> Unit,
    onRetryStyle: () -> Unit,
) {
    when {
        !state.routeLoaded -> MapPlaceholder {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = WayloColors.Cyan,
            )
            Text(
                text = "Loading route...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.routeUnavailable -> MapPlaceholder {
            Text(
                text = "Route unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        state.map.loadState == WalkMapLoadState.Unavailable -> MapPlaceholder {
            Text(
                text = "The map can't load while offline. Your walk is still saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        state.map.loadState == WalkMapLoadState.StyleError -> MapPlaceholder {
            Text(
                text = "The map style failed to load.",
                style = MaterialTheme.typography.bodyMedium,
                color = WayloColors.Orange,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetryStyle) {
                Text("Retry")
            }
        }

        else -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(WayloShapes.medium),
        ) {
            WalkMap(
                mapState = state.map,
                route = state.route,
                onStyleLoaded = onStyleLoaded,
                onStyleFailed = onStyleFailed,
                onPanGesture = onPanGesture,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun MapPlaceholder(content: @Composable ColumnScope.() -> Unit) {
    WayloCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
                content = content,
            )
        }
    }
}

@Composable
private fun XpEarnedCard(xpAwarded: Int?) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "XP earned",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = xpAwarded?.let { "+${WayloFormat.count(it)} XP" } ?: WayloFormat.DASH,
                style = MaterialTheme.typography.titleMedium,
                color = if (xpAwarded != null) WayloColors.Cyan else WayloColors.Orange,
            )
        }
    }
}

@Composable
private fun AchievementUnlockCard(unlock: AchievementUnlockUi) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = WayloColors.Cyan,
                modifier = Modifier.size(28.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (unlock.isMultiple) {
                        "${unlock.count} achievements unlocked!"
                    } else {
                        "Achievement unlocked!"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = WayloColors.Cyan,
                )
                unlock.titles.forEach { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelUpBanner(levelUp: ProgressionAwardEvent) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Level up!",
                style = MaterialTheme.typography.titleLarge,
                color = WayloColors.Cyan,
            )
            Text(
                text = "You reached Level ${levelUp.levelAfter} · " +
                    "+${WayloFormat.count(levelUp.xpAwarded)} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsRow(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        WayloStatCard(
            value = firstValue,
            label = firstLabel,
            modifier = Modifier.weight(1f),
        )
        WayloStatCard(
            value = secondValue,
            label = secondLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CenteredMessage(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = WayloDimens.sectionSpacing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            content = content,
        )
    }
}

@Composable
private fun DeleteWalkDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete this walk?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            Text(
                text = "This activity and its recorded route will be permanently removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun ActivityDetailScreenPreview() {
    WayloTheme {
        ActivityDetailScreen(
            state = ActivityDetailUiState.Loaded(
                session = WalkingSession(
                    id = 1L,
                    state = WalkingState.Completed,
                    startMillis = 1_790_000_000_000L,
                    updatedMillis = 1_790_001_860_000L,
                    distanceMeters = 2_840.0,
                    activeMillis = 1_884_000L,
                    walkStartStepCount = 10_000L,
                    walkStepCount = 2_931L,
                ),
                route = WalkRoute(),
                routeLoaded = true,
                routeUnavailable = true,
                statistics = WorkoutStatistics(
                    distanceMeters = 2_840.0,
                    activeMillis = 1_884_000L,
                    averagePaceSecondsPerKm = 418.0,
                    averageSpeedMetersPerSecond = 1.51,
                    estimatedCaloriesKcal = 97.0,
                    walkSteps = 2_931L,
                ),
                map = WalkMapState(),
                weightKg = 70,
                dateLabel = "September 28, 2026",
                startTimeLabel = "6:42 PM",
                endTimeLabel = "7:13 PM",
            ),
            onBack = {},
            onRetry = {},
            onRequestDelete = {},
            onMapStyleLoaded = {},
            onMapStyleFailed = {},
            onMapPanGesture = {},
            onRetryMapStyle = {},
        )
    }
}
