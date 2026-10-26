package com.example.motionpulse.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class State {
        CONTENT,
        LOADING,
        EMPTY,
        ERROR
    }

    private val loadingView: LinearLayout
    private val emptyView: LinearLayout
    private val errorView: LinearLayout
    private val emptyTextView: TextView
    private val errorTextView: TextView
    private val retryButton: Button

    private var contentView: View? = null
    private var currentState: State = State.CONTENT

    var onRetryListener: (() -> Unit)? = null

    init {
        minimumHeight = dpToPx(200)

        // Loading Container
        loadingView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(ProgressBar(context))
            visibility = View.GONE
        }
        addView(loadingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Empty Container
        emptyTextView = TextView(context).apply {
            setTextColor(Color.parseColor("#D1C7D7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
        }
        emptyView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(emptyTextView)
            visibility = View.GONE
        }
        addView(emptyView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Error Container
        errorTextView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
        }
        retryButton = Button(context).apply {
            text = "Retry"
            setOnClickListener { onRetryListener?.invoke() }
        }
        errorView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(errorTextView)
            addView(retryButton)
            visibility = View.GONE
        }
        addView(errorView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (child != loadingView && child != emptyView && child != errorView) {
            contentView = child
            child.visibility = if (currentState == State.CONTENT) View.VISIBLE else View.GONE
        }
    }

    fun setState(state: State, message: String? = null) {
        this.currentState = state
        contentView?.visibility = if (state == State.CONTENT) View.VISIBLE else View.GONE
        loadingView.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE

        if (state == State.EMPTY) {
            emptyTextView.text = message ?: "No items available"
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }

        if (state == State.ERROR) {
            errorTextView.text = message ?: "Something went wrong"
            errorView.visibility = View.VISIBLE
        } else {
            errorView.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
