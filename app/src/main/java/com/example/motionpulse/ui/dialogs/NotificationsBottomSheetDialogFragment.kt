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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.LayoutNotificationsBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.domain.models.UiState
import com.example.motionpulse.ui.adapters.NotificationsAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.CommunityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutNotificationsBinding? = null
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

    private lateinit var notificationsAdapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutNotificationsBinding.inflate(inflater, container, false)
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
                                requireActivity(),
                                CommunityViewModel.Factory(db, uid, requireContext())
                            )[CommunityViewModel::class.java]

                            setupNotificationsContent(communityViewModel, uid)
                        }
                    }
                }
            }
        }
    }

    private fun setupNotificationsContent(viewModel: CommunityViewModel, currentUserId: String) {
        viewModel.cleanupOldNotifications()

        notificationsAdapter = NotificationsAdapter(
            onAcceptFriend = { notif ->
                // The requestId is the same as the notification id
                viewModel.respondToRequest(notif.id, notif.senderId, true)
            },
            onDeclineFriend = { notif ->
                // The requestId is the same as the notification id
                viewModel.respondToRequest(notif.id, notif.senderId, false)
            }
        )

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = notificationsAdapter

        // Swipe to dismiss deletion
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = notificationsAdapter.currentList.getOrNull(position)
                if (item != null) {
                    viewModel.deleteNotification(item.id)
                    Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvNotifications)

        binding.btnClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MotionPulse_Dialog)
                .setTitle("Clear All Notifications?")
                .setMessage("Are you sure you want to clear all notifications?")
                .setPositiveButton("Clear") { _, _ ->
                    val allIds = (viewModel.notificationsState.value as? UiState.Success)?.data?.map { it.id } ?: emptyList()
                    viewModel.clearAllNotifications(allIds)
                    Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Observe nudgeFeedback (which is now used for accept/decline feedback)
                    viewModel.nudgeFeedback.collectLatest { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearNudgeFeedback()
                        }
                    }
                }

                launch {
                    viewModel.notificationsState.collectLatest { state ->
                        when (state) {
                            is UiState.Loading -> binding.stateLayout.setState(StateLayout.State.LOADING)
                            is UiState.Empty -> binding.stateLayout.setState(StateLayout.State.EMPTY, "You're all caught up")
                            is UiState.Error -> binding.stateLayout.setState(StateLayout.State.ERROR, state.message)
                            is UiState.Success -> {
                                binding.stateLayout.setState(StateLayout.State.CONTENT)
                                notificationsAdapter.submitList(state.data)

                                // Auto-mark visible unread notifications as read in one batch
                                val unreadIds = state.data.filter { !it.isRead }.map { it.id }
                                if (unreadIds.isNotEmpty()) {
                                    viewModel.markAllRead(unreadIds)
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