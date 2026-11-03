package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.databinding.ItemCompletionHistoryBinding
import java.time.format.DateTimeFormatter

class CompletionHistoryAdapter : ListAdapter<HabitCompletionEntity, CompletionHistoryAdapter.CompletionViewHolder>(CompletionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletionViewHolder {
        val binding = ItemCompletionHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompletionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompletionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompletionViewHolder(private val binding: ItemCompletionHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HabitCompletionEntity) {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            binding.tvCompletionDate.text = item.date.format(formatter)

            val loggedVal = item.numericValueLogged
            if (loggedVal != null && loggedVal > 0f) {
                binding.tvCompletionValue.text = "Logged: ${loggedVal.toInt()}"
            } else {
                binding.tvCompletionValue.text = "Completed"
            }
        }
    }

    class CompletionDiffCallback : DiffUtil.ItemCallback<HabitCompletionEntity>() {
        override fun areItemsTheSame(oldItem: HabitCompletionEntity, newItem: HabitCompletionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HabitCompletionEntity, newItem: HabitCompletionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
