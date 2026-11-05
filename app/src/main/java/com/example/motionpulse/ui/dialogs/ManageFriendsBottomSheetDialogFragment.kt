package com.example.motionpulse.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutManageFriendsBinding
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.IncomingRequestsAdapter
import com.example.motionpulse.ui.adapters.ManageFriendsAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageFriendsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutManageFriendsBinding? = null
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

    private lateinit var friendsAdapter: ManageFriendsAdapter
    private lateinit var incomingAdapter: IncomingRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutManageFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? com.example.motionpulse.domain.auth.AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val communityViewModel = ViewModelProvider(
                                requireActivity(),
                                CommunityViewModel.Factory(db, uid, requireContext())
                            )[CommunityViewModel::class.java]

                            setupFriendsList(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupFriendsList(viewModel: CommunityViewModel, currentUserId: String) {
        friendsAdapter = ManageFriendsAdapter(
            mutedUserIds = viewModel.mutedUserIds.value,
            onMuteToggle = { friend, mute ->
                if (mute) viewModel.muteUser(friend.uid) else viewModel.unmuteUser(friend.uid)
                Toast.makeText(requireContext(), if (mute) "Muted ${friend.displayName}" else "Unmuted ${friend.displayName}", Toast.LENGTH_SHORT).show()
            },
            onBlock = { friend ->
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                    .setTitle("Block Friend?")
                    .setMessage("Are you sure you want to block ${friend.displayName}?")
                    .setPositiveButton("Block") { _, _ ->
                        viewModel.blockUser(friend.uid)
                        Toast.makeText(requireContext(), "Blocked ${friend.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onUnfriend = { friend ->
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                    .setTitle("Unfriend ${friend.displayName}?")
                    .setMessage("Active duels with ${friend.displayName} will be cancelled.")
                    .setPositiveButton("Unfriend") { _, _ ->
                        viewModel.unfriend(friend.uid)
                        Toast.makeText(requireContext(), "Unfriended ${friend.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        incomingAdapter = IncomingRequestsAdapter(
            onAccept = { req, onComplete ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        viewModel.respondToRequest(req.id, req.requesterUid, true)
                        Snackbar.make(binding.root, "You are now friends!", Snackbar.LENGTH_SHORT).show()
                        onComplete(true)
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, e.message ?: "This request is no longer available", Snackbar.LENGTH_LONG).show()
                        onComplete(false)
                    }
                }
            },
            onDelete = { req ->
                // Decline with 5s Undo snackbar
                Snackbar.make(binding.root, "Request declined", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        // Undo action: restore or do nothing
                    }
                    .addCallback(object : com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    try {
                                        viewModel.respondToRequest(req.id, req.requesterUid, false)
                                    } catch (e: Exception) {
                                        android.util.Log.e("ManageFriendsDialog", "Error declining request on dismiss: ${e.message}", e)
                                    }
                                }
                            }
                        }
                    })
                    .show()
            }
        )

        binding.rvManageFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvManageFriends.adapter = friendsAdapter

        binding.rvIncomingRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIncomingRequests.adapter = incomingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.incomingRequestsState.collectLatest { state ->
                        if (state is UiState.Success) {
                            binding.tvIncomingHeader.visibility = View.VISIBLE
                            binding.rvIncomingRequests.visibility = View.VISIBLE
                            incomingAdapter.submitList(state.data)
                        } else {
                            binding.tvIncomingHeader.visibility = View.GONE
                            binding.rvIncomingRequests.visibility = View.GONE
                            incomingAdapter.submitList(emptyList())
                        }
                    }
                }

                launch {
                    viewModel.friendsState.collectLatest { state ->
                        when (state) {
                            is UiState.Loading -> binding.stateLayout.setState(StateLayout.State.LOADING)
                            is UiState.Empty -> binding.stateLayout.setState(StateLayout.State.EMPTY, "No friends added yet")
                            is UiState.Error -> binding.stateLayout.setState(StateLayout.State.ERROR, state.message)
                            is UiState.Success -> {
                                binding.stateLayout.setState(StateLayout.State.CONTENT)
                                friendsAdapter.submitList(state.data)
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
