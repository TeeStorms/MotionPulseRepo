"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeFriendCode = normalizeFriendCode;
exports.generateFriendCode = generateFriendCode;
const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
function normalizeFriendCode(input) {
    if (!input || typeof input !== 'string') {
        return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
    }
    const trimmed = input.trim().toUpperCase();
    if (!trimmed) {
        return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
    }
    let clean = trimmed.replace(/[\s-]/g, "");
    if (clean.startsWith("MP")) {
        clean = clean.substring(2);
    }
    if (!clean) {
        return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
    }
    for (let i = 0; i < clean.length; i++) {
        if (!ALPHABET.includes(clean[i])) {
            return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
        }
    }
    if (clean.length === 8) {
        return { valid: true, canonicalCode: `MP-${clean.substring(0, 4)}-${clean.substring(4, 8)}` };
    }
    else if (clean.length === 6) {
        return { valid: true, canonicalCode: `MP-${clean}` };
    }
    else {
        return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
    }
}
function generateFriendCode() {
    const chars = ALPHABET;
    let code1 = "";
    let code2 = "";
    for (let i = 0; i < 4; i++) {
        code1 += chars.charAt(Math.floor(Math.random() * chars.length));
        code2 += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `MP-${code1}-${code2}`;
}
//# sourceMappingURL=friendCodeUtil.js.map