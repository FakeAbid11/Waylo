package com.waylo.app

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.waylo.app.core.permissions.PermissionManager
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.preferences.WayloPreferences
import com.waylo.app.data.preferences.wayloDataStore
import com.waylo.app.data.step.AndroidStepSensorDataSource
import com.waylo.app.data.step.StepRepository
import com.waylo.app.data.step.StepRepositoryImpl
import com.waylo.app.data.step.StepSensorDataSource
import com.waylo.app.data.step.StepStateStore
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WayloApplication : Application() {

    val wayloDatabase: WayloDatabase by lazy { WayloDatabase.getInstance(this) }

    val wayloPreferences: WayloPreferences by lazy { WayloPreferences(wayloDataStore) }

    val stepStateStore: StepStateStore by lazy { StepStateStore(wayloDataStore) }

    val stepSensorDataSource: StepSensorDataSource by lazy { AndroidStepSensorDataSource(this) }

    val stepRepository: StepRepository by lazy {
        StepRepositoryImpl(
            sensor = stepSensorDataSource,
            store = stepStateStore,
            permissionState = { permissionManager.stateOf(WayloPermission.Activity) },
            todayEpochDay = { LocalDate.now().toEpochDay() },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }

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
