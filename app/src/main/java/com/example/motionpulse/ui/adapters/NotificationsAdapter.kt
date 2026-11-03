package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.CommunityNotification
import com.example.motionpulse.databinding.ItemNotificationBinding
import com.example.motionpulse.util.HapticHelper
import com.example.motionpulse.util.TimeAgoFormatter

class NotificationsAdapter(
    private val onAcceptFriend: (CommunityNotification) -> Unit,
    private val onDeclineFriend: (CommunityNotification) -> Unit
) : ListAdapter<CommunityNotification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommunityNotification) {
            binding.avatarView.setDisplayName(item.senderName)

            binding.viewUnreadDot.visibility = if (!item.isRead) View.VISIBLE else View.GONE

            val displayMsg = when (item.type) {
                "NUDGE" -> "${item.senderName} nudged you. Keep your streak going!"
                "DUEL_INVITE" -> item.message ?: "${item.senderName} invited you to a duel"
                "DUEL_RESULT" -> item.message ?: "Your duel has ended"
                "FRIEND_REQUEST" -> "${item.senderName} sent you a friend request"
                "FRIEND_ACCEPTED" -> "${item.senderName} accepted your friend request"
                else -> item.message ?: "New notification"
            }
            binding.tvMessage.text = displayMsg
            binding.tvTimeAgo.text = TimeAgoFormatter.format(item.timestamp)

            if (item.type == "FRIEND_REQUEST") {
                binding.layoutFriendActions.visibility = View.VISIBLE
                binding.btnAccept.setOnClickListener {
                    HapticHelper.performClick(it)
                    onAcceptFriend(item)
                }
                binding.btnDecline.setOnClickListener {
                    HapticHelper.performClick(it)
                    onDeclineFriend(item)
                }
            } else {
                binding.layoutFriendActions.visibility = View.GONE
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<CommunityNotification>() {
        override fun areItemsTheSame(oldItem: CommunityNotification, newItem: CommunityNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommunityNotification, newItem: CommunityNotification): Boolean {
            return oldItem == newItem
        }
    }
}
