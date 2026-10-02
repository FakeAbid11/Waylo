package com.waylo.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.core.permissions.PermissionStatus
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.ui.components.WayloCard
import com.waylo.app.ui.components.WayloMascot
import com.waylo.app.ui.theme.WayloColors
import com.waylo.app.ui.theme.WayloDimens
import com.waylo.app.ui.theme.WayloGradients
import com.waylo.app.ui.theme.WayloShapes

@Composable
internal fun OnboardingPageContent(
    page: OnboardingPage,
    permissions: List<PermissionStatus>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        when (page) {
            OnboardingPage.Welcome -> WelcomePage()
            OnboardingPage.Concepts -> ConceptsPage()
            OnboardingPage.Companion -> CompanionPage()
            OnboardingPage.PermissionsExplanation -> PermissionsExplanationPage()
            OnboardingPage.PermissionRequest -> PermissionRequestPage(permissions)
            OnboardingPage.Ready -> ReadyPage()
        }
    }
}

@Composable
private fun WelcomePage() {
    WayloMascot(size = WayloDimens.mascotSize)
    PageHeadline(text = "Make Every Walk an Adventure")
    PageDescription(
        text = "Waylo turns everyday walks into a journey you actually want to take.",
    )
}

@Composable
private fun ConceptsPage() {
    PageHeadline(text = "Every step moves you forward")
    PageDescription(
        text = "Walk, track your progress, and watch your journey grow.",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            ConceptCard(
                icon = Icons.Filled.DirectionsWalk,
                title = "Walk",
                description = "Turn daily walks into an adventure.",
                modifier = Modifier.weight(1f),
            )
            ConceptCard(
                icon = Icons.Filled.TrackChanges,
                title = "Track",
                description = "See your steps and walks add up.",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing)) {
            ConceptCard(
                icon = Icons.Filled.TrendingUp,
                title = "Progress",
                description = "Grow your journey day by day.",
                modifier = Modifier.weight(1f),
            )
            ConceptCard(
                icon = Icons.Filled.Explore,
                title = "Explore",
                description = "Discover new places as you go.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConceptCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    WayloCard(modifier = modifier) {
        IconBadge(icon = icon)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompanionPage() {
    WayloMascot(size = WayloDimens.mascotSize)
    PageHeadline(text = "Meet your walking companion")
    PageDescription(
        text = "Your Waylo companion will celebrate your walks, milestones, and adventures with you.",
    )
}

@Composable
private fun PermissionsExplanationPage() {
    PageHeadline(text = "Permissions, explained")
    PageDescription(
        text = "Waylo only asks for what it needs, and always explains why.",
    )
    PermissionExplanationRow(
        icon = Icons.Filled.DirectionsWalk,
        title = WayloPermission.Activity.title,
        description = WayloPermission.Activity.purpose,
    )
    PermissionExplanationRow(
        icon = Icons.Filled.Notifications,
        title = WayloPermission.Notifications.title,
        description = WayloPermission.Notifications.purpose,
    )
    PermissionExplanationRow(
        icon = Icons.Filled.Place,
        title = "Location",
        description = "Waylo will use your location during walks to record your route.",
    )
}

@Composable
private fun PermissionExplanationRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        PermissionRowContent(icon = icon, title = title, description = description)
    }
}

@Composable
private fun PermissionRowContent(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge(icon = icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRequestPage(permissions: List<PermissionStatus>) {
    PageHeadline(text = "Allow Waylo to help")
    PageDescription(
        text = "You can change these at any time in Android settings.",
    )
    val applicable = permissions.filter { it.state != PermissionState.Unsupported }
    if (applicable.isEmpty()) {
        Text(
            text = "Waylo does not need any special permissions on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
        ) {
            applicable.forEach { status ->
                PermissionStatusCard(status = status)
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(status: PermissionStatus) {
    WayloCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WayloDimens.cardSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(icon = status.permission.icon())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status.permission.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(status = status)
                }
                Text(
                    text = status.permission.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                denialMessage(status)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = WayloColors.Orange,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(WayloColors.SurfaceElevated, WayloShapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WayloColors.Cyan,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: PermissionStatus) {
    val color = when (status.state) {
        PermissionState.Granted -> WayloColors.Success
        PermissionState.Denied,
        PermissionState.PermanentlyDenied -> WayloColors.Orange
        else -> WayloColors.OnSecondaryText
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

@Composable
private fun ReadyPage() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(WayloGradients.signature, WayloShapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = WayloColors.Background,
            modifier = Modifier.size(40.dp),
        )
    }
    PageHeadline(text = "You're ready")
    PageDescription(
        text = "Your next adventure starts with a single step.",
    )
}

@Composable
private fun PageHeadline(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PageDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun WayloPermission.icon(): ImageVector = when (this) {
    WayloPermission.Activity -> Icons.Filled.DirectionsWalk
    WayloPermission.Notifications -> Icons.Filled.Notifications
    WayloPermission.CoarseLocation -> Icons.Filled.Place
    WayloPermission.FineLocation -> Icons.Filled.Place
}

private fun denialMessage(status: PermissionStatus): String? = when (status.state) {
    PermissionState.Denied -> when (status.permission) {
        WayloPermission.Activity -> "Activity permission wasn't granted. Step counting may not be available until you allow it."
        WayloPermission.Notifications -> "Notifications aren't enabled, so reminders and walking updates won't be shown."
        WayloPermission.CoarseLocation -> "Location access wasn't granted, so walks can't record distance."
        WayloPermission.FineLocation -> "Precise location wasn't granted, so walk distances may be inaccurate."
    }
    PermissionState.PermanentlyDenied -> when (status.permission) {
        WayloPermission.Activity -> "Activity permission is blocked. You can enable it in Android settings."
        WayloPermission.Notifications -> "Notifications are blocked. You can enable them in Android settings."
        WayloPermission.CoarseLocation -> "Location access is blocked. You can enable it in Android settings."
        WayloPermission.FineLocation -> "Precise location is blocked. You can enable it in Android settings."
    }
    else -> null
}
