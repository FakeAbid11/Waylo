# Waylo

Waylo is a walking-focused Android application that turns everyday walking into an adventure:
step tracking, GPS walks, XP, levels, streaks, achievements, a fox mascot, and a virtual
exploration journey.

**Current implementation phase: Phase 5 — MapLibre map with live walking route.**

Phase 1 (foundation, architecture, design system), Phase 2 (onboarding + permissions),
Phase 3 (step tracking), Phase 4 (GPS walking engine) and Phase 5 are implemented: a
six-step onboarding flow (welcome,
concepts, companion, permission explanations, permission requests, ready), DataStore-persisted
onboarding completion, a permission architecture with `ACTIVITY_RECOGNITION` (API 29+) and
`POST_NOTIFICATIONS` (API 33+) support, hardware step tracking based on
`Sensor.TYPE_STEP_COUNTER` with a baseline/rollover calculation, an honest unsupported-device
state, a user-configurable daily step goal (default 6,000; range 1,000–100,000) edited from
the Profile screen, a GPS walking engine with foreground tracking, pause/resume, filtered
distance, and Room-persisted sessions, plus an online MapLibre map on the Active Walk screen
with a live route polyline, current-position marker, camera follow/recenter and honest
loading/offline/style-error states. Offline maps, XP/streak/achievement logic, and any cloud
features are intentionally *not* implemented yet; those screens show honest zero/empty
placeholder states.

## Technology stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (walk session + route point persistence, schema migration 1 → 2)
- DataStore Preferences (onboarding state, daily step goal + step baseline)
- Android sensor APIs (`TYPE_STEP_COUNTER`)
- Android location APIs (`LocationManager`, `Location.distanceBetween`)
- MapLibre Native for Android (`org.maplibre.gl:android-sdk-opengl:13.6.1`)
- OpenFreeMap public vector tiles (token-free, online only)
- Foreground service (location type) for background walk tracking
- Kotlin Coroutines / StateFlow
- JUnit 4 + Robolectric (unit tests)
- Android Gradle Plugin 9.4, Gradle 9.6, JDK 17

## Project structure

```
app/src/main/java/com/waylo/app
├── MainActivity.kt
├── WayloApplication.kt
├── core/common          pure calculation helpers (step baseline/rollover, WalkingEngine)
├── core/permissions     permission model, PermissionManager, settings intents
├── core/util            formatting helpers (distance, duration, counts)
├── data/local           Room database (settings + walking sessions + route points)
├── data/location        LocationDataSource (LocationManager) + framework distance
├── data/map             MapLibre style config, route GeoJSON, map/camera state policy
├── data/preferences     DataStore-backed WayloPreferences (onboarding state)
├── data/step            step sensor, step state store, StepRepository
├── data/walk            WalkingRepository (walk state machine + persistence)
├── domain/model         DailyGoal, DailyStepState, WalkingState/WalkingSession, models
├── service              WalkingForegroundService (location foreground service)
├── ui
│   ├── components/      reusable Waylo components
│   ├── explore/         Explore screen + decorative journey illustration
│   ├── home/            Home screen + HomeViewModel (live steps + goal + walk entry)
│   ├── navigation/      routes, NavHost, startup destination
│   ├── onboarding/      first-launch onboarding flow
│   ├── permissions/     PermissionsViewModel
│   ├── profile/         Profile screen + permissions + daily goal editor
│   ├── progress/        Progress screen + ProgressViewModel
│   ├── theme/           colors, typography, shapes, gradients, dimensions
│   └── walk/            ActiveWalkScreen + ActiveWalkViewModel + WalkMap (live walk UI)
└── ui/WayloApp.kt       startup decision + Scaffold + bottom navigation
```

Layering: Compose UI → ViewModel (`StateFlow<UiState>`) → domain models → repository
(`StepRepository`, `WalkingRepository`) → DataStore / Room.

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

