package com.waylo.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.core.permissions.PermissionStatus
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloGradients
import com.waylo.app.ui.theme.WayloShapes

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    permissions: List<PermissionStatus>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WayloDimens.screenHorizontalPadding)
            .padding(top = WayloDimens.screenVerticalPadding, bottom = WayloDimens.screenVerticalPadding),
    ) {
        OnboardingHeader(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            showBack = !state.isFirstPage,
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = state.page,
                modifier = Modifier.fillMaxSize(),
                label = "onboarding_page",
            ) { page ->
                OnboardingPageContent(
                    page = page,
                    permissions = permissions,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingActions(
            state = state,
            permissions = permissions,
            onContinue = onContinue,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings,
            onComplete = onComplete,
        )
    }
}

@Composable
private fun OnboardingHeader(
    currentPage: Int,
    totalPages: Int,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            Box(
                modifier = Modifier.size(WayloDimens.minTouchTarget),
            )
        }
        OnboardingProgressIndicator(
            currentPage = currentPage,
            totalPages = totalPages,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Page ${currentPage + 1} of $totalPages"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(totalPages) { index ->
            val segmentModifier = if (index <= currentPage) {
                Modifier.background(WayloGradients.signature, WayloShapes.extraSmall)
            } else {
                Modifier.background(WayloColors.ProgressTrack, WayloShapes.extraSmall)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .then(segmentModifier),
            )
        }
    }
}

@Composable
private fun OnboardingActions(
    state: OnboardingUiState,
    permissions: List<PermissionStatus>,
    onContinue: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onComplete: () -> Unit,
) {
    when (state.page) {
        OnboardingPage.PermissionRequest -> PermissionRequestActions(
            permissions = permissions,
            onContinue = onContinue,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings,
        )
        OnboardingPage.Ready -> WayloPrimaryButton(
            text = "Let's Go",
            onClick = onComplete,
        )
        else -> WayloPrimaryButton(
            text = "Continue",
            onClick = onContinue,
        )
    }
}

@Composable
private fun PermissionRequestActions(
    permissions: List<PermissionStatus>,
    onContinue: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val canRequest = permissions.any {
        it.state == PermissionState.NotRequested || it.state == PermissionState.Denied
    }
    val isBlocked = permissions.any { it.state == PermissionState.PermanentlyDenied }

    when {
        canRequest -> {
            val alreadyDenied = permissions.any { it.state == PermissionState.Denied }
            WayloPrimaryButton(
                text = if (alreadyDenied) "Try Again" else "Allow",
                onClick = onRequestPermissions,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WayloDimens.minTouchTarget),
            ) {
                Text(
                    text = "Not now",
                    color = WayloColors.OnSecondaryText,
                )
            }
        }
        isBlocked -> {
            WayloPrimaryButton(
                text = "Continue",
                onClick = onContinue,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WayloDimens.minTouchTarget),
            ) {
                Text(
                    text = "Open Settings",
                    color = WayloColors.OnSecondaryText,
                )
            }
        }
        else -> WayloPrimaryButton(
            text = "Continue",
            onClick = onContinue,
        )
    }
}
