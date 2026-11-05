package com.example.motionpulse

import com.example.motionpulse.util.FriendCodeResult
import com.example.motionpulse.util.FriendCodeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FriendCodeUtilTest {

    private fun unescapeJson(str: String): String {
        val unicodeRegex = Regex("""\\u([0-9a-fA-F]{4})""")
        val unescapedUnicode = unicodeRegex.replace(str) { match ->
            val codeHex = match.groupValues[1]
            codeHex.toInt(16).toChar().toString()
        }
        return unescapedUnicode
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    @Test
    fun normalize_testVectors_matchesSharedVectorsJson() {
        val vectorsFile = File("../docs/friend-code-vectors.json")
        val fileToRead = if (vectorsFile.exists()) vectorsFile else File("docs/friend-code-vectors.json")
        assertTrue(fileToRead.exists())

        val content = fileToRead.readText()
        val itemRegex = Regex("""\{\s*"input":\s*"(.*?)",\s*"valid":\s*(true|false),\s*"expected":\s*("(.*?)"|null)\s*\}""")
        val matches = itemRegex.findAll(content).toList()
        assertTrue("Expected vectors in file", matches.isNotEmpty())

        for (match in matches) {
            val rawInput = match.groupValues[1]
            val input = unescapeJson(rawInput)
            val valid = match.groupValues[2].toBoolean()
            val rawExpected = if (match.groupValues[3] == "null") null else match.groupValues[4]
            val expected = if (rawExpected != null) unescapeJson(rawExpected) else null

            val res = FriendCodeUtil.normalize(input)
            if (valid) {
                assertTrue("Expected valid for input '$input'", res is FriendCodeResult.Valid)
                assertEquals("Mismatch for input '$input'", expected, (res as FriendCodeResult.Valid).canonicalCode)
            } else {
                assertTrue("Expected invalid for input '$input'", res is FriendCodeResult.InvalidFormat)
            }
        }
    }

    @Test
    fun normalize_exactCode_MP_B8P2_PF8G_succeeds() {
        val res = FriendCodeUtil.normalize("MP-B8P2-PF8G")
        assertTrue(res is FriendCodeResult.Valid)
        assertEquals("MP-B8P2-PF8G", (res as FriendCodeResult.Valid).canonicalCode)
    }

    @Test
    fun normalize_lowercaseAndSpaces_returnsCanonicalCode() {
        val res = FriendCodeUtil.normalize("  mp 2345 6789 \n")
        assertTrue(res is FriendCodeResult.Valid)
        assertEquals("MP-2345-6789", (res as FriendCodeResult.Valid).canonicalCode)
    }

    @Test
    fun normalize_missingPrefixAndHyphens_returnsCanonicalCode() {
        val res = FriendCodeUtil.normalize("23456789")
        assertTrue(res is FriendCodeResult.Valid)
        assertEquals("MP-2345-6789", (res as FriendCodeResult.Valid).canonicalCode)
    }

    @Test
    fun normalize_legacySixChar_returnsLegacyCode() {
        val res = FriendCodeUtil.normalize("MP-ABCD23")
        assertTrue(res is FriendCodeResult.Valid)
        assertEquals("MP-ABCD23", (res as FriendCodeResult.Valid).canonicalCode)
    }

    @Test
    fun normalize_tooShort_returnsInvalidFormat() {
        val res = FriendCodeUtil.normalize("MP-123")
        assertTrue(res is FriendCodeResult.InvalidFormat)
    }

    @Test
    fun normalize_tooLong_returnsInvalidFormat() {
        val res = FriendCodeUtil.normalize("MP-1234-567890")
        assertTrue(res is FriendCodeResult.InvalidFormat)
    }

    @Test
    fun normalize_invalidCharacters_returnsInvalidFormat() {
        val res = FriendCodeUtil.normalize("MP-1230-O0IL") // 0, O, I, L not in alphabet
        assertTrue(res is FriendCodeResult.InvalidFormat)
    }

    @Test
    fun formatAsYouType_formatsProgressively() {
        assertEquals("MP-2345", FriendCodeUtil.formatAsYouType("2345"))
        assertEquals("MP-2345-6", FriendCodeUtil.formatAsYouType("23456"))
        assertEquals("MP-2345-6789", FriendCodeUtil.formatAsYouType("23456789"))
    }
}
