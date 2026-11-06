# Motion.Pulse

**Build your rhythm.**

Motion.Pulse is a native Android habit and mood tracking application designed to help users build consistent routines, understand the relationship between their habits and emotional well-being, and stay motivated alongside friends. 

Built with a focus on offline-first reliability, native performance, and a seamless Material 3 user experience, the app combines local data persistence with cloud synchronization to provide real-time community features, gamified progress, and personal insights.

## 🚀 Features

### Authentication & Security
- **Flexible Sign-In:** Supports traditional Email/Password authentication alongside Google Sign-In via the Credential Manager API.
- **Session Management:** Automatically checks for an active session on launch with a "Skip" option for offline or guest usage.
- **Account Management:** Comprehensive flows for password recovery, profile name changes, and secure account deletion.

### Dashboard (Home)
- **Daily Progress:** A custom Canvas-rendered progress ring dynamically tracks and displays the percentage of habits completed for the current day.
- **Motivation:** Displays a daily motivational quote and personalized user welcome based on the current time and logged mood.
- **Quick Actions:** Direct access to today's scheduled habits, complete with active streak indicators and completion percentages.

### Mood Tracking
- **Rhythm Scale:** Users log their state on a five-level scale (Drained, Low, Steady, Good, Energized).
- **Contextual Tagging:** Associates moods with specific factors like Sleep, Work, Health, Social, and Weather.
- **Journaling:** Allows users to attach optional notes to their daily mood entries.

### Habit Management
- **Flexible Scheduling:** Habits can be configured with custom categories, reminder times, and goal types (Yes/No completion or Numeric Targets).
- **Search & Filtering:** A real-time search and category filter system (Mind & Focus, Fitness & Health, Daily Routine) allows users to quickly find specific habits.
- **Habit Strength:** Calculates a completion percentage (Habit Strength) for each habit based on historical consistency.

### Pulse Community
- **Activity Feed:** A real-time feed displaying milestones, completed habits, and streaks from accepted friends.
- **Leaderboards:** Competitive rankings by Weekly XP, Current Streak, or Level.
- **Duels:** Allows users to challenge friends to a 7-day head-to-head consistency race, with live score tracking.
- **Friend System:** Secure friend connections using unique, generated personal codes (e.g., `MP-XXXX-XXXX`) with built-in rate-limiting and blocking features.

### Stats ("Your Rhythm")
- **Weekly Overview:** Displays aggregated data including habits done today, current streak, and total completed habits vs. the previous week.
- **Custom Data Visualization:** Utilizes custom Android Canvas graphics (`MoodHabitsGraphCardView`) to render side-by-side correlations between mood fluctuations and habit completion without relying on heavy third-party charting libraries.
- **Insights:** Automatically calculates and highlights the user's best day of the week based on completion rates.

### Profile & Data
- **Preferences:** Toggles for habit reminders, sharing milestones with friends, and hiding habit names in public posts.
- **Localization:** Full UI translation support for English, Afrikaans, Setswana, isiZulu, and Xitsonga.
- **Data Portability:** One-tap export of all local habit data to CSV format.
- **Linked Accounts:** Management interface for linking or unlinking Google and Email providers.

---

## 🏗 Architecture & Tech Stack

Motion.Pulse is built using **Modern Android Development (MAD)** principles, favoring native components and reactive streams over third-party abstractions.

### Core Stack
- **Language:** Kotlin (2.1.0)
- **UI Framework:** XML Views + ViewBinding + Material 3 Components (No Jetpack Compose)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **State Management:** Kotlin Coroutines, `StateFlow`, and `SharedFlow`

### Local & Cloud Data
- **Local Database:** Room DB (Single Source of Truth)
- **Cloud Database:** Firebase Cloud Firestore
- **Offline Sync:** WorkManager handles background synchronization, ensuring data integrity even when the device is offline.
- **Data Contracts:** Kotlinx Serialization for parsing and mapping complex data structures.

