package com.example.motionpulse.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentLeaderboardBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.LeaderboardAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.example.motionpulse.ui.viewmodels.CommunityViewModel.LeaderboardMetric
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
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

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
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

                            setupLeaderboardContent(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupLeaderboardContent(viewModel: CommunityViewModel, currentUserId: String) {
        leaderboardAdapter = LeaderboardAdapter(
            currentUserId = currentUserId,
            onChallengeSwords = { friendProfile ->
                Toast.makeText(requireContext(), "Challenge ${friendProfile.displayName} clicked (R13)", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = leaderboardAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.retry()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.cgMetrics.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_weekly_xp) -> viewModel.setLeaderboardMetric(LeaderboardMetric.WEEKLY_XP)
                checkedIds.contains(R.id.chip_streak) -> viewModel.setLeaderboardMetric(LeaderboardMetric.STREAK)
                checkedIds.contains(R.id.chip_level) -> viewModel.setLeaderboardMetric(LeaderboardMetric.LEVEL)
            }
        }

        binding.btnInviteFriends.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join me on Motion.Pulse! Track habits, compete on the leaderboard, and build your rhythm together.")
            }
            startActivity(Intent.createChooser(shareIntent, "Invite Friends"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.leaderboardMetric.collectLatest { metric ->
                        leaderboardAdapter.setMetric(metric)
                    }
                }

                launch {
                    viewModel.leaderboardState.collectLatest { state ->
                        when (state) {
                            is UiState.Loading -> binding.stateLayout.setState(StateLayout.State.LOADING)
                            is UiState.Empty -> {
                                binding.stateLayout.setState(StateLayout.State.EMPTY, "No leaderboard data")
                                binding.cardInviteFriends.visibility = View.VISIBLE
                            }
                            is UiState.Error -> binding.stateLayout.setState(StateLayout.State.ERROR, state.message)
                            is UiState.Success -> {
                                binding.stateLayout.setState(StateLayout.State.CONTENT)
                                leaderboardAdapter.submitList(state.data)

                                // Show invite card if user is alone
                                if (state.data.size <= 1) {
                                    binding.cardInviteFriends.visibility = View.VISIBLE
                                } else {
                                    binding.cardInviteFriends.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
