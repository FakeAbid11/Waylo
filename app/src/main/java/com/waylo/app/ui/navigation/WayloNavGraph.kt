package com.waylo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.waylo.app.ui.explore.ExploreScreen
import com.waylo.app.ui.home.HomeRoute
import com.waylo.app.ui.onboarding.OnboardingRoute
import com.waylo.app.ui.profile.ProfileRoute
import com.waylo.app.ui.progress.ProgressRoute

const val ONBOARDING_ROUTE = "onboarding"

@Composable
fun WayloNavGraph(
    navController: NavHostController,
    startDestination: String,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ONBOARDING_ROUTE) {
            OnboardingRoute(onFinished = onOnboardingFinished)
        }
        composable(WayloDestination.Home.route) { HomeRoute() }
        composable(WayloDestination.Progress.route) { ProgressRoute() }
        composable(WayloDestination.Explore.route) { ExploreScreen() }
        composable(WayloDestination.Profile.route) { ProfileRoute() }
    }
}
