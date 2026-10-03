package com.waylo.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.util.WayloFormat
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloSectionHeader
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ActivityHistoryRoute(
    onBack: () -> Unit,
    onStartWalk: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val application = LocalContext.current.applicationContext as WayloApplication
    val viewModel: ActivityHistoryViewModel = viewModel(
        factory = ActivityHistoryViewModel.factory(
            repository = application.walkingRepository,
            now = { System.currentTimeMillis() },
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ActivityHistoryScreen(
        state = state,
        onBack = onBack,
        onStartWalk = onStartWalk,
        onOpenActivity = onOpenActivity,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun ActivityHistoryScreen(
    state: ActivityHistoryUiState,
    onBack: () -> Unit,
    onStartWalk: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = WayloDimens.screenHorizontalPadding,
                vertical = WayloDimens.screenVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.sectionSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to profile",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Activity history",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when (state) {
            ActivityHistoryUiState.Loading -> CenteredMessage {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Loading activity history" },
                    color = WayloColors.Primary,
                )
            }

            ActivityHistoryUiState.Error -> CenteredMessage {
                Text(
                    text = "Couldn't load your walks.",
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

            ActivityHistoryUiState.Empty -> CenteredMessage {
                Text(
                    text = "No walks yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your completed walks will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                WayloPrimaryButton(
                    text = "Start your first walk",
                    onClick = onStartWalk,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is ActivityHistoryUiState.Content -> ActivityList(
                sections = state.sections,
                onOpenActivity = onOpenActivity,
            )
        }
    }
}

@Composable
private fun ActivityList(
    sections: List<ActivitySection>,
    onOpenActivity: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = WayloDimens.sectionSpacing),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        sections.forEach { section ->
            item(key = "header-${section.label}") {
                WayloSectionHeader(title = section.label)
            }
            items(section.entries, key = { it.id }) { entry ->
                ActivityCard(
                    entry = entry,
                    onClick = { onOpenActivity(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    entry: ActivityEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WayloCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.timestampLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = WayloFormat.distance(entry.distanceMeters / 1_000.0),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = WayloFormat.duration(entry.activeMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            val details = buildList {
                add(WayloFormat.pace(entry.averagePaceSecondsPerKm))
                if (entry.walkSteps != null) {
                    add("${WayloFormat.steps(entry.walkSteps)} steps")
                }
            }
            Text(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = WayloDimens.sectionSpacing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun ActivityHistoryScreenPreview() {
    WayloTheme {
        ActivityHistoryScreen(
            state = ActivityHistoryUiState.Content(
                sections = listOf(
                    ActivitySection(
                        label = "Today",
                        entries = listOf(
                            ActivityEntry(
                                id = 1L,
                                timestampLabel = "Today · 6:42 PM",
                                distanceMeters = 2_840.0,
                                activeMillis = 1_884_000L,
                                averagePaceSecondsPerKm = 418.0,
                                walkSteps = 2_931L,
                            ),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onStartWalk = {},
            onOpenActivity = {},
            onRetry = {},
        )
    }
}
