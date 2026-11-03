package com.example.motionpulse.ui.fragments

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
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentCommunityFeedBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.CommunityFeedAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommunityFeedFragment : Fragment() {

    private var _binding: FragmentCommunityFeedBinding? = null
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

    private lateinit var feedAdapter: CommunityFeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityFeedBinding.inflate(inflater, container, false)
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

                            setupFeedContent(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupFeedContent(viewModel: CommunityViewModel, currentUserId: String) {
        feedAdapter = CommunityFeedAdapter(
            currentUserId = currentUserId,
            nudgedUserIdsToday = viewModel.nudgedUsersToday.value,
            onReactionToggle = { entry -> viewModel.toggleReaction(entry.id) },
            onNudge = { entry ->
                viewModel.sendNudge(entry.actorId)
                Toast.makeText(requireContext(), "Nudge sent to ${entry.actorName}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.adapter = feedAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.retry()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.stateLayout.onRetryListener = { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.feedState.collectLatest { state ->
                        when (state) {
                            is UiState.Loading -> binding.stateLayout.setState(StateLayout.State.LOADING)
                            is UiState.Empty -> binding.stateLayout.setState(StateLayout.State.EMPTY, "No activity yet. Add a friend to see their progress!")
                            is UiState.Error -> binding.stateLayout.setState(StateLayout.State.ERROR, state.message)
                            is UiState.Success -> {
                                binding.stateLayout.setState(StateLayout.State.CONTENT)
                                feedAdapter.submitList(state.data)
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
