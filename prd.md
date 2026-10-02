# WAYLO

## Master Product Requirements Document

**Product:** Waylo
**Platform:** Android
**Technology:** Kotlin + Jetpack Compose
**Product Category:** Walking / Fitness / Gamified Activity Tracker
**Product Philosophy:** Duolingo for walking
**Document Status:** Master PRD — Initial Specification
**Version:** 1.0

---

# 1. PRODUCT VISION

## 1.1 What is Waylo?

Waylo is a walking-focused Android application that combines:

* Step tracking
* GPS walking tracking
* Interactive maps
* Distance
* Pace
* Speed
* Calories
* Walking history
* XP
* Levels
* Daily goals
* Streaks
* Achievements
* A mascot companion
* Virtual exploration

Waylo is not intended to be a full social fitness network.

Its primary purpose is to make **walking enjoyable, motivating, and game-like**.

### Core concept

> **Waylo turns everyday walking into an adventure.**

The product should feel closer to a **game with fitness tracking** than a traditional fitness dashboard.

---

# 2. PRODUCT PHILOSOPHY

Waylo follows four principles.

## 2.1 Walking first

The app should focus primarily on walking.

Running, cycling, swimming, and other sports are outside the initial scope.

## 2.2 Motivation over statistics

Statistics are important, but they should support motivation.

The user should not open Waylo only to look at numbers.

The user should open Waylo because they want to:

* Complete today's goal
* Maintain their streak
* Earn XP
* Level up
* Explore
* Unlock achievements
* Progress with their mascot

## 2.3 Simple for beginners

A person who knows nothing about fitness tracking should immediately understand:

> What should I do today?

The Home screen should answer this immediately.

## 2.4 Privacy-first

The first version should work primarily with local data.

No social network or mandatory cloud backend is required.

---

# 3. TARGET USER

Waylo is primarily designed for people who:

* Want to walk more
* Want daily activity motivation
* Enjoy games and progression
* Want simple fitness statistics
* Want GPS routes
* Want to explore their surroundings
* Do not want the complexity of a professional fitness platform

Waylo should work for both:

### Casual walkers

Example:

> "I want to reach 6,000 steps today."

and:

### Dedicated walkers

Example:

> "I want to walk 8 km and track my route."

---

# 4. PRODUCT DIFFERENTIATION

Waylo combines three experiences.

## Layer 1 — Fitness

* Steps
* Distance
* Time
* Pace
* Speed
* Calories
* GPS route

## Layer 2 — Game

* XP
* Levels
* Goals
* Streaks
* Achievements
* Rewards

## Layer 3 — Adventure

* Mascot
* Virtual journey
* Exploration
* Unlockable locations
* Progression

The combination of these three layers forms the Waylo identity.

---

# 5. BRAND IDENTITY

## 5.1 Product Name

**Waylo**

The name should always be displayed consistently as:

> Waylo

Do not rename the application to a Strava-like name.

---

# 6. VISUAL IDENTITY

## 6.1 Primary visual direction

Waylo should feel:

* Modern
* Friendly
* Playful
* Premium
* Energetic
* Slightly futuristic
* Game-like

It should not look like a traditional medical or corporate fitness application.

---

# 7. COLOR SYSTEM

## Dark theme — default

### Background

`#080B12`

### Surface

`#111722`

### Primary blue

`#3B82F6`

### Violet

`#8B5CF6`

### Cyan

`#22D3EE`

### Success

`#22C55E`

### Warning / calories

`#F97316`

### Primary text

`#F8FAFC`

### Secondary text

`#94A3B8`

---

# 8. WAYLO SIGNATURE GRADIENT

The signature Waylo gradient is:

**Cyan → Blue → Violet**

This gradient should be used selectively for:

* GPS route
* Progress indicators
* XP effects
* Level-up effects
* Selected UI accents
* Special animations

The gradient should not be applied to every UI element.

---

# 9. MASCOT

## 9.1 Mascot concept

Waylo's mascot is a **friendly fox**.

The fox represents:

* Exploration
* Curiosity
* Adventure
* Movement
* Friendship

The fox is not merely an icon.

It is the user's **walking companion**.

---

# 10. MASCOT PERSONALITY

The mascot should feel:

* Friendly
* Encouraging
* Playful
* Positive
* Occasionally humorous

It should never shame the user for missing a goal.

Instead of:

> "You failed."

Waylo should say:

