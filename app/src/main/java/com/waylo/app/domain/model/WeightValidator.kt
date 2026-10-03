package com.waylo.app.domain.model

sealed interface WeightValidationResult {
    data class Valid(val weightKg: Int) : WeightValidationResult
    data class Invalid(val message: String) : WeightValidationResult
}

object WeightValidator {

    const val MIN_WEIGHT_KG = 20
    const val MAX_WEIGHT_KG = 300

    fun validate(input: String): WeightValidationResult {
        val text = input.trim()
        if (text.isEmpty()) return WeightValidationResult.Invalid(EMPTY_MESSAGE)
        val weight = text.toIntOrNull() ?: return WeightValidationResult.Invalid(NOT_A_NUMBER_MESSAGE)
        if (weight < MIN_WEIGHT_KG) return WeightValidationResult.Invalid(TOO_SMALL_MESSAGE)
        if (weight > MAX_WEIGHT_KG) return WeightValidationResult.Invalid(TOO_LARGE_MESSAGE)
        return WeightValidationResult.Valid(weight)
    }

    private const val EMPTY_MESSAGE = "Enter your weight"
    private const val NOT_A_NUMBER_MESSAGE = "Use whole numbers only"
    private const val TOO_SMALL_MESSAGE = "Enter at least 20 kg"
    private const val TOO_LARGE_MESSAGE = "Enter at most 300 kg"
}