### Custom Rendering & Media
- **Charts & Graphics:** Custom Android Canvas implementations (`MoodHabitsGraphCardView`, `DashboardProgressRingView`) for pixel-perfect, highly performant rendering.
- **Media Playback:** AndroidX Media3 ExoPlayer for audio/video integrations.

### Environment
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 36 (Android 15)
- **Compile SDK:** API 37
- **IDE:** Android Studio (Otter 2025.2.3 or newer)

---

## 📂 Project Structure

The codebase is organized by feature and layer, adhering to Clean Architecture principles:

```text
com.example.motionpulse
│
├── MainActivity.kt                            # Main Navigation Host Activity
│
├── data
│   ├── export
│   │   └── CsvExportManager.kt               # Exports local Room data to CSV files
│   │
│   ├── local
│   │   ├── AppDatabase.kt                    # Room Database initialization & migrations
│   │   ├── Converters.kt                     # Room TypeConverters (Date, List, Map, Instant)
│   │   ├── dao                               # Data Access Objects for Room Queries
│   │   │   ├── BadgeDao.kt
│   │   │   ├── FriendDao.kt
│   │   │   ├── HabitCompletionDao.kt
│   │   │   ├── HabitDao.kt
│   │   │   ├── MoodDao.kt
│   │   │   └── UserProfileDao.kt
│   │   └── entity                            # Local Room Entities
│   │       ├── ActivityFeedEntry.kt
│   │       ├── BadgeEntity.kt
│   │       ├── Challenge.kt
│   │       ├── CommunityNotification.kt
│   │       ├── Duel.kt
│   │       ├── FriendEntity.kt
│   │       ├── HabitCompletionEntity.kt
│   │       ├── HabitEntity.kt
│   │       ├── MoodEntity.kt
│   │       ├── PublicProfileEntity.kt
│   │       └── UserProfileEntity.kt
│   │
│   ├── repository                            # Data Repositories (Room + Firestore)
│   │   ├── AuthRepository.kt                 # Auth & Profile Firestore sync
│   │   ├── CommunityPreferences.kt          # Shared Preferences for tips/nudges
│   │   ├── CommunityRepository.kt           # Feed, Leaderboards, Duels, Friends Firestore API
│   │   ├── FriendCodeDataSource.kt          # Friend Code lookups & batch mapping
│   │   ├── GoogleAuthUiClient.kt            # Google Credential Manager integration
│   │   ├── HabitRepository.kt               # Habit & Completion Firestore sync
│   │   ├── MoodRepository.kt                # Mood check-in Firestore sync
│   │   ├── PreferenceRepository.kt          # User settings preferences
│   │   └── SettingsRepository.kt            # Offline/online settings sync
│   │
│   └── sync
│       └── HabitSyncWorker.kt                # WorkManager background sync worker
│
├── domain
│   ├── auth                                  # Authentication State & ViewModel
│   │   ├── AuthState.kt
│   │   ├── AuthViewModel.kt
│   │   └── UserData.kt
│   │
│   ├── models                                # Pure Kotlin Domain Models
│   │   ├── FrequencyConfig.kt
│   │   ├── FriendCodeUiState.kt
│   │   ├── FriendEligibility.kt
│   │   ├── FriendLookupResult.kt
│   │   ├── FriendRelationshipState.kt
│   │   ├── FriendRequestState.kt
│   │   ├── ReminderScheduler.kt
│   │   ├── UiState.kt
│   │   ├── UserSettings.kt
│   │   └── WeeklyRhythmModels.kt
│   │
│   ├── scoring                               # Business Rules & Gamification Algorithms
│   │   ├── GamificationEngine.kt            # XP, Level & Badge calculation
│   │   ├── HabitScoreCalculator.kt          # Habit strength score calculation
│   │   ├── MoodHabitInsightGenerator.kt     # Mood & Habit correlation insights
│   │   └── StreakCalculator.kt              # Current & Longest streak calculation
│   │
│   ├── stats                                 # Statistical Calculations
│   │   ├── StatsCalculations.kt
│   │   └── WeeklyCorrelationData.kt
│   │
│   └── usecase                               # Domain Use Cases
│       └── CompleteHabitUseCase.kt          # Central UseCase for Habit Completion
│
├── ui
│   ├── adapters                              # RecyclerView Adapters
│   │   ├── BadgesAdapter.kt
│   │   ├── CommunityFeedAdapter.kt
│   │   ├── CompletionHistoryAdapter.kt
│   │   ├── DuelsAdapter.kt
│   │   ├── HabitsAdapter.kt
│   │   ├── IncomingRequestsAdapter.kt
│   │   ├── LeaderboardAdapter.kt
│   │   ├── LinkedAccountsAdapter.kt
│   │   ├── ManageFriendsAdapter.kt
│   │   ├── NotificationsAdapter.kt
│   │   └── SentRequestsAdapter.kt
│   │
│   ├── dialogs                               # Bottom Sheets & Dialog Fragments
│   │   ├── AddFriendBottomSheetDialogFragment.kt
│   │   ├── AddHabitBottomSheetDialogFragment.kt
│   │   ├── ExportDataBottomSheetDialogFragment.kt
│   │   ├── LinkedAccountsBottomSheetDialogFragment.kt
│   │   ├── ManageFriendsBottomSheetDialogFragment.kt
│   │   └── NotificationsBottomSheetDialogFragment.kt
│   │
│   ├── fragments                             # Screen UI Fragments (XML ViewBinding)
│   │   ├── BadgesGalleryFragment.kt
│   │   ├── CommunityFeedFragment.kt
│   │   ├── CommunityFragment.kt
│   │   ├── DashboardFragment.kt             # Home / Dashboard
│   │   ├── DuelsFragment.kt
│   │   ├── EmailVerificationFragment.kt
│   │   ├── HabitDetailFragment.kt
│   │   ├── HabitsFragment.kt                # Habits List
│   │   ├── LandingFragment.kt
│   │   ├── LeaderboardFragment.kt
│   │   ├── LoginFragment.kt
│   │   ├── MoodFragment.kt                 # Mood Check-in
│   │   ├── OnboardingFragment.kt
│   │   ├── ProfileFragment.kt              # Profile & Settings
│   │   ├── RegisterFragment.kt
│   │   └── StatsFragment.kt                # Stats & Insights
│   │
│   ├── notifications                         # AlarmManager & FCM Receivers
│   │   ├── HabitActionReceiver.kt
│   │   ├── HabitReminderReceiver.kt
│   │   ├── MotionPulseMessagingService.kt
│   │   └── ReminderManager.kt
│   │
│   ├── viewmodels                            # UI State ViewModels
│   │   ├── CommunityViewModel.kt
│   │   ├── HabitsViewModel.kt
│   │   ├── MoodViewModel.kt
│   │   ├── ProfileViewModel.kt
│   │   └── StatsViewModel.kt
│   │
│   └── views                                 # Custom Android Views
│       ├── DashboardProgressRingView.kt
│       ├── DropletHeaderView.kt
│       ├── MoodBarsView.kt
│       ├── MoodHabitsGraphCardView.kt
│       ├── MoodPillView.kt
│       ├── MotionPulseBottomNav.kt
│       ├── StateLayout.kt
│       └── UserAvatarView.kt
│
└── util                                      # Utilities & Helpers
    ├── AppConstants.kt
    ├── ChromeFontScaleHelper.kt
    ├── ConnectivityMonitor.kt
    ├── FeedTextFormatter.kt
    ├── FriendCodeUtil.kt
    ├── HabitStrengthFormatter.kt
    ├── HapticHelper.kt
    ├── LocaleHelper.kt
    ├── TimeAgoFormatter.kt
    └── ValidationUtils.kt
