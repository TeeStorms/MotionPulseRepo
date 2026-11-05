# Motion.Pulse - Friend Code Specification

## Format
- **Canonical Format**: `MP-XXXX-XXXX` (12 characters total).
- **Prefix**: `"MP-"` (case-insensitive on input, normalized to uppercase).
- **Body**: 8 alphanumeric characters grouped into two 4-character blocks separated by a hyphen (`"-"`).
- **Alphabet**: `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (30 characters). Excludes visually confusable characters: `0`, `O`, `1`, `I`, `L`.
- **Legacy Format**: `MP-XXXXXX` (6-character body, supported for backward compatibility).

## Normalization Rules
1. Trim whitespace and convert to uppercase.
2. Remove all spaces and hyphens.
3. If prefix `"MP"` is present, strip it.
4. Verify every character belongs to the alphabet.
5. If clean body length is 8, format as `MP-XXXX-XXXX`.
6. If clean body length is 6, format as `MP-XXXXXX`.
7. Otherwise, return `InvalidFormat`.
