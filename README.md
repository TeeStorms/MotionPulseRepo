Motion.Pulse

Build your rhythm.

Motion.Pulse is a mobile habit and mood tracking app that helps you build consistent routines, understand how your habits shape your mood, and stay motivated alongside friends through duels, challenges and leaderboards.

Version 1.0 · Android

Features
Authentication
Email and password registration and login
Continue with Google sign-in
Automatic session check on launch (with a Skip option)
Forgot password flow
Change profile name and password from the Profile screen
Home
Personalised welcome with your current mood
Daily progress ring showing habits completed today (e.g. 3/3)
Motivation of the day
Today's habits list with completion percentages
Mood Tracking
Log your mood on a five-level rhythm scale: Drained, Low, Steady, Good, Energized
Tag what is shaping your mood: Sleep, Work, Health, Social, Weather
Add an optional note
Update your mood any time during the day
Habits
Search and filter habits by category (Mind & Focus, Fitness & Health, Daily Routine, and more)
Add new habits with:
Category
Name
Goal type: Yes / No or Numeric Target
Reminder time
Per-habit completion percentage
Share habits with friends
Pulse Community
Feed: milestones and streaks from your friends
Leaderboard: rank by Weekly XP, Streak or Level
Duels: challenge a friend to a 7-day head-to-head consistency race
Group Challenges: join community events such as the Global Consistency Marathon
Add friends using a personal friend code
Stats ("Your Rhythm")
Weekly navigation (e.g. 2 Nov - 8 Nov)
Habits done today, current streak, total completed and weekly progress vs last week
Mood and habits side-by-side chart
Insights and best day of the week
Profile
Day streak and badges
Recent badges
Shareable friend code (copy, share, reset)
Preferences:
Habit reminders
Share milestones with friends
Show habit names in posts
Language selection: English, Afrikaans, Setswana, isiZulu, Xitsonga
Manage linked accounts
Export your data as CSV
Log out and delete account
Screens
Screen	Description
Splash	Branded launch screen with session check
Login / Register	Email or Google sign-in
Home	Daily progress and today's habits
Mood	Rhythm bars, mood factors and notes
Habits	Search, filter and add habits
Community	Feed, leaderboard and duels
Stats	Weekly insights and mood/habit correlation
Profile	Account, preferences and data tools
Getting Started
Prerequisites
Android device or emulator
(Add your tooling here, e.g. Android Studio, Flutter SDK, Node.js)
Installation
bash
# Clone the repository
git clone https://github.com/TeeStorms/MotionPulseRepo/new/main?filename=README.md)
# Video Link
https://www.dropbox.com/scl/fi/smfd1vrp9rxvbtd4qttif/WhatsApp-Video-2026-11-05-at-21.53.57.mp4?rlkey=1gao8wzttumh2lwlhpalzi6er&st=hru3ey3w&dl=0

# Install dependencies
# (replace with your project's command)
Run the app
bash
# (replace with your project's run command)
Configuration

If the app uses Google sign-in or a backend, add your keys or config files here (for example google-services.json or a .env file). Do not commit secrets.

Tech Stack

Language: Kotlin (2.1.0)
IDE: Android Studio
Database: Room DB (2.8.4 - local) + Firebase Cloud Firestore (34.18.0 - cloud)
Authentication: Firebase Authentication + Google Credential Manager API (1.3.0)
UI / Layouts: XML Views + ViewBinding + Material 3 Components (1.14.0) (No Jetpack Compose)
Charts & Custom Views: Custom Android Canvas Graphics (MoodHabitsGraphCardView, DashboardProgressRingView)
Async & Offline Sync: Kotlin Coroutines + StateFlow / SharedFlow + WorkManager (2.11.2)
Media / Audio: AndroidX Media3 ExoPlayer (1.5.1)
Cloud Messaging: Firebase Cloud Messaging (FCM)
Architecture: MVVM (ViewModel + StateFlow + Repository + Room Single Source of Truth)
Min SDK: API 26 (Android 8.0)
Target SDK: API 36 (Android 15)
Compile SDK: API 37
Key Differences vs. Your Template String:
1.
Min SDK: Your template listed API 24 (Android 7.0). In  app/build.gradle.kts, the actual project setting is minSdk = 26 (Android 8.0).
2.
Target SDK: targetSdk = 36 (Android 15) is accurate, and compileSdk = 37.
3.
Charts: Your template mentioned MPAndroidChart. Motion.Pulse uses lightweight, native Custom Canvas Graphics (MoodHabitsGraphCardView and DashboardProgressRingView) for pixel-perfect rendering without 3rd-party charting library overhead.
4.
AI/ML: Your template mentioned Google ML Kit Text Recognition. Motion.Pulse does not use ML Kit; instead, it uses Media3 ExoPlayer for media playback and Kotlinx Serialization for data contracts.
5.
Architecture: Motion.Pulse uses MVVM with Kotlin Coroutines StateFlow & SharedFlow (modern reactive streams) instead of legacy LiveData.)

Framework:
Language:
Backend / Database:
Authentication: Email/password and Google Sign-In
Notifications: Local habit reminders
Roadmap
 Badge and achievement system
 Habit streak recovery
 More languages
 iOS support
Contributing

Contributions are welcome. Fork the repo, create a feature branch, and open a pull request.

License

Add your license here (e.g. MIT).

Zandile Selao 
Ndabezinhle Mathunyane 
Matlhogonolo Keebine 
Tshiamo Mosetlha 
Muhluri Nkuna 
