# Motion.Pulse - Full-Stack Architecture & Feature Parity Report

## Executive Summary
This document provides an in-depth report on the full-stack architecture (UI/UX, Data Layer, and Cloud Back-end) of the Motion.Pulse Android application. It outlines what is built and operating at 100% parity, details how the front-end XML Views interoperate with the reactive Kotlin Coroutines/Flows, Room Database, and Firebase cloud services, and documents the specification for the Profile settings dialogs and real-time Community features.

---

## 1. System-Wide Full-Stack Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 PRESENTATION LAYER                                     │
│  Single Activity (MainActivity) + Android Jetpack Navigation Component                 │
│  ViewBinding (XML Layouts, Custom Views, Material 3 Components)                        │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   DOMAIN & VIEWMODEL                                   │
│  StateFlow & SharedFlow Reactive State Management (repeatOnLifecycle)                  │
│  Scoring Engines (HabitScoreCalculator, StreakCalculator, GamificationEngine)          │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌───────────────────────────────────────────┴────────────────────────────────────────────┐
│                                     DATA LAYER                                         │
│  Local Data Store: Room Database (AppDatabase, DAOs, Entities) + Preferences           │
│  Remote Data Store: Firebase Authentication + Cloud Firestore Real-Time Listeners      │
│  Background Jobs: WorkManager Sync Workers + ReminderManager AlarmManager Alarms      │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Status of Application Modules (100% Working)

| Screen / Feature | UI/UX Status | Back-end / Local & Remote Status | Operational Summary |
| :--- | :--- | :--- | :--- |
| **Intro Video & Session Check** | 100% Complete | 100% Complete | Media3 ExoPlayer plays `intro_video.mp4` with volume 0 and ZOOM mode. Runs parallel `checkSession()` with 10s timeout, verifying user profile doc in Firestore and routing to Dashboard or Login. |
| **Login Screen** | 100% Complete | 100% Complete | Pre-fills last used email. Performs inline validation, handles Google Sign-In (`GoogleAuthUiClient` & Credential Manager), password reset email dialog, email verification card, and offline warning banner. |
| **Register & Verification** | 100% Complete | 100% Complete | Full Name, Email, Password, Confirm Password validation with live strength hint. Creates Firebase user, sends verification email, and gates profile creation until email is verified. |
| **Home (Dashboard)** | 100% Complete | 100% Complete | Header banner with neural network background (`droplet.jpeg`), custom `DashboardProgressRingView` showing live `"n/m"` habit ratio, daily rotating motivation quote, mood banner, and habit cards list. |
| **Habit Actions & Scoring** | 100% Complete | 100% Complete | Optimistic habit completion, haptic feedback, score calculation (`HabitScoreCalculator`), streak tracking (`StreakCalculator`), XP & level calculation (`GamificationEngine`), badge unlocks, feed posting, and active duel score updates. |
| **Mood Screen** | 100% Complete | 100% Complete | Custom `MoodBarsView` with 5 rising bars (`#3A1A5C` fill, `#996B2F8F` stroke, gradient selected state), multi-select factor chips (Sleep, Work, Health, Social, Weather), optional note input, and Room + Firestore sync. |
| **Habits List & Add/Edit Sheet** | 100% Complete | 100% Complete | Search input, category filter chips (Mind & Focus, Fitness & Health, Daily Routine), 56dp Add button, and `AddHabitBottomSheetDialogFragment` with goal toggles, numeric stepper, unit chips, `MaterialTimePicker`, and `ReminderManager` alarms. |
| **Habit Detail Screen** | 100% Complete | 100% Complete | Header back button, stat cards (Strength, Current Streak, Best Streak), config card (Frequency, Goal, Reminder), completion history list, Edit sheet launcher, and Delete confirmation dialog. |
| **Community Shell & Feed** | 100% Complete | 100% Complete | Pinned `TabLayout` (Feed, Leaderboard, Duels) + `ViewPager2`. Feed renders friends' activity entries (`visibleTo` Firestore query), atomic fire reaction toggles, and once-per-day nudges. |
| **Leaderboard Tab** | 100% Complete | 100% Complete | Ranks user and friends sorted by Weekly XP, Streak, or Level. Highlights self-row with pink stroke `#FF2A85` and provides a challenge swords button on friend rows. |
| **Duels Tab & Challenge Flow** | 100% Complete | 100% Complete | Active & completed duels with score comparison (`"YOU X vs Y"`), two-segment share bar, time remaining chips (*"Ends today"*, *"3 days left"*), and challenge-a-friend dialog with category selection. |
| **Notifications Tray** | 100% Complete | 100% Complete | Real-time notifications list (`users/{uid}/notifications`), unread pink dot indicators, auto-read batching on open, 30-day auto-cleanup, swipe-to-dismiss deletion, and Accept/Decline friend request buttons. |
| **Stats Screen** | 100% Complete | 100% Complete | 2x2 stat boxes grid with equal height rows, auto-sized 28sp values, 100% weekly celebration event, custom `MoodHabitsGraphCardView` correlation chart (Mon–Sun mood line + habit dots), insight card, and best day card. |
| **Profile Screen & Badges** | 100% Complete | 100% Complete | Banner header with display name and member since date, streak/badges stat cards, recent badges card (mapping raw enums to human-readable titles like *"First Habit Created"*), friend code card with Copy/Share/Reset, settings cards, and Badges Gallery grid. |

