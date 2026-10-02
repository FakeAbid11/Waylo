package com.waylo.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupDestinationTest {

    @Test
    fun notCompletedStartsOnboarding() {
        assertEquals(StartupDestination.Onboarding, StartupDestination.from(false))
        assertEquals(ONBOARDING_ROUTE, StartupDestination.from(false).route)
    }

    @Test
    fun completedStartsMainApp() {
        assertEquals(StartupDestination.MainApp, StartupDestination.from(true))
        assertEquals(WayloDestination.defaultRoute, StartupDestination.from(true).route)
    }

    @Test
    fun onboardingIsNotAnExistingBottomDestination() {
        assertEquals(null, WayloDestination.fromRoute(ONBOARDING_ROUTE))
    }
}
