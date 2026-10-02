package com.waylo.app.core.permissions

enum class PermissionState {
    NotRequested,
    Granted,
    Denied,
    PermanentlyDenied,
    Unsupported,
}

data class PermissionStatus(
    val permission: WayloPermission,
    val state: PermissionState,
) {
    val label: String
        get() = when (state) {
            PermissionState.Granted -> "Allowed"
            PermissionState.Unsupported -> "Not needed"
            PermissionState.NotRequested,
            PermissionState.Denied,
            PermissionState.PermanentlyDenied -> "Not allowed"
        }
}
