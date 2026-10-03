package com.waylo.app.core.common

import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class WorkoutStatisticsCalculatorTest {

    @Test
    fun averagePaceAndSpeedUseAcceptedDistanceOverActiveDuration() {
        val pace = WorkoutStatisticsCalculator.averagePaceSecondsPerKm(1_000.0, 300_000L)
        val speed = WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(1_000.0, 300_000L)

        assertEquals(300.0, pace!!, 0.001)
        assertEquals(1_000.0 / 300.0, speed!!, 0.001)
    }

    @Test
    fun averagesNeedAtLeastTenMetersOfDistance() {
        assertNull(WorkoutStatisticsCalculator.averagePaceSecondsPerKm(9.9, 60_000L))
        assertNull(WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(9.9, 60_000L))
        assertNotNull(WorkoutStatisticsCalculator.averagePaceSecondsPerKm(10.0, 60_000L))
        assertNotNull(WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(10.0, 60_000L))
    }

    @Test
    fun averagesNeedPositiveDuration() {
        assertNull(WorkoutStatisticsCalculator.averagePaceSecondsPerKm(10_000.0, 0L))
        assertNull(WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(10_000.0, 0L))
        assertNull(WorkoutStatisticsCalculator.averagePaceSecondsPerKm(10_000.0, -5_000L))
        assertNull(WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(10_000.0, -5_000L))
    }

    @Test
    fun averagesRejectImpossibleValues() {
        assertNull(WorkoutStatisticsCalculator.averagePaceSecondsPerKm(Double.NaN, 60_000L))
        assertNull(WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(Double.NaN, 60_000L))
        assertNull(
            WorkoutStatisticsCalculator.averageSpeedMetersPerSecond(1_000_000_000.0, 1_000L),
        )
        assertNull(
            WorkoutStatisticsCalculator.averagePaceSecondsPerKm(1_000_000_000.0, 1_000L),
        )
        assertNull(
            WorkoutStatisticsCalculator.averagePaceSecondsPerKm(Double.POSITIVE_INFINITY, 1L),
        )
    }

    @Test
    fun currentMovementUsesOnlyRecentAcceptedPoints() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 20.0, 40.0, 60.0, 80.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )

        val movement = WorkoutStatisticsCalculator.currentMovement(route, nowMillis = T0 + 40_000L)

        assertNotNull(movement)
        assertEquals(80.0, movement!!.distanceMeters, 0.001)
        assertEquals(40_000L, movement.durationMillis)
        assertEquals(2.0, movement.speedMetersPerSecond, 0.001)
        assertEquals(500.0, movement.paceSecondsPerKm, 0.001)
    }

    @Test
    fun currentMovementNeedsAtLeastTwoPoints() {
        val single = WalkRoute(
            points = listOf(point(metersNorth = 0.0, timestampMillis = T0)),
        )

        assertNull(WorkoutStatisticsCalculator.currentMovement(single, nowMillis = T0))
        assertNull(
            WorkoutStatisticsCalculator.currentMovement(WalkRoute(), nowMillis = T0),
        )
    }

    @Test
    fun stalePointsOutsideTheWindowProduceNoCurrentStats() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 30.0, 60.0, 90.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )

        val movement = WorkoutStatisticsCalculator.currentMovement(
            route,
            nowMillis = T0 + 30_000L + WorkoutStatisticsCalculator.CURRENT_WINDOW_MILLIS + 1_000L,
        )

        assertNull(movement)
    }

    @Test
    fun tinyRecentMovementIsIgnoredAsNoise() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 1.0, 2.0, 3.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )

        val movement = WorkoutStatisticsCalculator.currentMovement(route, nowMillis = T0 + 30_000L)

        assertNull(movement)
    }

    @Test
    fun pauseBreakExcludesPrePausePointsFromTheWindow() {
        val prePause = routeAlongNorth(
            segmentsMeters = listOf(0.0, 200.0, 400.0, 600.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )
        val resumedPoint = point(metersNorth = 610.0, timestampMillis = T0 + 40_000L)
        val route = prePause.markBreakBeforeNextPoint().withPoint(resumedPoint)

        val movement = WorkoutStatisticsCalculator.currentMovement(
            route,
            nowMillis = T0 + 40_000L,
        )

        assertNull(movement)
    }

    @Test
    fun resumedSegmentOnlyUsesPointsAfterTheBreak() {
        val prePause = routeAlongNorth(
            segmentsMeters = listOf(0.0, 200.0, 400.0),
            startMillis = T0 - 30_000L,
            stepMillis = 10_000L,
        )
        val postPause = listOf(
            point(metersNorth = 420.0, timestampMillis = T0 + 10_000L),
            point(metersNorth = 440.0, timestampMillis = T0 + 20_000L),
            point(metersNorth = 460.0, timestampMillis = T0 + 30_000L),
        )
        val route = WalkRoute(
            points = prePause.points + postPause,
            breakBeforeIndexes = setOf(3),
        )

        val movement = WorkoutStatisticsCalculator.currentMovement(
            route,
            nowMillis = T0 + 30_000L,
        )

        assertNotNull(movement)
        assertEquals(40.0, movement!!.distanceMeters, 0.001)
        assertEquals(20_000L, movement.durationMillis)
        assertEquals(2.0, movement.speedMetersPerSecond, 0.001)
        assertEquals(500.0, movement.paceSecondsPerKm, 0.001)
    }

    @Test
    fun aLongGapSplitsTheWindowAndOnlyTheLatestChunkCounts() {
        val olderChunk = routeAlongNorth(
            segmentsMeters = listOf(0.0, 50.0, 100.0),
            startMillis = T0 - 14_000L,
            stepMillis = 2_000L,
        )
        val recentChunk = routeAlongNorth(
            segmentsMeters = listOf(100.0, 120.0, 140.0),
            startMillis = T0 + 10_000L,
            stepMillis = 10_000L,
        )
        val route = WalkRoute(points = olderChunk.points + recentChunk.points)

        val movement = WorkoutStatisticsCalculator.currentMovement(
            route,
            nowMillis = T0 + 30_000L,
        )

        assertNotNull(movement)
        assertEquals(40.0, movement!!.distanceMeters, 0.001)
        assertEquals(20_000L, movement.durationMillis)
    }

    @Test
    fun currentPaceAndSpeedAlwaysDescribeTheSameWindow() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 30.0, 60.0, 90.0, 120.0),
            startMillis = T0,
            stepMillis = 8_000L,
        )

        val movement = WorkoutStatisticsCalculator.currentMovement(route, nowMillis = T0 + 32_000L)

        assertNotNull(movement)
        assertEquals(
            1_000.0,
            movement!!.paceSecondsPerKm * movement.speedMetersPerSecond,
            0.001,
        )
    }

    @Test
    fun caloriesNeedAValidWeightAndStayUnavailableWithoutOne() {
        assertNull(
            WorkoutStatisticsCalculator.estimatedCaloriesKcal(
                distanceMeters = 1_000.0,
                activeMillis = 600_000L,
                weightKg = null,
            ),
        )
        assertNull(
            WorkoutStatisticsCalculator.estimatedCaloriesKcal(1_000.0, 600_000L, weightKg = 10.0),
        )
        assertNull(
            WorkoutStatisticsCalculator.estimatedCaloriesKcal(1_000.0, 600_000L, weightKg = 400.0),
        )
        assertNull(
            WorkoutStatisticsCalculator.estimatedCaloriesKcal(
                1_000.0,
                600_000L,
                weightKg = Double.NaN,
            ),
        )
    }

    @Test
    fun zeroDurationYieldsZeroCalories() {
        val calories = WorkoutStatisticsCalculator.estimatedCaloriesKcal(
            distanceMeters = 0.0,
            activeMillis = 0L,
            weightKg = 70.0,
        )

        assertEquals(0.0, calories!!, 0.0001)
    }

    @Test
    fun caloriesUseDocumentedMetForAverageSpeed() {
        val calories = WorkoutStatisticsCalculator.estimatedCaloriesKcal(
            distanceMeters = 1_000.0,
            activeMillis = 600_000L,
            weightKg = 70.0,
        )

        val expected = 5.0 * 70.0 * (600_000.0 / 3_600_000.0)
        assertEquals(expected, calories!!, 0.01)
    }

    @Test
    fun caloriesNeverGoNegative() {
        val calories = WorkoutStatisticsCalculator.estimatedCaloriesKcal(
            distanceMeters = -100.0,
            activeMillis = 60_000L,
            weightKg = 70.0,
        )

        assertNotNull(calories)
        assertTrue(calories!! >= 0.0)
    }

    @Test
    fun metBinsFollowTheDocumentedWalkingSpeeds() {
        assertEquals(3.5, WorkoutStatisticsCalculator.metForSpeed(null), 0.0)
        assertEquals(3.5, WorkoutStatisticsCalculator.metForSpeed(Double.NaN), 0.0)
        assertEquals(2.8, WorkoutStatisticsCalculator.metForSpeed(0.5), 0.0)
        assertEquals(3.0, WorkoutStatisticsCalculator.metForSpeed(1.0), 0.0)
        assertEquals(3.5, WorkoutStatisticsCalculator.metForSpeed(1.3), 0.0)
        assertEquals(4.3, WorkoutStatisticsCalculator.metForSpeed(1.5), 0.0)
        assertEquals(5.0, WorkoutStatisticsCalculator.metForSpeed(1.7), 0.0)
        assertEquals(6.3, WorkoutStatisticsCalculator.metForSpeed(2.0), 0.0)
        assertEquals(2.8, WorkoutStatisticsCalculator.metForSpeed(3.19 / 3.6), 0.0)
        assertEquals(3.0, WorkoutStatisticsCalculator.metForSpeed(3.21 / 3.6), 0.0)
        assertEquals(5.0, WorkoutStatisticsCalculator.metForSpeed(6.39 / 3.6), 0.0)
        assertEquals(6.3, WorkoutStatisticsCalculator.metForSpeed(6.41 / 3.6), 0.0)
    }

    @Test
    fun walkStepsPreferThePersistedFinalSnapshot() {
        val steps = WorkoutStatisticsCalculator.walkSteps(
            baselineSensorCount = 100L,
            currentSensorCount = 999L,
            finalWalkStepCount = 2_800L,
        )

        assertEquals(2_800L, steps)
    }

    @Test
    fun walkStepsUseTheLiveSensorDeltaWhileWalking() {
        val steps = WorkoutStatisticsCalculator.walkSteps(
            baselineSensorCount = 1_000L,
            currentSensorCount = 3_400L,
            finalWalkStepCount = null,
        )

        assertEquals(2_400L, steps)
    }

    @Test
    fun walkStepsNeverGoNegativeWhenTheCounterResets() {
        assertNull(
            WorkoutStatisticsCalculator.walkSteps(
                baselineSensorCount = 5_000L,
                currentSensorCount = 300L,
                finalWalkStepCount = null,
            ),
        )
    }

    @Test
    fun walkStepsAreUnavailableWithoutABaselineOrACounter() {
        assertNull(
            WorkoutStatisticsCalculator.walkSteps(
                baselineSensorCount = null,
                currentSensorCount = 3_400L,
                finalWalkStepCount = null,
            ),
        )
        assertNull(
            WorkoutStatisticsCalculator.walkSteps(
                baselineSensorCount = 1_000L,
                currentSensorCount = null,
                finalWalkStepCount = null,
            ),
        )
    }

    @Test
    fun aRealZeroStepWalkStaysZeroInsteadOfUnavailable() {
        assertEquals(
            0L,
            WorkoutStatisticsCalculator.walkSteps(1_000L, 1_000L, finalWalkStepCount = 0L),
        )
        assertEquals(
            0L,
            WorkoutStatisticsCalculator.walkSteps(1_000L, 1_000L, finalWalkStepCount = null),
        )
    }

    @Test
    fun statisticsClampInvalidDistanceAndDuration() {
        val statistics = WorkoutStatisticsCalculator.statistics(
            distanceMeters = -5.0,
            activeMillis = -1_000L,
            liveMovement = true,
            route = WalkRoute(),
            nowMillis = T0,
            weightKg = 70.0,
            walkStartStepCount = null,
            currentSensorCount = null,
            finalWalkStepCount = null,
        )

        assertEquals(0.0, statistics.distanceMeters, 0.0)
        assertEquals(0L, statistics.activeMillis)
        assertNull(statistics.averagePaceSecondsPerKm)
        assertEquals(0.0, statistics.estimatedCaloriesKcal!!, 0.0001)
    }

    @Test
    fun nonFiniteDistanceFallsBackToZero() {
        val statistics = WorkoutStatisticsCalculator.statistics(
            distanceMeters = Double.NaN,
            activeMillis = 60_000L,
            liveMovement = false,
            route = WalkRoute(),
            nowMillis = T0,
            weightKg = null,
            walkStartStepCount = null,
            currentSensorCount = null,
            finalWalkStepCount = null,
        )

        assertEquals(0.0, statistics.distanceMeters, 0.0)
    }

    @Test
    fun pausedOrInactiveWalksKeepAveragesButLoseCurrentMovement() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 30.0, 60.0, 90.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )

        val paused = WorkoutStatisticsCalculator.statistics(
            distanceMeters = 1_000.0,
            activeMillis = 300_000L,
            liveMovement = false,
            route = route,
            nowMillis = T0 + 30_000L,
            weightKg = 70.0,
            walkStartStepCount = null,
            currentSensorCount = null,
            finalWalkStepCount = null,
        )

        assertEquals(300.0, paused.averagePaceSecondsPerKm!!, 0.001)
        assertEquals(1_000.0 / 300.0, paused.averageSpeedMetersPerSecond!!, 0.001)
        assertNull(paused.currentPaceSecondsPerKm)
        assertNull(paused.currentSpeedMetersPerSecond)
        assertNotNull(paused.estimatedCaloriesKcal)
    }

    @Test
    fun liveWalksExposeCurrentAndWalkStepsTogether() {
        val route = routeAlongNorth(
            segmentsMeters = listOf(0.0, 30.0, 60.0, 90.0),
            startMillis = T0,
            stepMillis = 10_000L,
        )

        val live = WorkoutStatisticsCalculator.statistics(
            distanceMeters = 90.0,
            activeMillis = 30_000L,
            liveMovement = true,
            route = route,
            nowMillis = T0 + 30_000L,
            weightKg = 75.0,
            walkStartStepCount = 1_000L,
            currentSensorCount = 1_260L,
            finalWalkStepCount = null,
        )

        assertNotNull(live.currentPaceSecondsPerKm)
        assertNotNull(live.currentSpeedMetersPerSecond)
        assertEquals(260L, live.walkSteps)
        assertTrue(live.estimatedCaloriesKcal!! >= 0.0)
    }

    private fun routeAlongNorth(
        segmentsMeters: List<Double>,
        startMillis: Long,
        stepMillis: Long,
    ): WalkRoute = WalkRoute(
        points = segmentsMeters.mapIndexed { index, meters ->
            point(metersNorth = meters, timestampMillis = startMillis + index * stepMillis)
        },
    )

    private fun point(metersNorth: Double, timestampMillis: Long): WalkingLocationPoint =
        WalkingLocationPoint(
            sessionId = 1L,
            sequence = 0,
            latitude = 52.0 + metersNorth / METERS_PER_DEGREE,
            longitude = 13.0,
            timestampMillis = timestampMillis,
        )

    private companion object {
        const val T0 = 1_700_000_000_000L
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val METERS_PER_DEGREE = EARTH_RADIUS_METERS * PI / 180.0
    }
}
