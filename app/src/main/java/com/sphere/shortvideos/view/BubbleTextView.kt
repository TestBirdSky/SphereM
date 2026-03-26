package com.sphere.shortvideos.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import com.sphere.shortvideos.R

/**
 * 顶部居中「小三角 + 圆角矩形」气泡样式的 TextView。
 *
 * 固定：圆角 12dp、箭头高度 8dp、箭头宽度 14dp、箭头水平居中。
 * 可配置：填充色、描边色、描边宽度（XML：`bubbleFillColor` / `bubbleStrokeColor` / `bubbleStrokeWidth`）。
 *
 * 默认：填充 #653AA2，白色描边 1dp。
 */
class BubbleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    companion object {
        const val ARROW_GRAVITY_CENTER = 0
        const val ARROW_GRAVITY_START = 1
    }

    private val density = resources.displayMetrics.density

    /** 圆角半径（固定 12dp） */
    private val cornerRadiusPx = 12f * density

    /** 箭头高度（固定 8dp） */
    private val arrowHeightPx = 8f * density

    /** 箭头底边宽度（固定 14dp，仅内部常量） */
    private val arrowWidthPx = 14f * density

    private var bubbleFillColor = Color.parseColor("#653AA2")
    private var bubbleStrokeColor = Color.WHITE
    private var bubbleStrokeWidthPx = 1f * density
    private var bubbleArrowGravity = ARROW_GRAVITY_CENTER

    private val bubblePath = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var pathDirty = true

    init {
        context.withStyledAttributes(attrs, R.styleable.BubbleTextView, defStyleAttr, 0) {
            bubbleFillColor = getColor(
                R.styleable.BubbleTextView_bubbleFillColor,
                Color.parseColor("#653AA2"),
            )
            bubbleStrokeColor = getColor(
                R.styleable.BubbleTextView_bubbleStrokeColor,
                Color.WHITE,
            )
            bubbleStrokeWidthPx = getDimension(
                R.styleable.BubbleTextView_bubbleStrokeWidth,
                1f * density,
            )
            bubbleArrowGravity = getInt(
                R.styleable.BubbleTextView_bubbleArrowGravity,
                ARROW_GRAVITY_CENTER,
            )
        }
        updatePaints()
        background = null
        applyBubbleContentPadding()
    }

    private fun updatePaints() {
        fillPaint.color = bubbleFillColor
        strokePaint.color = bubbleStrokeColor
        strokePaint.strokeWidth = bubbleStrokeWidthPx
    }

    /**
     * 在 XML 已应用的 padding 基础上，增加箭头区域与内边距，避免文字压住描边。
     */
    private fun applyBubbleContentPadding() {
        val strokeHalf = (bubbleStrokeWidthPx / 2f).toInt().coerceAtLeast(1)
        val innerH = (6f * density).toInt()
        val innerV = (6f * density).toInt()
        val topForArrow = (arrowHeightPx + bubbleStrokeWidthPx * 0.5f).toInt()
        setPaddingRelative(
            paddingStart + innerH + strokeHalf,
            paddingTop + topForArrow + innerV,
            paddingEnd + innerH + strokeHalf,
            paddingBottom + innerV + strokeHalf,
        )
    }

    fun setBubbleFillColor(color: Int) {
        bubbleFillColor = color
        fillPaint.color = color
        invalidate()
    }

    /**
     * 兼容外部调用 setBackgroundColor：统一转为气泡填充色，保证尖角仍然可见。
     */
    override fun setBackgroundColor(color: Int) {
        setBubbleFillColor(color)
    }

    fun setBubbleStrokeColor(color: Int) {
        bubbleStrokeColor = color
        strokePaint.color = color
        invalidate()
    }

    fun setBubbleStrokeWidthDp(widthDp: Float) {
        bubbleStrokeWidthPx = widthDp * density
        strokePaint.strokeWidth = bubbleStrokeWidthPx
        pathDirty = true
        invalidate()
    }

    fun setBubbleArrowGravity(gravity: Int) {
        val safe = if (gravity == ARROW_GRAVITY_START) ARROW_GRAVITY_START else ARROW_GRAVITY_CENTER
        if (bubbleArrowGravity == safe) return
        bubbleArrowGravity = safe
        pathDirty = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pathDirty = true
    }

    override fun draw(canvas: Canvas) {
        if (width > 0 && height > 0) {
            if (pathDirty) {
                buildBubblePath(width.toFloat(), height.toFloat())
                pathDirty = false
            }
            canvas.drawPath(bubblePath, fillPaint)
            canvas.drawPath(bubblePath, strokePaint)
        }
        super.draw(canvas)
    }

    private fun buildBubblePath(w: Float, h: Float) {
        bubblePath.rewind()
        if (w <= 0f || h <= 0f) return

        // 箭头高度不超过总高度一半，避免极小高度时路径异常
        val ah = minOf(arrowHeightPx, h * 0.45f)
        val bodyTop = ah
        val bodyHeight = h - bodyTop
        val r = minOf(cornerRadiusPx, w / 2f, bodyHeight / 2f)
        val aw = minOf(arrowWidthPx, w * 0.45f)
        val minCx = r + aw / 2f
        val maxCx = w - r - aw / 2f
        val startCx = r + aw / 2f + 12f * density
        val cxRaw = when (bubbleArrowGravity) {
            ARROW_GRAVITY_START -> startCx
            else -> w / 2f
        }
        val cx = cxRaw.coerceIn(minCx, maxCx)

        bubblePath.moveTo(r, bodyTop)
        bubblePath.lineTo(cx - aw / 2f, bodyTop)
        bubblePath.lineTo(cx, 0f)
        bubblePath.lineTo(cx + aw / 2f, bodyTop)
        bubblePath.lineTo(w - r, bodyTop)
        bubblePath.quadTo(w, bodyTop, w, bodyTop + r)
        bubblePath.lineTo(w, h - r)
        bubblePath.quadTo(w, h, w - r, h)
        bubblePath.lineTo(r, h)
        bubblePath.quadTo(0f, h, 0f, h - r)
        bubblePath.lineTo(0f, bodyTop + r)
        bubblePath.quadTo(0f, bodyTop, r, bodyTop)
        bubblePath.close()
    }

    fun setTextAndDismiss(triple: Triple<Int, Long, String>) {
        postDelayed({
            visibility = View.GONE
        }, triple.second - System.currentTimeMillis())
        val tips = triple.third
        when (triple.first) {
            0 -> {
                val fullText = context.getString(R.string.add_money_info_tips1, tips)
                setColorText(fullText, tips, "#FFDD00".toColorInt())
            }

            1 -> {
                val fullText = context.getString(R.string.add_money_info_tips2, tips)
                setColorText(fullText, tips, "#FFDD00".toColorInt())
            }

            2 -> {
                setText(R.string.add_money_info_tips3)
                setBubbleFillColor(context.resources.getColor(R.color.color_129, null))
            }
        }
        visibility = View.VISIBLE
    }
}
