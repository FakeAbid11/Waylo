# Waylo

Waylo is a walking-focused Android application that turns everyday walking into an adventure:
step tracking, GPS walks, XP, levels, streaks, achievements, a fox mascot, and a virtual
exploration journey.

**Current implementation phase: Phase 2 — first-launch onboarding and the permission
foundation.**

Phase 1 (foundation, architecture, design system) and Phase 2 are implemented: a six-step
onboarding flow (welcome, concepts, companion, permission explanations, permission requests,
ready), DataStore-persisted onboarding completion, and a permission architecture with
`ACTIVITY_RECOGNITION` (API 29+) and `POST_NOTIFICATIONS` (API 33+) support. GPS tracking, step
counting, maps, XP/streak/achievement logic, and any cloud features are intentionally *not*
implemented yet; the app screens show honest zero/empty placeholder states.

## Technology stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (local database foundation)
- DataStore Preferences (onboarding state)
- Kotlin Coroutines / StateFlow
- JUnit 4 + Robolectric (unit tests)
- Android Gradle Plugin 9.4, Gradle 9.6, JDK 17

## Project structure

```
app/src/main/java/com/waylo/app
├── MainActivity.kt
├── WayloApplication.kt
├── core/permissions       permission model, PermissionManager, settings intents
├── core/util              formatting helpers
├── data/local             Room database foundation
├── data/preferences       DataStore-backed WayloPreferences (onboarding state)
├── domain/model           DailyGoal, UserProgress
├── ui
│   ├── components/        reusable Waylo components
│   ├── explore/           Explore screen + decorative journey illustration
│   ├── home/              Home screen + HomeViewModel
│   ├── navigation/        routes, NavHost, startup destination
│   ├── onboarding/        first-launch onboarding flow
│   ├── permissions/       PermissionsViewModel
│   ├── profile/           Profile screen + permission status section
│   ├── progress/          Progress screen + ProgressViewModel
│   └── theme/             colors, typography, shapes, gradients, dimensions
└── ui/WayloApp.kt         startup decision + Scaffold + bottom navigation
```

Layering: Compose UI → ViewModel (`StateFlow<UiState>`) → domain models → (in later phases)
repositories → Room.

## Build

Requires JDK 17 and an Android SDK (compileSdk 37, build-tools 36.0.0).

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

On Windows use `gradlew.bat assembleDebug`.

## Test

```bash
./gradlew test
```

Runs JVM unit tests: domain progress math, formatting, navigation routes, ViewModel initial
states, Room initialization (Robolectric), onboarding persistence across restarts, the startup
onboarding/main-app decision, and permission logic across API levels (Robolectric).

## GitHub Actions

`.github/workflows/android.yml` runs on pushes and pull requests to `main`. It sets up JDK 17
and the Android SDK, runs `./gradlew test`, builds `./gradlew assembleDebug`, and uploads the
debug APK as the downloadable `waylo-debug-apk` workflow artifact.

Failing compilation or failing unit tests fail the workflow.

## Design system

- Dark theme (`#080B12` background, `#111722` surfaces) is the default and launches first; a
  light theme foundation exists in the same architecture.
- Centralized palette (`WayloColors`), typography (`WayloTextStyles`, `WayloTypography`),
  shapes (`WayloShapes`), dimensions (`WayloDimens`) and the signature cyan → blue → violet
  gradient (`WayloGradients`).
- Reusable components: `WayloCard`, `WayloPrimaryButton`, `WayloProgressBar`, `WayloStatCard`,
  `WayloSectionHeader`, `WayloMascot`, `WayloBottomNavigation`.
