# Implementation Plan - Mood & Habits Weekly Rhythm Graph

This plan details the implementation of a high-fidelity "Mood & Habits" correlation graph and associated data models, matching the "Senior UI/UX" requirements and the provided design specifications.

## Proposed Changes

### Data Models & Domain Logic

#### [NEW] [WeeklyRhythmModels.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/domain/models/WeeklyRhythmModels.kt)
- Define `DayMoodHabitData`: `dayLabel`, `moodScore` (1-5), `habitsCompleted`, `totalHabits`.
- Define `WeeklyRhythmUiState`: List of 7 `DayMoodHabitData`.

#### [MODIFY] [StatsViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/viewmodels/StatsViewModel.kt)
- Integrate `WeeklyRhythmUiState` into the ViewModel.
- Implement a helper function `computeBestDay(state: WeeklyRhythmUiState)`:
    - Score = `moodScore + (habitsCompleted / totalHabits * 5)`.
    - Handle ties and empty data.
- Add mock data provider for Compose Previews.

### UI Components

#### [MODIFY] [MoodHabitCorrelationCard.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/stats/components/MoodHabitCorrelationCard.kt)
- Rename or refactor to `MoodHabitsGraphCard`.
- **Canvas Implementation**:
    - **X-Axis**: Centered single-letter labels ('M', 'T', etc.).
    - **Mood Line**: Draw a smooth path with a **Cyan-to-Magenta gradient** stroke. Place circular nodes at each day's peak.
    - **Habit Dot Matrix**: Implement vertical columns of dots directly beneath each X-axis label.
        - Filled Purple: Completed.
        - Semi-transparent Grey: Pending.
- **Legend Bar**: Use the specified "Mood" (Blue) and "Habit done" (Purple) labels.

### Screen Integration

#### [MODIFY] [StatsScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/stats/StatsScreen.kt)
- Update to use the new `MoodHabitsGraphCard` and the refined data structures.

## Verification Plan

### Automated Tests
- Build and check for any layout constraint violations.
- Verify "Best Day" calculation logic with unit tests.

### Manual Verification
1.  **Visual Audit**: Compare the rendered Compose Preview against the reference screenshot.
2.  **Responsiveness**: Verify the graph scales correctly on different screen widths without overlapping labels.
3.  **Data Sync**: Complete a habit and verify the corresponding dot in the vertical column flips from grey to purple in real-time.
