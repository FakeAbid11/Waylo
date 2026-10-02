package com.waylo.app.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun activityPermissionIsNotRequiredBelowApi29() {
        val manager = createManager(sdkInt = 28)

        assertFalse(manager.isRequired(WayloPermission.Activity))
        assertEquals(PermissionState.Unsupported, manager.stateOf(WayloPermission.Activity))
    }

    @Test
    fun activityPermissionIsRequiredFromApi29() {
        val manager = createManager(sdkInt = 29)

        assertTrue(manager.isRequired(WayloPermission.Activity))
    }

    @Test
    fun notificationPermissionIsNotRequiredBelowApi33() {
        val manager = createManager(sdkInt = 32)

        assertFalse(manager.isRequired(WayloPermission.Notifications))
        assertEquals(PermissionState.Unsupported, manager.stateOf(WayloPermission.Notifications))
    }

    @Test
    fun notificationPermissionIsRequiredFromApi33() {
        val manager = createManager(sdkInt = 33)

        assertTrue(manager.isRequired(WayloPermission.Notifications))
    }

    @Test
    fun grantedPermissionIsReportedAsGranted() {
        val manager = createManager(
            sdkInt = 33,
            granted = setOf(WayloPermission.Activity.manifestPermission),
        )

        assertEquals(PermissionState.Granted, manager.stateOf(WayloPermission.Activity))
    }

    @Test
    fun neverRequestedPermissionIsReportedAsNotRequested() {
        val manager = createManager(sdkInt = 33)

        assertEquals(PermissionState.NotRequested, manager.stateOf(WayloPermission.Activity))
    }

    @Test
    fun deniedPermissionIsReportedAsDenied() {
        val manager = createManager(
            sdkInt = 33,
            rationale = setOf(WayloPermission.Activity.manifestPermission),
            requested = setOf(WayloPermission.Activity),
        )

        assertEquals(PermissionState.Denied, manager.stateOf(WayloPermission.Activity))
    }

    @Test
    fun permanentlyDeniedPermissionIsReportedAsPermanentlyDenied() {
        val manager = createManager(
            sdkInt = 33,
            requested = setOf(WayloPermission.Activity),
        )

        assertEquals(PermissionState.PermanentlyDenied, manager.stateOf(WayloPermission.Activity))
    }

    @Test
    fun permissionsToRequestOnlyIncludesRequiredAndUngranted() {
        val manager = createManager(
            sdkInt = 33,
            granted = setOf(WayloPermission.Notifications.manifestPermission),
        )

        assertEquals(listOf(WayloPermission.Activity), manager.permissionsToRequest())
    }

    @Test
    fun unsupportedPermissionsAreNeverRequested() {
        val manager = createManager(sdkInt = 26)

        assertEquals(emptyList<WayloPermission>(), manager.permissionsToRequest())
    }

    @Test
    fun statusLabelReflectsState() {
        assertEquals("Allowed", PermissionStatus(WayloPermission.Activity, PermissionState.Granted).label)
        assertEquals("Not needed", PermissionStatus(WayloPermission.Activity, PermissionState.Unsupported).label)
        assertEquals("Not allowed", PermissionStatus(WayloPermission.Activity, PermissionState.Denied).label)
        assertEquals(
            "Not allowed",
            PermissionStatus(WayloPermission.Activity, PermissionState.PermanentlyDenied).label,
        )
        assertEquals(
            "Not allowed",
            PermissionStatus(WayloPermission.Activity, PermissionState.NotRequested).label,
        )
    }

    private fun createManager(
        sdkInt: Int,
        granted: Set<String> = emptySet(),
        rationale: Set<String> = emptySet(),
        requested: Set<WayloPermission> = emptySet(),
    ): PermissionManager = PermissionManager(
        sdkInt = sdkInt,
        isGranted = { it in granted },
        shouldShowRationale = { it in rationale },
    ).also { manager ->
        manager.markRequested(requested)
    }
}
