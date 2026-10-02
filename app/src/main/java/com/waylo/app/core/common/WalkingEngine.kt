package com.waylo.app.core.common

import com.waylo.app.domain.model.LocationSample

/**
 * Conservative GPS filtering and cumulative distance for a walk session.
 *
 * A sample is accepted only when all of the following hold:
 * - coordinates are finite and inside the valid latitude/longitude ranges
 * - reported accuracy is finite and not worse than [maxAccuracyMeters]
 *   (a missing accuracy value is tolerated, an implausible one is not)
 * - the timestamp moves strictly forward compared to the current anchor
 *   (duplicate, stale and backwards timestamps are rejected)
 * - the implied speed between the anchor and the sample does not exceed
 *   [maxSpeedMps], which also rejects impossible jump distances over
 *   unrealistically short periods
 *
 * The first accepted sample establishes the initial anchor and contributes
 * zero distance. Rejected samples never replace the anchor, so an isolated
 * bad jump cannot split or inflate the accumulated distance.
 */
class WalkingEngine(
    private val measureDistance: (LocationSample, LocationSample) -> Double,
    initialDistanceMeters: Double = 0.0,
    private val maxAccuracyMeters: Float = MAX_ACCURACY_METERS,
    private val maxSpeedMps: Double = MAX_SPEED_MPS,
) {

    private var anchor: LocationSample? = null

    var distanceMeters: Double = initialDistanceMeters.coerceAtLeast(0.0)
        private set

    val hasAnchor: Boolean
        get() = anchor != null

    fun submit(sample: LocationSample): Boolean {
        if (!sample.hasValidCoordinates) return false
        val accuracy = sample.accuracyMeters
        if (accuracy != null && (!accuracy.isFinite() || accuracy < 0f || accuracy > maxAccuracyMeters)) {
            return false
        }
        val previous = anchor
        if (previous == null) {
            anchor = sample
            return true
        }
        if (sample.timestampMillis <= previous.timestampMillis) return false
        val segment = measureDistance(previous, sample)
        if (!segment.isFinite() || segment < 0.0) return false
        val elapsedSeconds = (sample.timestampMillis - previous.timestampMillis) / 1000.0
        if (elapsedSeconds <= 0.0) return false
        if (segment / elapsedSeconds > maxSpeedMps) return false
        distanceMeters += segment
        anchor = sample
        return true
    }

    fun resetAnchor() {
        anchor = null
    }

    companion object {
        /** Samples reporting accuracy worse than this many meters are ignored. */
        const val MAX_ACCURACY_METERS: Float = 50f

        /** Walking-oriented ceiling: 5 m/s is 18 km/h, well beyond a plausible walk. */
        const val MAX_SPEED_MPS: Double = 5.0
    }
}
