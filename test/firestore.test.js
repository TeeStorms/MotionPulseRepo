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
    const adminDb = testEnv.authenticatedContext("alice").firestore();
    await adminDb.collection("friendRequests").doc("alice_bob").set({
      requesterUid: "alice",
      recipientUid: "bob",
      participants: ["alice", "bob"],
      status: "PENDING"
    });

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertSucceeds(
      bobDb.collection("friendRequests").doc("alice_bob").update({
        status: "ACCEPTED",
        respondedAt: Date.now()
      })
    );
  });

  test("activityFeed: user can read their own entry, but non-friend cannot read", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await aliceDb.collection("activityFeed").doc("entry1").set({
      actorId: "alice",
      actorName: "Alice",
      timestamp: Date.now(),
      eventType: "STREAK_MILESTONE"
    });

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(charlieDb.collection("activityFeed").doc("entry1").get());
  });

  test("challenges: participant can read challenge", async () => {
    const adminDb = testEnv.authenticatedContext("alice").firestore();
    await adminDb.collection("challenges").doc("c1").set({
      title: "14-Day Streak",
      participantIds: ["alice", "bob"],
      progress: { alice: 0.5, bob: 0.3 }
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(aliceDb.collection("challenges").doc("c1").get());

    const charlieDb = testEnv.authenticatedContext("charlie").firestore();
    await assertFails(charlieDb.collection("challenges").doc("c1").get());
  });

  test("duels: participant can update only their own score", async () => {
    const adminDb = testEnv.authenticatedContext("alice").firestore();
    await adminDb.collection("duels").doc("d1").set({
      habitType: "Running",
      participants: ["alice", "bob"],
      scores: { alice: 2, bob: 1 },
      status: "ACTIVE"
    });

    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    // Alice updating her own score should succeed
    await assertSucceeds(
      aliceDb.collection("duels").doc("d1").update({
        scores: { alice: 3, bob: 1 }
      })
    );

    // Alice attempting to update Bob's score should fail
    await assertFails(
      aliceDb.collection("duels").doc("d1").update({
        scores: { alice: 3, bob: 5 }
      })
    );
  });

  test("notifications: user can access only their own notifications subcollection", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("notifications").doc("n1").set({
        message: "Hello Alice",
        timestamp: Date.now()
      })
    );

    const bobDb = testEnv.authenticatedContext("bob").firestore();
    await assertFails(
      bobDb.collection("users").doc("alice").collection("notifications").doc("n1").get()
    );
  });
});
