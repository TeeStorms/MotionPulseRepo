const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

export interface NormalizeResult {
  valid: boolean;
  canonicalCode?: string;
  message?: string;
}

export function normalizeFriendCode(input: string): NormalizeResult {
  if (!input || typeof input !== 'string') {
    return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
  }

  // 1. Remove all whitespace, zero-width characters (\u200B,\u200C,\u200D,\uFEFF), non-breaking spaces (\u00A0), soft hyphens (\u00AD)
  const cleanedChars = input.replace(/[\s\u200B\u200C\u200D\uFEFF\u00A0\u00AD]/g, "");
  const trimmed = cleanedChars.toUpperCase();
  if (!trimmed) {
    return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
  }

  let clean = trimmed.replace(/-/g, "");
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
  } else if (clean.length === 6) {
    return { valid: true, canonicalCode: `MP-${clean}` };
  } else {
    return { valid: false, message: "That doesn't look like a friend code. Codes look like MP-XXXX-XXXX." };
  }
}

export function generateFriendCode(): string {
  const chars = ALPHABET;
  let code1 = "";
  let code2 = "";
  for (let i = 0; i < 4; i++) {
    code1 += chars.charAt(Math.floor(Math.random() * chars.length));
    code2 += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `MP-${code1}-${code2}`;
}
