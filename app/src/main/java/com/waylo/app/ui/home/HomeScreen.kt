package com.waylo.app.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.domain.model.DailyGoal
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
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsWithLifecycle()
    HomeScreen(state = state)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
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
        DailyGoalCard(state.goal)
        WayloPrimaryButton(
            text = "Start Walk",
            onClick = {},
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
private fun DailyGoalCard(goal: DailyGoal) {
    WayloCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Today's Goal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${goal.progressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = WayloColors.Cyan,
            )
        }
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
            text = "${WayloFormat.count(progress.xp)} XP",
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
        HomeScreen(state = HomeUiState())
    }
}
