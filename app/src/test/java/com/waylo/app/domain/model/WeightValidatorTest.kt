package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightValidatorTest {

    @Test
    fun validWholeNumbersAreAccepted() {
        assertEquals(70, (WeightValidator.validate("70") as WeightValidationResult.Valid).weightKg)
        assertEquals(20, (WeightValidator.validate("20") as WeightValidationResult.Valid).weightKg)
        assertEquals(300, (WeightValidator.validate("300") as WeightValidationResult.Valid).weightKg)
        assertEquals(72, (WeightValidator.validate("  72  ") as WeightValidationResult.Valid).weightKg)
    }

    @Test
    fun emptyInputAsksForAWeight() {
        val result = WeightValidator.validate("")

        assertTrue(result is WeightValidationResult.Invalid)
        assertEquals(
            "Enter your weight",
            (result as WeightValidationResult.Invalid).message,
        )
    }

    @Test
    fun nonNumericInputIsRejected() {
        val result = WeightValidator.validate("70.5")

        assertTrue(result is WeightValidationResult.Invalid)
        assertEquals(
            "Use whole numbers only",
            (result as WeightValidationResult.Invalid).message,
        )
        assertTrue(WeightValidator.validate("heavy") is WeightValidationResult.Invalid)
    }

    @Test
    fun outOfRangeWeightsAreRejected() {
        val tooSmall = WeightValidator.validate("19")
        val tooLarge = WeightValidator.validate("301")

        assertEquals(
            "Enter at least 20 kg",
            (tooSmall as WeightValidationResult.Invalid).message,
        )
        assertEquals(
            "Enter at most 300 kg",
            (tooLarge as WeightValidationResult.Invalid).message,
        )
    }
}
