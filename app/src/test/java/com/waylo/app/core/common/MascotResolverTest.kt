package com.waylo.app.core.common

import com.waylo.app.domain.model.UserProgress
import com.waylo.app.domain.model.WalkingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotResolverTest {

    @Test
    fun idleWhenNoSignalsArePresent() {
        val decision = MascotResolver.resolve(MascotContext())
        assertEquals(MascotState.Idle, decision.state)
        assertEquals(MascotReaction.None, decision.reaction)
    }

    @Test
    fun walkingWhenWalkIsActivelyTracking() {
        val decision = MascotResolver.resolve(MascotContext(walkState = WalkingState.Active))
        assertEquals(MascotState.Walking, decision.state)
        assertEquals(MascotReaction.Walking, decision.reaction)
    }

    @Test
    fun walkingWhileStoppingStillCountsAsWalking() {
        val decision = MascotResolver.resolve(MascotContext(walkState = WalkingState.Stopping))
        assertEquals(MascotState.Walking, decision.state)
        assertEquals(MascotReaction.Walking, decision.reaction)
    }

    @Test
    fun pausedOverridesTheWalkingTier() {
        val decision = MascotResolver.resolve(MascotContext(walkState = WalkingState.Paused))
        assertEquals(MascotState.Paused, decision.state)
        assertEquals(MascotReaction.WalkPaused, decision.reaction)
    }

    @Test
    fun startingWalkIsEncouragingWithWalkStartedReaction() {
        val decision = MascotResolver.resolve(MascotContext(walkState = WalkingState.Starting))
        assertEquals(MascotState.Encouraging, decision.state)
        assertEquals(MascotReaction.WalkStarted, decision.reaction)
    }

    @Test
    fun walkErrorIsEncouragingWithWalkInterruptedReaction() {
        val decision = MascotResolver.resolve(MascotContext(walkState = WalkingState.Error))
        assertEquals(MascotState.Encouraging, decision.state)
        assertEquals(MascotReaction.WalkInterrupted, decision.reaction)
    }

    @Test
    fun justCompletedCelebratesTheFinishedWalk() {
        val decision = MascotResolver.resolve(MascotContext(justCompleted = true))
        assertEquals(MascotState.Celebrating, decision.state)
        assertEquals(MascotReaction.WalkCompleted, decision.reaction)
    }

    @Test
    fun celebrationOutranksTheWalkTierSoCompletionReachesHistoryScreens() {
        val decision = MascotResolver.resolve(
            MascotContext(walkState = WalkingState.Active, justCompleted = true),
        )
        assertEquals(MascotState.Celebrating, decision.state)
        assertEquals(MascotReaction.WalkCompleted, decision.reaction)
    }

    @Test
    fun levelUpOverridesEveryOtherSignal() {
        val decision = MascotResolver.resolve(
            MascotContext(
                levelUp = true,
                xpAwarded = 150,
                justCompleted = true,
                walkState = WalkingState.Active,
                streakDays = 5,
                hasWalkHistory = true,
            ),
        )
        assertEquals(MascotState.LevelUp, decision.state)
        assertEquals(MascotReaction.LevelUp, decision.reaction)
    }

    @Test
    fun achievementUnlockCelebratesBetweenLevelUpAndXp() {
        val decision = MascotResolver.resolve(
            MascotContext(
                achievementCount = 1,
                xpAwarded = 150,
                justCompleted = true,
                walkState = WalkingState.Active,
            ),
        )
        assertEquals(MascotState.Celebrating, decision.state)
        assertEquals(MascotReaction.AchievementUnlocked, decision.reaction)
    }

    @Test
    fun levelUpStillOutranksAnAchievementUnlock() {
        val decision = MascotResolver.resolve(
            MascotContext(levelUp = true, achievementCount = 3, xpAwarded = 300),
        )
        assertEquals(MascotState.LevelUp, decision.state)
        assertEquals(MascotReaction.LevelUp, decision.reaction)
    }

    @Test
    fun zeroAchievementsNeverTriggersTheAchievementReaction() {
        val decision = MascotResolver.resolve(MascotContext(achievementCount = 0, xpAwarded = 50))
        assertEquals(MascotState.XpEarned, decision.state)
        assertEquals(MascotReaction.XpEarned, decision.reaction)
        assertFalse(MascotContext().achievementUnlocked)
    }

    @Test
    fun xpAwardOutranksCelebrationAndTheWalkTier() {
        val decision = MascotResolver.resolve(
            MascotContext(
                xpAwarded = 150,
                justCompleted = true,
                walkState = WalkingState.Active,
            ),
        )
        assertEquals(MascotState.XpEarned, decision.state)
        assertEquals(MascotReaction.XpEarned, decision.reaction)
    }

    @Test
    fun streakMilestoneBeatsPassiveEncouragement() {
        val decision = MascotResolver.resolve(
            MascotContext(streakDays = 5, hasWalkHistory = true),
        )
        assertEquals(MascotState.Streak, decision.state)
        assertEquals(MascotReaction.StreakMilestone, decision.reaction)
    }

    @Test
    fun walkTierOutranksTheStreakState() {
        val decision = MascotResolver.resolve(
            MascotContext(walkState = WalkingState.Active, streakDays = 5, hasWalkHistory = true),
        )
        assertEquals(MascotState.Walking, decision.state)
        assertEquals(MascotReaction.Walking, decision.reaction)
    }

    @Test
    fun encouragingWhenTheUserHasWalkHistoryButNoStreak() {
        val decision = MascotResolver.resolve(MascotContext(hasWalkHistory = true))
        assertEquals(MascotState.Encouraging, decision.state)
        assertEquals(MascotReaction.Ready, decision.reaction)
    }

    @Test
    fun restingWhenThereIsNoActivityAtAll() {
        val decision = MascotResolver.resolve(MascotContext(hasNoActivity = true))
        assertEquals(MascotState.Resting, decision.state)
        assertEquals(MascotReaction.NoRecentActivity, decision.reaction)
    }

    @Test
    fun resolutionIsDeterministicForIdenticalContexts() {
        val context = MascotContext(walkState = WalkingState.Paused, streakDays = 2)
        val first = MascotResolver.resolve(context)
        repeat(20) {
            assertEquals(first, MascotResolver.resolve(context))
        }
    }

    @Test
    fun fromProgressDerivesHistoryFromTotals() {
        val progress = UserProgress.empty().copy(totalDistanceKm = 1.5)
        val context = MascotContext.fromProgress(progress)
        assertTrue(context.hasWalkHistory)
        assertFalse(context.hasNoActivity)
        assertEquals(0, context.streakDays)
    }

    @Test
    fun fromProgressMarksAFreshInstallAsHavingNoActivity() {
        val context = MascotContext.fromProgress(UserProgress.empty())
        assertTrue(context.hasNoActivity)
        assertFalse(context.hasWalkHistory)
    }

    @Test
    fun fromProgressForwardsStreakAndWalkState() {
        val progress = UserProgress.empty().copy(currentStreakDays = 3)
        val context = MascotContext.fromProgress(progress, walkState = WalkingState.Active)
        assertEquals(3, context.streakDays)
        assertEquals(WalkingState.Active, context.walkState)
        val decision = MascotResolver.resolve(context)
        assertEquals(MascotState.Walking, decision.state)
    }

    @Test
    fun fromProgressPassiveContextResolvesToStreakForAStreakedUser() {
        val progress = UserProgress.empty().copy(totalXp = 400, currentStreakDays = 2)
        val decision = MascotResolver.resolve(MascotContext.fromProgress(progress))
        assertEquals(MascotState.Streak, decision.state)
        assertEquals(MascotReaction.StreakMilestone, decision.reaction)
    }
}
