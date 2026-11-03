package com.example.motionpulse.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.R
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.databinding.ItemHabitCardBinding
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.ui.viewmodels.HabitWithStatus
import com.example.motionpulse.util.HapticHelper

class HabitsAdapter(
    private val showOverflow: Boolean = false,
    private val onToggleComplete: (HabitEntity) -> Unit,
    private val onItemClick: (String) -> Unit,
    private val onEdit: ((HabitEntity) -> Unit)? = null,
    private val onDelete: ((HabitEntity) -> Unit)? = null
) : ListAdapter<HabitWithStatus, HabitsAdapter.HabitViewHolder>(HabitDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).habit.id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(private val binding: ItemHabitCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HabitWithStatus) {
            val context = binding.root.context
            binding.tvTitle.text = item.habit.title

            val freqLabel = FrequencyConfig.decodeSafe(item.habit.frequencyConfig).getFrequencyLabel()
            binding.tvMetaLine.text = "${item.habit.category} • $freqLabel"

            // Category color dot
            val dotColorHex = when {
                item.habit.category.contains("Fitness", ignoreCase = true) -> "#FF2E2E"
                item.habit.category.contains("Routine", ignoreCase = true) -> "#5E82FF"
                else -> "#A3A000"
            }
            binding.viewCategoryDot.setBackgroundColor(Color.parseColor(dotColorHex))

            // Score pill
            val scorePct = (item.habit.habitStrengthScore * 100).toInt().coerceIn(0, 100)
            binding.tvScore.text = "$scorePct%"
            binding.cardScorePill.contentDescription = "Strength score $scorePct percent"

            // Status Control (Pending vs Done)
            if (item.isCompletedToday) {
                binding.ivStatusDot.setImageResource(R.drawable.ic_check)
                binding.flStatusDotContainer.setBackgroundResource(R.drawable.bg_active_tab_circle)
            } else {
                binding.ivStatusDot.setImageResource(0)
                binding.flStatusDotContainer.setBackgroundResource(R.drawable.bg_chip)
            }

            binding.flStatusDotContainer.setOnClickListener {
                HapticHelper.performClick(it)
                onToggleComplete(item.habit)
            }

            binding.cardHabit.setOnClickListener { onItemClick(item.habit.id) }

            // Overflow 3-Dot Button
            if (showOverflow) {
                binding.btnOverflow.visibility = View.VISIBLE
                binding.btnOverflow.setOnClickListener { view ->
                    HapticHelper.performClick(view)
                    val popup = PopupMenu(context, view)
                    popup.menu.add(0, 1, 0, "Edit")
                    popup.menu.add(0, 2, 1, "Delete")
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            1 -> {
                                onEdit?.invoke(item.habit)
                                true
                            }
                            2 -> {
                                onDelete?.invoke(item.habit)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            } else {
                binding.btnOverflow.visibility = View.GONE
            }
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
