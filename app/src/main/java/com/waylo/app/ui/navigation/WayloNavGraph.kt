package com.waylo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.waylo.app.ui.explore.ExploreScreen
import com.waylo.app.ui.history.ActivityDetailRoute
import com.waylo.app.ui.history.ActivityHistoryRoute
import com.waylo.app.ui.home.HomeRoute
import com.waylo.app.ui.onboarding.OnboardingRoute
import com.waylo.app.ui.profile.ProfileRoute
import com.waylo.app.ui.progress.ProgressRoute
import com.waylo.app.ui.walk.ActiveWalkRoute

const val ONBOARDING_ROUTE = "onboarding"
const val WALK_ROUTE = "walk"
const val HISTORY_ROUTE = "history"
const val ACTIVITY_ROUTE = "activity/{activityId}"

fun activityRoute(activityId: Long): String = "activity/$activityId"

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
        composable(WayloDestination.Home.route) {
            HomeRoute(
                onStartWalk = {
                    navController.navigate(WALK_ROUTE) { launchSingleTop = true }
                },
            )
        }
        composable(WALK_ROUTE) {
            ActiveWalkRoute(
                onDone = { navController.popBackStack() },
                onWalkFinished = { activityId ->
                    navController.navigate(activityRoute(activityId)) {
                        popUpTo(WALK_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        composable(HISTORY_ROUTE) {
            ActivityHistoryRoute(
                onBack = { navController.popBackStack() },
                onStartWalk = {
                    navController.navigate(WALK_ROUTE) { launchSingleTop = true }
                },
                onOpenActivity = { activityId ->
                    navController.navigate(activityRoute(activityId))
                },
            )
        }
        composable(
            route = ACTIVITY_ROUTE,
            arguments = listOf(navArgument("activityId") { type = NavType.LongType }),
        ) { entry ->
            ActivityDetailRoute(
                activityId = requireNotNull(entry.arguments?.getLong("activityId")),
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(WayloDestination.Progress.route) { ProgressRoute() }
        composable(WayloDestination.Explore.route) { ExploreScreen() }
        composable(WayloDestination.Profile.route) {
            ProfileRoute(
                onOpenHistory = { navController.navigate(HISTORY_ROUTE) },
            )
        }
    }
}
