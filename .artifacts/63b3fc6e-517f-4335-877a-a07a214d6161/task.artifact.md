# Task List - Profile Self-Healing & Sign-In Verification

- [ ] 1. Add `ensureProfileForUser(user: FirebaseUser)` to `AuthRepository.kt`
- [ ] 2. Call `authRepository.ensureProfileForUser(user)` in `AuthViewModel.kt` after sign-in
- [ ] 3. Replace `sendFriendRequest` and `acceptRequest` in `CommunityRepository.kt` with self-healing implementations
- [ ] 4. Confirm `firestore.rules` `friendRequests` rules
- [ ] 5. Run `npx firebase emulators:exec --only firestore "npm test"` and paste raw output
- [ ] 6. Run `./gradlew testDebugUnitTest` and paste raw output
- [ ] 7. Run `./gradlew assembleDebug` and paste raw output
- [ ] 8. Deploy rules to Firebase (`npx firebase deploy --only firestore:rules,firestore:indexes --project pulse-702e8 --non-interactive`)
- [ ] 9. Create `walkthrough.artifact.md`
