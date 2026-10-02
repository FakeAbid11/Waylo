package com.waylo.app.data.location

import android.location.Location
import com.waylo.app.domain.model.LocationSample

/**
 * Distance between two samples using the framework geographic calculation
 * (`Location.distanceBetween`), so the domain engine stays free of Android APIs.
 */
object FrameworkDistance {

    fun between(from: LocationSample, to: LocationSample): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude,
            results,
        )
        return results[0].toDouble()
    }
}
