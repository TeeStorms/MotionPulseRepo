package com.example.motionpulse.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.motionpulse.data.export.CsvExportManager
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutExportDataBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ExportDataBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutExportDataBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository by lazy { PreferenceRepository(requireContext()) }
    private val authViewModel: AuthViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(preferenceRepository = preferenceRepository) as T
            }
        }
    }

    private val db by lazy {
        Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    private var generatedExportFile: File? = null

    private val saveDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && generatedExportFile != null) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(generatedExportFile!!).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(requireContext(), "Archive saved successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutExportDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExportAction.setOnClickListener {
            startExport()
        }

        binding.btnSaveDevice.setOnClickListener {
            val file = generatedExportFile
            if (file != null && file.exists()) {
                saveDocumentLauncher.launch(file.name)
            } else {
                Toast.makeText(requireContext(), "Export file not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShareExport.setOnClickListener {
            val file = generatedExportFile
            if (file != null && file.exists()) {
                val manager = CsvExportManager(requireContext())
                manager.shareFile(file)
            } else {
                Toast.makeText(requireContext(), "Export file not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startExport() {
        val includeHabits = binding.cbHabits.isChecked
        val includeCompletions = binding.cbCompletions.isChecked
        val includeMoods = binding.cbMoods.isChecked
        val includeBadges = binding.cbBadges.isChecked
        val includeProfile = binding.cbProfile.isChecked

        val rangeDays = when (binding.rgRange.checkedRadioButtonId) {
            com.example.motionpulse.R.id.rb_30_days -> 30
            com.example.motionpulse.R.id.rb_90_days -> 90
            else -> 3650
        }

        binding.btnExportAction.visibility = View.GONE
        binding.layoutProgress.visibility = View.VISIBLE

        val uid = (authViewModel.authState.value as? AuthState.Authenticated)?.uid ?: ""
        if (uid.isEmpty()) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val habits = if (includeHabits) db.habitDao().getAllActiveHabits().first() else emptyList()
                val completions = if (includeCompletions) db.habitCompletionDao().getAllCompletionsFlow().first() else emptyList()
                val moods = if (includeMoods) db.moodDao().getMoodsForDateRange(uid, java.time.LocalDate.MIN, java.time.LocalDate.MAX).first() else emptyList()
                val badges = if (includeBadges) db.badgeDao().getBadgesForUser(uid).first() else emptyList()
                val profile = if (includeProfile) db.userProfileDao().getProfile(uid).first() else null

                val manager = CsvExportManager(requireContext())
                val options = CsvExportManager.ExportOptions(
                    includeHabits = includeHabits,
                    includeCompletions = includeCompletions,
                    includeMoods = includeMoods,
                    includeBadges = includeBadges,
                    includeProfile = includeProfile,
                    rangeDays = rangeDays
                )

                val file = manager.exportData(habits, completions, moods, badges, profile, options)

                withContext(Dispatchers.Main) {
                    binding.layoutProgress.visibility = View.GONE
                    if (file != null && file.exists()) {
                        generatedExportFile = file
                        binding.layoutResultActions.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Export archive ready", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.btnExportAction.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Nothing to export for these options", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ExportDialog", "Export failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.layoutProgress.visibility = View.GONE
                    binding.btnExportAction.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
