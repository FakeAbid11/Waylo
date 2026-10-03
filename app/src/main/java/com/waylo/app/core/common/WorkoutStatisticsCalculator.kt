package com.waylo.app.core.common

import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import com.waylo.app.domain.model.WorkoutStatistics

/**
 * Pure workout statistics. No Compose, no Android, no MapLibre, no LocationManager.
 * Time-dependent inputs (now, raw sensor count) are always injected by the caller.
 *
 * Units: distance in meters, duration in milliseconds, pace in seconds per kilometer,
 * speed in meters per second, calories in kilocalories (estimate), steps as a count.
 * Every value is derived from real recorded data; unavailable values are null and the
 * UI renders them as a placeholder instead of guessing.
 */
object WorkoutStatisticsCalculator {

    /** Below this accepted distance the average pace/speed ratios are meaningless. */
    const val MIN_AVERAGE_DISTANCE_METERS = 10.0

    /** Rolling window used for current pace/speed: accepted points from the last 45 s. */
    const val CURRENT_WINDOW_MILLIS = 45_000L

    /** A gap longer than this splits the rolling window (GPS dropout = separate movement). */
    const val MAX_POINT_GAP_MILLIS = 15_000L

    /** The rolling window needs at least this much movement to produce a stable reading. */
    const val MIN_WINDOW_DISTANCE_METERS = 10.0

    /** Sanity ceiling: accepted walking speeds never approach this (engine filters at 5 m/s). */
    const val MAX_REASONABLE_SPEED_METERS_PER_SECOND = 15.0

    /** pace below this (speed above the sanity ceiling) is physically impossible while walking. */
    const val MIN_REASONABLE_PACE_SECONDS_PER_KM = 1_000.0 / MAX_REASONABLE_SPEED_METERS_PER_SECOND

    const val MIN_WEIGHT_KG = 20.0
    const val MAX_WEIGHT_KG = 300.0

    /** Compendium of Physical Activities walking entry for ~3.0 mph (4.8 km/h). */
    const val DEFAULT_WALKING_MET = 3.5

    data class MovementWindow(
        val distanceMeters: Double,
        val durationMillis: Long,
        val speedMetersPerSecond: Double,
        val paceSecondsPerKm: Double,
    )

    fun statistics(
        distanceMeters: Double,
        activeMillis: Long,
        liveMovement: Boolean,
        route: WalkRoute,
        nowMillis: Long,
        weightKg: Double?,
        walkStartStepCount: Long?,
        currentSensorCount: Long?,
        finalWalkStepCount: Long?,
    ): WorkoutStatistics {
        val distance = if (distanceMeters.isFinite()) distanceMeters.coerceAtLeast(0.0) else 0.0
        val duration = activeMillis.coerceAtLeast(0L)
        val movement = if (liveMovement) currentMovement(route, nowMillis) else null
        return WorkoutStatistics(
            distanceMeters = distance,
            activeMillis = duration,
            averagePaceSecondsPerKm = averagePaceSecondsPerKm(distance, duration),
            currentPaceSecondsPerKm = movement?.paceSecondsPerKm,
            averageSpeedMetersPerSecond = averageSpeedMetersPerSecond(distance, duration),
            currentSpeedMetersPerSecond = movement?.speedMetersPerSecond,
            estimatedCaloriesKcal = estimatedCaloriesKcal(distance, duration, weightKg),
            walkSteps = walkSteps(walkStartStepCount, currentSensorCount, finalWalkStepCount),
        )
    }

    /** pace = active duration / distance, in seconds per kilometer. Null when meaningless. */
    fun averagePaceSecondsPerKm(distanceMeters: Double, activeMillis: Long): Double? {
        if (!distanceMeters.isFinite() || distanceMeters < MIN_AVERAGE_DISTANCE_METERS) return null
        if (activeMillis <= 0L) return null
        val pace = activeMillis.toDouble() / distanceMeters
        return pace.takeIf {
            it.isFinite() && it > 0.0 && it >= MIN_REASONABLE_PACE_SECONDS_PER_KM
        }
    }

    /** average speed = cumulative accepted distance / active duration. */
    fun averageSpeedMetersPerSecond(distanceMeters: Double, activeMillis: Long): Double? {
        if (!distanceMeters.isFinite() || distanceMeters < MIN_AVERAGE_DISTANCE_METERS) return null
        if (activeMillis <= 0L) return null
        val speed = distanceMeters / (activeMillis.toDouble() / 1_000.0)
        return speed.takeIf { it.isFinite() && it in 0.0..MAX_REASONABLE_SPEED_METERS_PER_SECOND }
    }

