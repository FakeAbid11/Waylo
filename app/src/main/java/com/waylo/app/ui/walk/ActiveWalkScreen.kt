package com.waylo.app.ui.walk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.core.permissions.openAppSettings
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.data.map.WalkMapLoadState
import com.waylo.app.data.map.WalkMapState
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import com.waylo.app.service.WalkingForegroundService
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloStatCard
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloShapes
import com.waylo.app.ui.theme.WayloTheme

@Composable
fun ActiveWalkRoute(
    onDone: () -> Unit,
    onWalkFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as WayloApplication
    val viewModel: ActiveWalkViewModel = viewModel(
        factory = ActiveWalkViewModel.factory(
            repository = application.walkingRepository,
            startService = { startWalkingService(context) },
            now = { System.currentTimeMillis() },
            networkStatus = application.mapNetworkStatus,
            stepState = application.stepRepository.state,
            weightKg = application.wayloPreferences.weightKg,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onScreenResumed(
            permission = application.permissionManager.stateOf(WayloPermission.FineLocation),
            locationEnabled = application.walkingLocationDataSource.isLocationEnabled(),
        )
    }

    fun evaluateEntry() {
        val action = viewModel.onScreenResumed(
            permission = application.permissionManager.stateOf(WayloPermission.FineLocation),
            locationEnabled = application.walkingLocationDataSource.isLocationEnabled(),
        )
        if (action == WalkEntryAction.RequestPermission) {
            requestLocationPermissions(application) { permissions ->
                permissionLauncher.launch(permissions)
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        evaluateEntry()
        onPauseOrDispose { }
    }

    BackHandler(enabled = state.status.state == WalkingState.Completed) {
        viewModel.finishCompleted()
        onDone()
    }

    LaunchedEffect(state.status.state, state.status.sessionId) {
        val finishedId = state.status.sessionId
        if (state.status.state == WalkingState.Completed && finishedId != null) {
            viewModel.finishCompleted()
            onWalkFinished(finishedId)
        }
    }

    ActiveWalkScreen(
        state = state,
        onRequestLocationPermission = {
            requestLocationPermissions(application) { permissions ->
                permissionLauncher.launch(permissions)
            }
        },
        onOpenSettings = { openAppSettings(context) },
        onOpenLocationSettings = { openLocationSettings(context) },
        onPause = viewModel::pauseWalk,
        onResume = viewModel::resumeWalk,
        onStopWalk = viewModel::finishWalk,
        onRetry = viewModel::retryWalk,
        onDone = {
            viewModel.finishCompleted()
            onDone()
        },
        onMapStyleLoaded = viewModel::onMapStyleLoaded,
        onMapStyleFailed = viewModel::onMapStyleFailed,
        onMapPanGesture = viewModel::onMapPanGesture,
        onRecenterMap = viewModel::onRecenterMap,
        onRetryMapStyle = viewModel::retryMapStyle,
        modifier = modifier,
    )
}

private fun requestLocationPermissions(
    application: WayloApplication,
    launch: (Array<String>) -> Unit,
) {
    application.permissionManager.markRequested(
        listOf(WayloPermission.CoarseLocation, WayloPermission.FineLocation),
    )
    launch(
        arrayOf(
            WayloPermission.FineLocation.manifestPermission,
            WayloPermission.CoarseLocation.manifestPermission,
        ),
    )
}

private fun startWalkingService(context: Context) {
    val intent = Intent(context, WalkingForegroundService::class.java)
        .setAction(WalkingForegroundService.ACTION_START)
    ContextCompat.startForegroundService(context, intent)
}

private fun openLocationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (exception: ActivityNotFoundException) {
        // Nothing to open on devices without a settings screen; state stays honest in the UI.
    }
}

@Composable
fun ActiveWalkScreen(
    state: ActiveWalkUiState,
    onRequestLocationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopWalk: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onMapStyleLoaded: () -> Unit,
    onMapStyleFailed: () -> Unit,
    onMapPanGesture: () -> Unit,
    onRecenterMap: () -> Unit,
    onRetryMapStyle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStopConfirmation by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        WalkMap(
            mapState = state.map,
            route = state.route,
            onStyleLoaded = onMapStyleLoaded,
            onStyleFailed = onMapStyleFailed,
            onPanGesture = onMapPanGesture,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = WayloDimens.screenHorizontalPadding,
                    vertical = WayloDimens.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            MapStatusBanner(
                mapState = state.map,
                onRetryStyle = onRetryMapStyle,
            )

            if (state.status.state.isOngoing) {
                WalkInfoPanel(state = state)
                WorkoutStatsPanel(state = state)
            }

            Spacer(modifier = Modifier.weight(1f))

            when {
                state.status.state == WalkingState.Completed -> CompletionContent(
                    state = state,
                    onDone = onDone,
                )

                state.readiness == WalkReadiness.PermissionNeeded -> ReadinessCard(
                    message = "Location access is needed to record your walks.",
                    actionLabel = "Allow location access",
                    onAction = onRequestLocationPermission,
                )

                state.readiness == WalkReadiness.PermissionPermanentlyDenied -> ReadinessCard(
                    message = "Location access is blocked. You can enable it in Android settings.",
                    actionLabel = "Open Settings",
                    onAction = onOpenSettings,
                )

                state.readiness == WalkReadiness.LocationDisabled -> ReadinessCard(
                    message = "Turn on location so Waylo can record your walk.",
                    actionLabel = "Turn on location",
                    onAction = onOpenLocationSettings,
                )

                state.status.state == WalkingState.Error -> ErrorCard(
                    state = state,
                    onRetry = onRetry,
                    onFinish = onStopWalk,
                )

                else -> TrackingControls(
                    state = state,
                    onPause = onPause,
                    onResume = onResume,
                    onStopWalk = { showStopConfirmation = true },
                    onRecenter = onRecenterMap,
                )
            }
        }
    }

    if (showStopConfirmation) {
        StopConfirmationDialog(
            onConfirm = {
                showStopConfirmation = false
                onStopWalk()
            },
            onDismiss = { showStopConfirmation = false },
        )
    }
}

@Composable
private fun MapStatusBanner(
    mapState: WalkMapState,
    onRetryStyle: () -> Unit,
) {
    val loadState = mapState.loadState
    if (loadState == WalkMapLoadState.Ready) return

    WayloCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            when (loadState) {
                WalkMapLoadState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = WayloColors.Cyan,
                    )
                    Text(
                        text = "Loading map...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                WalkMapLoadState.Unavailable -> Text(
                    text = "Map unavailable while offline. Your walk is still being recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                WalkMapLoadState.StyleError -> {
                    Text(
                        text = "The map style failed to load.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WayloColors.Orange,
                    )
                    TextButton(onClick = onRetryStyle) {
                        Text("Retry")
                    }
                }

                WalkMapLoadState.Ready -> Unit
            }
        }
    }
}