> "No worries. Let's try again tomorrow."

---

# 11. MASCOT STATES

The initial mascot system should support:

1. Idle
2. Happy
3. Walking
4. Excited
5. Celebrating
6. Encouraging
7. Sleep/resting
8. Level-up
9. Achievement unlocked
10. Goal completed

Additional states can be added later.

---

# 12. CORE USER LOOP

The primary Waylo loop is:

```text
Open Waylo
      ↓
See today's goal
      ↓
Start walking
      ↓
Track steps + GPS
      ↓
Finish walk
      ↓
Receive results
      ↓
Earn XP
      ↓
Progress toward level
      ↓
Maintain streak
      ↓
Unlock achievements/rewards
      ↓
Continue adventure
      ↓
Return tomorrow
```

---

# 13. MAIN APPLICATION AREAS

Waylo initially contains five primary areas.

## 13.1 Home

Purpose:

Give the user immediate motivation.

Displays:

* Mascot
* Daily step goal
* Current steps
* Progress
* XP
* Level
* Streak
* Start Walk button
* Today's summary

---

# 14. ACTIVE WALK

Purpose:

Track the current walk.

Displays:

* Live map
* Current route
* Steps
* Distance
* Duration
* Current pace
* Average pace
* Current speed
* Average speed
* Calories
* Pause
* Finish

The UI should prioritize the map and essential statistics.

---

# 15. FINISHED WALK

Purpose:

Make completing a walk feel rewarding.

Displays:

* Route map
* Distance
* Duration
* Steps
* Average pace
* Average speed
* Calories
* XP earned
* Goal progress
* Streak
* Mascot reaction

The finished screen should feel like a **reward screen**, not a boring statistics report.

---

# 16. PROGRESS

Displays:

* Current level
* XP
* XP required for next level
* Current streak
* Longest streak
* Total steps
* Total distance
* Total walking time
* Achievements

---

# 17. EXPLORE

Explore is the long-term adventure system.

It can display:

* Current virtual journey
* Distance progress
* Locations
* Unlockable destinations
* Discoveries
* Adventure milestones

Example:

```text
🏠 Home
   ↓
🌳 Forest Trail
   ↓
🌊 Riverside
   ↓
🏔️ Mountain Pass
   ↓
🏖️ Coastal Trail
```

The user's real walking distance contributes to virtual exploration.

---

# 18. SETTINGS / PROFILE

Contains:

* User profile
* Mascot customization
* Daily goal
* Units
* Notifications
* Permissions
* Offline maps
* Battery/tracking settings
* Privacy settings
* About Waylo

---

# 19. STEP TRACKING

Waylo should use Android's hardware step-counting capabilities.

Primary sensor:

`TYPE_STEP_COUNTER`

Optional supporting sensor:

`TYPE_STEP_DETECTOR`

Waylo must detect when a device does not provide an appropriate step sensor.

The app must not falsely claim accurate step tracking when the hardware is unavailable.

---

# 20. DAILY STEPS

The Home screen should show:

```text
6,428
/ 8,000 steps
```

with a visual progress indicator.

The user can configure their daily goal.

Example goals:

* 3,000
* 5,000
* 6,000
* 8,000
* 10,000
* Custom

---

# 21. GPS TRACKING

During an active walk, Waylo records GPS coordinates.

The GPS engine must handle:

* Location accuracy
* GPS updates
* Invalid coordinates
* Sudden jumps
* Poor GPS signal
* Pausing
* Resuming
* Background tracking
* Activity completion

The app should filter obvious GPS anomalies rather than blindly drawing every received coordinate.

---

# 22. DISTANCE

Distance should be calculated from accepted GPS route points.

The application should not simply trust raw GPS distance.

GPS points should be processed before contributing to the final route distance.

---

# 23. SPEED

Waylo should calculate:

### Current speed

Based on recent accepted movement.

### Average speed

Based on:

```text
distance / moving time
```

---

# 24. PACE

Walking pace is expressed as:

```text
minutes / kilometer
```

Example:

> 10:24 min/km

Waylo should display both:

* Current pace
* Average pace

when sufficient GPS data exists.

---

# 25. CALORIES

Waylo provides an **estimated calorie value**, not a medically precise measurement.

The calculation should consider available user information and activity data.

The UI should label this appropriately as an estimate.

---

# 26. MAP SYSTEM

Waylo should use:

### Map data

OpenStreetMap

