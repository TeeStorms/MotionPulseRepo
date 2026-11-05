"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.resetFriendCode = exports.ensureFriendCode = exports.lookupFriendCode = void 0;
const https_1 = require("firebase-functions/v2/https");
const admin = __importStar(require("firebase-admin"));
const friendCodeUtil_1 = require("./friendCodeUtil");
admin.initializeApp();
const db = admin.firestore();
exports.lookupFriendCode = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = request.auth.uid;
    const rawCode = request.data.code;
    const norm = (0, friendCodeUtil_1.normalizeFriendCode)(rawCode);
    if (!norm.valid || !norm.canonicalCode) {
        throw new https_1.HttpsError("invalid-argument", norm.message || "Invalid format.");
    }
    const canonical = norm.canonicalCode;
    // Rate limit: 10 lookups per user per minute
    const rateLimitRef = db.collection("rateLimits").doc(`lookup_${uid}`);
    const now = Date.now();
    await db.runTransaction(async (transaction) => {
        const rlDoc = await transaction.get(rateLimitRef);
        let timestamps = rlDoc.exists ? (rlDoc.data()?.timestamps || []) : [];
        timestamps = timestamps.filter((t) => now - t < 60000);
        if (timestamps.length >= 10) {
            const oldest = timestamps[0];
            const secondsToWait = Math.ceil((60000 - (now - oldest)) / 1000);
            throw new https_1.HttpsError("resource-exhausted", `Too many searches. Try again in ${secondsToWait} seconds.`);
        }
        timestamps.push(now);
        transaction.set(rateLimitRef, { timestamps });
    });
    // Lookup path: friendCodes/{canonical}.get(), then publicProfiles/{targetUid}.get()
    let targetUid = null;
    const codeDoc = await db.collection("friendCodes").document(canonical).get();
    if (codeDoc.exists) {
        targetUid = codeDoc.data()?.uid || null;
    }
    // Fallback if missing
    if (!targetUid) {
        const publicQuery = await db.collection("publicProfiles").where("friendCode", "==", canonical).limit(1).get();
        if (!publicQuery.empty) {
            targetUid = publicQuery.docs[0].id;
        }
        else {
            const userQuery = await db.collection("users").where("friendCode", "==", canonical).limit(1).get();
            if (!userQuery.empty) {
                targetUid = userQuery.docs[0].id;
            }
        }
    }
    if (!targetUid) {
        throw new https_1.HttpsError("not-found", "No account found with that code. Check it with your friend.");
    }
    // Self-heal friendCodes mapping in a transaction
    await db.runTransaction(async (transaction) => {
        const fcRef = db.collection("friendCodes").document(canonical);
        const fcSnap = await transaction.get(fcRef);
        if (!fcSnap.exists) {
            transaction.set(fcRef, { uid: targetUid });
        }
    });
    const publicDoc = await db.collection("publicProfiles").document(targetUid).get();
    if (!publicDoc.exists) {
        throw new https_1.HttpsError("not-found", "No account found with that code.");
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
exports.ensureFriendCode = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = request.auth.uid;
    const userRef = db.collection("users").document(uid);
    const publicRef = db.collection("publicProfiles").document(uid);
    let assignedCode = "";
    await db.runTransaction(async (transaction) => {
        const userSnap = await transaction.get(userRef);
        let currentCode = userSnap.exists ? userSnap.data()?.friendCode : null;
        if (currentCode) {
            const norm = (0, friendCodeUtil_1.normalizeFriendCode)(currentCode);
            if (norm.valid && norm.canonicalCode) {
                currentCode = norm.canonicalCode;
                const fcRef = db.collection("friendCodes").document(currentCode);
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
            const candidate = (0, friendCodeUtil_1.generateFriendCode)();
            const fcRef = db.collection("friendCodes").document(candidate);
            const fcSnap = await transaction.get(fcRef);
            if (!fcSnap.exists) {
                transaction.set(fcRef, { uid });
                assignedCode = candidate;
                break;
            }
            attempts++;
        }
        if (!assignedCode) {
            assignedCode = (0, friendCodeUtil_1.generateFriendCode)();
            transaction.set(db.collection("friendCodes").document(assignedCode), { uid });
        }
        transaction.set(userRef, { friendCode: assignedCode }, { merge: true });
        transaction.set(publicRef, { uid, displayName: userSnap.data()?.displayName || "User" }, { merge: true });
    });
    return { friendCode: assignedCode };
});
exports.resetFriendCode = (0, https_1.onCall)(async (request) => {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = request.auth.uid;
    const userRef = db.collection("users").document(uid);
    let newCode = "";
    await db.runTransaction(async (transaction) => {
        const userSnap = await transaction.get(userRef);
        const oldCode = userSnap.exists ? userSnap.data()?.friendCode : null;
        if (oldCode) {
            const norm = (0, friendCodeUtil_1.normalizeFriendCode)(oldCode);
            if (norm.valid && norm.canonicalCode) {
                transaction.delete(db.collection("friendCodes").document(norm.canonicalCode));
            }
        }
        let attempts = 0;
        while (attempts < 5) {
            const candidate = (0, friendCodeUtil_1.generateFriendCode)();
            const fcRef = db.collection("friendCodes").document(candidate);
            const fcSnap = await transaction.get(fcRef);
            if (!fcSnap.exists) {
                transaction.set(fcRef, { uid });
                newCode = candidate;
                break;
            }
            attempts++;
        }
        if (!newCode) {
            newCode = (0, friendCodeUtil_1.generateFriendCode)();
            transaction.set(db.collection("friendCodes").document(newCode), { uid });
        }
        transaction.set(userRef, { friendCode: newCode }, { merge: true });
    });
    return { friendCode: newCode };
});
//# sourceMappingURL=index.js.map