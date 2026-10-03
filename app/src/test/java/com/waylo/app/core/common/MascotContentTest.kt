package com.waylo.app.core.common

import com.waylo.app.domain.model.WalkingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotContentTest {

    @Test
    fun idleMessageInvitesAdventureWithoutPressure() {
        val context = MascotContext()
        val decision = MascotResolver.resolve(context)
        assertEquals("Make every walk an adventure.", MascotContent.message(decision, context))
    }

    @Test
    fun readyMessageWhenWaitingForTheUserToStart() {
        val context = MascotContext(hasWalkHistory = true)
        val decision = MascotResolver.resolve(context)
        assertEquals("Ready when you are!", MascotContent.message(decision, context))
    }

    @Test
    fun walkStartedUsesLetsGo() {
        val context = MascotContext(walkState = WalkingState.Starting)
        val decision = MascotResolver.resolve(context)
        assertEquals("Let's go!", MascotContent.message(decision, context))
    }

    @Test
    fun walkingMessageAffirmsMovement() {
        val context = MascotContext(walkState = WalkingState.Active)
        val decision = MascotResolver.resolve(context)
        assertEquals("You're moving!", MascotContent.message(decision, context))
    }

    @Test
    fun pausedMessageGivesPermissionToRest() {
        val context = MascotContext(walkState = WalkingState.Paused)
        val decision = MascotResolver.resolve(context)
        assertEquals("Take your time.", MascotContent.message(decision, context))
    }

    @Test
    fun celebratingMessageAnnouncesCompletion() {
        val context = MascotContext(justCompleted = true)
        val decision = MascotResolver.resolve(context)
        assertEquals("Walk complete!", MascotContent.message(decision, context))
    }

    @Test
    fun achievementMessageCountsTheUnlocks() {
        val single = MascotContext(achievementCount = 1)
        assertEquals(
            "Achievement unlocked!",
            MascotContent.message(MascotResolver.resolve(single), single),
        )
        val multiple = MascotContext(achievementCount = 3)
        assertEquals(
            "3 achievements unlocked!",
            MascotContent.message(MascotResolver.resolve(multiple), multiple),
        )
    }

    @Test
    fun achievementMessageAvoidsGuiltLanguage() {
        val context = MascotContext(achievementCount = 2, xpAwarded = 120)
        val message = MascotContent.message(MascotResolver.resolve(context), context).lowercase()
        val banned = listOf("fail", "missed", "lazy", "behind", "should", "sorry", "guilt", "bad")
        banned.forEach { word ->
            assertFalse("Message \"$message\" contains banned word \"$word\"", message.contains(word))
        }
    }

    @Test
    fun xpMessageIncludesTheAwardedAmount() {
        val context = MascotContext(xpAwarded = 150)
        val decision = MascotResolver.resolve(context)
        assertEquals("Nice work! +150 XP", MascotContent.message(decision, context))
    }

    @Test
    fun levelUpMessageIsShortAndCelebratory() {
        val context = MascotContext(levelUp = true)
        val decision = MascotResolver.resolve(context)
        assertEquals("Level up!", MascotContent.message(decision, context))
    }

    @Test
    fun streakMessageCoversTheAliveAndCountedThresholds() {
        val dayOne = MascotContext(streakDays = 1, hasWalkHistory = true)
        assertEquals(
            "Your streak is alive!",
            MascotContent.message(MascotResolver.resolve(dayOne), dayOne),
        )
        val dayThree = MascotContext(streakDays = 3, hasWalkHistory = true)
        assertEquals(
            "3 days strong!",
            MascotContent.message(MascotResolver.resolve(dayThree), dayThree),
        )
    }

    @Test
    fun restingMessageLooksForwardWithoutShame() {
        val context = MascotContext(hasNoActivity = true)
        val decision = MascotResolver.resolve(context)
        assertEquals("Your next walk is waiting.", MascotContent.message(decision, context))
    }

    @Test
    fun messagesAreDeterministicAcrossRepeatedCalls() {
        val scenarios = listOf(
            MascotContext() to MascotResolver.resolve(MascotContext()),
            MascotContext(walkState = WalkingState.Active) to
                MascotResolver.resolve(MascotContext(walkState = WalkingState.Active)),
            MascotContext(xpAwarded = 150) to MascotResolver.resolve(MascotContext(xpAwarded = 150)),
            MascotContext(streakDays = 7, hasWalkHistory = true) to
                MascotResolver.resolve(MascotContext(streakDays = 7, hasWalkHistory = true)),
        )
        scenarios.forEach { (context, decision) ->
            val first = MascotContent.message(decision, context)
            repeat(20) {
                assertEquals(first, MascotContent.message(decision, context))
            }
        }
    }

    @Test
    fun noMessageUsesGuiltOrShamingLanguage() {
        val banned = listOf("fail", "missed", "lazy", "behind", "should", "sorry", "guilt", "bad")
        val scenarios = listOf(
            MascotContext() to MascotResolver.resolve(MascotContext()),
            MascotContext(hasNoActivity = true) to MascotResolver.resolve(MascotContext(hasNoActivity = true)),
            MascotContext(walkState = WalkingState.Paused) to
                MascotResolver.resolve(MascotContext(walkState = WalkingState.Paused)),
            MascotContext(streakDays = 1, hasWalkHistory = true) to
                MascotResolver.resolve(MascotContext(streakDays = 1, hasWalkHistory = true)),
            MascotContext(justCompleted = true) to
                MascotResolver.resolve(MascotContext(justCompleted = true)),
        )
        scenarios.forEach { (context, decision) ->
            val message = MascotContent.message(decision, context).lowercase()
            banned.forEach { word ->
                assertFalse(
                    "Message \"$message\" contains banned word \"$word\"",
                    message.contains(word),
                )
            }
        }
    }

    @Test
    fun everyStateHasANonBlankUniqueContextualDescription() {
        val descriptions = MascotState.entries.associateWith { MascotContent.contentDescription(it) }
        descriptions.forEach { (state, description) ->
            assertTrue("Blank description for $state", description.isNotBlank())
            assertTrue(
                "Description for $state must identify Waylo's fox",
                description.contains("Waylo fox"),
            )
        }
        assertEquals(MascotState.entries.size, descriptions.values.toSet().size)
    }

    @Test
    fun descriptionsNeverFallBackToGenericLabels() {
        val banned = setOf("image", "fox", "mascot", "graphic")
        MascotState.entries.forEach { state ->
            val description = MascotContent.contentDescription(state)
            assertNotEquals(description, "Fox")
            assertNotEquals(description, "Image")
            banned.forEach { word ->
                assertFalse(
                    "Description \"$description\" uses generic label \"$word\"",
                    description.equals(word, ignoreCase = true),
                )
            }
        }
    }
}
