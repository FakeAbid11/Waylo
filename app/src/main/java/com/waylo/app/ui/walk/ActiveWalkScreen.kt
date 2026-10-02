package com.waylo.app.ui.walk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
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
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import com.waylo.app.service.WalkingForegroundService
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloStatCard
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloShapes
import com.waylo.app.ui.theme.WayloTheme

@Composable
fun ActiveWalkRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as WayloApplication
    val viewModel: ActiveWalkViewModel = viewModel(
        factory = ActiveWalkViewModel.factory(
            repository = application.walkingRepository,
            startService = { startWalkingService(context) },
            now = { System.currentTimeMillis() },
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
    modifier: Modifier = Modifier,
) {
    var showStopConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = WayloDimens.screenHorizontalPadding,
                vertical = WayloDimens.screenVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.sectionSpacing),
    ) {
        when {
            state.status.state == WalkingState.Completed -> CompletionContent(
                state = state,
                onDone = onDone,
            )

            state.readiness == WalkReadiness.PermissionNeeded -> ReadinessContent(
                message = "Location access is needed to record your walks.",
                actionLabel = "Allow location access",
                onAction = onRequestLocationPermission,
            )

            state.readiness == WalkReadiness.PermissionPermanentlyDenied -> ReadinessContent(
                message = "Location access is blocked. You can enable it in Android settings.",
                actionLabel = "Open Settings",
                onAction = onOpenSettings,
            )

            state.readiness == WalkReadiness.LocationDisabled -> ReadinessContent(
                message = "Turn on location so Waylo can record your walk.",
                actionLabel = "Turn on location",
                onAction = onOpenLocationSettings,
            )

            state.status.state == WalkingState.Error -> ErrorContent(
                state = state,
                onRetry = onRetry,
                onFinish = onStopWalk,
            )

            else -> TrackingContent(
                state = state,
                onPause = onPause,
                onResume = onResume,
                onStopWalk = { showStopConfirmation = true },
            )
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
private fun TrackingContent(
    state: ActiveWalkUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopWalk: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Text(
            text = "Walk",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = statusLabel(state),
            style = MaterialTheme.typography.titleLarge,
            color = WayloColors.Cyan,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = WayloFormat.duration(state.elapsedMillis),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = WayloFormat.distance(state.status.distanceMeters / 1_000.0),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
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
private fun ReadinessContent(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
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

@Composable
private fun ErrorContent(
    state: ActiveWalkUiState,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
) {
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

@Composable
private fun CompletionContent(
    state: ActiveWalkUiState,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Text(
            text = "Walk Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
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
        Spacer(modifier = Modifier.height(8.dp))
        WayloPrimaryButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
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
            ),
            onRequestLocationPermission = {},
            onOpenSettings = {},
            onOpenLocationSettings = {},
            onPause = {},
            onResume = {},
            onStopWalk = {},
            onRetry = {},
            onDone = {},
        )
    }
}
