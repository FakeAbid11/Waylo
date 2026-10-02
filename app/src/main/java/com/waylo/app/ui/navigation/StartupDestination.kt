package com.waylo.app.ui.navigation

enum class StartupDestination(val route: String) {
    Onboarding(ONBOARDING_ROUTE),
    MainApp(WayloDestination.defaultRoute),
    ;

    companion object {
        fun from(onboardingCompleted: Boolean): StartupDestination =
            if (onboardingCompleted) MainApp else Onboarding
    }
}
