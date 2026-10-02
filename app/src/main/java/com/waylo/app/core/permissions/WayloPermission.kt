package com.waylo.app.core.permissions

import android.Manifest

enum class WayloPermission(
    val manifestPermission: String,
    val requiredFromSdk: Int,
    val title: String,
    val purpose: String,
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
}
