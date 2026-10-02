package com.waylo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.waylo.app.WayloApplication
import com.waylo.app.ui.components.WayloBottomNavigation
import com.waylo.app.ui.navigation.ONBOARDING_ROUTE
import com.waylo.app.ui.navigation.StartupDestination
import com.waylo.app.ui.navigation.WALK_ROUTE
import com.waylo.app.ui.navigation.WayloDestination
import com.waylo.app.ui.navigation.WayloNavGraph
import com.waylo.app.ui.theme.WayloColors

@Composable
fun WayloApp() {
    val application = LocalContext.current.applicationContext as WayloApplication

    val onboardingCompleted by application.wayloPreferences.onboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)

    val completed = onboardingCompleted
    if (completed == null) {
        StartupLoadingScreen()
    } else {
        WayloContent(
            startDestination = remember { StartupDestination.from(completed).route },
        )
    }
}

@Composable
private fun WayloContent(
    startDestination: String,
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute != null && currentRoute != ONBOARDING_ROUTE && currentRoute != WALK_ROUTE) {
                WayloBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateToBottomDestination(route) },
                )
            }
        },
    ) { innerPadding ->
        WayloNavGraph(
            navController = navController,
            startDestination = startDestination,
            onOnboardingFinished = {
                navController.navigate(WayloDestination.defaultRoute) {
                    popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                }
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(36.dp)
                .semantics { contentDescription = "Loading Waylo" },
            color = WayloColors.Primary,
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
