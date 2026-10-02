package com.waylo.app.core.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateTest {

    @Test
    fun requestableStatesAllowAskingAgain() {
        assertTrue(PermissionState.NotRequested.canRequestAgain)
        assertTrue(PermissionState.Denied.canRequestAgain)
    }

    @Test
    fun settledStatesDoNotAllowAskingAgain() {
        assertFalse(PermissionState.Granted.canRequestAgain)
        assertFalse(PermissionState.PermanentlyDenied.canRequestAgain)
        assertFalse(PermissionState.Unsupported.canRequestAgain)
    }
}
