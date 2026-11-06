# Walkthrough - Resilient recentXp Deserialization Fix

We have updated [UserProfileEntity.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/data/local/entity/UserProfileEntity.kt), [LeaderboardAdapter.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/adapters/LeaderboardAdapter.kt), [CommunityViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/viewmodels/CommunityViewModel.kt), and [CompleteHabitUseCase.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/domain/usecase/CompleteHabitUseCase.kt) to use `Map<String, Any?>` with the safe `getRecentXpAsLong()` converter on branch `Ndabezinhle_Zothando_Branch`.

---

## Changes Implemented

### 1. Resilient `recentXp` Deserialization (`UserProfileEntity.kt`)
- Updated `recentXp` field type to `Map<String, Any?> = emptyMap()` to prevent Firestore's automatic object mapper from failing on individual numeric entries (Integer, Long, Double, Float, String).
- Added `fun getRecentXpAsLong(): Map<String, Long>` helper method to normalize any numeric Firestore value to `Long`.

### 2. Leaderboard & ViewModel Updates (`LeaderboardAdapter.kt`, `CommunityViewModel.kt`, `CompleteHabitUseCase.kt`)
- Updated `LeaderboardAdapter.kt` and `CommunityViewModel.kt` (`computeWeeklyXp`) to calculate weekly XP using `profile.getRecentXpAsLong()`.
- Updated `CompleteHabitUseCase.kt` and `HabitsViewModel.kt` to extract and mutate `recentXp` safely using `getRecentXpAsLong()`.

---

## Verification Results
- **Android Unit Tests**: **78/78 PASSED** (`gradle_build("app:testDebugUnitTest")`).
- **Gradle Build**: **Build finished successfully** (`gradle_build("app:assembleDebug")`).
- **Git Status**: As instructed ("Do not commit"), **no commit or push** has been performed.
