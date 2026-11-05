# Motion.Pulse - Data Contract

| Field Name | Entity / Model | Type | Unit | Owner Screen | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `habitStrengthScore` | `HabitEntity` | Float | Percent (0.0..100.0) | Habits / Detail | Habit strength health score |
| `currentStreak` | `HabitEntity` / `UserProfileEntity` | Int | Days (Integer) | Habits / Profile / Stats | Current active streak |
| `longestStreak` | `HabitEntity` | Int | Days (Integer) | Habit Detail / Profile | All-time personal best streak |
| `totalAuraXp` | `UserProfileEntity` / `PublicProfileEntity` | Long | XP (Integer) | Profile / Stats / Community | Cumulative Aura XP |
| `currentLevel` | `UserProfileEntity` / `PublicProfileEntity` | Int | Level (Integer) | Profile / Stats / Community | User level derived from XP |
| `moodLevel` | `MoodEntity` | Enum (`MoodLevel`) | Enum / String label | Mood / Stats / Home | Daily logged mood rating |
| `doneToday` | Summary Stats | Int | Count (Integer) | Home / Stats | Number of habits completed today |
| `weeklyPercent` | Summary Stats | Int | Percent (0..100) | Stats / Home | Weekly completion percentage |
