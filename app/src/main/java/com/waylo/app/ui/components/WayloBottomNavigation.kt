package com.waylo.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waylo.app.ui.navigation.WayloDestination
import com.waylo.app.ui.theme.WayloColors

@Composable
fun WayloBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = WayloColors.SurfaceElevated,
        tonalElevation = 0.dp,
    ) {
        WayloDestination.bottomNavItems.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WayloColors.Background,
                    selectedTextColor = WayloColors.OnBackground,
                    indicatorColor = WayloColors.Primary,
                    unselectedIconColor = WayloColors.OnSecondaryText,
                    unselectedTextColor = WayloColors.OnSecondaryText,
                ),
            )
        }
    }
}
