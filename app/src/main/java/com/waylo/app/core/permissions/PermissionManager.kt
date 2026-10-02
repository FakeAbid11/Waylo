package com.waylo.app.core.permissions

class PermissionManager(
    private val sdkInt: Int,
    private val isGranted: (String) -> Boolean,
    private val shouldShowRationale: (String) -> Boolean,
) {

    private val requestedThisSession = mutableSetOf<WayloPermission>()

    fun isRequired(permission: WayloPermission): Boolean = sdkInt >= permission.requiredFromSdk

    fun stateOf(permission: WayloPermission): PermissionState {
        if (!isRequired(permission)) return PermissionState.Unsupported
        if (isGranted(permission.manifestPermission)) return PermissionState.Granted
        if (permission !in requestedThisSession) return PermissionState.NotRequested
        return if (shouldShowRationale(permission.manifestPermission)) {
            PermissionState.Denied
        } else {
            PermissionState.PermanentlyDenied
        }
    }

    fun statusOf(permission: WayloPermission): PermissionStatus =
        PermissionStatus(permission = permission, state = stateOf(permission))

    fun statuses(): List<PermissionStatus> = WayloPermission.entries.map { statusOf(it) }

    fun permissionsToRequest(): List<WayloPermission> = WayloPermission.entries.filter { permission ->
        isRequired(permission) && !isGranted(permission.manifestPermission)
    }

    fun markRequested(permissions: Collection<WayloPermission>) {
        requestedThisSession += permissions
    }
}
