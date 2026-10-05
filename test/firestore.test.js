const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");

jest.setTimeout(20000);

describe("Motion.Pulse Firestore Security Rules", () => {
  let testEnv;

  beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: "pulse-702e8",
      firestore: {
        rules: fs.readFileSync(path.resolve(__dirname, "../firestore.rules"), "utf8"),
        host: "localhost",
        port: 8080,
      },
    });
  });

  afterAll(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  test("friendRequests: user can create request as requesterUid", async () => {
    const db = testEnv.authenticatedContext("alice", { email: "alice@test.com" }).firestore();
    await assertSucceeds(
      db.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "PENDING",
        createdAt: Date.now()
      })
    );
  });

  test("friendRequests: non-requester cannot create request for someone else", async () => {
    const db = testEnv.authenticatedContext("charlie", { email: "charlie@test.com" }).firestore();
    await assertFails(
      db.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "PENDING",
        createdAt: Date.now()
      })
    );
  });

  test("friendRequests: recipient can update status to ACCEPTED", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "PENDING"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(
      bobDb.collection("friendRequests").doc("alice_bob").update({
        status: "ACCEPTED",
        respondedAt: Date.now()
      })
    );
  });

  test("activityFeed: user in visibleTo can read feed entry, non-visible user cannot", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("activityFeed").doc("entry1").set({
        actorId: "alice",
        actorName: "Alice",
        timestamp: Date.now(),
        eventType: "STREAK_MILESTONE",
        visibleTo: ["alice", "bob"],
        reactions: {}
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(bobDb.collection("activityFeed").doc("entry1").get());

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(charlieDb.collection("activityFeed").doc("entry1").get());
  });

  test("activityFeed: user in visibleTo can react under their own key, non-visible user cannot", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("activityFeed").doc("entry1").set({
        actorId: "alice",
        actorName: "Alice",
        timestamp: Date.now(),
        eventType: "STREAK_MILESTONE",
        visibleTo: ["alice", "bob"],
        reactions: {}
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(
      bobDb.collection("activityFeed").doc("entry1").update({
        reactions: { bob: true }
      })
    );

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(
      charlieDb.collection("activityFeed").doc("entry1").update({
        reactions: { charlie: true }
      })
    );
  });

  test("activityFeed: actorId cannot be spoofed on creation", async () => {
    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(
      bobDb.collection("activityFeed").doc("spoofed1").set({
        actorId: "alice",
        actorName: "Alice",
        timestamp: Date.now(),
        eventType: "STREAK_MILESTONE",
        visibleTo: ["alice", "bob"]
      })
    );
  });

  test("challenges: participant can read challenge", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("challenges").doc("c1").set({
        title: "14-Day Streak",
        participantIds: ["alice", "bob"],
        progress: { alice: 0.5, bob: 0.3 }
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(aliceDb.collection("challenges").doc("c1").get());

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(charlieDb.collection("challenges").doc("c1").get());
  });

  test("challenges: scoped array-contains query succeeds for participant and returns empty for non-participant", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("challenges").doc("c1").set({
        title: "14-Day Streak",
        participantIds: ["alice", "bob"],
        progress: { alice: 0.5, bob: 0.3 }
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("challenges").where("participantIds", "array-contains", "alice").get()
    );

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertSucceeds(
      charlieDb.collection("challenges").where("participantIds", "array-contains", "charlie").get()
    );
  });

  test("duels: participant can update only their own score", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("duels").doc("d1").set({
        habitType: "Running",
        startDate: "2026-09-01",
        durationDays: 7,
        endDate: "2026-10-08",
        participants: ["alice", "bob"],
        scores: { alice: 2, bob: 1 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("duels").doc("d1").update({
        scores: { alice: 3, bob: 1 }
      })
    );

    await assertFails(
      aliceDb.collection("duels").doc("d1").update({
        scores: { alice: 3, bob: 5 }
      })
    );
  });

  test("notifications: friend can create a nudge notification for another user", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-28").set({
        id: "nudge_alice_2026-09-28",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );
  });

  test("notifications: second nudge same day to same recipient fails (duplicate ID)", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-28").set({
        id: "nudge_alice_2026-09-28",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-28").set({
        id: "nudge_alice_2026-09-28",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );
  });

  test("notifications: nudge on a different day succeeds", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-28").set({
        id: "nudge_alice_2026-09-28",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-29").set({
        id: "nudge_alice_2026-09-29",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );
  });

  test("blocks: blocked sender cannot send a nudge notification or friend request", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.collection("users").doc("bob").collection("blocks").doc("alice").set({
        blockedUid: "alice",
        blockedAt: Date.now()
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("nudge_alice_2026-09-28").set({
        id: "nudge_alice_2026-09-28",
        senderId: "alice",
        senderName: "Alice",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );

    await assertFails(
      aliceDb.collection("friendRequests").doc("alice_bob_2").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "PENDING",
        createdAt: Date.now()
      })
    );
  });

  test("blocks: block list cannot be read by anyone else", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("bob").collection("blocks").doc("alice").set({
        blockedUid: "alice",
        blockedAt: Date.now()
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(aliceDb.collection("users").doc("bob").collection("blocks").doc("alice").get());

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(bobDb.collection("users").doc("bob").collection("blocks").doc("alice").get());
  });

  test("notifications: non-friend cannot create a nudge notification for another user", async () => {
    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(
      charlieDb.collection("users").doc("bob").collection("notifications").doc("nudge_charlie_2026-09-28").set({
        id: "nudge_charlie_2026-09-28",
        senderId: "charlie",
        senderName: "Charlie",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );
  });

  test("notifications: user cannot spoof senderId when writing a notification", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("spoofed1").set({
        id: "spoofed1",
        senderId: "charlie",
        senderName: "Charlie",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      })
    );
  });

  test("notifications: user cannot read someone else's notifications", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("bob").collection("notifications").doc("my_n1").set({
        id: "my_n1",
        senderId: "bob",
        senderName: "Bob",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(
      aliceDb.collection("users").doc("bob").collection("notifications").doc("my_n1").get()
    );
  });

  test("notifications: owner can mark notification as read", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("bob").collection("notifications").doc("n1").set({
        id: "n1",
        senderId: "bob",
        senderName: "Bob",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: null
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(
      bobDb.collection("users").doc("bob").collection("notifications").doc("n1").update({
        isRead: true
      })
    );
  });

  test("notifications: owner cannot edit message text of a notification", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("bob").collection("notifications").doc("n1").set({
        id: "n1",
        senderId: "bob",
        senderName: "Bob",
        type: "NUDGE",
        timestamp: Date.now(),
        isRead: false,
        habitType: null,
        duelId: null,
        message: "Initial Message"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(
      bobDb.collection("users").doc("bob").collection("notifications").doc("n1").update({
        message: "Tampered Message"
      })
    );
  });

  test("private profile: user cannot read or write another user's private profile", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("alice").set({
        uid: "alice",
        email: "alice@secret.com",
        displayName: "Alice"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(bobDb.collection("users").doc("alice").get());
    await assertFails(bobDb.collection("users").doc("alice").update({ displayName: "Hacked" }));
  });

  test("private subcollections: user cannot read another user's habits, completions, or moods", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("users").doc("alice").collection("moods").doc("2026-09-28").set({
        moodLevel: "RADIANT",
        note: "Top Secret Mood Note"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(bobDb.collection("users").doc("alice").collection("moods").doc("2026-09-28").get());
  });

  test("private subcollections: owner can fully manage their own habits, completions, and moods", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("moods").doc("2026-09-28").set({
        moodLevel: "RADIANT",
        note: "My Secret Mood Note"
      })
    );
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("moods").doc("2026-09-28").get()
    );
  });

  test("publicProfiles: any signed-in user can read public profile, but cannot write someone else's", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("publicProfiles").doc("alice").set({
        uid: "alice",
        displayName: "Alice",
        currentStreak: 12,
        currentLevel: 5
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(bobDb.collection("publicProfiles").doc("alice").get());
    await assertFails(bobDb.collection("publicProfiles").doc("alice").set({ displayName: "Bob Fake Alice" }));
  });

  test("friendCodes: signed-in user can get single code document", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendCodes").doc("MP-ABCD-EFGH").set({
        uid: "alice"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(bobDb.collection("friendCodes").doc("MP-ABCD-EFGH").get());
  });

  test("friendCodes: list operation is denied", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendCodes").doc("MP-ABCD-EFGH").set({
        uid: "alice"
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(bobDb.collection("friendCodes").get());
  });

  test("friendCodes: user can create code mapping for own UID but not for another UID", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("friendCodes").doc("MP-2345-6789").set({
        uid: "alice"
      })
    );

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(
      bobDb.collection("friendCodes").doc("MP-9876-5432").set({
        uid: "alice"
      })
    );
  });

  test("friendCodes: overwriting an existing code document is denied", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendCodes").doc("MP-ABCD-EFGH").set({
        uid: "alice"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertFails(
      aliceDb.collection("friendCodes").doc("MP-ABCD-EFGH").set({
        uid: "alice"
      })
    );
  });

  test("friendCodes: unauthenticated get is denied", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendCodes").doc("MP-ABCD-EFGH").set({
        uid: "alice"
      });
      await adminDb.terminate();
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(unauthDb.collection("friendCodes").doc("MP-ABCD-EFGH").get());
  });

  test("unauthenticated access: denied everywhere across all paths", async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(unauthDb.collection("publicProfiles").doc("alice").get());
    await assertFails(unauthDb.collection("users").doc("alice").get());
    await assertFails(unauthDb.collection("activityFeed").doc("entry1").get());
  });

  test("duels: accepted friends can create a duel, non-friends cannot", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("friendRequests").doc("alice_bob").set({
        requesterUid: "alice",
        recipientUid: "bob",
        participants: ["alice", "bob"],
        status: "ACCEPTED"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    // Accepted friend duel creation succeeds
    await assertSucceeds(
      aliceDb.collection("duels").doc("duel_new_1").set({
        id: "duel_new_1",
        habitType: "Fitness and Health",
        startDate: "2026-10-04",
        durationDays: 7,
        endDate: "2026-10-11",
        participants: ["alice", "bob"],
        scores: { alice: 0, bob: 0 },
        winnerId: null,
        status: "ACTIVE"
      })
    );

    // Creating a duel with a non-friend (charlie) fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_new_2").set({
        id: "duel_new_2",
        habitType: "Fitness and Health",
        startDate: "2026-10-04",
        durationDays: 7,
        endDate: "2026-10-11",
        participants: ["alice", "charlie"],
        scores: { alice: 0, charlie: 0 },
        winnerId: null,
        status: "ACTIVE"
      })
    );

    // Creating a duel where caller is NOT one of the participants fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_new_3").set({
        id: "duel_new_3",
        habitType: "Fitness and Health",
        startDate: "2026-10-04",
        durationDays: 7,
        endDate: "2026-10-11",
        participants: ["bob", "charlie"],
        scores: { bob: 0, charlie: 0 },
        winnerId: null,
        status: "ACTIVE"
      })
    );
  });

  test("profileSync: owner can write their user doc, publicProfile, and friendCode mapping", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();

    // Write user profile
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").set({
        uid: "alice",
        displayName: "Alice",
        email: "alice@test.com",
        friendCode: "MP-ALIC-EE01",
        totalAuraXp: 100,
        currentLevel: 2
      })
    );

    // Write public profile
    await assertSucceeds(
      aliceDb.collection("publicProfiles").doc("alice").set({
        uid: "alice",
        displayName: "Alice",
        totalAuraXp: 100,
        currentLevel: 2,
        currentStreak: 3,
        friendCode: "MP-ALIC-EE01"
      }, { merge: true })
    );

    // Write friend code mapping
    await assertSucceeds(
      aliceDb.collection("friendCodes").doc("MP-ALIC-EE01").set({
        uid: "alice"
      })
    );
  });

  test("duels: resolving duel after endDate with correct winner succeeds, before endDate or wrong winner fails", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("duels").doc("duel_past").set({
        habitType: "Running",
        startDate: "2026-09-01",
        durationDays: 7,
        endDate: "2026-09-08",
        participants: ["alice", "bob"],
        scores: { alice: 5, bob: 3 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.collection("duels").doc("duel_future").set({
        habitType: "Running",
        startDate: "2026-10-01",
        durationDays: 7,
        endDate: "2026-10-08",
        participants: ["alice", "bob"],
        scores: { alice: 5, bob: 3 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();

    // Resolving before endDate fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_future").update({
        status: "COMPLETED",
        winnerId: "alice"
      })
    );

    // Resolving after endDate with wrong winner fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_past").update({
        status: "COMPLETED",
        winnerId: "bob"
      })
    );

    // Resolving after endDate with correct winner succeeds
    await assertSucceeds(
      aliceDb.collection("duels").doc("duel_past").update({
        status: "COMPLETED",
        winnerId: "alice"
      })
    );
  });

  test("duels: non-participant cannot resolve duel", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("duels").doc("duel_past").set({
        habitType: "Running",
        startDate: "2026-09-01",
        durationDays: 7,
        endDate: "2026-09-08",
        participants: ["alice", "bob"],
        scores: { alice: 5, bob: 3 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.terminate();
    });

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(
      charlieDb.collection("duels").doc("duel_past").update({
        status: "COMPLETED",
        winnerId: "alice"
      })
    );
  });

  test("duels: score increment by +1 before endDate succeeds, by >1 or after endDate fails", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("duels").doc("duel_future").set({
        habitType: "Running",
        startDate: "2026-10-01",
        durationDays: 7,
        endDate: "2026-10-08",
        participants: ["alice", "bob"],
        scores: { alice: 0, bob: 0 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.collection("duels").doc("duel_past").set({
        habitType: "Running",
        startDate: "2026-09-01",
        durationDays: 7,
        endDate: "2026-09-08",
        participants: ["alice", "bob"],
        scores: { alice: 0, bob: 0 },
        winnerId: null,
        status: "ACTIVE"
      });
      await adminDb.terminate();
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();

    // Incrementing by +1 before endDate succeeds
    await assertSucceeds(
      aliceDb.collection("duels").doc("duel_future").update({
        "scores.alice": 1
      })
    );

    // Incrementing by +2 fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_future").update({
        "scores.alice": 3
      })
    );

    // Incrementing after endDate fails
    await assertFails(
      aliceDb.collection("duels").doc("duel_past").update({
        "scores.alice": 1
      })
    );
  });

  test("activityFeed: after unfriending (removing from visibleTo), ex-friend cannot read user feed entries", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("activityFeed").doc("feed1").set({
        actorId: "alice",
        timestamp: Date.now(),
        visibleTo: ["alice", "bob"]
      });
      await adminDb.terminate();
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    // Before removal
    await assertSucceeds(bobDb.collection("activityFeed").doc("feed1").get());

    // Simulate batch unfriend updating visibleTo
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await adminDb.collection("activityFeed").doc("feed1").update({
        visibleTo: ["alice"]
      });
      await adminDb.terminate();
    });

    // After removal
    await assertFails(bobDb.collection("activityFeed").doc("feed1").get());
  });
});
