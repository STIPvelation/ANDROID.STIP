package com.stip.stip.iphome.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.stip.stip.R

class OhlcMiniBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var open = 0f
    private var high = 0f
    private var low = 0f
    private var close = 0f
    private var isRise = true

    private val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.2f
        color = ContextCompat.getColor(context, R.color.gray_300)
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setOhlc(open: Float, high: Float, low: Float, close: Float, isRise: Boolean) {
        this.open = open
        this.high = high
        this.low = low
        this.close = close
        this.isRise = isRise
        invalidate()
    }

    fun setPriceRange(current: Float, min: Float, max: Float) {
        this.open = current
        this.close = current
        this.high = max
        this.low = min
        this.isRise = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (height == 0 || high == low) return

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val barWidth = w * 0.6f

        val priceRange = high - low
        if (priceRange <= 0f) return

        val paddingRatio = 0.2f
        val chartHeight = h * (1f - 2 * paddingRatio)
        val chartTop = h * paddingRatio

        fun toY(value: Float): Float {
            val ratio = (value - low) / priceRange
            return chartTop + chartHeight * (1f - ratio)
        }

        val yOpen = toY(open)
        val yClose = toY(close)
        val yHigh = toY(high)
        val yLow = toY(low)

        bodyPaint.color = ContextCompat.getColor(
            context,
            if (isRise) R.color.color_rise else R.color.color_fall
        )

        // 중앙 수직선 (고가~저가)
        canvas.drawLine(cx, yHigh, cx, yLow, wickPaint)

        // 시가~종가 바디 (중앙에 하나만)
        val top = minOf(yOpen, yClose)
        val bottom = maxOf(yOpen, yClose)
        canvas.drawRect(cx - barWidth / 2f, top, cx + barWidth / 2f, bottom, bodyPaint)
    }
}
