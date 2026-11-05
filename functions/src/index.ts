import { onCall, HttpsError } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { normalizeFriendCode, generateFriendCode } from "./friendCodeUtil";

setGlobalOptions({ region: "africa-south1" });

admin.initializeApp();
const db = admin.firestore();

export const lookupFriendCode = onCall(async (request: any) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const uid = request.auth.uid;
  const rawCode = request.data.code;

  const norm = normalizeFriendCode(rawCode);
  if (!norm.valid || !norm.canonicalCode) {
    throw new HttpsError("invalid-argument", norm.message || "Invalid format.");
  }
  const canonical = norm.canonicalCode;

  // Rate limit: 10 lookups per user per minute
  const rateLimitRef = db.collection("rateLimits").doc(`lookup_${uid}`);
  const now = Date.now();

  await db.runTransaction(async (transaction: any) => {
    const rlDoc = await transaction.get(rateLimitRef);
    let timestamps: number[] = rlDoc.exists ? (rlDoc.data()?.timestamps || []) : [];
    timestamps = timestamps.filter((t: number) => now - t < 60000);

    if (timestamps.length >= 10) {
      const oldest = timestamps[0];
      const secondsToWait = Math.ceil((60000 - (now - oldest)) / 1000);
      throw new HttpsError("resource-exhausted", `Too many searches. Try again in ${secondsToWait} seconds.`);
    }

    timestamps.push(now);
    transaction.set(rateLimitRef, { timestamps });
  });

  // Lookup path: friendCodes/{canonical}.get(), then publicProfiles/{targetUid}.get()
  let targetUid: string | null = null;
  const codeDoc = await db.collection("friendCodes").doc(canonical).get();
  if (codeDoc.exists) {
    targetUid = codeDoc.data()?.uid || null;
  }

  // Fallback if missing
  if (!targetUid) {
    const publicQuery = await db.collection("publicProfiles").where("friendCode", "==", canonical).limit(1).get();
    if (!publicQuery.empty) {
      targetUid = publicQuery.docs[0].id;
    } else {
      const userQuery = await db.collection("users").where("friendCode", "==", canonical).limit(1).get();
      if (!userQuery.empty) {
        targetUid = userQuery.docs[0].id;
      }
    }
  }

  if (!targetUid) {
    throw new HttpsError("not-found", "No account found with that code. Check it with your friend.");
  }

  // Self-heal friendCodes mapping in a transaction
  await db.runTransaction(async (transaction: any) => {
    const fcRef = db.collection("friendCodes").doc(canonical);
    const fcSnap = await transaction.get(fcRef);
    if (!fcSnap.exists) {
      transaction.set(fcRef, { uid: targetUid });
    }
  });

  const publicDoc = await db.collection("publicProfiles").doc(targetUid).get();
  if (!publicDoc.exists) {
    throw new HttpsError("not-found", "No account found with that code.");
  }

  const profileData = publicDoc.data() || {};
  return {
    uid: targetUid,
    displayName: profileData.displayName || "User",
    avatarUrl: profileData.avatarUrl || null,
    totalAuraXp: profileData.totalAuraXp || 0,
    currentLevel: profileData.currentLevel || 1,
    currentStreak: profileData.currentStreak || 0
  };
});

export const ensureFriendCode = onCall(async (request: any) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const uid = request.auth.uid;
  const userRef = db.collection("users").doc(uid);
  const publicRef = db.collection("publicProfiles").doc(uid);

  let assignedCode: string = "";

  await db.runTransaction(async (transaction: any) => {
    const userSnap = await transaction.get(userRef);
    let currentCode = userSnap.exists ? userSnap.data()?.friendCode : null;

    if (currentCode) {
      const norm = normalizeFriendCode(currentCode);
      if (norm.valid && norm.canonicalCode) {
        currentCode = norm.canonicalCode;
        const fcRef = db.collection("friendCodes").doc(currentCode);
        const fcSnap = await transaction.get(fcRef);
        if (!fcSnap.exists || fcSnap.data()?.uid === uid) {
          transaction.set(fcRef, { uid }, { merge: true });
          assignedCode = currentCode;
          return;
        }
      }
    }

    // Generate unique code with retry
    let attempts = 0;
    while (attempts < 5) {
      const candidate = generateFriendCode();
      const fcRef = db.collection("friendCodes").doc(candidate);
      const fcSnap = await transaction.get(fcRef);
      if (!fcSnap.exists) {
        transaction.set(fcRef, { uid });
        assignedCode = candidate;
        break;
      }
      attempts++;
    }

    if (!assignedCode) {
      assignedCode = generateFriendCode();
      transaction.set(db.collection("friendCodes").doc(assignedCode), { uid });
    }

    transaction.set(userRef, { friendCode: assignedCode }, { merge: true });
    transaction.set(publicRef, { uid, displayName: userSnap.data()?.displayName || "User" }, { merge: true });
  });

  return { friendCode: assignedCode };
});

export const resetFriendCode = onCall(async (request: any) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const uid = request.auth.uid;
  const userRef = db.collection("users").doc(uid);
  let newCode = "";

  await db.runTransaction(async (transaction: any) => {
    const userSnap = await transaction.get(userRef);
    const oldCode = userSnap.exists ? userSnap.data()?.friendCode : null;

    if (oldCode) {
      const norm = normalizeFriendCode(oldCode);
      if (norm.valid && norm.canonicalCode) {
        transaction.delete(db.collection("friendCodes").doc(norm.canonicalCode));
      }
    }

    let attempts = 0;
    while (attempts < 5) {
      const candidate = generateFriendCode();
      const fcRef = db.collection("friendCodes").doc(candidate);
      const fcSnap = await transaction.get(fcRef);
      if (!fcSnap.exists) {
        transaction.set(fcRef, { uid });
        newCode = candidate;
        break;
      }
      attempts++;
    }

    if (!newCode) {
      newCode = generateFriendCode();
      transaction.set(db.collection("friendCodes").doc(newCode), { uid });
    }

    transaction.set(userRef, { friendCode: newCode }, { merge: true });
  });

  return { friendCode: newCode };
});