@Composable
private fun WalkInfoPanel(state: ActiveWalkUiState) {
    WayloCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Walk",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = statusLabel(state),
                    style = MaterialTheme.typography.titleLarge,
                    color = WayloColors.Cyan,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = WayloFormat.duration(state.elapsedMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = WayloFormat.distance(state.status.distanceMeters / 1_000.0),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutStatsPanel(state: ActiveWalkUiState) {
    WayloCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatsRow(
                currentLabel = "Current pace",
                currentValue = WayloFormat.pace(state.statistics.currentPaceSecondsPerKm),
                averageLabel = "Average pace",
                averageValue = WayloFormat.pace(state.statistics.averagePaceSecondsPerKm),
            )
            StatsRow(
                currentLabel = "Current speed",
                currentValue = WayloFormat.speed(state.statistics.currentSpeedMetersPerSecond),
                averageLabel = "Average speed",
                averageValue = WayloFormat.speed(state.statistics.averageSpeedMetersPerSecond),
            )
            StatsRow(
                currentLabel = "Est. calories",
                currentValue = WayloFormat.calories(state.statistics.estimatedCaloriesKcal),
                averageLabel = "Walk steps",
                averageValue = WayloFormat.steps(state.statistics.walkSteps),
            )
            if (state.weightKg == null) {
                Text(
                    text = "Add your weight in Profile to estimate calories.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    currentLabel: String,
    currentValue: String,
    averageLabel: String,
    averageValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        StatCell(
            label = currentLabel,
            value = currentValue,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = averageLabel,
            value = averageValue,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackingControls(
    state: ActiveWalkUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopWalk: () -> Unit,
    onRecenter: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            RecenterButton(
                enabled = state.status.hasFix,
                onClick = onRecenter,
            )
        }
        when (state.status.state) {
            WalkingState.Active -> ControlRow(
                primaryLabel = "Pause",
                onPrimary = onPause,
                onStop = onStopWalk,
            )

            WalkingState.Paused -> ControlRow(
                primaryLabel = "Resume",
                onPrimary = onResume,
                onStop = onStopWalk,
            )

            else -> WayloPrimaryButton(
                text = "Stop Walk",
                onClick = onStopWalk,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecenterButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = if (enabled) {
                    "Recenter map on your location"
                } else {
                    "Waiting for a GPS fix before the map can recenter"
                },
                tint = if (enabled) {
                    WayloColors.Cyan
                } else {
                    WayloColors.OnSecondaryText.copy(alpha = 0.4f)
                },
            )
        }
    }
}

@Composable
private fun ControlRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        WayloPrimaryButton(
            text = primaryLabel,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = onStop,
            shape = WayloShapes.medium,
            modifier = Modifier
                .weight(1f)
                .height(WayloDimens.primaryButtonHeight),
        ) {
            Text(
                text = "Stop Walk",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReadinessCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WayloPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorCard(
    state: ActiveWalkUiState,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Text(
                text = "Walk tracking",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = state.status.errorMessage ?: "Something went wrong while tracking this walk.",
                style = MaterialTheme.typography.bodyLarge,
                color = WayloColors.Orange,
            )
            WayloPrimaryButton(
                text = "Try again",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onFinish,
                shape = WayloShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WayloDimens.primaryButtonHeight),
            ) {
                Text(
                    text = "Finish walk",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun CompletionContent(
    state: ActiveWalkUiState,
    onDone: () -> Unit,
) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Text(
                text = "Walk Complete",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            ) {
                WayloStatCard(
                    value = WayloFormat.distance(state.status.distanceMeters / 1_000.0),
                    label = "Distance",
                    modifier = Modifier.weight(1f),
                )
                WayloStatCard(
                    value = WayloFormat.duration(state.elapsedMillis),
                    label = "Duration",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            ) {
                WayloStatCard(
                    value = WayloFormat.pace(state.statistics.averagePaceSecondsPerKm),
                    label = "Avg pace",
                    modifier = Modifier.weight(1f),
                )
                WayloStatCard(
                    value = WayloFormat.speed(state.statistics.averageSpeedMetersPerSecond),
                    label = "Avg speed",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            ) {
                WayloStatCard(
                    value = WayloFormat.calories(state.statistics.estimatedCaloriesKcal),
                    label = "Est. kcal",
                    modifier = Modifier.weight(1f),
                )
                WayloStatCard(
                    value = WayloFormat.steps(state.statistics.walkSteps),
                    label = "Steps",
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.weightKg == null) {
                Text(
                    text = "Add your weight in Profile to estimate calories.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WayloPrimaryButton(
                text = "Done",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StopConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Finish this walk?",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = "Your recorded distance will be saved.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Finish Walk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun statusLabel(state: ActiveWalkUiState): String = when (state.status.state) {
    WalkingState.Starting -> "Starting..."
    WalkingState.Paused -> "Paused"
    WalkingState.Active -> if (state.status.hasFix) "Active" else "Waiting for GPS..."
    else -> ""
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun ActiveWalkScreenPreview() {
    WayloTheme {
        ActiveWalkScreen(
            state = ActiveWalkUiState(
                status = WalkingStatus(
                    state = WalkingState.Active,
                    sessionId = 1L,
                    distanceMeters = 2_140.0,
                    activeMillis = 1_601_000L,
                    activeSegmentStartMillis = null,
                    hasFix = true,
                ),
                readiness = WalkReadiness.Ready,
                elapsedMillis = 1_601_000L,
                map = WalkMapState(styleLoaded = true),
                weightKg = 70,
                statistics = com.waylo.app.domain.model.WorkoutStatistics(
                    distanceMeters = 2_140.0,
                    activeMillis = 1_601_000L,
                    averagePaceSecondsPerKm = 748.1,
                    currentPaceSecondsPerKm = 420.0,
                    averageSpeedMetersPerSecond = 1.34,
                    currentSpeedMetersPerSecond = 2.38,
                    estimatedCaloriesKcal = 143.6,
                    walkSteps = 2_860L,
                ),
            ),
            onRequestLocationPermission = {},
            onOpenSettings = {},
            onOpenLocationSettings = {},
            onPause = {},
            onResume = {},
            onStopWalk = {},
            onRetry = {},
            onDone = {},
            onMapStyleLoaded = {},
            onMapStyleFailed = {},
            onMapPanGesture = {},
            onRecenterMap = {},
            onRetryMapStyle = {},
        )
    }
}
