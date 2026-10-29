# Task List - Duel Race Conditions & Atomic Unfriend

- [x] 1. Update `Duel.kt` to include `endDate` field
- [x] 2. Update `firestore.rules` with hardened duel validation rules
- [x] 3. Update `test/firestore.test.js` with new test cases for duels and unfriending feed privacy
- [x] 4. Run emulator test suite (`firebase emulators:exec --only firestore "npm test"`)
- [x] 5. Update `CommunityRepository.kt` & `CommunityViewModel.kt` (transactional duel resolution, deterministic notification IDs, atomic WriteBatch unfriend + Room cache cleanup)
- [x] 6. Run gradle build & verify compilation success
- [x] 7. Create `walkthrough.artifact.md`
