# Waylo

Waylo is a walking-focused Android application that turns everyday walking into an adventure:
step tracking, GPS walks, XP, levels, streaks, achievements, a fox mascot, and a virtual
exploration journey.

**Current implementation phase: Phase 9 — Mascot system.**

Phase 1 (foundation, architecture, design system), Phase 2 (onboarding + permissions),
Phase 3 (step tracking), Phase 4 (GPS walking engine), Phase 5 (MapLibre map),
Phase 6 (workout statistics), Phase 7 (finished activity + history), Phase 8
(XP + levels + streaks) and Phase 9 (mascot system) are implemented: a
six-step onboarding flow (welcome,
concepts, companion, permission explanations, permission requests, ready), DataStore-persisted
onboarding completion, a permission architecture with `ACTIVITY_RECOGNITION` (API 29+) and
`POST_NOTIFICATIONS` (API 33+) support, hardware step tracking based on
`Sensor.TYPE_STEP_COUNTER` with a baseline/rollover calculation, an honest unsupported-device
state, a user-configurable daily step goal (default 6,000; range 1,000–100,000) edited from
the Profile screen, a GPS walking engine with foreground tracking, pause/resume, filtered
distance, and Room-persisted sessions, plus an online MapLibre map on the Active Walk screen
with a live route polyline, current-position marker, camera follow/recenter and honest
loading/offline/style-error states, workout statistics (distance, duration, pace, speed,
estimated calories, walk steps) with a completion summary, and a finished-activity screen
with an activity history (date-grouped, newest first) offering route replay and confirmed
deletion, plus a local-first XP ledger with levels and day-streaks shown on Home, Profile,
Progress and the finished-activity screen, plus a state-driven fox mascot whose expression,
message and animation follow a deterministic priority resolver. Offline maps, achievements,
the exploration journey, and any cloud features are intentionally *not* implemented yet;
those screens show honest zero/empty placeholder states.

## Technology stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room (walk session + route point persistence, XP ledger + progression, schema migrations 1 → 4)
- DataStore Preferences (onboarding state, daily step goal, step baseline + weight)
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
├── core/common          pure calculation helpers (step baseline/rollover, WalkingEngine, XP/level/streak math, mascot resolver/content)
├── core/permissions     permission model, PermissionManager, settings intents
├── core/util            formatting helpers (distance, duration, counts)
├── data/local           Room database (settings + walking sessions + route points + XP ledger/progression)
├── data/location        LocationDataSource (LocationManager) + framework distance
├── data/map             MapLibre style config, route GeoJSON, map/camera state policy
├── data/preferences     DataStore-backed WayloPreferences (onboarding state)
├── data/progression     ProgressionRepository (XP awards, level/streak recalculation)
├── data/step            step sensor, step state store, StepRepository
├── data/walk            WalkingRepository (walk state machine + persistence)
├── domain/model         DailyGoal, DailyStepState, WalkingState/WalkingSession, UserProgress, models
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
the finished route), `FrameworkDistance` under Robolectric, the Room 1 → 2, 2 → 3 and 3 → 4
schema migrations against hand-built v1/v2/v3 databases, the pure `WorkoutStatisticsCalculator`
(average thresholds and invalid inputs, the rolling current-pace window with pause/gap/noise
rules, MET-based calorie estimates, walk-step deltas and counter resets), the pace/speed/
calorie/step formatters plus the meters/kilometers distance format, `WeightValidator`,
`WayloPreferences` weight persistence, raw sensor-counter exposure, repository walk-step
baselines (capture/snapshot/unavailable/reset/process-death recovery/`lastCompletedSession`
restore) and `ActiveWalkViewModel` statistics wiring, the pure `XpCalculator`,
`LevelCalculator` and `StreakCalculator`, the `xp_awards` DAO (unique-award index, ledger
sums, reactive flows), `ProgressionRepository` behaviour (award outcomes, idempotency across
repository restarts, day streaks, cascade deletion, era-guarded recovery, one-shot award
events), completion-driven XP awards inside `WalkingRepository`, the Home/Progress/
ActivityDetail ViewModel progression wiring (level-up banner, XP card, one-shot event),
and the mascot system: the pure state/reaction priority resolver (`MascotResolverTest`:
every signal, override order, determinism, context derivation from `UserProgress`) and the
deterministic mascot messaging (`MascotContentTest`: per-state messages, XP/streak amounts,
guilt-free language, unique contextual accessibility descriptions).

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
  "Daily goal complete!" (step goals grant no XP — XP comes from walked distance, Phase 8).

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

## Workout statistics (Phase 6)

