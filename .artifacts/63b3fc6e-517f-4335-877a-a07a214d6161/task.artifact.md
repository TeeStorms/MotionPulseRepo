# Task List - Scope-Restricted Localization (Home, Mood, Community, Stats)

- [ ] 1. Create `docs/translations/scope.txt` with in-scope keys for Home, Mood, Community, Stats
- [ ] 2. Extract in-scope strings into `res/values/strings.xml` and update layouts (`fragment_dashboard.xml`, `fragment_mood.xml`, `fragment_duels.xml`, `fragment_stats.xml`, `item_duel.xml`)
- [ ] 3. Update `tools/translate-strings.mjs` to filter translation keys by `docs/translations/scope.txt`
- [ ] 4. Implement `MoodFactor.labelRes` and `MoodLevel.labelRes` enum mapping functions
- [ ] 5. Run `node tools/translate-strings.mjs --dry-run` and paste raw output
- [ ] 6. Run `./gradlew testDebugUnitTest` and paste raw output
- [ ] 7. Run `./gradlew assembleDebug` and paste raw output
- [ ] 8. Run `./gradlew lintDebug` and paste raw output
- [ ] 9. Create `walkthrough.artifact.md`
