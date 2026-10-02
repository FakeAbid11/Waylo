package com.waylo.app

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.waylo.app.core.permissions.PermissionManager
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.preferences.WayloPreferences
import com.waylo.app.data.preferences.wayloDataStore

class WayloApplication : Application() {

    val wayloDatabase: WayloDatabase by lazy { WayloDatabase.getInstance(this) }

    val wayloPreferences: WayloPreferences by lazy { WayloPreferences(wayloDataStore) }

    var currentActivity: Activity? = null
        internal set

    val permissionManager: PermissionManager by lazy {
        PermissionManager(
            sdkInt = Build.VERSION.SDK_INT,
            isGranted = { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            },
            shouldShowRationale = { permission ->
                currentActivity?.shouldShowRequestPermissionRationale(permission) ?: true
            },
        )
    }
}
