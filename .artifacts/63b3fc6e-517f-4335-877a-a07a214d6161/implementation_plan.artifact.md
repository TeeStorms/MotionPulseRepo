# Implementation Plan - Complete Migration to XML Layouts & ViewBinding + Compose Removal

This plan details the full screen-by-screen migration of Motion.Pulse from Jetpack Compose host fragments to native XML layouts (`Fragment` + `ViewBinding`), followed by the complete removal of Jetpack Compose dependencies and compiler plugins.

---

## User Review Required

> [!IMPORTANT]
> - All 13 screens will be refactored into native Fragments using ViewBinding (`Fragment*Binding`).
> - Reusable custom Views built in Phase 2 (`MotionPulseBottomNav`, `DropletHeaderView`, `UserAvatarView`, `MoodPillView`, `StateLayout`) will be integrated into the XML layouts.
> - RecyclerView adapters will be created for lists (Habits, Community feed, Leaderboard, Duels, Badges).
> - Once all screens are on XML, all Jetpack Compose dependencies, compiler plugins, and BOM will be purged from Gradle.

---

## Proposed Migration Order & Architecture

### Phase A: Auth & Landing Screens
1. **LandingFragment**: Video view (ExoPlayer `PlayerView`), loading indicator, auth state observer.
2. **OnboardingFragment**: ViewPager2 / intro slider layout, Get Started button.
3. **LoginFragment**: TextInputLayouts for email/password, Login button, Google sign-in button, Forgot password dialog.
4. **RegisterFragment**: TextInputLayouts for name/email/password, Register button.
5. **EmailVerificationFragment**: Inbox verification banner, Continue button, Resend email button.

### Phase B: Core Main Tabs
6. **DashboardFragment (Home)**: `DropletHeaderView`, progress circle / stat overview, Motivation of the Day card, RecyclerView for today's habits.
7. **MoodFragment**: `DropletHeaderView`, mood selection bars, factor selection chips, note EditText, Save button.
8. **HabitsFragment**: `DropletHeaderView`, search bar, category filter chips, RecyclerView for habit management with swipe-to-edit/archive.
9. **HabitDetailFragment**: `DropletHeaderView`, completion history stats, back navigation.
10. **CommunityFragment**: `DropletHeaderView`, TabLayout / ViewPager2 for Feed, Leaderboard, Duels.
11. **StatsFragment**: `DropletHeaderView`, summary stat boxes, correlation chart view.
12. **ProfileFragment**: `DropletHeaderView` (Member since), stats cards, achievement section, settings items (change password, logout, delete account).
13. **BadgesGalleryFragment**: `DropletHeaderView`, RecyclerView Grid for unlocked/locked badges.

### Phase C: Compose Purge
1. Delete all Compose bridge fragments (`BaseComposeHostFragment` subclasses replaced by native `Fragment` classes).
2. Remove Compose BOM, UI libraries, compiler plugin, and `buildFeatures { compose = true }` from Gradle.
3. Run dependency tree check (`./gradlew :app:dependencies --configuration debugRuntimeClasspath`) to verify zero Compose leakage.
4. Run unit tests and verify build.

---

## Verification Plan

### Automated Tests
- Run `gradle_build("app:assembleDebug")` to verify clean compilation.
- Run `npx firebase emulators:exec --only firestore "npm test"` to verify security rules tests.

### Manual Verification
- Deploy app on physical device / emulator and test complete navigation flow.
