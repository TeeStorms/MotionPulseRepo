package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.BadgeEntity
import com.example.motionpulse.databinding.ItemBadgeBinding
import com.example.motionpulse.util.TimeAgoFormatter

class BadgesAdapter : ListAdapter<BadgeEntity, BadgesAdapter.BadgeViewHolder>(BadgeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BadgeViewHolder(private val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BadgeEntity) {
            binding.tvBadgeName.text = item.badgeType.name.replace("_", " ")
            binding.tvBadgeDesc.text = "Unlocked achievement badge"
            binding.tvBadgeDate.text = TimeAgoFormatter.format(item.unlockedAt.toEpochMilli())
        }
    }

    class BadgeDiffCallback : DiffUtil.ItemCallback<BadgeEntity>() {
        override fun areItemsTheSame(oldItem: BadgeEntity, newItem: BadgeEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BadgeEntity, newItem: BadgeEntity): Boolean {
            return oldItem == newItem
        }
    }
}
