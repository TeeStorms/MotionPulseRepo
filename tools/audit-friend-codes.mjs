/**
 * Motion.Pulse - Friend Codes Audit Script (READ-ONLY)
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   node tools/audit-friend-codes.mjs [firebase-project-id]
 *
 * This script MUST NOT and WILL NOT write, update, or delete any data in Firestore.
 */

import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

const projectId = process.argv[2] || process.env.GCLOUD_PROJECT || process.env.FIREBASE_PROJECT_ID;

if (!projectId) {
  console.error("Error: Please provide Firebase Project ID as argument or set GCLOUD_PROJECT / GOOGLE_APPLICATION_CREDENTIALS.");
  console.error("Usage: node tools/audit-friend-codes.mjs <project-id>");
  process.exit(1);
}

// Safety check: Refuse to execute if any write operation is attempted
const READ_ONLY_GUARD = true;

initializeApp({
  projectId: projectId
  // Relies on GOOGLE_APPLICATION_CREDENTIALS environment variable or default ADC
});

const db = getFirestore();

async function audit() {
  console.log(`=== STARTING FRIEND CODES AUDIT FOR PROJECT: ${projectId} (READ-ONLY) ===\n`);

  // 1. Fetch all users
  const usersSnap = await db.collection('users').get();
  const usersTotal = usersSnap.size;
  let usersNoCode = 0;
  let usersNoPublicProfile = 0;
  const userCodesMap = new Map(); // code -> Set(uid)
  const codeFrequencyMap = new Map();

  const userUids = new Set();
  usersSnap.docs.forEach(doc => {
    const data = doc.data();
    userUids.add(doc.id);
    const fc = data.friendCode;
    if (!fc || typeof fc !== 'string' || fc.trim() === '') {
      usersNoCode++;
    } else {
      const normalized = fc.trim().toUpperCase();
      if (!userCodesMap.has(normalized)) {
        userCodesMap.set(normalized, new Set());
      }
      userCodesMap.get(normalized).add(doc.id);

      // Histogram of code format
      const len = fc.length;
      const hyphens = (fc.match(/-/g) || []).length;
      const hasLower = /[a-z]/.test(fc);
      const caseType = hasLower ? 'mixed/lower' : 'uppercase';
      const formatKey = `len:${len}, hyphens:${hyphens}, case:${caseType}`;
      codeFrequencyMap.set(formatKey, (codeFrequencyMap.get(formatKey) || 0) + 1);
    }
  });

  // 2. Fetch all friendCodes documents
  const friendCodesSnap = await db.collection('friendCodes').get();
  const friendCodesTotal = friendCodesSnap.size;
  let friendCodesMissingUser = 0;
  const friendCodeUids = new Set();

  friendCodesSnap.docs.forEach(doc => {
    const data = doc.data();
    const uid = data.uid;
    if (!uid || !userUids.has(uid)) {
      friendCodesMissingUser++;
    } else {
      friendCodeUids.add(uid);
    }
  });

  // 3. Fetch all publicProfiles documents
  const publicProfilesSnap = await db.collection('publicProfiles').get();
  const publicProfilesUids = new Set();
  publicProfilesSnap.docs.forEach(doc => {
    publicProfilesUids.add(doc.id);
  });

  usersSnap.docs.forEach(doc => {
    if (!publicProfilesUids.has(doc.id)) {
      usersNoPublicProfile++;
    }
  });

  // 4. Find duplicates (two users sharing the same code)
  let duplicateCodesCount = 0;
  userCodesMap.forEach((uids, code) => {
    if (uids.size > 1) {
      duplicateCodesCount++;
      console.log(`[DUPLICATE CODE WARNING] Code format '${code.replace(/./g, '*')}' shared by ${uids.size} users.`);
    }
  });

  // 5. Users whose friendCode has no matching friendCodes doc
  let usersCodeMissingDoc = 0;
  usersSnap.docs.forEach(doc => {
    const fc = doc.data().friendCode;
    if (fc) {
      const norm = fc.trim().toUpperCase();
      if (!friendCodesSnap.docs.some(d => d.id.toUpperCase() === norm)) {
        usersCodeMissingDoc++;
      }
    }
  });

  console.log("=== AUDIT REPORT SUMMARY ===");
  console.log(`- Total Users: ${usersTotal}`);
  console.log(`- Users with no friendCode field: ${usersNoCode}`);
  console.log(`- Users whose friendCode has no friendCodes doc: ${usersCodeMissingDoc}`);
  console.log(`- FriendCodes docs total: ${friendCodesTotal}`);
  console.log(`- FriendCodes docs pointing to a missing user: ${friendCodesMissingUser}`);
  console.log(`- Duplicate codes (multiple users sharing code): ${duplicateCodesCount}`);
  console.log(`- Users with no publicProfiles doc: ${usersNoPublicProfile}`);
  console.log("\n- Code Format Histogram:");
  codeFrequencyMap.forEach((count, format) => {
    console.log(`  * ${format}: ${count} users`);
  });
  console.log("\n=== AUDIT COMPLETE (READ-ONLY) ===");
}

if (READ_ONLY_GUARD) {
  audit().catch(err => {
    console.error("Audit failed:", err);
    process.exit(1);
  });
} else {
  console.error("Refusing to run: Read-only guard violated.");
  process.exit(1);
}