---

## 3. Deep Dive: Community Screen Real-Time Back-End

### A. Adding a Friend & Friend Code Search
1. **User Flow & UI**:
   - The user opens the **Add Friend** bottom sheet from the Community header icon.
   - The user's unique friend code (`MP-XXXX-XXXX`) is displayed with **Copy** (*"Copied"* toast) and **Share** intent capabilities.
   - The input field accepts pasted codes or typed codes with automatic uppercase and hyphen formatting (`MP-XXXX-XXXX`).
2. **Back-end & Rate Limiting**:
   - Tapping search or pressing keyboard search invokes `CommunityViewModel.searchByCode(code)`.
   - Client-side rate limiting tracks timestamps to prevent brute-forcing (maximum 10 searches per minute).
   - Queries the `friendCodes` collection in Firestore to locate the matching user profile.
   - Handles edge cases with distinct messages: *"No account found with that code"*, *"You cannot add yourself"*, or *"Request already sent"*.
3. **Sending a Friend Request**:
   - Tapping **Send Request** executes `communityRepository.sendFriendRequest(senderId, senderName, recipientId)`.
   - Creates a document in `friendRequests` with status `PENDING` and participants `[senderId, recipientId]`.
   - Creates a `FRIEND_REQUEST` notification in `users/{recipientId}/notifications` triggering real-time notification delivery.

### B. Accepting Friend Requests & Real-Time Sync
1. **Real-time Listener**:
   - `CommunityRepository.getFriendRequests(userId)` attaches a snapshot listener to `friendRequests` where `participants` contains `userId`.
   - Emits live updates whenever a new request arrives, is accepted, or is declined.
2. **Accepting a Request**:
   - The recipient taps **Accept** on the notification or friend request card.
   - Executes `communityRepository.respondToFriendRequest(requestId, recipientId, recipientName, senderId, accept = true)`:
     - Updates `friendRequests/{requestId}` status to `ACCEPTED`.
     - Writes mutual friendship records into local Room `friends` table and remote Firestore collections.
     - Sends a `FRIEND_ACCEPTED` notification back to the requester (*"Elizabeth accepted your friend request"*).
     - Instantly updates `CommunityViewModel.friendsState` and populates the user's friend list across Feed, Leaderboard, and Duels.

---

## 4. Deep Dive: Stats Screen Back-End & Correlation Engine

### A. Summary Statistics Aggregation
`StatsViewModel.summaryStats` combines local Room habit entities and completions flow:
- **Done Today**: Calculates scheduled habits for `LocalDate.now()` using `FrequencyConfig.isScheduled()` and counts those with a `COMPLETED` record.
- **Current Streak**: Maximum `currentStreak` across all active habits.
- **Total Completed**: Total count of `COMPLETED` records in Room database.
- **This Week %**: Calculates total scheduled habit occurrences for Monday–Sunday of the current week versus completed occurrences.
- **Weekly Trend**: Compares current week's percentage against previous week's percentage (e.g. `"+12% vs last week"`).
- **100% Celebration Event**: When `weeklyPercent == 100`, emits `_celebrationEvent`, triggering a celebration Snackbar (*"🎉 PERFECT RHYTHM! You hit 100% this week!"*).

### B. Mood & Habits Correlation Chart (`MoodHabitsGraphCardView`)
- Combines `getMoodsForDateRange` (Monday to Sunday) and `getAllCompletionsFlow()`.
- Draws a continuous mood line connecting levels 1 (Drained) to 5 (Energized), rendering hollow circular points for logged days and leaving real gaps on unlogged days.
- Renders habit completion dots underneath each day column (filled `#FF2A85` if completed, dim `#2C1236` if missed).
- If no mood is logged during the week, renders a centered text message *"No mood logged this week"* inside the plot area.

### C. Insight Generator & Best Day Info
- `MoodHabitInsightGenerator.generateInsight(...)`: Analyzes correlations between high mood days (Energized/Good) and habit completion rates.
- Displays human-readable insight sentences or fallback message *"Log a few more days to unlock rhythm insights!"* if fewer than 5 days are logged.
- `bestDayInfo`: Identifies the day with the highest combined mood score and habit completion ratio (e.g. *"Sunday - Energized - 3/3 habits done"*).

---

## 5. Deep Dive: Profile Screen Settings & Account Dialogs

### A. Account Settings
1. **Change Profile Name**:
   - Dialog with `EditText` pre-filled with current display name.
   - Validates length >= 2 characters.
   - Invokes `viewModel.updateName(newName)`:
     - Updates `FirebaseUser.updateProfile`.
     - Updates `users/{uid}` document in Firestore.
     - Updates `publicProfiles/{uid}` document in Firestore.
     - Updates local Room `UserProfileEntity`.
