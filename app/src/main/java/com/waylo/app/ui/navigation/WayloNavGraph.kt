package com.waylo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.waylo.app.ui.explore.ExploreScreen
import com.waylo.app.ui.home.HomeRoute
import com.waylo.app.ui.profile.ProfileScreen
import com.waylo.app.ui.progress.ProgressRoute

@Composable
fun WayloNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = WayloDestination.defaultRoute,
        modifier = modifier,
    ) {
        composable(WayloDestination.Home.route) { HomeRoute() }
        composable(WayloDestination.Progress.route) { ProgressRoute() }
        composable(WayloDestination.Explore.route) { ExploreScreen() }
        composable(WayloDestination.Profile.route) { ProfileScreen() }
    }
}
