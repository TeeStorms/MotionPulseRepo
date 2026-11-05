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
            val writer = BufferedWriter(java.io.OutputStreamWriter(zos, Charsets.UTF_8))
            
            // UTF-8 BOM
            writer.write("\uFEFF")

            if (options.includeHabits && habits.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("habits.csv"))
                writer.write("ID,Title,Category,GoalType,Target,Unit,Frequency,ReminderTime,CreatedAt,Archived\n")
                habits.forEach { h ->
                    val line = listOf(h.id, h.title, h.category, h.goalType.name, h.numericTarget?.toString() ?: "", h.numericUnit ?: "", h.frequencyType.name, h.reminderTime ?: "", h.createdAt.toString(), h.isArchived.toString())
                        .joinToString(",") { escapeCsvCell(it) } + "\n"
                    writer.write(line)
                }
                writer.flush()
                zos.closeEntry()
            }

            if (options.includeCompletions && filteredCompletions.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("completions.csv"))
                writer.write("Date,HabitId,Status,Value\n")
                filteredCompletions.forEach { c ->
                    val line = listOf(c.date.toString(), c.habitId, c.status.name, c.numericValueLogged?.toString() ?: "")
                        .joinToString(",") { escapeCsvCell(it) } + "\n"
                    writer.write(line)
                }
                writer.flush()
                zos.closeEntry()
            }

            if (options.includeMoods && filteredMoods.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("moods.csv"))
                writer.write("Date,Level,Factors,Note\n")
                filteredMoods.forEach { m ->
                    val line = listOf(m.date.toString(), m.moodLevel.name, m.factors.joinToString(";"), m.note ?: "")
                        .joinToString(",") { escapeCsvCell(it) } + "\n"
                    writer.write(line)
                }
                writer.flush()
                zos.closeEntry()
            }

            if (options.includeBadges && filteredBadges.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("badges.csv"))
                writer.write("Type,UnlockedAt\n")
                filteredBadges.forEach { b ->
                    val line = listOf(b.badgeType.name, b.unlockedAt.toString())
                        .joinToString(",") { escapeCsvCell(it) } + "\n"
                    writer.write(line)
                }
                writer.flush()
                zos.closeEntry()
            }

            if (options.includeProfile && profile != null) {
                zos.putNextEntry(ZipEntry("profile.csv"))
                writer.write("DisplayName,Level,XP,CurrentStreak,CreatedAt\n")
                val line = listOf(profile.displayName, profile.currentLevel.toString(), profile.totalAuraXp.toString(), profile.currentStreak.toString(), profile.createdAt.toString())
                    .joinToString(",") { escapeCsvCell(it) } + "\n"
                writer.write(line)
                writer.flush()
                zos.closeEntry()
            }

            // README.txt
            zos.putNextEntry(ZipEntry("README.txt"))
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
