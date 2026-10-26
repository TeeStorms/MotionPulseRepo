# Prompt 0 Audit Report: Jetpack Compose to XML Views Migration Plan

> [!IMPORTANT]
> **Branch**: `Ndabezinhle_Zothando_Branch`
> **Status**: Audit completed. No code changes made.

---

## 1. Compose Screens Inventory
| Screen Name | File Path | Route in AppNavigation.kt | ViewModel | Line Count (Approx) |
| :--- | :--- | :--- | :--- | :--- |
| Landing Screen | `ui/screens/LandingScreen.kt` | `"landing"` | `AuthViewModel` | ~230 |
| Onboarding Screen | `ui/screens/onboarding/OnboardingScreen.kt` | `"onboarding"` | None (stateless) | ~120 |
| Login Screen | `ui/screens/auth/LoginScreen.kt` | `"login"` | `AuthViewModel` | ~380 |
| Register Screen | `ui/screens/auth/RegisterScreen.kt` | `"register"` | `AuthViewModel` | ~350 |
| Email Verification | `ui/screens/auth/EmailVerificationScreen.kt` | `"email_verification"` | `AuthViewModel` | ~160 |
| Dashboard / Home | `ui/screens/dashboard/DashboardScreen.kt` | `"dashboard"` | `HabitsViewModel` | ~260 |
| Habit Detail | `ui/screens/dashboard/HabitDetailScreen.kt` | `"habit_detail/{habitId}"` | `HabitsViewModel` | ~60 |
| Habits List | `ui/screens/habits/HabitsScreen.kt` | `"habits"` | `HabitsViewModel` | ~330 |
| Mood Tracker | `ui/screens/mood/MoodScreen.kt` | `"mood"` | `MoodViewModel` | ~250 |
| Community Hub | `ui/screens/community/CommunityScreen.kt` | `"community"` | `CommunityViewModel` | ~650 |
| Stats & Analytics | `ui/screens/stats/StatsScreen.kt` | `"stats"` | `StatsViewModel` | ~130 |
| Profile & Settings | `ui/screens/profile/ProfileScreen.kt` | `"profile"` | `ProfileViewModel` | ~320 |
| Badges Gallery | `ui/screens/profile/BadgesGalleryScreen.kt` | `"badges_gallery"` | `ProfileViewModel` | ~130 |
| Dialogs / Sheets / Overlays | Various (`AddHabitOverlay`, `ProfileDialogs`, `AuthOverlay`, `NotificationTray`, etc.) | Embedded in screens | Parent ViewModels | ~50–150 each |

---

## 2. Shared Composables & Usage Locations
| Composable / Component | File Path | Where Used |
| :--- | :--- | :--- |
| `MotionPulseBottomNav` | `ui/components/MotionPulseBottomNav.kt` | All main tabs (Dashboard, Mood, Habits, Community, Stats, Profile) |
| `GradientHeader` / `DropletHeader` | `ui/components/GradientHeader.kt`, `DropletHeader.kt` | Screen headers across Dashboard, Profile, Community, etc. |
| `UserAvatarInitials` | `ui/components/UserAvatarInitials.kt` | Profile headers, Friend rows, Feed items, Leaderboard items, Duel items |
| `MotionPulseTextField` | `ui/components/MotionPulseComponents.kt` | Login, Register, Add Habit, Add Friend overlays |
| `GradientButton` | `ui/components/MotionPulseComponents.kt` | Login, Register, Email Verification, Add Habit, etc. |
| `CapFontScale` | `ui/components/AppAdaptive.kt` | Chrome UI wrapping (headers, bottom nav, tab bar) |
| `MotionPulseTheme` | `ui/theme/Theme.kt` | Root of `MainAppContent` and previews |

---

## 3. Compose-Only Code Requiring Views Redesign
1. **Canvas / Custom Drawing**:
   - `DashboardProgressCircle`: Circular progress indicator.
   - `MoodHabitsGraphCard`: Custom Canvas line chart drawing mood correlation paths.
   - Habit streak day dots / rhythm graphs.
2. **Pagers / Tabs**:
   - Community tabs (`Feed`, `Leaderboard`, `Duels`) using `TabRow` and stateful view switching.
3. **Nested-Scroll / Collapsing Headers**:
   - Community screen custom `NestedScrollConnection` collapsing header with `graphicsLayer`.
