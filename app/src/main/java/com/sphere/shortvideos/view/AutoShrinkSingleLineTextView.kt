package com.sphere.shortvideos.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.text.TextPaint
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.sphere.shortvideos.R
import kotlin.math.max

/**
 * 单行文本自动缩放：当内容超出可用宽度时，缩小字号直到完整显示。
 */
class AutoShrinkSingleLineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var minTextSizePx = 10f * density
    private var maxTextSizePx = textSize
    private var applyingSize = false

    init {
        context.withStyledAttributes(attrs, R.styleable.AutoShrinkSingleLineTextView, defStyleAttr, 0) {
            minTextSizePx = getDimension(
                R.styleable.AutoShrinkSingleLineTextView_autoShrinkMinTextSize,
                10f * density,
            )
        }
        maxLines = 1
        isSingleLine = true
        ellipsize = null
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        refitTextIfNeeded()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) refitTextIfNeeded()
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        if (!applyingSize) {
            maxTextSizePx = textSize
            refitTextIfNeeded()
        }
    }

    private fun refitTextIfNeeded() {
        val content = text?.toString().orEmpty()
        val available = width - paddingLeft - paddingRight
        if (content.isEmpty() || available <= 0) return

        val targetSize = calculateTargetSize(content, available.toFloat())
        if (kotlin.math.abs(textSize - targetSize) > 0.5f) {
            applyingSize = true
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetSize)
            applyingSize = false
        }
    }

    private fun calculateTargetSize(content: String, availableWidth: Float): Float {
        val probePaint = TextPaint(paint)

        probePaint.textSize = maxTextSizePx
        if (probePaint.measureText(content) <= availableWidth) return maxTextSizePx

        var low = minTextSizePx.coerceAtMost(maxTextSizePx)
        var high = maxTextSizePx
        var best = low

        repeat(16) {
            val mid = (low + high) / 2f
            probePaint.textSize = mid
            if (probePaint.measureText(content) <= availableWidth) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return max(best, minTextSizePx)
    }
}
