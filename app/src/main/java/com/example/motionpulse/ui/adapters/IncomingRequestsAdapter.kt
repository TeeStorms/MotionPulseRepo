package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.databinding.ItemIncomingRequestBinding
import com.example.motionpulse.util.HapticHelper

class IncomingRequestsAdapter(
    private val onAccept: (FriendEntity, (Boolean) -> Unit) -> Unit,
    private val onDelete: (FriendEntity) -> Unit
) : ListAdapter<FriendEntity, IncomingRequestsAdapter.IncomingViewHolder>(IncomingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomingViewHolder {
        val binding = ItemIncomingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncomingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncomingViewHolder(private val binding: ItemIncomingRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendEntity) {
            binding.avatarView.setDisplayName(item.requesterUid)
            binding.tvName.text = "User (${item.requesterUid.take(4)}...)"

            binding.btnAccept.isEnabled = true
            binding.btnDecline.isEnabled = true

            binding.btnAccept.setOnClickListener { view ->
                HapticHelper.performClick(view)
                binding.btnAccept.isEnabled = false
                binding.btnDecline.isEnabled = false
                onAccept(item) { success ->
                    if (!success) {
                        binding.btnAccept.isEnabled = true
                        binding.btnDecline.isEnabled = true
                    }
                }
            }

            binding.btnDecline.setOnClickListener { view ->
                HapticHelper.performClick(view)
                binding.btnAccept.isEnabled = false
                binding.btnDecline.isEnabled = false
                onDelete(item)
            }
        }
    }

    class IncomingDiffCallback : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean {
            return oldItem == newItem
        }
    }
}
