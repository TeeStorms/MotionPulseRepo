package com.example.motionpulse.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import java.io.File

class CsvExportManager(private val context: Context) {

    fun exportCompletions(completions: List<HabitCompletionEntity>) {
        val csvHeader = "ID,HabitId,Date,Status,Value,LoggedAt\n"
        val csvContent = completions.joinToString("\n") { 
            "${it.id},${it.habitId},${it.date},${it.status},${it.numericValueLogged ?: ""},${it.loggedAt}"
        }
        
        val fileName = "habit_history.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, fileName)
        file.writeText(csvHeader + csvContent)
        
        shareFile(file)
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Habit History"))
    }
}
