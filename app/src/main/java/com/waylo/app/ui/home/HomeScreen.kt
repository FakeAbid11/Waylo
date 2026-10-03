package com.waylo.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.core.permissions.openAppSettings
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.StepStatus
import com.waylo.app.domain.model.UserProgress
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloProgressBar
import com.waylo.app.ui.components.WayloSectionHeader
import com.waylo.app.ui.components.WayloStatCard
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloTheme

@Composable
fun HomeRoute(
    onStartWalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as WayloApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            stepRepository = application.stepRepository,
            walkingRepository = application.walkingRepository,
            progression = application.progressionRepository,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refresh()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        onStepPermissionAction = {
            val permission = application.permissionManager.stateOf(WayloPermission.Activity)
            if (permission.canRequestAgain) {
                application.permissionManager.markRequested(listOf(WayloPermission.Activity))
                permissionLauncher.launch(arrayOf(WayloPermission.Activity.manifestPermission))
            } else {
                openAppSettings(context)
            }
        },
        onStartWalk = onStartWalk,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onStepPermissionAction: () -> Unit = {},
    onStartWalk: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = WayloDimens.screenHorizontalPadding,
                vertical = WayloDimens.screenVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.sectionSpacing),
    ) {
        Greeting(state.greeting)
        MascotSection()
        StepsCard(state = state, onPermissionAction = onStepPermissionAction)
        WayloPrimaryButton(
            text = state.startWalkLabel,
            onClick = onStartWalk,
            enabled = state.isStartWalkAvailable,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            XpCard(state.progress, Modifier.weight(1f))
            StreakCard(state.progress.currentStreakDays, Modifier.weight(1f))
        }
        TodaySummary(state)
    }
}

@Composable
private fun Greeting(greeting: String) {
    Text(
        text = greeting,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun MascotSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        WayloMascot()
        Text(
            text = "Make every walk an adventure.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepsCard(state: HomeUiState, onPermissionAction: () -> Unit) {
    WayloCard {
        when (state.stepState.status) {
            StepStatus.Loading -> LoadingSteps()
            StepStatus.SensorUnavailable -> UnavailableSteps()
            StepStatus.PermissionNeeded -> PermissionSteps(
                permissionState = state.stepState.permissionState,
                onPermissionAction = onPermissionAction,
            )

            StepStatus.Active -> ActiveSteps(state)
        }
    }
}

@Composable
private fun StepsHeader(percent: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Today's Steps",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (percent != null) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelLarge,
                color = WayloColors.Cyan,
            )
        }
    }
}

@Composable
private fun LoadingSteps() {
    StepsHeader()
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Loading…",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UnavailableSteps() {
    StepsHeader()
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Step counting isn't available on this device.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PermissionSteps(
    permissionState: PermissionState,
    onPermissionAction: () -> Unit,
) {
    StepsHeader()
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Allow activity access so Waylo can count your steps.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    WayloPrimaryButton(
        text = if (permissionState.canRequestAgain) "Allow Activity access" else "Open Settings",
        onClick = onPermissionAction,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActiveSteps(state: HomeUiState) {
    val goal = state.goal
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(WayloFormat.count(goal.completedSteps))
                    append(" steps today. Daily goal ")
                    append(WayloFormat.count(goal.targetSteps))
                    append(". ")
                    append(goal.progressPercent)
                    append(" percent complete.")
                }
            },
    ) {
        StepsHeader(percent = goal.progressPercent)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = WayloFormat.count(goal.completedSteps),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "/ ${WayloFormat.count(goal.targetSteps)} steps",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        WayloProgressBar(
            progress = goal.progress,
            modifier = Modifier.fillMaxWidth(),
        )
        if (goal.isCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Daily goal complete!",
                style = MaterialTheme.typography.titleMedium,
                color = WayloColors.Success,
            )
        }
    }
}

@Composable
private fun XpCard(progress: UserProgress, modifier: Modifier = Modifier) {
    WayloCard(modifier = modifier) {
        Text(
            text = "Level ${progress.level}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${WayloFormat.count(progress.totalXp)} XP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    WayloCard(modifier = modifier) {
        Text(
            text = "🔥 ${WayloFormat.count(streakDays)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "day streak",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodaySummary(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Today's Summary")
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            WayloStatCard(
                value = WayloFormat.count(state.todaySteps),
                label = "Steps",
                modifier = Modifier.weight(1f),
            )
            WayloStatCard(
                value = WayloFormat.distance(state.todayDistanceKm),
                label = "Distance",
                modifier = Modifier.weight(1f),
            )
            WayloStatCard(
                value = WayloFormat.minutes(state.todayWalkingMinutes),
                label = "Walking Time",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun HomeScreenPreview() {
    WayloTheme {
        HomeScreen(
            state = HomeUiState(
                stepState = DailyStepState(
                    steps = 3_482,
                    goal = 6_000,
                    sensorAvailable = true,
                    permissionState = PermissionState.Granted,
                    isTracking = true,
                    isLoaded = true,
                ),
            ),
        )
    }
}
