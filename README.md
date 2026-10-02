# Waylo

Waylo is a walking-focused Android application that turns everyday walking into an adventure:
step tracking, GPS walks, XP, levels, streaks, achievements, a fox mascot, and a virtual
exploration journey.

**Current implementation phase: Phase 3 — step tracking and daily goals.**

Phase 1 (foundation, architecture, design system), Phase 2 (onboarding + permissions) and
Phase 3 are implemented: a six-step onboarding flow (welcome, concepts, companion, permission
explanations, permission requests, ready), DataStore-persisted onboarding completion, a
permission architecture with `ACTIVITY_RECOGNITION` (API 29+) and `POST_NOTIFICATIONS`
(API 33+) support, and hardware step tracking based on `Sensor.TYPE_STEP_COUNTER` with a
baseline/rollover calculation, an honest unsupported-device state, and a user-configurable
daily step goal (default 6,000; range 1,000–100,000) edited from the Profile screen. GPS
tracking, maps, XP/streak/achievement logic, and any cloud features are intentionally *not*
implemented yet; those screens show honest zero/empty placeholder states.

## Technology stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (local database foundation)
- DataStore Preferences (onboarding state, daily step goal + step baseline)
- Android sensor APIs (`TYPE_STEP_COUNTER`)
- Kotlin Coroutines / StateFlow
- JUnit 4 + Robolectric (unit tests)
- Android Gradle Plugin 9.4, Gradle 9.6, JDK 17

## Project structure

```
app/src/main/java/com/waylo/app
├── MainActivity.kt
├── WayloApplication.kt
├── core/common          pure step-calculation helpers (baseline, rollover)
├── core/permissions     permission model, PermissionManager, settings intents
├── core/util            formatting helpers
├── data/local           Room database foundation
├── data/preferences     DataStore-backed WayloPreferences (onboarding state)
├── data/step            step sensor, step state store, StepRepository
├── domain/model         DailyGoal, DailyStepState, DailyGoalValidator, UserProgress
├── ui
│   ├── components/      reusable Waylo components
│   ├── explore/         Explore screen + decorative journey illustration
│   ├── home/            Home screen + HomeViewModel (live steps + goal)
│   ├── navigation/      routes, NavHost, startup destination
│   ├── onboarding/      first-launch onboarding flow
│   ├── permissions/     PermissionsViewModel
│   ├── profile/         Profile screen + permissions + daily goal editor
│   ├── progress/        Progress screen + ProgressViewModel
│   └── theme/           colors, typography, shapes, gradients, dimensions
└── ui/WayloApp.kt       startup decision + Scaffold + bottom navigation
```

Layering: Compose UI → ViewModel (`StateFlow<UiState>`) → domain models → repository
(`StepRepository`) → DataStore; Room remains the foundation for later phases.

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
onboarding/main-app decision, permission logic across API levels (Robolectric), the pure step
calculation (baseline anchoring, stale readings, counter resets, daily rollover), daily goal
validation, step-state status mapping, step-state DataStore persistence across restarts,
`StepRepository` behaviour with a fake sensor (permission gating, missing sensor, rollover,
persistence throttling, goal updates), and HomeViewModel state mirroring.

## GitHub Actions

`.github/workflows/android.yml` runs on pushes and pull requests to `main`. It sets up JDK 17
and the Android SDK, runs `./gradlew test`, builds `./gradlew assembleDebug`, and uploads the
debug APK as the downloadable `waylo-debug-apk` workflow artifact.

Failing compilation or failing unit tests fail the workflow.

## Step tracking (Phase 3)

- Primary sensor: `Sensor.TYPE_STEP_COUNTER` (cumulative since boot). No GPS estimation and
  no fabricated data — if the device has no step sensor, the Home card states "Step counting
  isn't available on this device."
- Daily steps are computed as `todaySteps = currentSensorValue - baselineSensorValue`; the
  raw cumulative counter is never shown to the user.
- Baseline handling: first reading anchors the baseline (0 steps), a reading below the
  baseline (sensor restart / reboot) rebases safely instead of going negative, stale
  out-of-order readings are ignored, and a device-local day rollover resets today's steps to
  zero with a fresh baseline anchored to the last known reading (a gap of two or more days
  starts unanchored instead of guessing).
- Persistence: baseline day, baseline count, last known count and today's steps are stored in
  the existing DataStore preferences store — written on rollover, on stop, and at most once
  per 20 sensor events (never on every event).
- `ACTIVITY_RECOGNITION` (API 29+) gates reading; below API 29 no permission is needed. The
  Home card distinguishes loading, permission needed, sensor unavailable and active states,
  and the listener lives in `StepRepositoryImpl` (no Composable holds a sensor listener, no
  foreground service).
- Daily goal: default 6,000 steps, editable from Profile → Preferences → Daily step goal
  (validated to 1,000–100,000 whole steps, persisted in DataStore). Progress is capped at
  100% visually while stored steps are never capped; completing the goal shows the text
  "Daily goal complete!" (no XP/streak rewards yet).

## Design system

- Dark theme (`#080B12` background, `#111722` surfaces) is the default and launches first; a
  light theme foundation exists in the same architecture.
- Centralized palette (`WayloColors`), typography (`WayloTextStyles`, `WayloTypography`),
  shapes (`WayloShapes`), dimensions (`WayloDimens`) and the signature cyan → blue → violet
  gradient (`WayloGradients`).
- Reusable components: `WayloCard`, `WayloPrimaryButton`, `WayloProgressBar`, `WayloStatCard`,
  `WayloSectionHeader`, `WayloMascot`, `WayloBottomNavigation`.
