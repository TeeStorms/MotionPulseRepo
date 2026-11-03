package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.Duel
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentDuelsBinding
import com.example.motionpulse.databinding.LayoutChallengeDialogBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.DuelsAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class DuelsFragment : Fragment() {

    private var _binding: FragmentDuelsBinding? = null
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

    private lateinit var duelsAdapter: DuelsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDuelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val communityViewModel = ViewModelProvider(
                                requireParentFragment(),
                                CommunityViewModel.Factory(db, uid)
                            )[CommunityViewModel::class.java]

                            setupDuelsContent(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupDuelsContent(viewModel: CommunityViewModel, currentUserId: String) {
        val myName = viewModel.userProfile.value?.displayName ?: "You"
        val friendsList = (viewModel.friendsState.value as? UiState.Success)?.data ?: emptyList()
        val friendMap = friendsList.associate { it.uid to it.displayName }

        duelsAdapter = DuelsAdapter(
            currentUserId = currentUserId,
            myDisplayName = myName,
            friendNamesMap = friendMap
        )
        binding.rvDuels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDuels.adapter = duelsAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.retry()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.stateLayout.onRetryListener = { viewModel.retry() }

        binding.btnChallengeBanner.setOnClickListener {
            showChallengeDialog(viewModel)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.duelsState.collectLatest { state ->
                        when (state) {
                            is UiState.Loading -> binding.stateLayout.setState(StateLayout.State.LOADING)
                            is UiState.Empty -> binding.stateLayout.setState(StateLayout.State.EMPTY, "No active duels. Challenge a friend to get started!")
                            is UiState.Error -> binding.stateLayout.setState(StateLayout.State.ERROR, state.message)
                            is UiState.Success -> {
                                binding.stateLayout.setState(StateLayout.State.CONTENT)
                                // Sort: ACTIVE first (soonest end first), then COMPLETED/CANCELLED (newest first)
                                val sorted = state.data.sortedWith(
                                    compareBy<Duel> { if (it.status == "ACTIVE") 0 else 1 }
                                        .thenBy { if (it.status == "ACTIVE") it.endDate else "9999-12-31" }
                                        .thenByDescending { it.startDate }
                                )
                                duelsAdapter.submitList(sorted)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showChallengeDialog(viewModel: CommunityViewModel) {
        val dialogBinding = LayoutChallengeDialogBinding.inflate(layoutInflater)
        
        val friendsState = viewModel.friendsState.value
        val friends = (friendsState as? UiState.Success)?.data ?: emptyList()

        if (friends.isEmpty()) {
            Toast.makeText(requireContext(), "Add friends first before starting a duel!", Toast.LENGTH_LONG).show()
            return
        }

        val friendNames = friends.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, friendNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerFriend.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
            .setView(dialogBinding.root)
            .create()

        var selectedCategory = "Mind & Focus"
        dialogBinding.cgChallengeCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.contains(R.id.chip_chal_fitness) -> "Fitness & Health"
                checkedIds.contains(R.id.chip_chal_routine) -> "Daily Routine"
                else -> "Mind & Focus"
            }
        }

        dialogBinding.btnStartChallenge.setOnClickListener {
            val selectedIdx = dialogBinding.spinnerFriend.selectedItemPosition
            if (selectedIdx in friends.indices) {
                val friend = friends[selectedIdx]
                viewModel.startDuel(friend.uid, selectedCategory, friend.displayName)
                Toast.makeText(requireContext(), "Duel started! Complete your $selectedCategory habits to score.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