    /**
     * Recent valid movement: accepted route points after the last pause break and inside
     * [CURRENT_WINDOW_MILLIS], split on gaps longer than [MAX_POINT_GAP_MILLIS] with only
     * the latest chunk used. Requires at least two points and [MIN_WINDOW_DISTANCE_METERS]
     * of movement so one noisy segment or idle standing cannot produce an extreme reading.
     * Current pace and speed are always computed from the same window so they never
     * contradict each other.
     */
    fun currentMovement(route: WalkRoute, nowMillis: Long): MovementWindow? {
        val points = route.points
        if (points.size < 2) return null
        val segmentStart = route.breakBeforeIndexes.maxOrNull() ?: 0
        if (segmentStart >= points.size) return null
        val cutoff = nowMillis - CURRENT_WINDOW_MILLIS
        val recent = points.subList(segmentStart, points.size)
            .filter { it.timestampMillis >= cutoff }
        val chunk = splitByGap(recent).lastOrNull() ?: return null
        if (chunk.size < 2) return null
        val distance = pathLengthMeters(chunk)
        if (!distance.isFinite() || distance < MIN_WINDOW_DISTANCE_METERS) return null
        val duration = chunk.last().timestampMillis - chunk.first().timestampMillis
        if (duration <= 0L) return null
        val speed = distance / (duration.toDouble() / 1_000.0)
        if (!speed.isFinite() || speed <= 0.0 || speed > MAX_REASONABLE_SPEED_METERS_PER_SECOND) return null
        val pace = duration.toDouble() / distance
        if (!pace.isFinite() || pace <= 0.0) return null
        return MovementWindow(
            distanceMeters = distance,
            durationMillis = duration,
            speedMetersPerSecond = speed,
            paceSecondsPerKm = pace,
        )
    }

    /**
     * Transparent estimate: calories = MET x weight_kg x active hours.
     * The MET comes from the documented walking-speed bins of the Compendium of
     * Physical Activities (2.0-4.5 mph entries), selected by the walk's average speed,
     * falling back to the documented 3.5 MET default when no average speed exists yet.
     * Returns null when no valid weight is configured - Waylo never invents one.
     */
    fun estimatedCaloriesKcal(
        distanceMeters: Double,
        activeMillis: Long,
        weightKg: Double?,
    ): Double? {
        val weight = weightKg ?: return null
        if (!weight.isFinite() || weight < MIN_WEIGHT_KG || weight > MAX_WEIGHT_KG) return null
        if (activeMillis <= 0L) return 0.0
        val met = metForSpeed(averageSpeedMetersPerSecond(distanceMeters, activeMillis))
        val hours = activeMillis.toDouble() / 3_600_000.0
        val calories = met * weight * hours
        if (!calories.isFinite()) return null
        return calories.coerceAtLeast(0.0)
    }

    /** Walking MET by speed bins (km/h): <3.2 -> 2.8, <4.0 -> 3.0, <4.8 -> 3.5, <5.6 -> 4.3, <6.4 -> 5.0, else 6.3. */
    fun metForSpeed(speedMetersPerSecond: Double?): Double {
        if (speedMetersPerSecond == null ||
            !speedMetersPerSecond.isFinite() ||
            speedMetersPerSecond < 0.0
        ) {
            return DEFAULT_WALKING_MET
        }
        val kilometersPerHour = speedMetersPerSecond * 3.6
        return when {
            kilometersPerHour < 3.2 -> 2.8
            kilometersPerHour < 4.0 -> 3.0
            kilometersPerHour < 4.8 -> 3.5
            kilometersPerHour < 5.6 -> 4.3
            kilometersPerHour < 6.4 -> 5.0
            else -> 6.3
        }
    }

    /**
     * Steps taken during the walk from the raw cumulative sensor counter:
     * baseline captured when the walk started, snapshot taken when it finished.
     * Null when either side is missing or the counter reset mid-walk (never negative,
     * never guessed from GPS).
     */
    fun walkSteps(
        baselineSensorCount: Long?,
        currentSensorCount: Long?,
        finalWalkStepCount: Long?,
    ): Long? {
        if (finalWalkStepCount != null) return finalWalkStepCount.coerceAtLeast(0L)
        val baseline = baselineSensorCount ?: return null
        val current = currentSensorCount ?: return null
        if (current < baseline) return null
        return (current - baseline).coerceAtLeast(0L)
    }

    private fun splitByGap(points: List<WalkingLocationPoint>): List<List<WalkingLocationPoint>> {
        if (points.isEmpty()) return emptyList()
        val chunks = mutableListOf(mutableListOf(points.first()))
        for (index in 1 until points.size) {
            val gap = points[index].timestampMillis - points[index - 1].timestampMillis
            if (gap > MAX_POINT_GAP_MILLIS || gap < 0L) {
                chunks.add(mutableListOf())
            }
            chunks.last().add(points[index])
        }
        return chunks
    }

    private fun pathLengthMeters(points: List<WalkingLocationPoint>): Double {
        var total = 0.0
        for (index in 1 until points.size) {
            total += flatDistanceMeters(points[index - 1], points[index])
        }
        return total
    }

    private fun flatDistanceMeters(from: WalkingLocationPoint, to: WalkingLocationPoint): Double {
        val meanLatitude = Math.toRadians((from.latitude + to.latitude) / 2.0)
        val east = Math.toRadians(to.longitude - from.longitude) *
            kotlin.math.cos(meanLatitude) * EARTH_RADIUS_METERS
        val north = Math.toRadians(to.latitude - from.latitude) * EARTH_RADIUS_METERS
        return kotlin.math.sqrt(east * east + north * north)
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
