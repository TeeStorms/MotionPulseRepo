package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.databinding.ItemHabitBinding
import com.example.motionpulse.ui.viewmodels.HabitWithStatus

class HabitsAdapter(
    private val onToggleComplete: (HabitEntity) -> Unit,
    private val onItemClick: (String) -> Unit
) : ListAdapter<HabitWithStatus, HabitsAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(private val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HabitWithStatus) {
            binding.tvTitle.text = item.habit.title
            binding.tvCategory.text = item.habit.category
            binding.tvStreak.text = "🔥 ${item.habit.currentStreak}d"
            binding.btnComplete.isActivated = item.isCompletedToday

            binding.btnComplete.setOnClickListener { onToggleComplete(item.habit) }
            binding.root.setOnClickListener { onItemClick(item.habit.id) }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitWithStatus>() {
        override fun areItemsTheSame(oldItem: HabitWithStatus, newItem: HabitWithStatus): Boolean {
            return oldItem.habit.id == newItem.habit.id
        }

        override fun areContentsTheSame(oldItem: HabitWithStatus, newItem: HabitWithStatus): Boolean {
            return oldItem == newItem
        }
    }
}
