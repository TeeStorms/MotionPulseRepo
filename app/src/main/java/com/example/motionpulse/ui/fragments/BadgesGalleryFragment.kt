package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
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
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.FragmentBadgesGalleryBinding
import com.example.motionpulse.domain.auth.AuthState
import com.example.motionpulse.domain.auth.AuthViewModel
import com.example.motionpulse.ui.adapters.BadgesAdapter
import com.example.motionpulse.ui.views.StateLayout
import com.example.motionpulse.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BadgesGalleryFragment : Fragment() {

    private var _binding: FragmentBadgesGalleryBinding? = null
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

    private lateinit var badgesAdapter: BadgesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgesGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Badge Gallery")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        val backIcon = ImageView(requireContext()).apply {
            setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back))
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.bg_chip)
            setOnClickListener { findNavController().popBackStack() }
        }
        binding.dropletHeader.setNavigationIconView(backIcon)

        badgesAdapter = BadgesAdapter()
        binding.rvBadges.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBadges.adapter = badgesAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.authState.collectLatest { state ->
                        val uid = (state as? AuthState.Authenticated)?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            val profileViewModel = ViewModelProvider(
                                this@BadgesGalleryFragment,
                                ProfileViewModel.Factory(db, uid)
                            )[ProfileViewModel::class.java]

                            profileViewModel.profileState.collectLatest { profileState ->
                                val badges = profileState.badges
                                binding.tvBadgeCount.text = "${badges.size} badges unlocked"

                                if (badges.isEmpty()) {
                                    binding.stateLayout.setState(StateLayout.State.EMPTY, "No badges unlocked yet")
                                } else {
                                    binding.stateLayout.setState(StateLayout.State.CONTENT)
                                    badgesAdapter.submitList(badges)
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
