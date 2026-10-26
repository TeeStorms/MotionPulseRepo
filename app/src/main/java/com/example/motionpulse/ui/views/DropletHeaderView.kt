package com.example.motionpulse.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.motionpulse.databinding.ViewDropletHeaderBinding
import com.example.motionpulse.util.ChromeFontScaleHelper

class DropletHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewDropletHeaderBinding

    init {
        binding = ViewDropletHeaderBinding.inflate(LayoutInflater.from(context), this)
        ChromeFontScaleHelper.applyMaxFontScale(binding.captionText)
    }

    fun setTitle(title: String) {
        binding.titleText.text = title
    }

    fun setCaption(caption: String?) {
        if (!caption.isNullOrBlank()) {
            binding.captionText.text = caption.uppercase()
            binding.captionText.visibility = View.VISIBLE
        } else {
            binding.captionText.visibility = View.GONE
        }
    }

    fun setTopActionsView(view: View?) {
        binding.actionsSlot.removeAllViews()
        if (view != null) {
            binding.actionsSlot.addView(view)
            binding.topRowContainer.visibility = View.VISIBLE
        } else {
            if (binding.navSlot.childCount == 0) {
                binding.topRowContainer.visibility = View.GONE
            }
        }
    }

    fun setNavigationIconView(view: View?) {
        binding.navSlot.removeAllViews()
        if (view != null) {
            binding.navSlot.addView(view)
            binding.topRowContainer.visibility = View.VISIBLE
        } else {
            if (binding.actionsSlot.childCount == 0) {
                binding.topRowContainer.visibility = View.GONE
            }
        }
    }

    fun setTrailingView(view: View?) {
        binding.trailingSlot.removeAllViews()
        if (view != null) {
            binding.trailingSlot.addView(view)
            binding.trailingSlot.visibility = View.VISIBLE
        } else {
            binding.trailingSlot.visibility = View.GONE
        }
    }
}
