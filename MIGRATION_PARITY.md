# MIGRATION PARITY AUDIT

This document tracks the feature parity audit for migrating Motion.Pulse from Jetpack Compose to native XML Views and Fragments.

---

## 1. Current Fragments Audit

| Fragment Name | Layout File | View Count (Approx) | Shows Content Besides Header? | ViewModel Used | Collects Flow? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `LandingFragment` | `fragment_landing.xml` | 3 | Yes (VideoView/ExoPlayer, Progress) | `AuthViewModel` | Yes (`authState`) |
| `OnboardingFragment` | `fragment_onboarding.xml` | 3 | Yes (Title, Subtitle, Get Started) | `AuthViewModel` | No |
| `LoginFragment` | `fragment_login.xml` | 8 | Yes (Logo, Subtitle, Email/Pass inputs, Log in, Forgot pass, Google, Footer) | `AuthViewModel` | Yes (`authState`) |
| `RegisterFragment` | `fragment_register.xml` | 10 | Yes (Logo, Subtitle, Section title, Name/Email/Pass/Confirm inputs, Register, Footer) | `AuthViewModel` | Yes (`authState`) |
| `EmailVerificationFragment` | `fragment_email_verification.xml` | 8 | Yes (Icon, Instructions, Email display, Card banner, Continue, Resend, Logout) | `AuthViewModel` | Yes (`authState`) |
| `DashboardFragment` | `fragment_dashboard.xml` | 7 | Yes (Sync warning, Motivation card, Daily quote, Habits RecyclerView) | `HabitsViewModel` & `AuthViewModel` | Yes (`authState`, `allHabits`) |
| `MoodFragment` | `fragment_mood.xml` | 6 | Yes (Mood pill, Note EditText, Save button) | `MoodViewModel` & `AuthViewModel` | Yes (`authState`) |
| `HabitsFragment` | `fragment_habits.xml` | 5 | Yes (Search EditText, Habits RecyclerView) | `HabitsViewModel` & `AuthViewModel` | Yes (`authState`, `allHabits`) |
| `HabitDetailFragment` | `fragment_habit_detail.xml` | 4 | Yes (Habit ID, history placeholder) | `HabitsViewModel` & `AuthViewModel` | No |
| `CommunityFragment` | `fragment_community.xml` | 3 | Yes (TabLayout, ViewPager2) | `CommunityViewModel` & `AuthViewModel` | No |
| `StatsFragment` | `fragment_stats.xml` | 4 | Yes (Weekly summary card, stats placeholder) | `StatsViewModel` & `AuthViewModel` | No |
| `ProfileFragment` | `fragment_profile.xml` | 4 | Yes (Badges Gallery button, Sign Out button) | `AuthViewModel` | No |
| `BadgesGalleryFragment` | `fragment_badges_gallery.xml` | 2 | Yes (Badges RecyclerView) | `ProfileViewModel` | No |

---

## 2. Data & Domain Layer Diff Status
- **Diff Stat (`git diff --stat pre-compose-removal HEAD -- app/src/main/java/com/example/motionpulse/data app/src/main/java/com/example/motionpulse/domain app/src/main/java/com/example/motionpulse/ui/viewmodels`)**: **0 files changed**.
- **Result**: The data layer, domain layer, and all ViewModels (`AuthViewModel`, `HabitsViewModel`, `MoodViewModel`, `CommunityViewModel`, `StatsViewModel`, `ProfileViewModel`), repositories (`AuthRepository`, `HabitRepository`, `CommunityRepository`, `MoodRepository`), and scoring engines are 100% intact and unchanged.

---

## 3. Screen Parity Checklists

### Intro
- [ ] ExoPlayer video playback (LandingFragment:onViewCreated)
- [ ] Network connectivity broadcast/callback listener (LandingFragment:setupConnectivity)
- [ ] Session validation & auth state routing (LandingFragment:authState collector)

### Login
- [x] Email & password validation (LoginFragment:btnLogin)
- [x] Firebase Auth login integration (AuthViewModel:login)
- [x] Password visibility toggle (TextInputLayout)
- [x] Forgot password dialog/action (LoginFragment:tvForgotPassword)
- [x] Google sign-in integration (LoginFragment:btnGoogle)
- [x] Offline mode banner / sync failure handling

### Register
- [x] Name, email, password, confirm password validation (RegisterFragment:btnRegister)
- [x] Firebase Auth registration & email verification trigger (AuthViewModel:register)
- [x] Password visibility toggle
- [x] Navigation back to login

### Email verification
- [x] Awaiting email verification state observation (EmailVerificationFragment:authState)
- [x] Resend verification email countdown timer & button (AuthViewModel:resendVerificationEmail)
- [x] Check verification status & continue on verify (AuthViewModel:checkVerificationStatus)
- [x] Logout / Back to Login

