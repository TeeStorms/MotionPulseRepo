package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.Duel
import com.example.motionpulse.databinding.ItemDuelBinding
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DuelsAdapter(
    private val currentUserId: String,
    private val myDisplayName: String = "You",
    private val friendNamesMap: Map<String, String> = emptyMap()
) : ListAdapter<Duel, DuelsAdapter.DuelViewHolder>(DuelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DuelViewHolder {
        val binding = ItemDuelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DuelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DuelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DuelViewHolder(private val binding: ItemDuelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Duel) {
            binding.tvDuelCategory.text = item.habitType.uppercase()

            val p1 = item.participants.getOrNull(0) ?: ""
            val p2 = item.participants.getOrNull(1) ?: ""
            val myScore = item.scores[currentUserId] ?: 0
            val opponentId = if (p1 == currentUserId) p2 else p1
            val opponentScore = item.scores[opponentId] ?: 0
            val opponentName = friendNamesMap[opponentId] ?: "Friend"

            binding.myAvatarView.setDisplayName(myDisplayName)
            binding.tvMyName.text = myDisplayName

            binding.opponentAvatarView.setDisplayName(opponentName)
            binding.tvOpponentName.text = opponentName

            binding.tvMyScore.text = myScore.toString()
            binding.tvOpponentScore.text = opponentScore.toString()

            // Calculate share percentage
            val totalScore = myScore + opponentScore
            if (totalScore > 0) {
                val share = (myScore.toFloat() / totalScore * 100).toInt()
                binding.pbScoreShare.progress = share
            } else {
                binding.pbScoreShare.progress = 50
            }

            // Time chip and status
            try {
                val endDate = LocalDate.parse(item.endDate)
                val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), endDate)
                when {
                    item.status == "ACTIVE" && daysLeft > 0 -> {
                        binding.tvDuelStatus.text = "$daysLeft days left"
                        binding.tvOutcomeBadge.visibility = View.GONE
                    }
                    item.status == "ACTIVE" && daysLeft == 0L -> {
                        binding.tvDuelStatus.text = "Ends today"
                        binding.tvOutcomeBadge.visibility = View.GONE
                    }
                    item.status == "COMPLETED" || daysLeft < 0 -> {
                        binding.tvDuelStatus.text = "Ended"
                        binding.tvOutcomeBadge.visibility = View.VISIBLE
                        binding.tvOutcomeBadge.text = when {
                            myScore > opponentScore -> "Victory 🎉"
                            myScore < opponentScore -> "Defeat 😔"
                            else -> "Draw 🤝"
                        }
                    }
                    else -> {
                        binding.tvDuelStatus.text = "Cancelled"
                        binding.tvOutcomeBadge.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                binding.tvDuelStatus.text = "Active"
                binding.tvOutcomeBadge.visibility = View.GONE
            }
        }
    }

    class DuelDiffCallback : DiffUtil.ItemCallback<Duel>() {
        override fun areItemsTheSame(oldItem: Duel, newItem: Duel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Duel, newItem: Duel): Boolean {
            return oldItem == newItem
        }
    }
}
