# Implementation Plan - Profile Self-Healing & Sign-In Verification Plan

This plan details adding `ensureProfileForUser` in `AuthRepository.kt`, wiring it to all sign-in success paths in `AuthViewModel.kt`, making `sendFriendRequest` and `acceptRequest` self-healing in `CommunityRepository.kt`, confirming `firestore.rules`, and deploying.

---

## Proposed Changes

### 1. `AuthRepository.kt`
- Add `ensureProfileForUser(user: FirebaseUser)` to idempotently verify and create `users/{uid}` and `publicProfiles/{uid}` if missing upon sign-in.

### 2. `AuthViewModel.kt`
- Call `authRepository.ensureProfileForUser(user)` immediately following successful email/password or Google sign-in.

### 3. `CommunityRepository.kt`
- Update `sendFriendRequest` and `acceptRequest` to verify and heal sender/recipient public profiles before setting requests or posting activity feed entries.

### 4. `firestore.rules`
- Confirm `friendRequests` rules have `allow get: if request.auth != null;` and `allow list: if request.auth != null && ('participants' in resource.data) && (request.auth.uid in resource.data.participants);`.

---

## Verification Plan

### Automated Tests
- Run `npx firebase emulators:exec --only firestore "npm test"` to verify rules pass 100%.
- Run `./gradlew testDebugUnitTest` to verify unit tests pass 100%.
- Run `./gradlew assembleDebug` to verify build succeeds.
- Deploy rules to Firebase (`npx firebase deploy --only firestore:rules,firestore:indexes --project pulse-702e8 --non-interactive`).
