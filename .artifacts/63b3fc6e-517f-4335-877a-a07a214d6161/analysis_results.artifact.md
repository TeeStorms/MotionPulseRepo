# Deep UI/UX & Backend Audit: Home, Mood, Habits, and Stats Screens

This report details a thorough audit of the **Home (Dashboard)**, **Mood**, **Habits**, and **Stats** screens in Motion.Pulse, identifying the exact root causes for reported bugs along with a technical fix strategy.

---

## 1. Executive Summary

| Issue Reported | Root Cause Found | Impact | Affected Files & Lines |
| :--- | :--- | :--- | :--- |
| **Home Screen: All habits say 100% done or 0% (undone)** | `HabitsAdapter.kt` and `HabitDetailFragment.kt` multiplied `habitStrengthScore` (which is already a 0-100 score from `HabitScoreCalculator`) by 100 again (`score * 100`). Any non-zero score became > 100% and was clamped to **100%**. | All habits display **100%** as soon as completed once, or **0%** if not completed. | `HabitsAdapter.kt` (line 60)<br>`HabitDetailFragment.kt` (line 124) |
| **Stats Screen: Mood and Habits graph out of sync when changing weeks** | `StatsViewModel.kt` queried `getMoodsForDateRange` with hardcoded current-week dates (`LocalDate.now()`) instead of target week dates (`targetDate`). | Navigating to previous/future weeks on the Stats screen fetched current week's moods, making mood correlation data empty (`null`) for all past/future weeks. | `StatsViewModel.kt` (lines 142–146) |
| **Stats Screen: No blue filled dot shown for Mood** | `MoodHabitsGraphCardView.kt` drew the legend dot for "Mood" in **Blue (`#4FC3F7`)**, but drew the actual Mood line and graph points in **Pink/Magenta (`#FF2A85`)**, matching the Habit Done dots. | Users saw Pink habit dots and Pink mood points, but **no Blue Mood dots** as promised by the legend. | `MoodHabitsGraphCardView.kt` (lines 28, 126, 131, 155) |

---

## 2. Screen-by-Screen Deep Audit

### A. Home Screen (Dashboard)
* **What Works**:
  * Greeting header and trailing mood pill display today's logged mood correctly.
  * Offline and sync failure banners update in real-time.
  * Progress ring and `$completed/$total` ratio calculate today's scheduled habits accurately.
  * Swiperefresh re-fetches habits.
* **What DOES NOT Work**:
  * **Score Percentage Clamping Bug**:
    - *Code Reference*: `HabitsAdapter.kt`:
      ```kotlin
      val scorePct = (item.habit.habitStrengthScore * 100).toInt().coerceIn(0, 100)
      ```
    - *Explanation*: `HabitScoreCalculator.calculateScore` returns a score between `0f` and `100f` (e.g. `14.5f` for 14.5%). `HabitsAdapter` multiplies this by 100 (`14.5f * 100 = 1450%`), which `coerceIn(0, 100)` clamps to **100%**.

---

### B. Mood Screen
* **What Works**:
  * Selecting mood levels (Energized, Good, Steady, Low, Drained) updates state and enables the submit button.
  * Factor chips (Sleep, Work, Health, Social, Weather) toggle state and persist to Room.
  * Existing mood for today loads automatically in edit mode ("Update mood").
  * Real-time Room flow notifies user upon saving ("Rhythm logged for today!").
* **What CAN BE IMPROVED**:
  * Notes input is optional but lacks character counter feedback.

---

### C. Habits Screen & Detail
* **What Works**:
  * Category chips filter habits ("Mind & Focus", "Fitness & Health", "Daily Routine").
  * Instant title search filters habit list in real-time.
  * Habit toggle completion logs progress and updates streak.
  * Habit deduplication cleans up redundant cloud records.
* **What DOES NOT Work**:
  * **Habit Strength Score in Habit Detail**:
    - *Code Reference*: `HabitDetailFragment.kt` line 124 uses `(habit.habitStrengthScore * 100).toInt().coerceIn(0, 100)`, producing the same 100% clamping bug.

---

### D. Stats Screen & Correlation Graph
* **What Works**:
  * Weekly date range label ("d MMM - d MMM") formats correctly.
  * Summary cards (Done today, Total completed, Weekly percent, Streak) compute metrics accurately.
  * Celebration event triggers when weekly completion reaches 100%.
* **What DOES NOT Work**:
  * **1. Week Offset Date Discrepancy**:
    - *Code Reference*: `StatsViewModel.kt` lines 142–146:
      ```kotlin
      db.moodDao().getMoodsForDateRange(
          userId,
          LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
          LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
      )
      ```
    - *Explanation*: The date range passed to `getMoodsForDateRange` was hardcoded to `LocalDate.now()` instead of factoring in `_weekOffset`.
  * **2. Legend vs. Canvas Paint Color Mismatch**:
    - *Code Reference*: `MoodHabitsGraphCardView.kt`:
      ```kotlin
      private val moodLegendDotPaint = Paint().apply { color = Color.parseColor("#4FC3F7") } // Blue Legend
      private val linePaint = Paint().apply { color = Color.parseColor("#FF2A85") } // Pink Line!
      private val hollowPointPaint = Paint().apply { color = Color.parseColor("#FF2A85") } // Pink Point!
      ```
    - *Explanation*: The Mood legend promised a **Blue (`#4FC3F7`)** dot, but the actual Mood line and hollow points were drawn in **Pink (`#FF2A85`)**.

---

## 3. Recommended Fixes

1. **Fix Habit Strength Score Percentage Calculation**:
   - Change `(habitStrengthScore * 100).toInt().coerceIn(0, 100)` to `habitStrengthScore.toInt().coerceIn(0, 100)` in both `HabitsAdapter.kt` and `HabitDetailFragment.kt`.
2. **Fix Week Navigation Date Range in `StatsViewModel.kt`**:
   - Update `getMoodsForDateRange` in `weeklyRawDataFlow` to use `startOfWeek` and `endOfWeek` calculated from `targetDate` (`LocalDate.now().plusWeeks(offset)`).
3. **Sync Mood Legend & Graph Point Colors in `MoodHabitsGraphCardView.kt`**:
   - Update `linePaint` and `hollowPointPaint` to use `#4FC3F7` (Blue) so the graph points match the Blue Mood legend dot.