4. **Swipe Gestures**:
   - `SwipeToDismissBox` used in Habits list (for quick edit/archive) and Notifications tray.
5. **Animations**:
   - `animateFloatAsState` (fade-ins, alpha transitions, scale states).
6. **AndroidView Interop**:
   - `AndroidView` wrapping `PlayerView` (Media3 ExoPlayer) in `LandingScreen.kt`.
7. **Haptics & Pointer Input**:
   - `LocalHapticFeedback.current.performHapticFeedback(...)` and click/drag detectors.

---

## 4. Compose Types Leaking into Non-UI Code
- **Status**: **None**. All ViewModels (`AuthViewModel`, `HabitsViewModel`, `MoodViewModel`, `CommunityViewModel`, `StatsViewModel`, `ProfileViewModel`), repositories, and domain models use standard Kotlin `StateFlow`, `SharedFlow`, and coroutines without any imports from `androidx.compose.runtime`.

---

## 5. Compose Icons Used
| Icon Name / Set | Where Used | Replacement XML Vector Drawable Needed |
| :--- | :--- | :--- |
| `Icons.Outlined.Home` | `MotionPulseBottomNav.kt` | `ic_home_outline` |
| `Icons.Outlined.FavoriteBorder` | `MotionPulseBottomNav.kt` | `ic_mood_outline` |
| `Icons.Outlined.Checklist` | `MotionPulseBottomNav.kt` | `ic_habits_outline` |
| `Icons.Outlined.Groups` | `MotionPulseBottomNav.kt` | `ic_community_outline` |
| `Icons.Outlined.BarChart` | `MotionPulseBottomNav.kt` | `ic_stats_outline` |
| `Icons.Outlined.Person` | `MotionPulseBottomNav.kt` | `ic_profile_outline` |
| `Icons.Default.Email` | `LoginScreen.kt`, `RegisterScreen.kt` | `ic_email` |
| `Icons.Default.Lock` | `LoginScreen.kt`, `RegisterScreen.kt` | `ic_lock` |
| `Icons.Default.Visibility` / `VisibilityOff` | `LoginScreen.kt`, `RegisterScreen.kt` | `ic_visibility`, `ic_visibility_off` |
| `Icons.Default.Person` | `RegisterScreen.kt`, `DashboardHeader.kt` | `ic_person` |
| `Icons.Default.Check` | `AuthComponents.kt` | `ic_check` |
| `Icons.Default.Close` | `CommunityScreen.kt`, `AddHabitOverlay.kt` | `ic_close` |
| `Icons.Default.ContentCopy` | `CommunityScreen.kt`, `ProfileScreen.kt` | `ic_content_copy` |
| `Icons.Default.Delete` | `CommunityScreen.kt` | `ic_delete` |
| `Icons.Default.Group` | `CommunityScreen.kt` | `ic_group` |
| `Icons.Default.MoreVert` | `CommunityScreen.kt` | `ic_more_vert` |
| `Icons.Default.Notifications` / `None` | `CommunityScreen.kt` | `ic_notifications`, `ic_notifications_none` |
| `Icons.Default.PersonAdd` | `CommunityScreen.kt` | `ic_person_add` |
| `Icons.Default.Search` / `Clear` | `CommunityScreen.kt`, `HabitsScreen.kt` | `ic_search`, `ic_clear` |
| `Icons.Default.Share` | `CommunityScreen.kt`, `ProfileScreen.kt` | `ic_share` |
| `Icons.Default.Favorite` | `DashboardScreen.kt` | `ic_favorite` |
| `Icons.Outlined.MonitorHeart` / `Remove` | `DashboardHabitCard.kt` | `ic_monitor_heart`, `ic_remove` |
| `Icons.Default.Add` / `Edit` / `Restore` | `HabitsScreen.kt` | `ic_add`, `ic_edit`, `ic_restore` |
| `Icons.Default.KeyboardArrowUp` / `Down` | `HabitsScreen.kt` | `ic_keyboard_arrow_up`, `ic_keyboard_arrow_down` |
| `Icons.Default.Schedule` | `AddHabitOverlay.kt` | `ic_schedule` |
| `Icons.Default.ArrowBack` | `BadgesGalleryScreen.kt` | `ic_arrow_back` |

