package com.waylo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class WayloDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home),
    Progress("progress", "Progress", Icons.Filled.EmojiEvents),
    Explore("explore", "Explore", Icons.Filled.Explore),
    Profile("profile", "Profile", Icons.Filled.Person);

    companion object {
        val defaultRoute: String = Home.route

        val bottomNavItems: List<WayloDestination> = entries.toList()

        fun fromRoute(route: String?): WayloDestination? = entries.firstOrNull { it.route == route }
    }
}
