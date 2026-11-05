package com.example.motionpulse.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.motionpulse.databinding.ItemLinkedAccountBinding
import com.example.motionpulse.util.HapticHelper
import com.example.motionpulse.util.ValidationUtils

data class LinkedAccountItem(
    val providerId: String, // "password" or "google.com"
    val displayName: String,
    val email: String,
    val isLinked: Boolean,
    val isVerified: Boolean = true
)

class LinkedAccountsAdapter(
    private val allProviders: List<String>,
    private val onAction: (LinkedAccountItem) -> Unit,
    private val onResendEmail: () -> Unit
) : ListAdapter<LinkedAccountItem, LinkedAccountsAdapter.LinkedViewHolder>(LinkedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkedViewHolder {
        val binding = ItemLinkedAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LinkedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LinkedViewHolder(private val binding: ItemLinkedAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LinkedAccountItem) {
            binding.tvProviderName.text = item.displayName
            binding.tvProviderEmail.text = item.email.ifBlank { "No email attached" }

            if (item.providerId == "password") {
                binding.layoutEmailVerification.visibility = View.VISIBLE
                if (item.isVerified) {
                    binding.tvVerificationStatus.text = "Status: Verified"
                    binding.btnResendEmail.visibility = View.GONE
                } else {
                    binding.tvVerificationStatus.text = "Status: Not verified"
                    binding.btnResendEmail.visibility = View.VISIBLE
                    binding.btnResendEmail.setOnClickListener {
                        HapticHelper.performClick(it)
                        onResendEmail()
                    }
                }
            } else {
                binding.layoutEmailVerification.visibility = View.GONE
            }

            if (item.isLinked) {
                val canUnlink = ValidationUtils.canUnlink(allProviders, item.providerId)
                if (canUnlink) {
                    binding.btnProviderAction.text = "Unlink"
                    binding.btnProviderAction.isEnabled = true
                    binding.tvGuardWarning.visibility = View.GONE
                } else {
                    binding.btnProviderAction.text = "Unlink"
                    binding.btnProviderAction.isEnabled = false
                    binding.tvGuardWarning.visibility = View.VISIBLE
                }
            } else {
                binding.tvGuardWarning.visibility = View.GONE
                if (item.providerId == "password") {
                    binding.btnProviderAction.text = "Add password"
                    binding.btnProviderAction.isEnabled = true
                } else {
                    binding.btnProviderAction.text = "Link"
                    binding.btnProviderAction.isEnabled = true
                }
            }

            binding.btnProviderAction.setOnClickListener {
                HapticHelper.performClick(it)
                onAction(item)
            }
        }
    }

    class LinkedDiffCallback : DiffUtil.ItemCallback<LinkedAccountItem>() {
        override fun areItemsTheSame(oldItem: LinkedAccountItem, newItem: LinkedAccountItem): Boolean {
            return oldItem.providerId == newItem.providerId
        }

        override fun areContentsTheSame(oldItem: LinkedAccountItem, newItem: LinkedAccountItem): Boolean {
            return oldItem == newItem
        }
    }
}