### Android map renderer

MapLibre

The map system should support offline usage.

---

# 27. OFFLINE MAPS

Users should be able to download map regions before walking.

Example:

```text
Offline Maps

Bangladesh
 └── Rajshahi Division
      └── Bogura Region
           └── Download
```

The exact map download architecture will be defined during technical implementation.

---

# 28. ROUTE VISUALIZATION

The user's GPS route should be rendered above the map.

Waylo's route style:

* Gradient
* Glowing
* Rounded
* Smooth
* Clearly visible

Primary gradient:

**Cyan → Blue → Violet**

The route should remain readable over different map backgrounds.

---

# 29. OFFLINE WALKING

An active walk should work without internet where possible.

Offline operation must support:

* Step counting
* GPS tracking
* Distance
* Time
* Pace
* Speed
* Calories estimate
* Local activity storage
* Offline map display

Internet should not be required merely to record a walk.

---

# 30. XP SYSTEM

Waylo uses XP as the primary progression currency.

XP can be earned from:

* Completing daily goals
* Walking distance
* Completing activities
* Achievements
* Special milestones
* Exploration

The exact XP formulas should be defined as implementation constants/configuration rather than hardcoded throughout the application.

---

# 31. LEVEL SYSTEM

Users progress through levels based on XP.

Example:

```text
Level 1
↓
Level 2
↓
Level 3
↓
...
```

The exact XP curve should be configurable.

Level-up should trigger:

* Animation
* Mascot reaction
* Reward presentation

---

# 32. STREAK SYSTEM

A streak represents consecutive days in which the user's configured daily walking goal was completed.

Example:

```text
🔥 7 day streak
```

Waylo should distinguish between:

* Current streak
* Longest streak

Missing a day should not delete historical statistics.

---

# 33. ACHIEVEMENTS

Initial achievement categories:

### Distance

* First kilometer
* 5 km
* 10 km
* 50 km
* 100 km

### Steps

* First 1,000
* 10,000
* 50,000
* 100,000
* 1,000,000

### Activities

* First walk
* 10 walks
* 50 walks
* 100 walks

### Streaks

* 3 days
* 7 days
* 14 days
* 30 days
* 100 days

### Exploration

* First discovery
* First destination
* Exploration milestones

---

# 34. REWARD SYSTEM

Rewards may include:

* XP
* Achievement badges
* Mascot accessories
* Adventure locations
* Cosmetic effects

Rewards should primarily be cosmetic or progression-based.

The initial version does not require real-money rewards.

---

# 35. ADVENTURE SYSTEM

The adventure system converts real-world walking into virtual progression.

Example:

```text
User walks 1 km
        ↓
Adventure progress +1 km
        ↓
Virtual journey advances
        ↓
New location becomes available
```

The adventure system should remain independent from GPS location.

The user's actual geographic coordinates should not need to correspond to the virtual adventure world.

---

# 36. DATA STORAGE

Waylo should use a local database.

Recommended:

**Room**

Core entities include:

* Activity
* RoutePoint
* DailyProgress
* UserProgress
* Achievement
* Streak
* AdventureProgress
* MascotCustomization
* Settings
* OfflineMap metadata

The final schema will be defined in the technical architecture section.

---

# 37. BACKGROUND TRACKING

During an active walk, Waylo must support screen-off/background operation.

The initial architecture should use an Android foreground service for active workout tracking.

The service should:

* Continue receiving location updates
* Maintain activity state
* Update the activity record
* Communicate with the UI
* Provide an ongoing notification
* Stop cleanly when the activity ends

---

# 38. BATTERY

Battery consumption is an important requirement.

Waylo should avoid unnecessarily aggressive GPS polling.

The implementation should balance:

**Accuracy ↔ battery consumption**

GPS configuration should be configurable and tested on real devices.

---

# 39. PRIVACY

Waylo should clearly explain why it requests:

* Location
* Background location where required
* Physical activity recognition
* Notifications where required

The app should request permissions contextually rather than requesting every permission immediately without explanation.

---

# 40. SOCIAL FEATURES

The initial release intentionally excludes:

* Followers
* Following
* Likes
* Comments
* Public profiles
* Social feeds
* Messaging
* Friend competitions

These are outside the initial Waylo scope.

---

# 41. CLOUD BACKEND

The initial version does not require:

* Dedicated server
* User accounts
* Cloud database
* Cloud synchronization
* Social backend

