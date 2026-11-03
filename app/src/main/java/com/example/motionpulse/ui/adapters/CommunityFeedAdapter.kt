package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.ActivityFeedEntry
import com.example.motionpulse.data.local.entity.FeedEventType
import com.example.motionpulse.databinding.ItemCommunityFeedBinding
import com.example.motionpulse.util.HapticHelper
import com.example.motionpulse.util.TimeAgoFormatter

class CommunityFeedAdapter(
    private val currentUserId: String,
    private val nudgedUserIdsToday: Set<String>,
    private val onReactionToggle: (ActivityFeedEntry) -> Unit,
    private val onNudge: (ActivityFeedEntry) -> Unit
) : ListAdapter<ActivityFeedEntry, CommunityFeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemCommunityFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedViewHolder(private val binding: ItemCommunityFeedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ActivityFeedEntry) {
            binding.avatarView.setDisplayName(item.actorName)

            val sentence = when (item.eventType) {
                FeedEventType.ALL_HABITS_COMPLETED -> "${item.actorName} completed all habits"
                FeedEventType.STREAK_MILESTONE -> "${item.actorName} reached a ${item.streakCount ?: 0}-day streak"
                FeedEventType.STREAK_AT_RISK -> "${item.actorName}'s streak is at risk"
            }
            binding.tvSentence.text = sentence
            binding.tvTimeAgo.text = TimeAgoFormatter.format(item.timestamp)

            // Reactions count & toggle
            val activeReactionsCount = item.reactions.filterValues { it }.size
            val isReactedByMe = item.reactions[currentUserId] == true
            binding.btnFireReaction.text = "🔥 $activeReactionsCount"
            binding.btnFireReaction.isSelected = isReactedByMe

            binding.btnFireReaction.setOnClickListener {
                HapticHelper.performClick(it)
                onReactionToggle(item)
            }

            // Nudge button
            if (item.actorId == currentUserId) {
                binding.btnNudge.visibility = View.GONE
            } else {
                binding.btnNudge.visibility = View.VISIBLE
                val isNudgedToday = nudgedUserIdsToday.contains(item.actorId)
                if (isNudgedToday) {
                    binding.btnNudge.text = "Nudged today"
                    binding.btnNudge.isEnabled = false
                } else {
                    binding.btnNudge.text = "Nudge"
                    binding.btnNudge.isEnabled = true
                    binding.btnNudge.setOnClickListener {
                        HapticHelper.performClick(it)
                        onNudge(item)
                    }
                }
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<ActivityFeedEntry>() {
        override fun areItemsTheSame(oldItem: ActivityFeedEntry, newItem: ActivityFeedEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityFeedEntry, newItem: ActivityFeedEntry): Boolean {
            return oldItem == newItem
        }
    }
}
