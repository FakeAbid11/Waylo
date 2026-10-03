package com.waylo.app

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import com.waylo.app.core.permissions.PermissionManager
import com.waylo.app.core.permissions.WayloPermission
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.location.AndroidLocationDataSource
import com.waylo.app.data.location.FrameworkDistance
import com.waylo.app.data.location.LocationDataSource
import com.waylo.app.data.preferences.WayloPreferences
import com.waylo.app.data.preferences.wayloDataStore
import com.waylo.app.data.step.AndroidStepSensorDataSource
import com.waylo.app.data.step.StepRepository
import com.waylo.app.data.step.StepRepositoryImpl
import com.waylo.app.data.step.StepSensorDataSource
import com.waylo.app.data.step.StepStateStore
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.data.walk.WalkingRepositoryImpl
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

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

    val walkingLocationDataSource: LocationDataSource by lazy { AndroidLocationDataSource(this) }

    val mapNetworkStatus: Flow<Boolean> by lazy {
        callbackFlow {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            runCatching { connectivityManager.registerNetworkCallback(request, callback) }
            trySend(hasInternetConnection(connectivityManager))
            awaitClose {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }.conflate().distinctUntilChanged()
    }

    val walkingRepository: WalkingRepository by lazy {
        WalkingRepositoryImpl(
            locationDataSource = walkingLocationDataSource,
            dao = wayloDatabase.walkingDao(),
            permissionState = { permissionManager.stateOf(WayloPermission.FineLocation) },
            now = { System.currentTimeMillis() },
            measureDistance = { from, to -> FrameworkDistance.between(from, to) },
            stepCountNow = { stepRepository.currentSensorCount() },
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

    private fun hasInternetConnection(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
