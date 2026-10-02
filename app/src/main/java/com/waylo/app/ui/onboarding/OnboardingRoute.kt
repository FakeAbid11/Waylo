package com.waylo.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.permissions.openAppSettings
import com.waylo.app.ui.permissions.PermissionsViewModel

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as WayloApplication

    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(application.wayloPreferences),
    )
    val permissionsViewModel: PermissionsViewModel = viewModel(
        factory = PermissionsViewModel.factory(application.permissionManager),
    )

    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val permissionsState by permissionsViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        permissionsViewModel.refresh()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsViewModel.refresh()
    }

    LaunchedEffect(onboardingState.onboardingCompleted) {
        if (onboardingState.onboardingCompleted) {
            onFinished()
        }
    }

    BackHandler(enabled = !onboardingState.isFirstPage) {
        onboardingViewModel.previousPage()
    }

    OnboardingScreen(
        state = onboardingState,
        permissions = permissionsState.statuses.filter { status ->
            status.permission.requestDuringOnboarding
        },
        onBack = onboardingViewModel::previousPage,
        onContinue = onboardingViewModel::nextPage,
        onRequestPermissions = {
            val permissions = permissionsViewModel.permissionsToRequest()
            if (permissions.isNotEmpty()) {
                permissionsViewModel.onRequestLaunched(permissions)
                permissionLauncher.launch(
                    permissions.map { it.manifestPermission }.toTypedArray(),
                )
            }
        },
        onOpenSettings = { openAppSettings(context) },
        onComplete = onboardingViewModel::completeOnboarding,
        modifier = modifier,
    )
}
