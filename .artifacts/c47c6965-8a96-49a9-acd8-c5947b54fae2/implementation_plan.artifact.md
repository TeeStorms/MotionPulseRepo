# Implementation Plan - Simplified Binary Habit Completion

Simplify habit completion to a frictionless binary toggle for all habit types. Tapping an incomplete habit marks it complete (full goal value for numeric), while tapping a completed habit requires confirmation before reversal.

## Current Status Report (Before Changes)

### Scoring & Streak Handling
- **HabitScoreCalculator**: Currently grants partial credit for the `PARTIAL` status. `MISSED` causes decay, `COMPLETED` grants full gain.
- **StreakCalculator**: Currently strictly requires `COMPLETED` status to extend a streak. `PARTIAL` is treated the same as `MISSED` (it breaks the streak).
- **GamificationEngine**: Purely additive. Reversal is handled manually in ViewModels by deleting badge records and deducting XP.

### Proposed Scoring Change
- I will **remove the `PARTIAL` branch** from `HabitScoreCalculator`. Since all completions will now be binary, habits will either get full credit or decay.

---

## User Review Required

> [!IMPORTANT]
> **Logic Removal:** I will be completely removing the `PARTIAL` enum value and the `QuickLogBottomSheet.kt` / `HabitGoalStepper.kt` components. Any existing partial progress in your database will likely be treated as `MISSED` once the enum value is gone (or cause Room migration issues if not handled). Since we just bumped the DB version to 5, I will bump it to **6** to ensure a clean slate.

---

## Proposed Changes

### [Data Layer]

#### [MODIFY] [HabitCompletionEntity.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/data/local/entity/HabitCompletionEntity.kt)
- Remove `PARTIAL` from `CompletionStatus` enum.

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/data/local/AppDatabase.kt)
- Increment version to **6** for clean schema migration.

### [Domain Layer]

#### [MODIFY] [HabitScoreCalculator.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/domain/scoring/HabitScoreCalculator.kt)
- Remove `PARTIAL` case and corresponding logic.

#### [MODIFY] [HabitsViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/viewmodels/HabitsViewModel.kt) & [StatsViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/viewmodels/StatsViewModel.kt)
- Remove `PARTIAL` references.
- Remove undo snackbar logic.
- Implement `deleteHabitProgress` (reversal logic).
- Simplify `logHabitProgress` to always set `COMPLETED` and full goal value for numeric types.

### [UI Layer]

#### [DELETE] [QuickLogBottomSheet.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/dashboard/components/QuickLogBottomSheet.kt)
#### [DELETE] [HabitGoalStepper.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/components/HabitGoalStepper.kt)

#### [MODIFY] [HabitCard.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/dashboard/components/HabitCard.kt)
- Revert progress ring UI to a solid solid dot.
- Remove `onOpenQuickLog` parameter.
- Add state for showing the "Remove progress?" `AlertDialog`.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/dashboard/DashboardScreen.kt)
- Remove `QuickLogBottomSheet` usage.
- Ensure only binary toggle is passed to `HabitCard`.

#### [MODIFY] [HabitConsistencyCard.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/stats/components/HabitConsistencyCard.kt)
- Update `DayCell` to simple solid dot.
- Update `onCellClick` to trigger the toggle/confirmation logic directly.

## Verification Plan

### Automated Tests
- Build the project to ensure all `PARTIAL` references are gone.

### Manual Verification
1.  **Single Tap Completion**: Tap an empty dot. Verify it fills immediately and haptic triggers.
2.  **Confirmation on Reversal**: Tap a filled dot. Verify the dark-themed `AlertDialog` appears.
3.  **Confirm Reversal**: Tap "Remove". Verify XP is deducted, badge revoked (if any), and dot is cleared.
4.  **Backfilling**: Go to Stats, tap a past cell. Verify same behavior (toggle or confirm).
5.  **Future Guard**: Tap a future cell. Verify nothing happens.
