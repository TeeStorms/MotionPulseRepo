package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.databinding.ItemSentRequestBinding
import com.example.motionpulse.util.HapticHelper

class SentRequestsAdapter(
    private val onCancel: (FriendEntity) -> Unit
) : ListAdapter<FriendEntity, SentRequestsAdapter.SentViewHolder>(SentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentViewHolder {
        val binding = ItemSentRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SentViewHolder(private val binding: ItemSentRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendEntity) {
            binding.tvRecipientName.text = "Request to: ${item.recipientUid}"
            binding.btnCancelRequest.setOnClickListener {
                HapticHelper.performClick(it)
                onCancel(item)
            }
        }
    }

    class SentDiffCallback : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean {
            return oldItem == newItem
        }
    }
}
