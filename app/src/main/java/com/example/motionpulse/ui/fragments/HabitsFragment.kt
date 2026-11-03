package com.example.motionpulse.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentHabitsBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.adapters.HabitsAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
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

    private lateinit var habitsAdapter: HabitsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Habits")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        setupListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val habitsViewModel = ViewModelProvider(
                                this@HabitsFragment,
                                HabitsViewModel.Factory(db, uid, requireContext())
                            )[HabitsViewModel::class.java]

                            setupHabitsContent(habitsViewModel)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddHabit.setOnClickListener {
            val sheet = com.example.motionpulse.ui.dialogs.AddHabitBottomSheetDialogFragment()
            sheet.show(childFragmentManager, "ADD_HABIT_SHEET")
        }
    }

    private fun setupHabitsContent(viewModel: HabitsViewModel) {
        habitsAdapter = HabitsAdapter(
            showOverflow = true,
            onToggleComplete = { habit -> viewModel.logHabitProgress(habit) },
            onItemClick = { habitId ->
                val bundle = Bundle().apply { putString("habitId", habitId) }
                findNavController().navigate(R.id.habit_detail, bundle)
            },
            onEdit = { _ ->
                val sheet = com.example.motionpulse.ui.dialogs.AddHabitBottomSheetDialogFragment()
                sheet.show(childFragmentManager, "EDIT_HABIT_SHEET")
            },
            onDelete = { habit -> showArchiveConfirmationDialog(viewModel, habit) }
        )

        binding.rvAllHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllHabits.adapter = habitsAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshHabits()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.cgCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty() || checkedIds.contains(R.id.chip_all)) {
                viewModel.setCategoryFilter(null)
            } else if (checkedIds.contains(R.id.chip_mind)) {
                viewModel.setCategoryFilter("Mind & Focus")
            } else if (checkedIds.contains(R.id.chip_fitness)) {
                viewModel.setCategoryFilter("Fitness & Health")
            } else if (checkedIds.contains(R.id.chip_routine)) {
                viewModel.setCategoryFilter("Daily Routine")
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allHabits.collectLatest { habits ->
                        val isFiltered = viewModel.searchQuery.value.isNotBlank() || viewModel.selectedCategory.value != null
                        if (habits.isEmpty()) {
                            if (isFiltered) {
                                binding.stateLayout.setState(StateLayout.State.EMPTY, "No habits match")
                            } else {
                                binding.stateLayout.setState(StateLayout.State.EMPTY, "No habits yet")
                            }
                        } else {
                            binding.stateLayout.setState(StateLayout.State.CONTENT)
                            habitsAdapter.submitList(habits)
                        }
                    }
                }
            }
        }
    }

    private fun showArchiveConfirmationDialog(viewModel: HabitsViewModel, habit: HabitEntity) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setTitle("Archive Habit?")
            .setMessage("Are you sure you want to archive '${habit.title}'?")
            .setPositiveButton("Archive") { _, _ ->
                viewModel.archiveHabit(habit)
                Toast.makeText(requireContext(), "Habit archived", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