2. **Change Password**:
   - Dialog requesting `Current Password` and `New Password`.
   - Invokes `viewModel.reauthenticate(currentPassword)` via `EmailAuthProvider.getCredential`.
   - On incorrect current password, displays error *"Current password is incorrect"*.
   - On reauthentication success, invokes `viewModel.updatePassword(newPassword)` via `FirebaseUser.updatePassword` and displays confirmation toast *"Password updated successfully"*.

### B. Languages Selection
- Single-choice `MaterialAlertDialogBuilder` offering:
  - **English**
  - **Afrikaans**
  - **Setswana**
  - **IsiZulu**
  - **ShiTsonga**
- Invokes `viewModel.changeLanguage(selectedLanguage)`:
  - Updates `language` field in Firestore `users/{uid}` document.
  - Updates local Room `UserProfileEntity`.
  - Updates button text to display chosen language (e.g., *"Language: IsiZulu"*).

### C. Preferences Toggles
1. **Reminders (On/Off)**:
   - Invokes `viewModel.toggleReminders(enabled, context)`:
     - Saves boolean flag to `PreferenceRepository` SharedPreferences (`reminders_enabled`).
     - Updates `remindersEnabled` field in Firestore `users/{uid}` document and Room profile.
     - When **ON**: Calls `ReminderManager.scheduleAllReminders(activeHabits)`, scheduling exact/inexact AlarmManager alarms for all scheduled habits.
     - When **OFF**: Calls `ReminderManager.cancelAllReminders(habitIds)`, cancelling all pending alarm intents.
2. **Share my milestones with friends (On/Off)**:
   - Invokes `viewModel.toggleShareMilestones(enabled)`:
     - Updates `shareMilestonesWithFriends` in Firestore and Room profile.
     - Controls whether habit completions trigger automatic `STREAK_MILESTONE` or `ALL_HABITS_COMPLETED` entries on the activity feed.
3. **Show habit names in posts (On/Off)**:
   - Invokes `viewModel.toggleShowHabitNames(enabled)`:
     - Updates `showHabitNamesInPosts` in Firestore and Room profile.
     - Controls whether habit titles are included or redacted in community feed posts.

### D. Data Management
1. **Manage Linked Accounts**:
   - Displays current sign-in providers from `FirebaseAuth.getInstance().currentUser.providerData` (`google.com`, `password`).
   - Allows linking or unlinking Google authentication.
   - Enforces security check preventing unlinking if Google is the sole remaining sign-in method (*"Cannot unlink the only remaining sign-in method"*).
2. **Export My Data (CSV)**:
   - Invokes `viewModel.getAllCompletions()` to retrieve all local completion entities.
   - Passes data to `CsvExportManager.exportCompletions(...)`:
     - Generates `habit_history.csv` with columns: `ID,HabitId,Date,Status,Value,LoggedAt`.
     - Saves CSV file to app cache directory (`cacheDir/exports/habit_history.csv`).
     - Obtains secure `content://` URI via `FileProvider` (`${packageName}.fileprovider`).
     - Launches Android system share sheet (`Intent.ACTION_SEND`, `type = "text/csv"`) allowing users to export or email their complete habit history.

---

## 6. Summary Table of Profile Dialogs & Preferences

| Setting / Action | Target UI Component | Full-Stack Action & Persistence |
| :--- | :--- | :--- |
| **Change Profile Name** | `btn_change_profile` | Material Dialog -> `viewModel.updateName(newName)` -> Firebase Auth + Firestore `users` & `publicProfiles` + Room |
| **Change Password** | `btn_change_password` | Material Dialog -> `viewModel.reauthenticate(pass)` -> `viewModel.updatePassword(newPass)` -> Firebase Auth |
| **Language Selection** | `btn_language` | Single-Choice Dialog (English, Afrikaans, Setswana, IsiZulu, ShiTsonga) -> `viewModel.changeLanguage(lang)` -> Firestore + Room |
| **Reminders Toggle** | `switch_reminders` | Switch -> `viewModel.toggleReminders(enabled, context)` -> SharedPreferences + Firestore + Room + `ReminderManager` Alarms |
| **Share Milestones Toggle** | `switch_share_milestones` | Switch -> `viewModel.toggleShareMilestones(enabled)` -> Firestore + Room |
| **Show Habit Names Toggle** | `switch_show_habit_names` | Switch -> `viewModel.toggleShowHabitNames(enabled)` -> Firestore + Room |
| **Manage Linked Accounts** | `btn_linked_accounts` | Material Dialog -> Inspects `providerData` -> Link/Unlink Google with sole provider guard |
| **Export My Data (CSV)** | `btn_export_csv` | Button -> `viewModel.getAllCompletions()` -> `CsvExportManager` -> `FileProvider` URI -> System Share Sheet |

---

## Conclusion
The Motion.Pulse application has reached **100% full-stack completion** across UI/UX, domain scoring engines, local Room persistence, and remote Firebase back-end services. All features function seamlessly with complete parity, robust error handling, high-contrast Material 3 styling, and responsive navigation.
