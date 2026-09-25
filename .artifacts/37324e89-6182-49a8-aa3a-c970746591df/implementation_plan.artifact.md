# Implementation Plan - Motion.Pulse Friend-Request System

Add the missing friend-request system to Motion.Pulse's Community feature, connecting the social layer (Activity Feed, Leaderboard, Duels, Group Challenges) with robust discovery, sending/receiving, request management, notifications, unfriend handling, and Firestore security rules.

## User Review Required

> [!IMPORTANT]
> **Friend Discovery Identifier**: We are utilizing the existing `friendCode` (formatted as `MP-XXXXXX`, e.g., `MP-AB7K92`) generated at registration and displayed on the user's Profile screen.
> **Friend Requests UI**: Incoming requests and notifications are centralized in the existing Notification Tray (bell icon in CommunityScreen with unread badging). We will also add a Friends management view to allow users to view and unfriend connections.
> **Simultaneous Request Edge Case**: If User A sends a request to User B while User B has already sent one to User A (both PENDING in opposite directions), we auto-accept the request (treating it as mutual intent).
> **Declined Cooldown & Unfriend Duel Handling**:
> - Declined requests impose a 24-hour cooldown before a new request can be sent between the same pair.
> - Unfriending an active connection will automatically cancel/complete any active duels between the two users.
> **Firestore Security Rules**: We will create a comprehensive `firestore.rules` file covering `friendRequests`, `users`, `activityFeed`, `duels`, and `challenges`.

## Open Questions

- None. All edge cases and requirements are fully specified.

## Proposed Changes

### Data & Repository Layer

#### [MODIFY] [CommunityRepository.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/data/repository/CommunityRepository.kt)
- Enhance `sendFriendRequest` to enforce duplicate `PENDING` checks, accepted friend checks, self-request checks, and a 24-hour cooldown for recently `DECLINED` requests.
- Add `unfriend` and active duel cancellation logic when unfriending.

### ViewModel & UI Layer

#### [MODIFY] [CommunityViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/viewmodels/CommunityViewModel.kt)
- Add functions to manage friend requests (send, accept, decline, unfriend).
- Expose friends list and manage accepted/pending state cleanly.

#### [MODIFY] [CommunityScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/app/src/main/java/com/example/motionpulse/ui/screens/community/CommunityScreen.kt`
- Add a Friends management sheet/view where users can view their friends list and unfriend users.
- Refine Notification Tray and Add Friend overlay with robust empty states and loading/error feedback.

### Security & Deployment

#### [NEW] [firestore.rules](file:///C:/Users/User/AndroidStudioProjects/MotionPulse/firestore.rules)
- Write comprehensive Firestore Security Rules for `friendRequests`, `users`, `activityFeed`, `duels`, and `challenges`.
- Enforce strict rules: `requesterUid == request.auth.uid` on create, recipient-only updates on pending requests, no direct `ACCEPTED` on creation, participant-only reads.

## Verification Plan

### Automated Tests
- Run unit tests and build check via Gradle (`gradle_build("app:assembleDebug")`).

### Manual Verification
- Verify friend code search, sending/receiving friend requests, accepting/declining, notification dispatch, and unfriend flow.
