/**
 * Motion.Pulse - Friend Codes Backfill Script
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   node tools/backfill-friend-codes.mjs <project-id> [--run]
 *
 * Default is a DRY RUN (prints counts and planned actions without writing).
 * Passing `--run` executes the actual backfill writes.
 */

import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

const projectId = process.argv[2] || process.env.GCLOUD_PROJECT || process.env.FIREBASE_PROJECT_ID;
const isRunMode = process.argv.includes('--run');

if (!projectId || projectId.startsWith('--')) {
  console.error("Error: Please provide Firebase Project ID.");
  console.error("Usage: node tools/backfill-friend-codes.mjs <project-id> [--run]");
  process.exit(1);
}

initializeApp({ projectId });
const db = getFirestore();

const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
function generateFriendCode() {
  let c1 = "", c2 = "";
  for (let i = 0; i < 4; i++) {
    c1 += ALPHABET.charAt(Math.floor(Math.random() * ALPHABET.length));
    c2 += ALPHABET.charAt(Math.floor(Math.random() * ALPHABET.length));
  }
  return `MP-${c1}-${c2}`;
}

async function backfill() {
  console.log(`=== FRIEND CODES BACKFILL (${isRunMode ? 'LIVE RUN' : 'DRY RUN'}) ===`);
  console.log(`Project: ${projectId}\n`);

  const usersSnap = await db.collection('users').get();
  console.log(`Total users found: ${usersSnap.size}`);

  const codeCounts = new Map();
  let missingCodeCount = 0;
  let missingMappingCount = 0;
  const actions = [];

  for (const doc of usersSnap.docs) {
    const uid = doc.id;
    const data = doc.data();
    const fc = data.friendCode;

    let validCode = null;
    if (fc && typeof fc === 'string') {
      const trimmed = fc.trim().toUpperCase();
      const clean = trimmed.replace(/[\s-]/g, "").replace(/^MP/, "");
      if (clean.length === 8 && [...clean].every(ch => ALPHABET.includes(ch))) {
        validCode = `MP-${clean.substring(0, 4)}-${clean.substring(4, 8)}`;
      } else if (clean.length === 6 && [...clean].every(ch => ALPHABET.includes(ch))) {
        validCode = `MP-${clean}`;
      }
    }

    if (!validCode) {
      missingCodeCount++;
      validCode = generateFriendCode();
      actions.push({ type: 'ASSIGN_NEW', uid, code: validCode });
    }

    codeCounts.set(validCode, (codeCounts.get(validCode) || 0) + 1);

    // Check friendCodes/{validCode}
    const fcRef = db.collection('friendCodes').document(validCode);
    const fcSnap = await fcRef.get();
    if (!fcSnap.exists) {
      missingMappingCount++;
      actions.push({ type: 'CREATE_MAPPING', uid, code: validCode });
    } else if (fcSnap.data()?.uid !== uid) {
      console.log(`[CONFLICT WARNING] Code ${validCode} assigned to user ${uid} but friendCodes doc points to ${fcSnap.data()?.uid}`);
    }
  }

  // Check for duplicate codes across users
  let duplicates = 0;
  codeCounts.forEach((count, code) => {
    if (count > 1) {
      duplicates++;
      console.log(`[DUPLICATE CODE] Code ${code} is used by ${count} users.`);
    }
  });

  console.log("\n--- AUDIT RESULTS ---");
  console.log(`- Users needing new/fixed codes: ${missingCodeCount}`);
  console.log(`- Missing friendCodes mapping documents: ${missingMappingCount}`);
  console.log(`- Duplicate codes found: ${duplicates}`);
  console.log(`- Total planned actions: ${actions.length}`);

  if (!isRunMode) {
    console.log("\n[DRY RUN] No writes performed. Run with `--run` to apply changes.");
    return;
  }

  console.log("\n--- EXECUTING BACKFILL ---");
  let executed = 0;
  for (const act of actions) {
    const batch = db.batch();
    if (act.type === 'ASSIGN_NEW') {
      batch.update(db.collection('users').document(act.uid), { friendCode: act.code });
      batch.set(db.collection('friendCodes').document(act.code), { uid: act.uid });
      batch.set(db.collection('publicProfiles').document(act.uid), { uid: act.uid, displayName: 'User' }, { merge: true });
    } else if (act.type === 'CREATE_MAPPING') {
      batch.set(db.collection('friendCodes').document(act.code), { uid: act.uid });
    }
    await batch.commit();
    executed++;
    if (executed % 50 === 0) {
      console.log(`Executed ${executed}/${actions.length} actions...`);
    }
  }

  console.log(`\n=== BACKFILL COMPLETED SUCCESSFULLY (${executed} actions executed) ===`);
}

backfill().catch(err => {
  console.error("Backfill failed:", err);
  process.exit(1);
});
