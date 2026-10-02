# Waylo

Waylo is a walking-focused Android application that turns everyday walking into an adventure:
step tracking, GPS walks, XP, levels, streaks, achievements, a fox mascot, and a virtual
exploration journey.

**Current implementation phase: Phase 1 — Foundation, architecture and design system.**

Only Phase 1 is implemented. GPS tracking, step counting, maps, XP/streak/achievement logic,
onboarding, permissions, and any cloud features are intentionally *not* implemented yet; the
screens show honest zero/empty placeholder states.

## Technology stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (local database foundation)
- Kotlin Coroutines / StateFlow
- JUnit 4 + Robolectric (unit tests)
- Android Gradle Plugin 9.4, Gradle 9.6, JDK 17

## Project structure

```
app/src/main/java/com/waylo/app
├── MainActivity.kt
├── WayloApplication.kt
├── core/util            formatting helpers
├── data/local           Room database foundation
├── domain/model         DailyGoal, UserProgress
├── ui
│   ├── components/      reusable Waylo components
│   ├── explore/         Explore screen + decorative journey illustration
│   ├── home/            Home screen + HomeViewModel
│   ├── navigation/      routes + NavHost
│   ├── profile/         Profile screen
│   ├── progress/        Progress screen + ProgressViewModel
│   └── theme/           colors, typography, shapes, gradients, dimensions
└── ui/WayloApp.kt       Scaffold + bottom navigation
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
states and Room initialization (Robolectric).

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