- Architecture: GPS and step services are unchanged — both feed pure functions with
  injected time: `WalkingRepository` (persisted distance/active time/route/final step
  snapshot) and `StepRepository` (raw cumulative sensor counter) →
  `WorkoutStatisticsCalculator` (pure Kotlin in `core/common`, no Compose/Android/
  MapLibre imports) → `WorkoutStatistics` on `ActiveWalkUiState` → Active Walk overlay
  and completion summary. "Now", sensor values and every input are injected; no
  statistics are computed inside a Composable.
- Distance display: meters below one kilometer (`245 m`), two-decimal kilometers from
  one kilometer on (`1.24 km`, `10.00 km`); negative/non-finite input clamps to
  `0 km`.
- Average pace/speed: accepted walk distance over active duration, only when the walk
  has at least 10 m and positive active time; non-finite values and physically
  impossible results (speed above 15 m/s, mirrored as pace below ~66.7 s/km) are
  unavailable instead of displayed.
- Current pace/speed: one shared rolling window — accepted route points from the last
  45 s after the last pause break, split on gaps longer than 15 s (only the latest
  chunk counts), needing at least two points and at least 10 m of movement. Paused
  walks show `—` (no stale readings, no resume spike), and pace and speed always come
  from the same window so they can never contradict each other; the window measures
  point-to-point path length, not a straight line.
- Estimated calories: `MET × weight_kg × active hours`, with walking METs from the
  Compendium of Physical Activities speed bins (<3.2 → 2.8, <4.0 → 3.0, <4.8 → 3.5,
  <5.6 → 4.3, <6.4 → 5.0, ≥6.4 → 6.3 km/h) and the documented 3.5 MET default when
  no average speed exists yet. Weight is an optional Profile preference (20–300 kg,
  DataStore); without one the estimate is honestly `—` (with a Profile hint) rather
  than an invented default, and zero active time yields 0. It is an estimate, not a
  medical figure, and it is re-derived from persisted distance/duration plus the
  current weight whenever viewed (editing weight changes past estimates on re-view).
- Walk steps: delta of the raw cumulative sensor counter — the baseline is captured
  when the walk starts and the final snapshot is persisted when it stops
  (`walking_sessions.walkStartStepCount` / `walkStepCount`, Room migration 2→3 via
  non-destructive `ALTER TABLE`). While walking the live value is `current −
  baseline`; a counter reset (device reboot), a missing baseline or missing counts
  yield `—`, never a negative or GPS-guessed number. The baseline is per walk, so a
  midnight rollover cannot corrupt it, and daily step totals/goals stay untouched.
  Live deltas can include steps taken for other reasons while paused (the sensor is
  real and not frozen).
- Persistence: distance, duration and the final step count are stored at stop;
  averages and calories are re-derived from persisted data. A new
  `lastCompletedSession: StateFlow` exposes the most recent finished walk (restored
  at startup) for tests; Phase 7 builds the history UI on top of it.
- UI: an ongoing walk keeps status/duration/distance primary and adds a compact stats
  card (current/average pace, current/average speed, Est. calories, walk steps — all
  rendering `—` when unavailable); the completion summary shows six tiles (distance,
  duration, avg pace, avg speed, Est. kcal, steps).
- Tests (267 total, 53 added): calculator suite (average thresholds and invalid
  inputs, rolling-window/pause/gap/noise rules, window consistency, MET bins, calorie
  rules, step deltas and counter resets), formatter coverage for distance/pace/speed/
  calories/steps, `WeightValidatorTest`, repository step-baseline tests (capture,
  snapshot, unavailable, reset, process-death recovery, `lastCompletedSession`
  restore), Room migration 2→3 test (new columns, preserved data), preferences weight
  test, raw sensor counter test and ViewModel statistics tests.

## Finished activity and history (Phase 7)

- Navigation flow: finishing a walk dismisses the live session and opens the finished
  activity screen for that walk (`activity/{activityId}`), popping the walk screen so
  Back returns to where the walk was started and the next walk starts fresh. History is
  reachable from Profile → Activity → "Activity history"; both screens are full-screen
  sub-pages (bottom bar hidden) with a back arrow, and cards open their activity detail.
- Activity history: completed sessions only (never an ongoing walk) through one Room
  query sorted newest first (`startMillis DESC, id DESC` as the stable secondary key),
  grouped into Today / Yesterday / date sections in the device's local time, each card
  showing date · time, distance, duration, average pace and walk steps — session metadata
  only; route points are never loaded on this screen. Empty state: "No walks yet / Your
  completed walks will appear here. / Start your first walk".
- Finished activity detail: header with the full date and start (and end) times, the
  recorded route replayed on the existing MapLibre map (the camera fits the route exactly
  once with padding, never following), and the same six statistics as the Phase 6
  completion summary (distance, duration, avg pace, avg speed, Est. kcal, steps) computed
  by the same `WorkoutStatisticsCalculator` from persisted distance/active time plus the
  current Profile weight — identical values on completion, history and detail; missing
  weight, steps or pace render `—`.
