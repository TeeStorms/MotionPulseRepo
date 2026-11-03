package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import com.example.motionpulse.R
import com.example.motionpulse.databinding.ViewMotionPulseBottomNavBinding
import com.example.motionpulse.util.ChromeFontScaleHelper

class MotionPulseBottomNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMotionPulseBottomNavBinding
    private var navController: NavController? = null
    private var currentDestinationId: Int? = null

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false

        binding = ViewMotionPulseBottomNavBinding.inflate(LayoutInflater.from(context), this, true)

        ChromeFontScaleHelper.applyMaxFontScale(binding.labelHome)
        ChromeFontScaleHelper.applyMaxFontScale(binding.labelMood)
        ChromeFontScaleHelper.applyMaxFontScale(binding.labelHabits)
        ChromeFontScaleHelper.applyMaxFontScale(binding.labelCommunity)
        ChromeFontScaleHelper.applyMaxFontScale(binding.labelStats)
        ChromeFontScaleHelper.applyMaxFontScale(binding.labelProfile)

        setupWindowInsets()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }
    }

    fun attachNavController(navController: NavController) {
        this.navController = navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            setSelectedTabByDestination(destination.id)
        }
    }

    fun setSelectedTabByDestination(destinationId: Int) {
        if (currentDestinationId == destinationId) return
        currentDestinationId = destinationId

        resetTabStyles()

        when (destinationId) {
            R.id.dashboard -> selectTab(binding.iconContainerHome, binding.iconHome, binding.labelHome)
            R.id.mood -> selectTab(binding.iconContainerMood, binding.iconMood, binding.labelMood)
            R.id.habits -> selectTab(binding.iconContainerHabits, binding.iconHabits, binding.labelHabits)
            R.id.community -> selectTab(binding.iconContainerCommunity, binding.iconCommunity, binding.labelCommunity)
            R.id.stats -> selectTab(binding.iconContainerStats, binding.iconStats, binding.labelStats)
            R.id.profile -> selectTab(binding.iconContainerProfile, binding.iconProfile, binding.labelProfile)
        }
    }

    private fun setupClickListeners() {
        binding.tabHome.setOnClickListener { navigateTo(R.id.dashboard) }
        binding.tabMood.setOnClickListener { navigateTo(R.id.mood) }
        binding.tabHabits.setOnClickListener { navigateTo(R.id.habits) }
        binding.tabCommunity.setOnClickListener { navigateTo(R.id.community) }
        binding.tabStats.setOnClickListener { navigateTo(R.id.stats) }
        binding.tabProfile.setOnClickListener { navigateTo(R.id.profile) }
    }

    private fun navigateTo(destinationId: Int) {
        if (currentDestinationId == destinationId) return
        navController?.navigate(destinationId)
    }

    private fun resetTabStyles() {
        val inactiveColor = Color.parseColor("#B182B8")

        val containers = listOf(
            binding.iconContainerHome, binding.iconContainerMood,
            binding.iconContainerHabits, binding.iconContainerCommunity,
            binding.iconContainerStats, binding.iconContainerProfile
        )
        val icons = listOf(
            binding.iconHome, binding.iconMood,
            binding.iconHabits, binding.iconCommunity,
            binding.iconStats, binding.iconProfile
        )
        val labels = listOf(
            binding.labelHome, binding.labelMood,
            binding.labelHabits, binding.labelCommunity,
            binding.labelStats, binding.labelProfile
        )

        containers.forEach {
            it.setBackgroundResource(0)
            it.translationY = 0f
        }
        icons.forEach { it.setColorFilter(inactiveColor) }
        labels.forEach {
            it.setTextColor(inactiveColor)
            it.visibility = View.VISIBLE
        }

        // Show all 6 labels when screen width >= 360dp AND font scale <= 1.15; otherwise show selected only
        val screenWidthDp = resources.configuration.screenWidthDp
        val fontScale = resources.configuration.fontScale
        val showAllLabels = screenWidthDp >= 360 && fontScale <= 1.15f

        if (!showAllLabels) {
            labels.forEach { it.visibility = View.GONE }
        }
    }

    private fun selectTab(container: View, icon: android.widget.ImageView, label: android.widget.TextView) {
        val raiseOffsetPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            -8f,
            resources.displayMetrics
        )
        container.setBackgroundResource(R.drawable.bg_active_tab_circle)
        container.translationY = raiseOffsetPx
        icon.setColorFilter(Color.WHITE)
        label.setTextColor(Color.parseColor("#FF2A85"))
        label.visibility = View.VISIBLE
    }
}
