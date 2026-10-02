package com.waylo.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.BuildConfig
import com.waylo.app.WayloApplication
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.core.permissions.PermissionStatus
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.domain.model.DailyGoalValidator
import com.waylo.app.domain.model.GoalValidationResult
import com.waylo.app.domain.model.UserProgress
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloSectionHeader
import com.waylo.app.ui.permissions.PermissionsViewModel
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as WayloApplication
    val viewModel: PermissionsViewModel = viewModel(
        factory = PermissionsViewModel.factory(application.permissionManager),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stepState by application.stepRepository.state.collectAsStateWithLifecycle()
    val goalScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ProfileScreen(
        permissionStatuses = uiState.statuses,
        dailyStepGoal = stepState.goal,
        onSaveDailyGoal = { goal ->
            goalScope.launch { application.stepRepository.setDailyGoal(goal) }
        },
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    progress: UserProgress = UserProgress.empty(),
    permissionStatuses: List<PermissionStatus> = emptyList(),
    dailyStepGoal: Long = DailyGoalValidator.DEFAULT_STEPS,
    onSaveDailyGoal: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

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
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ProfileHeader(progress)
        PermissionsSection(permissionStatuses)
        PreferencesSection(
            dailyStepGoal = dailyStepGoal,
            onEditGoal = { showGoalDialog = true },
        )
        SettingsSection()

        if (showGoalDialog) {
            DailyGoalDialog(
                currentGoal = dailyStepGoal,
                onDismiss = { showGoalDialog = false },
                onSave = { goal ->
                    onSaveDailyGoal(goal)
                    showGoalDialog = false
                },
            )
        }
    }
}

@Composable
private fun ProfileHeader(progress: UserProgress) {
    WayloCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WayloMascot(size = WayloDimens.smallMascotSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Waylo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Level ${progress.level} · ${WayloFormat.count(progress.xp)} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "🔥 ${WayloFormat.count(progress.currentStreakDays)} day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionsSection(statuses: List<PermissionStatus>) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Permissions")
        WayloCard(contentPadding = PaddingValues(0.dp)) {
            statuses.forEachIndexed { index, status ->
                if (index > 0) {
                    SettingDivider()
                }
                SettingRow(
                    title = status.permission.title,
                    value = status.label,
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(dailyStepGoal: Long, onEditGoal: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Preferences")
        WayloCard(contentPadding = PaddingValues(0.dp)) {
            SettingRow(
                title = "Daily step goal",
                value = WayloFormat.count(dailyStepGoal),
                onClick = onEditGoal,
            )
        }
    }
}

@Composable
private fun DailyGoalDialog(
    currentGoal: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentGoal.toString()) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily step goal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    text = value
                    errorMessage = ""
                },
                label = { Text("Steps per day") },
                isError = errorMessage.isNotEmpty(),
                supportingText = {
                    Text(
                        text = if (errorMessage.isNotEmpty()) {
                            errorMessage
                        } else {
                            "${WayloFormat.count(DailyGoalValidator.MIN_STEPS)}–" +
                                "${WayloFormat.count(DailyGoalValidator.MAX_STEPS)} steps"
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val result = DailyGoalValidator.validate(text)) {
                        is GoalValidationResult.Valid -> onSave(result.steps)
                        is GoalValidationResult.Invalid -> errorMessage = result.message
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Settings")
        WayloCard(contentPadding = PaddingValues(0.dp)) {
            SettingRow(title = "Appearance", value = "Dark")
            SettingDivider()
            SettingRow(title = "Units", value = "Metric")
            SettingDivider()
            SettingRow(title = "About Waylo", value = "v${BuildConfig.VERSION_NAME}")
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(
                horizontal = WayloDimens.cardPadding,
                vertical = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun ProfileScreenPreview() {
    WayloTheme {
        ProfileScreen(
            permissionStatuses = WayloPermission.entries.map { permission ->
                PermissionStatus(permission = permission, state = PermissionState.NotRequested)
            },
        )
    }
}
