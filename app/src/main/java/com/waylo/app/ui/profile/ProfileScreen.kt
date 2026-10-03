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
import com.waylo.app.domain.model.WeightValidationResult
import com.waylo.app.domain.model.WeightValidator
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloSectionHeader
import com.waylo.app.ui.permissions.PermissionsViewModel
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as WayloApplication
    val viewModel: PermissionsViewModel = viewModel(
        factory = PermissionsViewModel.factory(application.permissionManager),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stepState by application.stepRepository.state.collectAsStateWithLifecycle()
    val weightKg by application.wayloPreferences.weightKg.collectAsStateWithLifecycle(initialValue = null)
    val goalScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ProfileScreen(
        permissionStatuses = uiState.statuses,
        dailyStepGoal = stepState.goal,
        weightKg = weightKg,
        onOpenHistory = onOpenHistory,
        onSaveDailyGoal = { goal ->
            goalScope.launch { application.stepRepository.setDailyGoal(goal) }
        },
        onSaveWeight = { weight ->
            goalScope.launch { application.wayloPreferences.setWeightKg(weight) }
        },
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    progress: UserProgress = UserProgress.empty(),
    permissionStatuses: List<PermissionStatus> = emptyList(),
    dailyStepGoal: Long = DailyGoalValidator.DEFAULT_STEPS,
    weightKg: Int? = null,
    onOpenHistory: () -> Unit = {},
    onSaveDailyGoal: (Long) -> Unit = {},
    onSaveWeight: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }

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
            weightKg = weightKg,
            onEditGoal = { showGoalDialog = true },
            onEditWeight = { showWeightDialog = true },
        )
        ActivitySection(onOpenHistory = onOpenHistory)
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

        if (showWeightDialog) {
            WeightDialog(
                currentWeightKg = weightKg,
                onDismiss = { showWeightDialog = false },
                onSave = { weight ->
                    onSaveWeight(weight)
                    showWeightDialog = false
                },
                onClear = {
                    onSaveWeight(null)
                    showWeightDialog = false
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
private fun PreferencesSection(
    dailyStepGoal: Long,
    weightKg: Int?,
    onEditGoal: () -> Unit,
    onEditWeight: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Preferences")
        WayloCard(contentPadding = PaddingValues(0.dp)) {
            SettingRow(
                title = "Daily step goal",
                value = WayloFormat.count(dailyStepGoal),
                onClick = onEditGoal,
            )
            SettingDivider()
            SettingRow(
                title = "Weight",
                value = weightKg?.let { "$it kg" } ?: WayloFormat.DASH,
                onClick = onEditWeight,
            )
        }
    }
}

@Composable
private fun ActivitySection(onOpenHistory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Activity")
        WayloCard(contentPadding = PaddingValues(0.dp)) {
            SettingRow(
                title = "Activity history",
                value = "View",
                onClick = onOpenHistory,
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
private fun WeightDialog(
    currentWeightKg: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentWeightKg?.toString() ?: "") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Weight",
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
                label = { Text("Weight in kilograms") },
                isError = errorMessage.isNotEmpty(),
                supportingText = {
                    Text(
                        text = if (errorMessage.isNotEmpty()) {
                            errorMessage
                        } else {
                            "Optional · used only to estimate calories"
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
                    when (val result = WeightValidator.validate(text)) {
                        is WeightValidationResult.Valid -> onSave(result.weightKg)
                        is WeightValidationResult.Invalid -> errorMessage = result.message
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentWeightKg != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
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
