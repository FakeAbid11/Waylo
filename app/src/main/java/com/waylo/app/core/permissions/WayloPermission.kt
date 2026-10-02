package com.waylo.app.core.permissions

import android.Manifest

enum class WayloPermission(
    val manifestPermission: String,
    val requiredFromSdk: Int,
    val title: String,
    val purpose: String,
    val requestDuringOnboarding: Boolean = true,
) {
    Activity(
        manifestPermission = Manifest.permission.ACTIVITY_RECOGNITION,
        requiredFromSdk = 29,
        title = "Activity",
        purpose = "Waylo uses your activity data to count your steps.",
    ),
    Notifications(
        manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
        requiredFromSdk = 33,
        title = "Notifications",
        purpose = "Waylo can remind you about your progress and important walking updates.",
    ),
    CoarseLocation(
        manifestPermission = Manifest.permission.ACCESS_COARSE_LOCATION,
        requiredFromSdk = 1,
        title = "Location",
        purpose = "Waylo uses your location during walks to record distance and your route.",
        requestDuringOnboarding = false,
    ),
    FineLocation(
        manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
        requiredFromSdk = 1,
        title = "Precise location",
        purpose = "Precise location keeps your walking distance and route accurate.",
        requestDuringOnboarding = false,
    ),
}
