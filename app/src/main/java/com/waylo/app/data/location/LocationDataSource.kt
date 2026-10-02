package com.waylo.app.data.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.waylo.app.BuildConfig
import com.waylo.app.domain.model.LocationSample

interface LocationDataSource {
    fun isLocationEnabled(): Boolean
    fun start(onSample: (LocationSample) -> Unit): Boolean
    fun stop()
}

class AndroidLocationDataSource(context: Context) : LocationDataSource {

    private val locationManager: LocationManager? =
        context.applicationContext.getSystemService(LocationManager::class.java)

    private var listener: LocationListener? = null

    override fun isLocationEnabled(): Boolean {
        val manager = locationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            PROVIDERS_OF_INTEREST.any { provider ->
                runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
            }
        }
    }

    override fun start(onSample: (LocationSample) -> Unit): Boolean {
        val manager = locationManager ?: return false
        if (listener != null) return true
        if (!isLocationEnabled()) return false
        val provider = selectProvider(manager) ?: return false
        val newListener = LocationListener { location ->
            toSample(location)?.let(onSample)
        }
        return try {
            manager.requestLocationUpdates(
                provider,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                newListener,
                Looper.getMainLooper(),
            )
            listener = newListener
            true
        } catch (exception: SecurityException) {
            debugLog("Location permission missing, updates not registered")
            false
        }
    }

    override fun stop() {
        val current = listener ?: return
        locationManager?.removeUpdates(current)
        listener = null
    }

    private fun selectProvider(manager: LocationManager): String? {
        return PROVIDERS_OF_INTEREST.firstOrNull { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    private fun toSample(location: Location): LocationSample? {
        val latitude = location.latitude
        val longitude = location.longitude
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) return null
        if (location.time <= 0L) return null
        return LocationSample(
            latitude = latitude,
            longitude = longitude,
            timestampMillis = location.time,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            speedMps = if (location.hasSpeed()) location.speed else null,
        )
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "WayloLocation"

        /** ~3 seconds between updates; together with the distance gate this keeps GPS battery use reasonable. */
        const val MIN_INTERVAL_MS = 3_000L

        /** ~5 meters of movement before a new callback, so stationary noise does not wake us up. */
        const val MIN_DISTANCE_METERS = 5f

        private val PROVIDERS_OF_INTEREST = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )
    }
}
