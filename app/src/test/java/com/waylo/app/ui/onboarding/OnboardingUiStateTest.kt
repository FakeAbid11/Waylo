package com.waylo.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiStateTest {

    @Test
    fun onboardingHasSixPages() {
        assertEquals(6, OnboardingUiState().totalPages)
        assertEquals(OnboardingPage.entries.size, OnboardingUiState().totalPages)
    }

    @Test
    fun startsOnWelcomePage() {
        val state = OnboardingUiState()

        assertEquals(OnboardingPage.Welcome, state.page)
        assertTrue(state.isFirstPage)
        assertFalse(state.isLastPage)
    }

    @Test
    fun lastPageIsReadyPage() {
        val state = OnboardingUiState(currentPage = OnboardingPage.entries.lastIndex)

        assertEquals(OnboardingPage.Ready, state.page)
        assertTrue(state.isLastPage)
        assertFalse(state.isFirstPage)
    }

    @Test
    fun pagesFollowExpectedOrder() {
        val order = OnboardingPage.entries.toList()

        assertEquals(
            listOf(
                OnboardingPage.Welcome,
                OnboardingPage.Concepts,
                OnboardingPage.Companion,
                OnboardingPage.PermissionsExplanation,
                OnboardingPage.PermissionRequest,
                OnboardingPage.Ready,
            ),
            order,
        )
    }

    @Test
    fun completedOnboardingIsReflectedInState() {
        assertTrue(OnboardingUiState(onboardingCompleted = true).onboardingCompleted)
        assertFalse(OnboardingUiState().onboardingCompleted)
    }
}