---

## 6. Build Configuration
- **Compose BOM**: `libs.versions.toml` / Compose compiler plugin enabled (`buildFeatures.compose = true`).
- **ViewBinding**: Already enabled (`buildFeatures.viewBinding = true`).
- **SDKs**: `minSdk = 26`, `targetSdk = 36`, `compileSdk = 37`.
- **Existing Dependencies**: `material`, `androidx.constraintlayout`, `androidx.navigation.fragment.ktx`, `androidx.navigation.ui.ktx` are already present in `app/build.gradle.kts`. (Need to add `androidx.recyclerview:recyclerview`, `androidx.viewpager2:viewpager2`, `androidx.swiperefreshlayout:swiperefreshlayout`, and `androidx.fragment:fragment-ktx` for full Views migration).

---

## 7. Template Leftovers from Views Template
- `res/layout/fragment_first.xml`, `fragment_second.xml`
- `res/layout/activity_main.xml`, `content_main.xml`, `menu_main.xml`
- `res/navigation/nav_graph.xml`
- `FirstFragment.kt`, `SecondFragment.kt`
- **Status**: **Unused**. None of these template leftovers are referenced in `AndroidManifest.xml`, code, or resource resolvers. They are dead files.

---

## 8. Navigation Architecture
- **Navigation Type**: Jetpack Navigation Compose (`NavHost`, `composable`, `rememberNavController`).
- **Triggering**: Callbacks/lambdas passed into composables (`onNavigateToNav`, `onNavigateToRegister`, `onLoginSuccess`, `onBack`).
- **Auth Flow & Start Destination**: Start destination is `"landing"`, which checks `AuthState` (`Authenticated`, `AwaitingEmailVerification`, `OnboardingRequired`, or unauthenticated `login`) and pops up to clear backstack (`popUpTo("landing") { inclusive = true }`).
- **Views Migration Path**: Must be replaced by Jetpack Navigation Fragment (`NavHostFragment`) with an XML `nav_graph.xml` and Fragment destinations.

---

## 9. State Surviving Configuration Changes
- **Status**: The app currently relies entirely on ViewModels (`AuthViewModel`, `HabitsViewModel`, `MoodViewModel`, `CommunityViewModel`, `StatsViewModel`, `ProfileViewModel`) and `remember` for transient UI states. There are **zero** uses of `rememberSaveable` in the codebase.
- **Migration Impact**: Minimal risk for state loss during configuration changes, provided ViewModel state flows and fragment instance states are preserved.

---

## 10. Risks & Effort Estimates / Recommended Migration Order

| Screen / Module | Effort Estimate | Risk Level | Notes |
| :--- | :--- | :--- | :--- |
| **1. Intro (Landing)** | Small | Low | ExoPlayer `PlayerView` inside Fragment |
| **2. Auth (Login/Register/Verify)** | Medium | Low | Standard forms, input validation, ViewBinding |
| **3. Home (Dashboard)** | Large | Medium | RecyclerView for habits, custom View for progress circle, PullToRefresh |
| **4. Mood Tracker** | Medium | Low | Custom View for mood bars, RecyclerView/Flexbox for factor chips |
| **5. Habits List & Add Sheet** | Large | High | Swipe-to-dismiss RecyclerView items, BottomSheetDialogFragment |
| **6. Community Hub** | Large | High | ViewPager2 / Fragment tabs (Feed, Leaderboard, Duels), collapsing AppBar |
| **7. Stats & Analytics** | Medium | Medium | Custom Canvas chart replacement or MPAndroidChart / custom View |
| **8. Profile & Badges** | Medium | Low | RecyclerView for settings items, `GridLayoutManager` for badges |

**Recommended Migration Order**:
1. Setup base XML layout infrastructure, remove dead template files, add required Views dependencies (`recyclerview`, `viewpager2`, `swiperefreshlayout`, `fragment-ktx`).
2. Intro (LandingScreen) & Auth flow (Login, Register, Email Verification).
3. Home (DashboardScreen) & ProfileScreen.
4. MoodTracker & HabitsScreen.
5. StatsScreen & BadgesGalleryScreen.
6. CommunityScreen (most complex due to 3 tabs and collapsing header).
7. Remove Compose dependencies and compiler plugin completely once all screens are on Views.
