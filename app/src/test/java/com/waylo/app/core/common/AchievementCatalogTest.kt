package com.waylo.app.core.common

import com.waylo.app.domain.model.AchievementCategory
import com.waylo.app.domain.model.AchievementRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementCatalogTest {

    private val definitions = AchievementCatalog.definitions

    @Test
    fun idsAreStableSnakeCaseIdentifiers() {
        val pattern = Regex("[a-z][a-z0-9_]*")
        definitions.forEach { definition ->
            assertTrue(
                "Id \"${definition.id}\" must be stable snake_case",
                pattern.matches(definition.id),
            )
        }
    }

    @Test
    fun idsAreUnique() {
        assertEquals(definitions.size, definitions.map { it.id }.toSet().size)
    }

    @Test
    fun titlesAreUniqueAndUserFacing() {
        assertEquals(definitions.size, definitions.map { it.title }.toSet().size)
        definitions.forEach { definition ->
            assertTrue(definition.title.isNotBlank())
            assertFalse(
                "Title must not leak the id: ${definition.title}",
                definition.title == definition.id,
            )
        }
    }

    @Test
    fun descriptionsArePresent() {
        definitions.forEach { definition ->
            assertTrue(definition.description.isNotBlank())
        }
    }

    @Test
    fun targetsAreAlwaysPositiveMilestones() {
        definitions.forEach { definition ->
            val target = AchievementEvaluator.requiredValue(definition.requirement)
            assertTrue(
                "Requirement for ${definition.id} must be > 0, was $target",
                target > 0L,
            )
        }
    }

    @Test
    fun noAchievementGrantsXpSoRewardsCannotInflateTheLedger() {
        definitions.forEach { definition ->
            assertEquals(
                "${definition.id} must be recognition-only",
                0L,
                definition.reward.xpBonus,
            )
        }
    }

    @Test
    fun everyDefinitionUsesAKnownCategory() {
        val known = setOf(
            AchievementCategory.WALKS,
            AchievementCategory.DISTANCE,
            AchievementCategory.STEPS,
            AchievementCategory.STREAK,
            AchievementCategory.XP,
            AchievementCategory.LEVEL,
        )
        definitions.forEach { definition ->
            assertTrue("${definition.id} has unknown category", definition.category in known)
        }
    }

    @Test
    fun milestoneThresholdsAreStrictlyAscendingWithinEachRequirementFamily() {
        fun meters(id: String): Long =
            (AchievementCatalog.definition(id)!!.requirement as AchievementRequirement.DistanceMeters).meters

        assertTrue(meters("distance_1km") < meters("distance_5km"))
        assertTrue(meters("distance_5km") < meters("distance_10km"))
        assertTrue(meters("distance_10km") < meters("distance_50km"))
        assertTrue(meters("distance_50km") < meters("distance_100km"))
    }

    @Test
    fun lookupByIdReturnsTheExpectedDefinition() {
        assertEquals("First Walk", AchievementCatalog.titleFor("first_walk"))
        assertEquals(null, AchievementCatalog.definition("does_not_exist"))
    }

    @Test
    fun theCatalogCoversThePhaseTenScope() {
        // PRD catalog: first walk + walk counts, distance, steps and streaks,
        // plus XP and level milestones from Phase 8 progression.
        assertTrue(definitions.any { it.id == "first_walk" })
        assertTrue(definitions.count { it.category == AchievementCategory.DISTANCE } == 5)
        assertTrue(definitions.count { it.category == AchievementCategory.STEPS } == 5)
        assertTrue(definitions.count { it.category == AchievementCategory.STREAK } == 5)
        assertTrue(definitions.count { it.category == AchievementCategory.WALKS } == 4)
        assertTrue(definitions.count { it.category == AchievementCategory.XP } == 2)
        assertTrue(definitions.count { it.category == AchievementCategory.LEVEL } == 2)
        assertEquals(23, definitions.size)
    }
}
