package com.waylo.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.waylo.app.ui.components.WayloBottomNavigation
import com.waylo.app.ui.navigation.WayloDestination
import com.waylo.app.ui.navigation.WayloNavGraph

@Composable
fun WayloApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            WayloBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToBottomDestination(route) },
            )
        },
    ) { innerPadding ->
        WayloNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavHostController.navigateToBottomDestination(route: String) {
    navigate(route) {
        popUpTo(WayloDestination.defaultRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
