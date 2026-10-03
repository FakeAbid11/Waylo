package com.waylo.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.common.MascotContent
import com.waylo.app.core.common.MascotContext
import com.waylo.app.core.common.MascotResolver
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.domain.model.UserProgress
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloProgressBar
import com.waylo.app.ui.components.WayloSectionHeader
import com.waylo.app.ui.components.WayloStatCard
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloTheme

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.factory(
            (LocalContext.current.applicationContext as WayloApplication).progressionRepository,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(state = state)
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress

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
            text = "Progress",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        MascotSection(progress = progress)
        LevelCard(progress)
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            WayloStatCard(
                value = "🔥 ${WayloFormat.count(progress.currentStreakDays)}",
                label = "Current Streak",
                modifier = Modifier.weight(1f),
            )
            WayloStatCard(
                value = WayloFormat.count(progress.longestStreakDays),
                label = "Longest Streak",
                modifier = Modifier.weight(1f),
            )
        }
        TotalsSection(progress)
        AchievementsSection()
    }
}

@Composable
private fun MascotSection(progress: UserProgress) {
    val mascotContext = MascotContext.fromProgress(progress)
    val decision = MascotResolver.resolve(mascotContext)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        WayloMascot(
            state = decision.state,
            size = WayloDimens.smallMascotSize,
        )
        Text(
            text = MascotContent.message(decision, mascotContext),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LevelCard(progress: UserProgress) {
    WayloCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level ${progress.level}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${WayloFormat.count(progress.xp)} / ${WayloFormat.count(progress.xpToNextLevel)} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${progress.xpProgressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = WayloColors.Cyan,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        WayloProgressBar(
            progress = progress.xpProgress,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TotalsSection(progress: UserProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Totals")
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            WayloStatCard(
                value = WayloFormat.count(progress.totalSteps),
                label = "Total Steps",
                modifier = Modifier.weight(1f),
            )
            WayloStatCard(
                value = WayloFormat.distance(progress.totalDistanceKm),
                label = "Total Distance",
                modifier = Modifier.weight(1f),
            )
            WayloStatCard(
                value = WayloFormat.minutes(progress.totalWalkingMinutes),
                label = "Walking Time",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AchievementsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
        WayloSectionHeader(title = "Achievements")
        WayloCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "No achievements yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Start walking to unlock your first achievement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun ProgressScreenPreview() {
    WayloTheme {
        ProgressScreen(state = ProgressUiState())
    }
}
