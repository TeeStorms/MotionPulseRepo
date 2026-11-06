package com.example.motionpulse.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.motionpulse.data.local.entity.*
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CsvExportManager(private val context: Context) {

    companion object {
        fun escapeCsvCell(text: String?): String {
            if (text == null) return ""
            var sanitized = text
            // CSV Injection Defense
            if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@")) {
                sanitized = "'$sanitized"
            }
            // RFC 4180 escaping
            val needsQuotes = sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")
            return if (needsQuotes) {
                "\"${sanitized.replace("\"", "\"\"")}\""
            } else {
                sanitized
            }
        }
    }

    data class ExportOptions(
        val includeHabits: Boolean = true,
        val includeCompletions: Boolean = true,
        val includeMoods: Boolean = true,
        val includeBadges: Boolean = true,
        val includeProfile: Boolean = true,
        val rangeDays: Int = 3650 // All time (~10 years)
    )

    fun exportData(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        moods: List<MoodEntity>,
        badges: List<BadgeEntity>,
        profile: UserProfileEntity?,
        options: ExportOptions
    ): File? {
        val cutoffDate = LocalDate.now().minusDays(options.rangeDays.toLong())

        val filteredCompletions = completions.filter { it.date >= cutoffDate }
        val filteredMoods = moods.filter { it.date >= cutoffDate }
        val filteredBadges = badges.filter { badge ->
            val badgeDate = badge.unlockedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            badgeDate >= cutoffDate
        }

        if ((!options.includeHabits || habits.isEmpty()) &&
            (!options.includeCompletions || filteredCompletions.isEmpty()) &&
            (!options.includeMoods || filteredMoods.isEmpty()) &&
            (!options.includeBadges || filteredBadges.isEmpty()) &&
            (!options.includeProfile || profile == null)) {
            return null
        }

        val exportDir = File(context.cacheDir, "exports")
        if (exportDir.exists()) {
            exportDir.listFiles()?.forEach { it.delete() }
        } else {
            exportDir.mkdirs()
        }

        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val zipFile = File(exportDir, "motionpulse_export_$dateStr.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->

            fun writeCsvFile(entryName: String, headers: List<String>, rows: List<List<String>>) {
                zos.putNextEntry(ZipEntry(entryName))
                // Use UTF-8 BOM so Excel reads special characters
                val writer = BufferedWriter(java.io.OutputStreamWriter(zos, Charsets.UTF_8))
                writer.write("\uFEFF")
                // Excel specific separator to force column splitting
                writer.write("sep=,\n")

                writer.write(headers.joinToString(",") { escapeCsvCell(it) } + "\n")
                rows.forEach { row ->
                    writer.write(row.joinToString(",") { escapeCsvCell(it) } + "\n")
                }
                writer.flush()
                zos.closeEntry()
            }

            if (options.includeHabits && habits.isNotEmpty()) {
                val headers = listOf("ID", "Title", "Category", "Goal Type", "Target", "Unit", "Frequency", "Reminder Time", "Created At", "Archived")
                val rows = habits.map { h ->
                    listOf(h.id, h.title, h.category, h.goalType.name, h.numericTarget?.toString() ?: "", h.numericUnit ?: "", h.frequencyType.name, h.reminderTime ?: "", h.createdAt.toString(), h.isArchived.toString())
                }
                writeCsvFile("habits.csv", headers, rows)
            }

            if (options.includeCompletions && filteredCompletions.isNotEmpty()) {
                val headers = listOf("Date", "Habit ID", "Status", "Value")
                val rows = filteredCompletions.map { c ->
                    listOf(c.date.toString(), c.habitId, c.status.name, c.numericValueLogged?.toString() ?: "")
                }
                writeCsvFile("completions.csv", headers, rows)
            }

            if (options.includeMoods && filteredMoods.isNotEmpty()) {
                val headers = listOf("Date", "Level", "Factors", "Note")
                val rows = filteredMoods.map { m ->
                    listOf(m.date.toString(), m.moodLevel.name, m.factors.joinToString(";"), m.note ?: "")
                }
                writeCsvFile("moods.csv", headers, rows)
            }

            if (options.includeBadges && filteredBadges.isNotEmpty()) {
                val headers = listOf("Type", "Unlocked At")
                val rows = filteredBadges.map { b ->
                    listOf(b.badgeType.name, b.unlockedAt.toString())
                }
                writeCsvFile("badges.csv", headers, rows)
            }

            if (options.includeProfile && profile != null) {
                val headers = listOf("Display Name", "Level", "XP", "Current Streak", "Created At")
                val rows = listOf(listOf(profile.displayName, profile.currentLevel.toString(), profile.totalAuraXp.toString(), profile.currentStreak.toString(), profile.createdAt.toString()))
                writeCsvFile("profile.csv", headers, rows)
            }

            // README.txt
            zos.putNextEntry(ZipEntry("README.txt"))
            val writer = BufferedWriter(java.io.OutputStreamWriter(zos, Charsets.UTF_8))
            writer.write("Motion.Pulse Data Export\nGenerated on: $dateStr\nContains CSV exports of your habits, completions, moods, badges, and profile summary.\n")
            writer.flush()
            zos.closeEntry()
        }

        return zipFile
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Motion.Pulse Data"))
    }
}