package com.example.motionpulse

import com.example.motionpulse.data.export.CsvExportManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

    @Test
    fun escapeCsvCell_normalString_returnsUnchanged() {
        assertEquals("Hello world", CsvExportManager.escapeCsvCell("Hello world"))
    }

    @Test
    fun escapeCsvCell_commaAndQuotes_escapesCorrectly() {
        assertEquals("\"Hello, \"\"world\"\"\"", CsvExportManager.escapeCsvCell("Hello, \"world\""))
    }

    @Test
    fun escapeCsvCell_injectionAttempt_prependsSingleQuote() {
        assertEquals("'=SUM(A1:A2)", CsvExportManager.escapeCsvCell("=SUM(A1:A2)"))
        assertEquals("'+cmd", CsvExportManager.escapeCsvCell("+cmd"))
        assertEquals("'-malicious", CsvExportManager.escapeCsvCell("-malicious"))
        assertEquals("'@alert", CsvExportManager.escapeCsvCell("@alert"))
    }

    @Test
    fun escapeCsvCell_accentsAndSpecialChars_handledCorrectly() {
        assertEquals("René", CsvExportManager.escapeCsvCell("René"))
        assertEquals("\"Hello, world\nNew line\"", CsvExportManager.escapeCsvCell("Hello, world\nNew line"))
    }
}