Waylo should be designed so cloud functionality can potentially be added later without rewriting the core walking engine.

---

# 42. TECHNOLOGY STACK

Initial recommended stack:

### Language

Kotlin

### UI

Jetpack Compose

### Architecture

Layered architecture with clear separation between:

* UI
* Domain
* Data
* Platform services

### Database

Room

### Background work

Foreground Service for active walks
WorkManager for suitable deferred/background tasks

### Location

Android location APIs

### Sensors

Android Sensor APIs

### Maps

MapLibre + OpenStreetMap-compatible map data

### Build

Gradle

### CI

GitHub Actions

---

# 43. ARCHITECTURAL PRINCIPLE

The walking engine must not be tightly coupled to the UI.

For example:

```text
GPS
 ↓
Location Provider
 ↓
Walking Engine
 ↓
Activity State
 ↓
 ├── UI
 ├── Database
 ├── XP Engine
 └── Statistics
```

This makes the system easier to test and maintain.

---

# 44. FIRST RELEASE SCOPE

The first usable Waylo version should prioritize:

### Required

* Onboarding
* Permissions
* Step tracking
* Daily goal
* GPS walking
* Distance
* Time
* Pace
* Speed
* Calories estimate
* Live map
* Route visualization
* Finished walk
* Activity history
* Local database
* Background tracking
* XP
* Levels
* Streaks
* Basic achievements
* Basic fox mascot
* Offline-capable walking

### Later

* Full mascot customization
* Large adventure world
* Extensive cosmetic rewards
* Advanced offline map management
* Social features
* Cloud sync
* Wearables
* AI coaching

---

# 45. DEVELOPMENT PRINCIPLE

Waylo must be developed incrementally.

The AI coding agent must not implement the entire application in a single uncontrolled operation.

Each development phase must contain:

1. Requirements
2. Files/modules affected
3. Implementation tasks
4. Database changes
5. UI changes
6. Tests
7. Acceptance criteria
8. Build verification
9. Device-testing instructions

A phase is considered complete only after its acceptance criteria are satisfied.

---

# 46. PROPOSED IMPLEMENTATION PHASES

## Phase 1

Project foundation + architecture + design system

## Phase 2

Onboarding + permissions

## Phase 3

Step tracking + daily goals

## Phase 4

GPS walking engine

## Phase 5

MapLibre + live map + route

## Phase 6

Workout statistics + calculations

## Phase 7

Finished activity + history

## Phase 8

XP + levels + streaks

## Phase 9

Mascot system

## Phase 10

Achievements + rewards

## Phase 11

Adventure system

## Phase 12

Offline maps

## Phase 13

Performance + battery + accessibility + polish

## Phase 14

Full testing + release preparation

---

# 47. SUCCESS CRITERIA

A successful Waylo v1 should allow a user to:

1. Install Waylo.
2. Complete onboarding.
3. Grant required permissions.
4. Set a walking goal.
5. See their current step progress.
6. Start a walk.
7. See themselves on an interactive map.
8. Record a GPS route.
9. Track steps during the walk.
10. See distance and time.
11. See pace and speed.
12. Receive an estimated calorie value.
13. Pause/resume the walk.
14. Finish the walk.
15. See a polished activity result.
16. See their route visualization.
17. Earn XP.
18. Progress toward the next level.
19. Maintain a streak.
20. Unlock achievements.
21. See their previous activities.
22. Continue tracking with the screen locked.
23. Record a walk without an internet connection when required offline resources are available.

---

# 48. CORE WAYLO EXPERIENCE

The final experience should feel like:

> **A walking tracker disguised as an adventure game.**

The user should think:

> "I want to go for a walk because I want to progress in Waylo."

rather than:

> "I need to open Waylo because I need to record my steps."

That distinction is central to the product.

---

# 49. PRODUCT MANTRA

## WALK.

## EXPLORE.

## PROGRESS.

**Waylo — Make every walk an adventure.**

---

# 50. OUT OF SCOPE FOR THE INITIAL BUILD

The following should not be added without explicitly updating this PRD:

* Social network
* Public activity feed
* Followers
* Messaging
* Real-money rewards
* Advertising system
* Wearable integrations
* Smartwatch application
* iOS application
* Web application
* AI coaching
* Medical/health diagnosis
* Cloud account requirement
* Complex multiplayer
* Live friend tracking

This prevents scope creep during AI-assisted development.
