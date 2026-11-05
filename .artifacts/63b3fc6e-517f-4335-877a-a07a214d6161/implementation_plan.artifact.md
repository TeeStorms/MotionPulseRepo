# Implementation Plan - Scope-Restricted Localization (Home, Mood, Community, Stats)

This plan details auditing, extracting, and localizing user-visible text strictly within scope (Home, Mood, Community, Stats), updating the translation script scope (`docs/translations/scope.txt`), and running all test suites.

---

## Part 1: String Audit Report (In-Scope Strings)

### HOME Screen
- **H1**: Header welcome message (`DashboardFragment.kt:88`): `"Welcome back,\n%1$s"` -> Key: `home_welcome_back`
- **H2**: Motivation label & quote (`fragment_dashboard.xml:177, 186`): `"MOTIVATION OF THE DAY"` -> Key: `home_motivation_title`, Daily quote array -> Key: `daily_quotes` (30 lines)

### MOOD Screen
- **M1**: Header text (`fragment_mood.xml:44`): `"How's your rhythm today?"` -> Key: `mood_header`
- **M2**: Section text (`fragment_mood.xml:77`): `"What's shaping it?"` -> Key: `mood_factors_title`
- **M3**: Factor chips (`fragment_mood.xml:100-132`): `"Sleep 😴"`, `"Work 💼"`, `"Health 🏃"`, `"Social 👥"`, `"Weather ☀️"` -> Keys: `mood_factor_sleep`, `mood_factor_work`, `mood_factor_health`, `mood_factor_social`, `mood_factor_weather`
- **M4**: Mood levels & UI text (`fragment_mood.xml:57, 175` & `MoodLevel` enum): `"Tap a beat to log..."` -> Key: `mood_subtitle`, Mood levels (Energized, Good, Steady, Low, Drained) -> Keys: `mood_level_energized`, `mood_level_good`, `mood_level_steady`, `mood_level_low`, `mood_level_drained`, Button -> Keys: `mood_button_log`, `mood_button_update`

### COMMUNITY Screen (Duels & Challenge)
- **C1**: Duels section title & Challenge button (`fragment_duels.xml:41, 62`): `"Habit Duels"` -> Key: `duels_title`, `"Challenge a Friend"` -> Key: `duels_challenge_button`
- **C2**: Challenge subtitle (`fragment_duels.xml:51` & `layout_challenge_dialog.xml:83`): `"Whoever logs the most habits in 7 days wins. Starts today, ends in 7 days."` -> Key: `duels_challenge_subtitle`
- **C3**: Duel card text (`item_duel.xml:49, 100, 164`): `"VS"` -> Key: `duel_vs`, `"You"` -> Key: `duel_you`, `"Unknown"` -> Key: `duel_unknown_user`, `"Victory 🎉"` -> Key: `duel_victory`, `"Ends today"` -> Key: `duel_ends_today`, Plural `duel_days_left`

### STATS Screen
- **S1**: Insight default text (`fragment_stats.xml:360`): `"Log a few more days to unlock rhythm insights!"` -> Key: `stats_insight_not_enough_data`
- **S2**: Best day text (`fragment_stats.xml:400`): `"No best day logged yet"` -> Key: `stats_best_day_none`

---

## User Review Required

> [!IMPORTANT]
> - **Part 2: Resource Extraction**:
>   - Extract all 22 in-scope strings into `res/values/strings.xml`.
>   - Add enum mapping functions for `MoodFactor` and `MoodLevel`.
>   - Update `HabitsViewModel` to expose `dayOfYear % size` and let Fragment load quote from `R.array.daily_quotes`.
> - **Part 3: Translation Scope File (`docs/translations/scope.txt`)**:
>   - Create `docs/translations/scope.txt` containing only the in-scope keys.
>   - Update `tools/translate-strings.mjs` to respect `scope.txt`.
> - **Part 4 & 5: Language Switch & Tests**:
>   - Ensure language switch applies immediately across Home, Mood, Community, and Stats.
>   - Run `node tools/translate-strings.mjs --dry-run` and paste output.
>   - Run unit tests (`./gradlew testDebugUnitTest`), debug build (`./gradlew assembleDebug`), and lint (`./gradlew lintDebug`).

---

## Proposed Changes

### 1. Resource Extraction (`strings.xml`, Layouts, Fragments)
- Extract in-scope strings into `strings.xml`.
- Update layouts to use `@string/...` references.

### 2. Scope File & Translation Script (`docs/translations/scope.txt`, `tools/translate-strings.mjs`)
- Create `scope.txt`.
- Update `translate-strings.mjs` to filter by `scope.txt`.

### 3. Enum Mappings & ViewModels (`data/local/entity/MoodEntity.kt`, `ui/viewmodels/HabitsViewModel.kt`)
- Implement `MoodFactor.labelRes` and `MoodLevel.labelRes`.

---

## Verification Plan

### Automated Tests
- Run `node tools/translate-strings.mjs --dry-run` and paste raw output.
- Run `./gradlew testDebugUnitTest` and paste raw output.
- Run `./gradlew assembleDebug` and paste raw output.
- Run `./gradlew lintDebug` and paste raw output.