- Honest map states: route loading, "Route unavailable" (no recorded points or an
  unloadable route), offline and style-error states with retry; statistics stay visible
  in every one of them, and no location permission is requested on these screens.
- Deletion: "Delete walk" opens a confirmation dialog ("Delete this walk?" / "This
  activity and its recorded route will be permanently removed." / Cancel / Delete) and
  removes the session row and its route points in a single Room transaction that refuses
  non-completed sessions; other walks, daily steps and settings are untouched, and a walk
  deleted while its screen is open becomes "Activity not found".
- Date/time: `WayloDateFormatter` (java.time, injectable zone/clock, fixed English
  labels) renders Today / Yesterday / `Sep 28, 2026` group labels, `6:42 PM` times and
  `September 28, 2026` full dates in local time — tests cover midnight, year and timezone
  boundaries.
- Limitation (honest): pause-break markers remain memory-only (Phase 5), so a route
  replayed after process death draws as one continuous line; calories are re-derived from
  the current weight whenever a walk is viewed (Phase 6 behavior).
- Tests (308 total, 41 added): `WalkingDaoTest` (completed-only ordering, start-time
  tie-breaks, reactive flow, atomic delete, active-session refusal), repository
  history/route/delete tests, `WayloDateFormatterTest`, `ActivityHistoryViewModelTest`
  (grouping, labels, error, retry, deletion updates, timezone) and
  `ActivityDetailViewModelTest` (Phase 6 statistics regression, NotFound/Error/retry,
  route-failure fallback, camera fitting, deletion). No Compose UI tests exist in this
  project, so none were added.

## XP, levels and streaks (Phase 8)

- XP formula: distance-driven — `round(distanceMeters / 1000 × 100)` (100 XP per
  kilometer) via the pure `XpCalculator`, awarded only when a completed walk recorded at
  least 100.0 m; shorter walks earn 0 XP and never create a ledger row (no empty awards).
- Idempotent ledger: every award is one `xp_awards` row with a unique index on
  `activityId`, so retries — stop-time award, restore-time recovery, a second repository
  instance — return `AlreadyAwarded` instead of double-counting; total XP is always
  `SUM(xp_awards.xp)` recomputed transactionally with the award, never a mutable counter.
- Levels: pure `LevelCalculator` — advancing level `L → L+1` costs `100 × L²`, so level 1
  starts at 0 XP, level 2 at 100, level 3 at 500 and so on (cumulative sums are computed
  closed-form; `MAX_LEVEL` 401 keeps every value inside `Int`). The UI shows XP into the
  current level over that level's requirement.
- Streaks: pure `StreakCalculator` over distinct **local calendar days** (device zone,
  injectable clock) that contain at least one qualifying award (`xp > 0`); the current
  streak accepts a one-day grace (today *or* yesterday), the longest streak is the maximum
  run ever seen. Display values are recomputed live from the ledger on every database
  change, so they can never drift; the single `progression` row mirrors them only for the
  stored aggregates.
- Persistence: Room schema version 4 adds `xp_awards` (activityId, xp, awardedAt) and
  `progression` (single row id = 1: totalXp, streak mirrors, `updatedAtMillis`,
  `createdAtMillis` era marker) with migration `3 → 4`. The migration creates both tables,
  backfills an empty progression row and awards **nothing** for pre-existing history —
  the ledger starts clean at Phase 8 (retroactive XP for old walks is out of scope), and
  `createdAtMillis` marks the era for restore-time recovery.
- Award trigger: `WalkingRepositoryImpl.stopWalk()` awards explicitly after the session
  row persists `Completed` (never driven by Flow observation alone); on restart,
  `recoverAwardIfNeeded` closes the process-death window for interrupted `Stopping →
  Completed` and missed `Completed` rows, guarded by the era marker
  (`sessionUpdatedMillis ≥ createdAtMillis`) and made harmless by the unique index.
- Deletion: deleting a walk removes its award and re-derives total/streaks inside the
  *same* Room transaction (`ProgressionRepository.deleteActivityCascade` wraps the
  session/route delete), so history deletion can never strand XP or freeze a streak.
- UI: Home's XP card and the Profile header show level and lifetime total XP; the
  Progress screen shows the level, XP bar (into/required), total XP and current/longest
  streaks from the real ledger; the finished-activity screen shows the persisted "XP
  earned" for that walk plus a one-shot "Level up!" banner (`Level n → n+1`) consumed from
  a single-consumption `awardEvent` only when the event belongs to the walk on screen.
- Limitations (honest): XP derives from walked distance only (steps and the daily goal
  grant none); pre-Phase-8 walks earn no XP; streaks refresh when the database changes
  (opening a history/detail screen recomputes them) rather than from a midnight timer;
  awards are produced locally with no cloud sync, achievements or exploration
  journey yet.
- Tests (367 total, 59 added): `XpCalculatorTest` (formula + distance threshold),
  `LevelCalculatorTest` (curve, exact thresholds, huge-value bounds),
  `StreakCalculatorTest` (day runs, grace, gaps), `ProgressionDaoTest` (unique-award
  index, ledger sums, reactive flows), `ProgressionRepositoryTest` (award outcomes,
  idempotency across restarts, streak days, cascade deletion, era-guarded recovery,
  one-shot events), `WayloDatabaseMigrationTest` (3 → 4: history preserved, empty ledger,
  schema round-trip), repository completion/short-walk/`Stopping`-recovery/deletion XP
  tests, and Home/Progress/ActivityDetail ViewModel tests (total XP wiring, level-up
  banner, consumed event). No Compose UI tests exist in this project, so none were added.

## Mascot system (Phase 9)

- Presentation-only companion: a friendly fox drawn entirely in code (Compose `Canvas` —
  no raster artwork, no emoji) inside the existing `WayloMascot` component, which is now
  state-driven: `WayloMascot(state, size, decorative)` renders expression, message and
  animation for a `MascotState` and replaces the previous `Icons.Filled.Pets` placeholder.
- Pure decision layer in `core/common`: `MascotContext` (walk state, completion, XP,
  level-up, streak days, history flags) + `MascotResolver.resolve` returning a
  `MascotDecision(state, reaction)` through one ordered `when` chain — priority is
  **LevelUp > XpEarned > Celebrating > walk tier (Paused > Starting > Error >
  Active/Stopping) > Streak > Encouraging (history) > Resting (no activity) > Idle**.
  One-shot award events are *not* re-consumed here: screens derive `levelUp`/`xpAwarded`
  from what they already show (Phase 8's banner and XP card), so there is exactly one
  level-up mechanism.
- Deterministic content: `MascotContent.message` maps each state/reaction to a fixed,
  guilt-free line ("Make every walk an adventure.", "Let's go!", "Take your time.",
  "Walk complete!", "Nice work! +N XP", "Level up!", "N days strong!",
  "Your next walk is waiting.") with no randomness and no shaming language;
  `MascotContent.contentDescription` gives every state a unique, contextual TalkBack
  description ("Waylo fox …") instead of "Image"/"Fox".
- Screen integration (no new destinations, no ViewModels, no persistence): Home's
  mascot section resolves live state from `HomeUiState` (excluding `Idle`/`Completed` so
  celebration can never stick); Active Walk shows a small decorative mascot in the
  status panel (Starting → Encouraging, Active → Walking, Paused → Paused) and a
  celebrating mascot above "Walk Complete"; the finished-activity screen shows a small
  mascot + reaction message (XP/level-up aware) under the date; Progress shows a passive
  mascot section (streak/history/resting); Profile's header mascot is decorative; onboarding
  keeps its original layout and now shows the default idle fox with a meaningful
  description.
- Motion: breathing idle for Idle/Encouraging/Streak, gentle waddle for Walking, a finite
  two-bounce celebration with a level-up glow for Celebrating/XpEarned/LevelUp; animations
  are conditional composes that stop when the screen leaves composition, and all motion is
  skipped when the system animator duration scale is 0 (reduce-motion). Decorative mascots
  clear semantics; non-decorative ones expose the state's content description.
- Limitations (honest): the fox is vector shapes drawn in Compose (a future art pass can
  swap in authored assets behind the same `WayloMascot` API); there is no mascot
  customization, accessories, animation on every screen, or per-user personality, and no
  Compose UI tests exist in this project — behaviour is covered by pure JVM tests instead.
- Tests (400 total, 33 added): `MascotResolverTest` (all states and reactions, full
  priority matrix, determinism, `MascotContext.fromProgress` derivation) and
  `MascotContentTest` (per-state messages, XP/streak formatting, guilt-language guard,
  unique contextual descriptions).

## Design system

- Dark theme (`#080B12` background, `#111722` surfaces) is the default and launches first; a
  light theme foundation exists in the same architecture.
- Centralized palette (`WayloColors`), typography (`WayloTextStyles`, `WayloTypography`),
  shapes (`WayloShapes`), dimensions (`WayloDimens`) and the signature cyan → blue → violet
  gradient (`WayloGradients`).
- Reusable components: `WayloCard`, `WayloPrimaryButton`, `WayloProgressBar`, `WayloStatCard`,
  `WayloSectionHeader`, `WayloMascot` (state-driven fox mascot), `WayloBottomNavigation`.