Runs JVM unit tests: domain progress math, formatting (including walk durations), navigation
routes, ViewModel initial states, Room initialization (Robolectric), onboarding persistence
across restarts, the startup onboarding/main-app decision, permission logic across API levels
(Robolectric) including the location entries, the pure step calculation (baseline anchoring,
stale readings, counter resets, daily rollover), daily goal validation, step-state status
mapping, step-state DataStore persistence across restarts, `StepRepository` behaviour with a
fake sensor (permission gating, missing sensor, rollover, persistence throttling, goal
updates), HomeViewModel state mirroring plus walk-state labels, the GPS filtering engine
(accuracy/timestamp/speed rejection, anchor rules), `ActiveWalkViewModel` readiness, entry
behaviour and map state (style lifecycle, offline transitions, camera follow/pan/recenter,
completion fitting), `WalkingRepository` state transitions with a fake location source
(start/pause/resume/stop, distance accumulation, duration folding, process-death recovery,
error paths) plus its live `route` flow (append/pause segments/dismiss/DB restore),
`WalkRoute` segments and bounds, exact route/marker GeoJSON rendering (including pause
splitting), `WalkMapCameraPolicy` (load-state machine, follow thresholds, recenter, fitting
the finished route), `FrameworkDistance` under Robolectric, and the Room 1 → 2 schema
migration against a hand-built v1 database.

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

## GPS walking engine (Phase 4)

- Entry: Home → "Start Walk" (or "Return to Walk" while one is ongoing) opens the Active
  Walk route; the bottom bar is hidden during a walk. The screen auto-requests the location
  permission the first time it is needed and otherwise blocks on honest readiness cards
  (permission needed / permanently denied / location services off).
- Tracking: `WalkingForegroundService` (foreground service, `location` type,
  `FOREGROUND_SERVICE_LOCATION`) keeps updates alive via `LocationManager` (GPS provider
  preferred, network fallback) with a ~3 s / 5 m update gate. No Play Services and no
  third-party libraries. Sticky restart re-attaches to a recovered session.
- State machine: `Idle → Starting → Active ⇄ Paused → Stopping → Completed`, plus `Error`
  with a human-readable reason (permission denied, location off, provider unavailable,
  tracking interrupted). All transitions are exposed as a single `StateFlow<WalkingStatus>`.
- Distance: the pure `WalkingEngine` accepts a sample only with valid coordinates, accuracy
  better than 50 m (unknown accuracy tolerated), a strictly newer timestamp, and an implied
  speed ≤ 5 m/s — which also rejects impossible GPS jumps. The first accepted sample anchors
  with zero distance; rejected samples never move the anchor; the anchor resets on every
  resume so a pause cannot create a teleport. Distances come from the framework
  `Location.distanceBetween`.
- Duration: active time is folded on pause/stop and excludes paused time; the live timer is
  `activeMillis + running segment`, never counting dead time after a process death.
- Persistence: Room schema version 2 (`walking_sessions` + `walking_location_points`,
  migration `1 → 2` that preserves settings). The session row is inserted at start, updated
  with every accepted sample (transactionally with the route point), and finalized to
  `Completed` on stop. Recovery folds open segments at the last persisted timestamp, never
  resurrects a completed walk into the UI, and finalizes an interrupted `Stopping` row.
- Permissions: `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` are required but are *not*
  requested during onboarding — they are requested in context, on the Active Walk screen.
