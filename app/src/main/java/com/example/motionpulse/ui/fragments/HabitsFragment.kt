package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentHabitsBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.adapters.HabitsAdapter
import com.example.motionpulse.ui.viewmodels.HabitsViewModel
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

        binding.dropletHeader.setTitle("My Habits")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                if (uid.isNotEmpty()) {
                    val habitsViewModel = ViewModelProvider(
                        this@HabitsFragment,
                        HabitsViewModel.Factory(db, uid, requireContext())
                    )[HabitsViewModel::class.java]

                    setupHabitsRecyclerView(habitsViewModel)
                }
            }
        }
    }

    private fun setupHabitsRecyclerView(viewModel: HabitsViewModel) {
        habitsAdapter = HabitsAdapter(
            onToggleComplete = { habit -> viewModel.toggleHabitCompletion(habit) },
            onItemClick = { habitId ->
                val bundle = Bundle().apply { putString("habitId", habitId) }
                findNavController().navigate(R.id.habit_detail, bundle)
            }
        )

        binding.rvAllHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllHabits.adapter = habitsAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshHabits()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allHabits.collectLatest { habits ->
                habitsAdapter.submitList(habits)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
