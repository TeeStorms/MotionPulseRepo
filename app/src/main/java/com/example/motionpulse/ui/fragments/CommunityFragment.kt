package com.example.motionpulse.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.motionpulse.R
import com.example.motionpulse.databinding.FragmentCommunityBinding
import com.google.android.material.tabs.TabLayoutMediator

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dropletHeader.setTitle("Pulse Community")
        binding.dropletHeader.setCaption("MOTION.PULSE")

        setupViewPagerAndTabs()
        setupHeaderTopActions()
    }

    private fun setupViewPagerAndTabs() {
        val adapter = CommunityTabAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Feed"
                1 -> "Leaderboard"
                2 -> "Duels"
                else -> ""
            }
        }.attach()
    }

    private fun setupHeaderTopActions() {
        // Wire top action icons to placeholders logging messages until R14/R15
        val topActionsView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val groupIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_group)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    val sheet = com.example.motionpulse.ui.dialogs.ManageFriendsBottomSheetDialogFragment()
                    sheet.show(childFragmentManager, "MANAGE_FRIENDS_SHEET")
                }
            }

            val personAddIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_person_add)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    val sheet = com.example.motionpulse.ui.dialogs.AddFriendBottomSheetDialogFragment()
                    sheet.show(childFragmentManager, "ADD_FRIEND_SHEET")
                }
            }

            val notificationsIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_notifications)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    val sheet = com.example.motionpulse.ui.dialogs.NotificationsBottomSheetDialogFragment()
                    sheet.show(childFragmentManager, "NOTIFICATIONS_SHEET")
                }
            }

            addView(groupIcon)
            addView(personAddIcon)
            addView(notificationsIcon)
        }

        binding.dropletHeader.setTopActionsView(topActionsView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class CommunityTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CommunityFeedFragment()
                1 -> LeaderboardFragment()
                2 -> DuelsFragment()
                else -> CommunityFeedFragment()
            }
        }
    }
}