- UI: start/pause/resume, a stop confirmation ("Finish this walk?" / "Your recorded distance
  will be saved."), a completion summary (distance + duration), and error states with a
  retry. No pace/speed/calories (later phases); the map overlay is described in Phase 5 below.

## Map and live route (Phase 5)

- Library: MapLibre Native for Android, `org.maplibre.gl:android-sdk-opengl:13.6.1` from
  Maven Central, hosted in `AndroidView`/`MapView`. The explicit **OpenGL** build is chosen
  over the default `android-sdk` package because MapLibre 11+ switched that package to the
  Vulkan backend as a breaking change; the OpenGL backend has the widest device support on
  minSdk 26 and needs no experimental/pre-1.0 library. The official
  `org.maplibre.compose` artifact was skipped because it is still pre-1.0 and aimed at
  Compose Multiplatform. No API key is required (`MapLibre.getInstance(context)`).
- Style/tiles: the OpenFreeMap public style
  `https://tiles.openfreemap.org/styles/dark` (dark, matching Waylo's theme). OpenFreeMap is
  donation funded, needs no key/registration and documents no request limits; OSM and
  OpenMapTiles attribution is rendered by the MapLibre UI automatically. The map is
  **online only** — Waylo never caches tiles to disk (offline maps are a later phase), so
  offline the map area shows an honest "Map unavailable while offline. Your walk is still
  being recorded." message while GPS tracking continues unaffected.
- Data flow: `WalkingForegroundService → WalkingRepository → persisted route points →
  ActiveWalkViewModel → ActiveWalkUiState → WalkMap`. The repository exposes a new
  `route: StateFlow<WalkRoute>` fed only by GPS-accepted points (same engine and filters as
  Phase 4 — no second location source); `WalkRoute` also tracks pause breaks so the polyline
  renders as a MultiLineString and never draws a fake straight line across a pause.
- Rendering: a GeoJSON source (`lineMetrics: true`) feeds a `LineLayer` with a
  cyan → blue → violet `lineGradient` (`#22D3EE → #3B82F6 → #8B5CF6`, Waylo's palette), plus
  a circle layer for the current-position marker drawn above the line. Route updates only
  call `setGeoJson` on the existing source — the map and layers are created once per style
  load. No marker or camera move is fabricated without a real GPS fix.
- Camera: follow mode is on at walk start, recenters on the first fix and thereafter only
  when the position moved ≥ 8 m (no per-update animation churn); manually panning the map
  stops following, the recenter button (disabled with an explanatory content description
  until a fix exists) re-centers and re-enables it, and finishing a walk fits the full route
  bounds with padding (single point → center at zoom 16). The policy is a pure,
  unit-tested object (`WalkMapCameraPolicy`).
- Map states: `Loading` (initial), `Ready`, `Unavailable` (no network — auto-recovers and
  retries the style when the connection returns) and `StyleError` (style failed while
  online, with a Retry button), driven by `ConnectivityManager` callbacks
  (`ACCESS_NETWORK_STATE`, new normal permission) and MapLibre load listeners. `INTERNET`
  was added to the manifest (the app previously had no network permission at all).
- UI: the map is the main visual area of the Active Walk screen; status/duration/distance
  sit in a compact overlay card, pause/resume/stop and the recenter button stay reachable at
  the bottom, and readiness/error/completion panels render as cards over the map with the
  fitted route still visible on the completion screen.
- Limitation (honest): pause-break markers live in memory only — after a process death the
  restored route replays as a single continuous line from the persisted points; distance and
  time accounting are unaffected.
- Tests (214 total, 40 added): `WalkRouteTest` (segments/bounds/breaks),
  `MapRoutePresentationTest` (exact GeoJSON, pause splitting, empty states),
  `WalkMapCameraPolicyTest` (load-state machine, follow/pan/recenter thresholds, completion
  fits), repository route tests (append/pause segments/dismiss/process-death restore/error
  restore) and ViewModel map tests (style lifecycle, network transitions, camera commands,
  completion fits exactly once).

## Design system

- Dark theme (`#080B12` background, `#111722` surfaces) is the default and launches first; a
  light theme foundation exists in the same architecture.
- Centralized palette (`WayloColors`), typography (`WayloTextStyles`, `WayloTypography`),
  shapes (`WayloShapes`), dimensions (`WayloDimens`) and the signature cyan → blue → violet
  gradient (`WayloGradients`).
- Reusable components: `WayloCard`, `WayloPrimaryButton`, `WayloProgressBar`, `WayloStatCard`,
  `WayloSectionHeader`, `WayloMascot`, `WayloBottomNavigation`.
