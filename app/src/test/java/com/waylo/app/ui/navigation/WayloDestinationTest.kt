package com.waylo.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WayloDestinationTest {

    @Test
    fun bottomNavigationContainsTheFourWayloAreas() {
        assertEquals(
            listOf("home", "progress", "explore", "profile"),
            WayloDestination.bottomNavItems.map { it.route },
        )
    }

    @Test
    fun bottomNavigationLabelsMatchTheAreas() {
        assertEquals(
            listOf("Home", "Progress", "Explore", "Profile"),
            WayloDestination.bottomNavItems.map { it.label },
        )
    }

    @Test
    fun routesAreUnique() {
        val routes = WayloDestination.bottomNavItems.map { it.route }

        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun homeIsTheStartDestination() {
        assertEquals("home", WayloDestination.defaultRoute)
        assertSame(WayloDestination.Home, WayloDestination.fromRoute(WayloDestination.defaultRoute))
    }

    @Test
    fun fromRouteResolvesEveryBottomNavigationRoute() {
        WayloDestination.bottomNavItems.forEach { destination ->
            assertSame(destination, WayloDestination.fromRoute(destination.route))
        }
    }

    @Test
    fun fromRouteReturnsNullForUnknownRoutes() {
        assertNull(WayloDestination.fromRoute("active_walk"))
        assertNull(WayloDestination.fromRoute(null))
        assertNotNull(WayloDestination.fromRoute("settings"))
    }
}
