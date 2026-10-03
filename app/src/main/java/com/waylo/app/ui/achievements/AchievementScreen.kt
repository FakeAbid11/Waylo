package com.waylo.app.ui.achievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waylo.app.WayloApplication
import com.waylo.app.core.common.MascotState
import com.waylo.app.domain.model.AchievementCategory
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.components.WayloPrimaryButton
import com.waylo.app.ui.components.WayloProgressBar
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloShapes
import com.waylo.app.ui.theme.WayloTheme

@Composable
fun AchievementRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AchievementViewModel = viewModel(
        factory = AchievementViewModel.factory(
            (LocalContext.current.applicationContext as WayloApplication).achievementRepository,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AchievementScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onFilter = viewModel::setFilter,
        onDismissBanner = viewModel::dismissUnlockBanner,
        modifier = modifier,
    )
}

@Composable
fun AchievementScreen(
    state: AchievementUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onFilter: (AchievementCategory?) -> Unit = {},
    onDismissBanner: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = WayloDimens.screenHorizontalPadding,
                vertical = WayloDimens.screenVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (!state.loading && !state.error) {
            Text(
                text = "${state.unlockedCount} / ${state.totalCount} unlocked",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 52.dp),
            )
        }

        state.unlockBanner?.let { banner ->
            UnlockBanner(banner = banner, onDismiss = onDismissBanner)
        }

        if (!state.loading && !state.error) {
            CategoryFilterRow(selected = state.filter, onFilter = onFilter)
        }

        when {
            state.loading -> LoadingAchievements()

            state.error -> ErrorContent(onRetry = onRetry)

            else -> {
                if (state.allLocked) {
                    JourneyStartsCard()
                }
                if (state.allUnlocked) {
                    AllUnlockedCard()
                }
                if (state.filterIsEmpty) {
                    Text(
                        text = "No achievements in this category yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.achievements.forEach { item ->
                        AchievementCardItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: AchievementCategory?,
    onFilter: (AchievementCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(label = "All", selected = selected == null, onClick = { onFilter(null) })
        AchievementCategory.entries.forEach { category ->
            FilterChip(
                label = category.label,
                selected = selected == category,
                onClick = { onFilter(category) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = WayloShapes.small,
        color = if (selected) WayloColors.Primary else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun UnlockBanner(
    banner: AchievementUnlockBannerUi,
    onDismiss: () -> Unit,
) {
    WayloCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = WayloColors.Cyan,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (banner.isMultiple) {
                        "${banner.count} achievements unlocked!"
                    } else {
                        "Achievement unlocked!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = WayloColors.Cyan,
                )
                banner.titles.forEach { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "Tap to dismiss",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementCardItem(
    item: AchievementCardUi,
    modifier: Modifier = Modifier,
) {
    WayloCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = item.accessibilityText },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.unlocked) WayloColors.Success else WayloColors.OnSecondaryText,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                CategoryIcon(category = item.category, unlocked = item.unlocked)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.unlocked) {
                Text(
                    text = "✓ Completed",
                    style = MaterialTheme.typography.titleSmall,
                    color = WayloColors.Success,
                )
                item.dateLabel?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item.progressText?.let { progress ->
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WayloProgressBar(
                        progress = item.progressFraction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Single, replaceable icon mapping — swap these vectors for custom art later. */
@Composable
private fun CategoryIcon(category: AchievementCategory, unlocked: Boolean) {
    val imageVector = when (category) {
        AchievementCategory.WALKS -> Icons.AutoMirrored.Outlined.DirectionsWalk
        AchievementCategory.DISTANCE -> Icons.Outlined.Route
        AchievementCategory.STEPS -> Icons.Outlined.Stairs
        AchievementCategory.STREAK -> Icons.Outlined.Whatshot
        AchievementCategory.XP -> Icons.Outlined.Stars
        AchievementCategory.LEVEL -> Icons.AutoMirrored.Outlined.TrendingUp
    }
    Surface(
        shape = WayloShapes.small,
        color = WayloColors.SurfaceElevated,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = if (unlocked) WayloColors.Cyan else WayloColors.OnSecondaryText,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun LoadingAchievements() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WayloDimens.sectionSpacing),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(36.dp)
                .clearAndSetSemantics { contentDescription = "Loading achievements" },
            color = WayloColors.Primary,
        )
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WayloDimens.sectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Text(
            text = "Couldn't load achievements.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WayloPrimaryButton(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JourneyStartsCard() {
    EmptyStateCard(
        mascotState = MascotState.Encouraging,
        title = "Your journey starts here.",
        message = "Complete your first walk to unlock your first achievement.",
    )
}

@Composable
private fun AllUnlockedCard() {
    EmptyStateCard(
        mascotState = MascotState.Celebrating,
        title = "All achievements unlocked!",
        message = "Keep walking.",
    )
}

@Composable
private fun EmptyStateCard(
    mascotState: MascotState,
    title: String,
    message: String,
) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            WayloMascot(state = mascotState, size = WayloDimens.smallMascotSize, decorative = true)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B12)
@Composable
private fun AchievementScreenPreview() {
    WayloTheme {
        AchievementScreen(
            state = AchievementUiState(
                loading = false,
                unlockedCount = 2,
                totalCount = 3,
                achievements = listOf(
                    AchievementCardUi(
                        id = "first_walk",
                        title = "First Walk",
                        description = "Complete your first walk.",
                        category = AchievementCategory.WALKS,
                        unlocked = true,
                        unlockedAtMillis = 1_790_000_000_000L,
                        statusLabel = "Unlocked",
                        progressText = null,
                        progressFraction = 1f,
                        dateLabel = "September 28, 2026",
                        accessibilityText = "Complete your first walk. Completed on September 28, 2026.",
                    ),
                    AchievementCardUi(
                        id = "distance_10km",
                        title = "Walk 10 km",
                        description = "Walk a total of 10 kilometers.",
                        category = AchievementCategory.DISTANCE,
                        unlocked = false,
                        unlockedAtMillis = null,
                        statusLabel = "Locked",
                        progressText = "1.24 km / 10.00 km",
                        progressFraction = 0.124f,
                        dateLabel = null,
                        accessibilityText =
                            "Walk a total of 10 kilometers. Locked. Progress: " +
                                "1.24 kilometers of 10 kilometers.",
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
