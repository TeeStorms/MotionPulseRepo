package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.R
import com.example.motionpulse.data.local.entity.UserProfileEntity
import com.example.motionpulse.databinding.ItemLeaderboardBinding
import com.example.motionpulse.ui.viewmodels.CommunityViewModel.LeaderboardMetric
import com.example.motionpulse.util.HapticHelper
import java.time.LocalDate

class LeaderboardAdapter(
    private val currentUserId: String,
    private var currentMetric: LeaderboardMetric = LeaderboardMetric.WEEKLY_XP,
    private val onChallengeSwords: (UserProfileEntity) -> Unit
) : ListAdapter<UserProfileEntity, LeaderboardAdapter.LeaderboardViewHolder>(LeaderboardDiffCallback()) {

    fun setMetric(metric: LeaderboardMetric) {
        this.currentMetric = metric
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class LeaderboardViewHolder(private val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UserProfileEntity, rank: Int) {
            val context = binding.root.context
            val isSelf = item.uid == currentUserId

            binding.tvRank.text = rank.toString()
            binding.avatarView.setDisplayName(item.displayName)
            binding.tvName.text = if (isSelf) "${item.displayName} (You)" else item.displayName
            binding.tvLevel.text = "Lvl ${item.currentLevel}"

            // Format Metric Value
            binding.tvMetricValue.text = when (currentMetric) {
                LeaderboardMetric.WEEKLY_XP -> {
                    val weekAgo = LocalDate.now().minusDays(7)
                    // Safely parse recentXp and sum values for the last 7 days
                    val weeklyXp = item.recentXp.filterKeys { dateStr ->
                        try {
                            val date = LocalDate.parse(dateStr)
                            !date.isBefore(weekAgo)
                        } catch (e: Exception) {
                            false
                        }
                    }.values.sum()

                    "$weeklyXp XP"
                }
                LeaderboardMetric.STREAK -> "🔥 ${item.currentStreak}d"
                LeaderboardMetric.LEVEL -> "Lvl ${item.currentLevel}"
            }

            // Self-row highlighting & swords button visibility
            if (isSelf) {
                binding.cardLeaderboardRow.strokeColor = ContextCompat.getColor(context, R.color.accent_primary)
                binding.btnChallengeSwords.visibility = View.GONE
            } else {
                binding.cardLeaderboardRow.strokeColor = ContextCompat.getColor(context, R.color.card_border)
                binding.btnChallengeSwords.visibility = View.VISIBLE
                binding.btnChallengeSwords.setOnClickListener {
                    HapticHelper.performClick(it)
                    onChallengeSwords(item)
                }
            }
        }
    }

    class LeaderboardDiffCallback : DiffUtil.ItemCallback<UserProfileEntity>() {
        override fun areItemsTheSame(oldItem: UserProfileEntity, newItem: UserProfileEntity): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserProfileEntity, newItem: UserProfileEntity): Boolean {
            return oldItem == newItem
        }
    }
}