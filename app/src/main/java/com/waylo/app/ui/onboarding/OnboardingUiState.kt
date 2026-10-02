package com.waylo.app.ui.onboarding

enum class OnboardingPage {
    Welcome,
    Concepts,
    Companion,
    PermissionsExplanation,
    PermissionRequest,
    Ready,
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val onboardingCompleted: Boolean = false,
) {
    val totalPages: Int = OnboardingPage.entries.size
    val page: OnboardingPage get() = OnboardingPage.entries[currentPage]
    val isFirstPage: Boolean get() = currentPage == 0
    val isLastPage: Boolean get() = currentPage == totalPages - 1
}
