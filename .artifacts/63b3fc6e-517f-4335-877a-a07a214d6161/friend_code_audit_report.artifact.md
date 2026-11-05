# Motion.Pulse - Friend Code System Structure Discovery Report (Prompt 1)

## Executive Summary (Section A)
* **Single Most Likely Root Cause**: Missing or un-synced `friendCodes/{code}` mapping documents in Firestore for certain user accounts, combined with strict direct document `get()` rules on `friendCodes/{code}` that return `NotFound` when the mapping document does not exist.
* **Alternative 1**: Case sensitivity or spacing discrepancies between how the user's friend code was registered versus how it was typed/pasted during lookup.
* **Alternative 2**: Stale Room profile cache in local memory holding outdated friend codes.

---

## Real Lookup Flow Sequence (Section B)
1. User enters friend code in `AddFriendBottomSheetDialogFragment` (`layout_add_friend.xml`).
2. Input text is normalized by `FriendCodeUtil.normalize(input)` into a canonical uppercase code (`MP-XXXX-XXXX`).
3. `CommunityViewModel.searchByCode(canonicalCode)` checks rate limiting (max 10 lookups/min).
4. `CommunityRepository.findUserByFriendCode(canonicalCode)` executes:
   * Direct `get()` on `friendCodes/{canonicalCode}`.
   * If exists, reads `publicProfiles/{targetUid}` using direct `get()`.
   * Returns `FriendLookupResult.Found(profile)` or `FriendLookupResult.NotFound`.

---

## Storage Map Table (Section C)

| Storage Path / Table | Document ID Format | Fields | Writer File & Function | When Written | Batched? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`friendCodes/{code}`** | Canonical uppercase (`MP-XXXX-XXXX`) | `uid` (string) | `AuthRepository.createUniqueFriendCodeDoc` & `ensureFriendCodeDocExists` | Registration, login, sync | Standalone batch/set |
| **`users/{uid}`** | Firebase Auth UID | `UserProfileEntity` fields (`displayName`, `friendCode`, etc.) | `AuthRepository.syncUserProfile` | Login & registration | Standalone set |
| **`publicProfiles/{uid}`** | Firebase Auth UID | `PublicProfileEntity` fields (`displayName`, `currentStreak`, etc.) | `AuthRepository.syncUserProfile` | Login & profile sync | Standalone set |
| **Room Database** | User UID / Habit ID / etc. | `UserProfileEntity`, `FriendEntity`, etc. | Local DAOs / Repositories | Runtime & sync workers | Room transactions |

---

## Discrepancies Between Code and Rules (Section D)
1. **Collection Queries vs. Document Gets**: Previous fallback mechanisms attempted `whereEqualTo` queries on `users` and `publicProfiles`, which violated security rules or triggered `PERMISSION_DENIED`.
2. **Missing Mapping Documents**: If an account was created offline or registration sync was interrupted, `friendCodes/{code}` might be absent in Firestore, resulting in a `NotFound` result during lookup.

---

## Unverified Items / Questions (Section E)
* **NOT RUN**: `tools/audit-friend-codes.mjs` (requires live Firebase Admin credentials which are environment-dependent).
* **UNKNOWN**: Whether specific production user accounts in the live Firebase project have fully populated `friendCodes` collection documents.
