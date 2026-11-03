package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.R
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.databinding.ItemManageFriendBinding
import com.example.motionpulse.util.HapticHelper

class ManageFriendsAdapter(
    private val mutedUserIds: Set<String>,
    private val onMuteToggle: (UserProfileEntity, Boolean) -> Unit,
    private val onBlock: (UserProfileEntity) -> Unit,
    private val onUnfriend: (UserProfileEntity) -> Unit
) : ListAdapter<UserProfileEntity, ManageFriendsAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemManageFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(private val binding: ItemManageFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UserProfileEntity) {
            val context = binding.root.context
            binding.avatarView.setDisplayName(item.displayName)
            binding.tvName.text = item.displayName
            binding.tvDetails.text = "Lvl ${item.currentLevel} • Streak ${item.currentStreak}d"

            val isMuted = mutedUserIds.contains(item.uid)
            binding.tvMuted.visibility = if (isMuted) View.VISIBLE else View.GONE

            binding.btnMenu.setOnClickListener { view ->
                HapticHelper.performClick(view)
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, if (isMuted) "Unmute nudges" else "Mute nudges")
                popup.menu.add(0, 2, 1, "Block")
                popup.menu.add(0, 3, 2, "Unfriend")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onMuteToggle(item, !isMuted)
                            true
                        }
                        2 -> {
                            onBlock(item)
                            true
                        }
                        3 -> {
                            onUnfriend(item)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<UserProfileEntity>() {
        override fun areItemsTheSame(oldItem: UserProfileEntity, newItem: UserProfileEntity): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserProfileEntity, newItem: UserProfileEntity): Boolean {
            return oldItem == newItem
        }
    }
}