### Home (Dashboard)
- [ ] Greeting & user display name in DropletHeaderView (DashboardFragment:onViewCreated)
- [ ] Today's mood pill badge in header
- [ ] Local mode sync failure warning banner
- [ ] Motivation of the day quote & reflection note card (DashboardFragment:setupHabitsRecyclerView)
- [ ] Today's habits RecyclerView (HabitsAdapter, toggle completion, navigate to detail)
- [ ] Pull-to-refresh list refresh handler (SwipeRefreshLayout)

### Mood
- [ ] Mood level selector bars (Rising bars selector)
- [ ] Mood factors selection chips
- [ ] Reflection note text input
- [ ] Save rhythm button (MoodViewModel:logMood)
- [ ] Toast feedback on save success

### Habits
- [ ] Habits list RecyclerView with search filtering (HabitsFragment:setupHabitsRecyclerView)
- [ ] Category filter tags (All, Mind & Focus, Fitness and Health, Daily Routine)
- [ ] Swipe-to-dismiss actions (Edit / Add)
- [ ] Habit reordering (Move Up / Move Down)
- [ ] Archived habits section & unarchive action
- [ ] Floating Action Button (FAB) for adding new habit

### Add/Edit habit
- [ ] AddHabitOverlay bottom sheet / dialog
- [ ] Habit title, description, category, frequency configuration
- [ ] Save / Update habit validation & database persistence

### Habit detail
- [ ] Habit ID & detailed completion history
- [ ] Back button & system back press handling

### Community Feed
- [ ] Activity feed entries RecyclerView
- [ ] Reaction buttons (likes/emojis)
- [ ] Nudge friend action
- [ ] Pull-to-refresh & empty/loading/error states

### Leaderboard
- [ ] Leaderboard ranking list
- [ ] Metric selector chips (Streak, Completed, etc.)
- [ ] Challenge friend action

### Duels
- [ ] Active & past 1-on-1 habit duels list
- [ ] Duel progress tracking & score increment
- [ ] Resolve duel action

### Manage Friends
- [ ] Friends list RecyclerView
- [ ] Unfriend, mute, unmute, block actions

### Add Friend
- [ ] Friend code display & clipboard copy
- [ ] Search user by friend code
- [ ] Send / cancel friend request actions

### Notifications
- [ ] Notifications tray / list
- [ ] Mark as read & delete actions
- [ ] Friend request accept / decline buttons

### Stats
- [ ] Weekly summary stat boxes (Done Today, Current Streak, Total Completed, Weekly Percent)
- [ ] Mood & Habits weekly correlation graph card
- [ ] Correlation insights & best day card

### Profile
- [ ] User avatar with initials (`UserAvatarView`)
- [ ] Member since date formatting
- [ ] Stats cards grid
- [ ] See all badges navigation
- [ ] Change password / language dialogs
- [ ] Sign out / logout confirmation dialog
- [ ] Delete account dialog

### Profile dialogs
- [ ] Logout confirmation dialog
- [ ] Delete account confirmation dialog
- [ ] Language selection dialog
- [ ] Password change dialog

### Badges gallery
- [ ] Unlocked & locked badges grid RecyclerView (`BadgesGalleryFragment`)
- [ ] Total badges count display
- [ ] Back navigation

### Bottom nav
- [ ] 6 equal-weight tabs (`MotionPulseBottomNav`: Home, Mood, Habits, Community, Stats, Profile)
- [ ] Active tab raised gradient circle indicator
- [ ] Adaptive label visibility (hides unselected labels on narrow screens / font scale > 1.15x)
- [ ] NavController destination synchronization & click navigation
- [ ] Re-selecting current tab no-op behavior

---

## 4. Shared Pieces Status
| Shared Component / Asset | File Path | Operational Status | Missing / Remaining Work |
| :--- | :--- | :--- | :--- |
| `DropletHeaderView` | `ui/views/DropletHeaderView.kt` | Working | Fully wired with title, caption, nav icon, and top actions slots |
| `UserAvatarView` | `ui/views/UserAvatarView.kt` | Working | Renders initials with gradient background & status dot |
| `MoodPillView` | `ui/views/MoodPillView.kt` | Working | Renders mood pill with per-mood color dot |
| `StateLayout` | `ui/views/StateLayout.kt` | Working | Loading, empty, and error states with equal minimum heights |
| `MotionPulseBottomNav` | `ui/views/MotionPulseBottomNav.kt` | Working | 6 tabs, active circle, NavController sync |
| Navigation Graph | `res/navigation/nav_graph.xml` | Working | All 13 destinations defined |
| Vector Icons | `res/drawable/ic_*.xml` | Working | Core icons created |
| Color Tokens | `res/values/colors.xml` | Working | Ported from Color.kt |
