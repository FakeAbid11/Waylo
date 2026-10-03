package com.waylo.app.core.common

import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {

    private fun context(
        walks: Int = 0,
        distanceMeters: Long = 0L,
        xp: Long = 0L,
        level: Int = 1,
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        steps: Long = 0L,
        activeSeconds: Long = 0L,
    ) = AchievementContext(
        completedWalkCount = walks,
        totalDistanceMeters = distanceMeters,
        totalXp = xp,
        level = level,
        currentStreakDays = currentStreak,
        longestStreakDays = longestStreak,
        totalSteps = steps,
        totalActiveSeconds = activeSeconds,
    )

    private fun unlocked(context: AchievementContext): Set<String> =
        AchievementEvaluator.evaluate(context, AchievementCatalog.definitions).toSet()

    private fun definition(
        id: String = "synthetic",
        requirement: AchievementRequirement,
    ) = AchievementDefinition(
        id = id,
        title = "Synthetic",
        description = "Synthetic requirement.",
        category = com.waylo.app.domain.model.AchievementCategory.WALKS,
        requirement = requirement,
    )

    // First walk ---------------------------------------------------------------

    @Test
    fun zeroWalksLeavesFirstWalkLocked() {
        assertFalse("first_walk" in unlocked(context(walks = 0)))
    }

    @Test
    fun oneWalkUnlocksFirstWalk() {
        assertTrue("first_walk" in unlocked(context(walks = 1)))
    }

    @Test
    fun fiveWalksStillUnlocksFirstWalk() {
        assertTrue("first_walk" in unlocked(context(walks = 5)))
    }

    // Distance -----------------------------------------------------------------

    @Test
    fun distanceBelowAKilometerStaysLocked() {
        assertFalse("distance_1km" in unlocked(context(distanceMeters = 999L)))
    }

    @Test
    fun distanceExactlyAKilometerUnlocks() {
        assertTrue("distance_1km" in unlocked(context(distanceMeters = 1_000L)))
    }

    @Test
    fun distancePastTheMilestoneUnlocksEvenWithoutLandingExactlyOnIt() {
        assertTrue("distance_1km" in unlocked(context(distanceMeters = 1_240L)))
    }

    @Test
    fun tenKilometerMilestoneComparesWholeMeters() {
        assertFalse("distance_10km" in unlocked(context(distanceMeters = 9_999L)))
        assertTrue("distance_10km" in unlocked(context(distanceMeters = 10_000L)))
        assertTrue("distance_10km" in unlocked(context(distanceMeters = 10_420L)))
    }

    // Walk count ---------------------------------------------------------------

    @Test
    fun fourWalksIsLockedAndFiveWalksUnlocksForAFiveWalkRequirement() {
        val requirement = AchievementRequirement.CompletedWalks(count = 5)
        val fiveWalks = definition(id = "walks_5", requirement = requirement)

        assertFalse(AchievementEvaluator.isSatisfied(fiveWalks, context(walks = 4)))
        assertTrue(AchievementEvaluator.isSatisfied(fiveWalks, context(walks = 5)))
    }

    @Test
    fun catalogWalkCountMilestonesUseTheSameThresholdRule() {
        assertFalse("walks_10" in unlocked(context(walks = 9)))
        assertTrue("walks_10" in unlocked(context(walks = 10)))
        assertTrue("walks_50" in unlocked(context(walks = 60)))
        assertTrue("walks_100" in unlocked(context(walks = 100)))
    }

    // XP -----------------------------------------------------------------------

    @Test
    fun totalXpBelowTheMilestoneStaysLocked() {
        assertFalse("xp_1000" in unlocked(context(xp = 999L)))
    }

    @Test
    fun totalXpAtTheMilestoneUnlocks() {
        assertTrue("xp_1000" in unlocked(context(xp = 1_000L)))
        assertTrue("xp_5000" in unlocked(context(xp = 5_000L)))
        assertFalse("xp_5000" in unlocked(context(xp = 4_999L)))
    }

    // Level --------------------------------------------------------------------

    @Test
    fun levelBelowTheMilestoneStaysLocked() {
        assertFalse("level_5" in unlocked(context(level = 4)))
    }

    @Test
    fun reachingTheMilestoneLevelUnlocks() {
        assertTrue("level_5" in unlocked(context(level = 5)))
        assertTrue("level_10" in unlocked(context(level = 10)))
        assertFalse("level_10" in unlocked(context(level = 9)))
    }

    // Streak -------------------------------------------------------------------

    @Test
    fun streakBelowTheMilestoneStaysLocked() {
        assertFalse("streak_3" in unlocked(context(longestStreak = 2)))
    }

    @Test
    fun streakAtTheMilestoneUnlocks() {
        assertTrue("streak_3" in unlocked(context(longestStreak = 3)))
        assertTrue("streak_7" in unlocked(context(longestStreak = 8, currentStreak = 1)))
        assertFalse("streak_7" in unlocked(context(longestStreak = 6, currentStreak = 6)))
    }

    // Steps --------------------------------------------------------------------

    @Test
    fun stepsBelowTheMilestoneStayLocked() {
        assertFalse("steps_10000" in unlocked(context(steps = 9_999L)))
    }

    @Test
    fun stepsAtTheMilestoneUnlock() {
        assertTrue("steps_10000" in unlocked(context(steps = 10_000L)))
        assertTrue("steps_1000" in unlocked(context(steps = 1_000L)))
        assertFalse("steps_1000" in unlocked(context(steps = 999L)))
    }

    // Active duration requirement ----------------------------------------------

    @Test
    fun activeDurationComparesWholeSeconds() {
        val requirement = AchievementRequirement.ActiveDurationSeconds(seconds = 600L)
        val achievement = definition(id = "time_10min", requirement = requirement)

        assertFalse(AchievementEvaluator.isSatisfied(achievement, context(activeSeconds = 599L)))
        assertTrue(AchievementEvaluator.isSatisfied(achievement, context(activeSeconds = 600L)))
        assertTrue(AchievementEvaluator.isSatisfied(achievement, context(activeSeconds = 661L)))
    }

    // Multiple unlocks ---------------------------------------------------------

    @Test
    fun oneContextCanUnlockSeveralAchievementsAtOnce() {
        val ids = unlocked(
            context(walks = 1, distanceMeters = 11_200L, steps = 12_000L, xp = 1_120L),
        )

        assertTrue("first_walk" in ids)
        assertTrue("distance_1km" in ids)
        assertTrue("distance_5km" in ids)
        assertTrue("distance_10km" in ids)
        assertTrue("steps_1000" in ids)
        assertTrue("steps_10000" in ids)
        assertTrue("xp_1000" in ids)
        assertFalse("walks_10" in ids)
        assertFalse("xp_5000" in ids)
    }

    @Test
    fun aFreshProfileUnlocksNothing() {
        assertTrue(unlocked(context()).isEmpty())
    }

    // Determinism --------------------------------------------------------------

    @Test
    fun theSameContextAlwaysProducesTheSameResult() {
        val value = context(walks = 12, distanceMeters = 13_500L, steps = 15_000L, xp = 1_500L)
        val first = AchievementEvaluator.evaluate(value, AchievementCatalog.definitions)
        repeat(25) {
            assertEquals(
                first,
                AchievementEvaluator.evaluate(value, AchievementCatalog.definitions),
            )
        }
    }

    @Test
    fun resultsFollowDefinitionOrderForStablePresentation() {
        val ids = AchievementEvaluator.evaluate(
            context(walks = 120, distanceMeters = 200_000L, steps = 1_500_000L, xp = 6_000L),
            AchievementCatalog.definitions,
        )
        assertEquals(AchievementCatalog.definitions.map { it.id }.filter { it in ids.toSet() }, ids)
        assertEquals(ids.distinct(), ids)
    }

    // Progress helpers ---------------------------------------------------------

    @Test
    fun progressFractionIsClampedToTheUnitInterval() {
        assertEquals(
            0.124,
            AchievementEvaluator.progressFraction(
                AchievementRequirement.DistanceMeters(meters = 10_000L),
                context(distanceMeters = 1_240L),
            ).toDouble(),
            0.0001,
        )
        assertEquals(
            1.0,
            AchievementEvaluator.progressFraction(
                AchievementRequirement.DistanceMeters(meters = 1_000L),
                context(distanceMeters = 5_000L),
            ).toDouble(),
            0.0,
        )
        assertEquals(
            0.0,
            AchievementEvaluator.progressFraction(
                AchievementRequirement.DistanceMeters(meters = 1_000L),
                context(),
            ).toDouble(),
            0.0,
        )
    }

    @Test
    fun currentAndRequiredValuesAreWholeNumbers() {
        val requirement = AchievementRequirement.Steps(steps = 10_000L)
        assertEquals(10_000L, AchievementEvaluator.requiredValue(requirement))
        assertEquals(2_500L, AchievementEvaluator.currentValue(requirement, context(steps = 2_500L)))
        assertEquals(
            3L,
            AchievementEvaluator.requiredValue(AchievementRequirement.CompletedWalks(count = 3)),
        )
        assertEquals(
            7L,
            AchievementEvaluator.currentValue(
                AchievementRequirement.StreakDays(days = 7),
                context(longestStreak = 7),
            ),
        )
    }
}
