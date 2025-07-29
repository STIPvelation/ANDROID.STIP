package com.stip.stip.order.adapter

import android.animation.ObjectAnimator
import android.graphics.Color // Color import
import android.graphics.drawable.ColorDrawable // ColorDrawable import
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.iphome.fragment.OnOrderBookItemClickListener
import com.stip.stip.iphome.model.OrderBookItem
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

class OrderBookAdapter(
    private var currentPrice: Float,
    private val listener: OnOrderBookItemClickListener
) : ListAdapter<OrderBookItem, RecyclerView.ViewHolder>(OrderBookDiffCallback()) {

    private var openPrice: Float = 0f
    private var maxValueForScale: Float = 1f
    private var currentDisplayModeIsTotalAmount: Boolean = false
    private var highlightedPrice: String? = null
    private var lastTradePrice: Float = 0f

    private val twoDecimalFormatter = DecimalFormat("#,##0.00").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = java.math.RoundingMode.DOWN
    }

    // 수량을 소수점 3번째 자리까지 표시하는 포맷터 (올림/반올림 없이 절사)
    private val quantityFormatter = DecimalFormat("#,##0.000").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        minimumFractionDigits = 3
        maximumFractionDigits = 3
        roundingMode = java.math.RoundingMode.DOWN
    }

    private val numberParseFormat = DecimalFormat.getNumberInstance(Locale.US)
    private var borderDrawable: Drawable? = null
    private var defaultBackground: Drawable? = null

    companion object {
        private const val TAG = "OrderBookAdapter"
        private const val VIEW_TYPE_SELL = 0
        private const val VIEW_TYPE_BUY = 1
    }

    fun updateOpenPrice(newOpenPrice: Float) {
        this.openPrice = newOpenPrice
        notifyDataSetChanged()
    }

    fun updateData(newList: List<OrderBookItem>, newCurrentPrice: Float) {
        Log.d(TAG, "🔁 updateData called with newCurrentPrice = $newCurrentPrice")

        val listWithoutGap = newList.filter { !it.isGap }

        // 하이라이트 position 계산
        var closestPosition = -1
        var minDifference = Float.MAX_VALUE
        var exactMatchPosition = -1
        val priceThreshold = 0.001f
        listWithoutGap.forEachIndexed { index, item ->
            try {
                val itemPrice = numberParseFormat.parse(item.price)?.toFloat() ?: 0f
                if (itemPrice > 0) {
                    val difference = kotlin.math.abs(itemPrice - newCurrentPrice)
                    if (difference <= priceThreshold) {
                        if (exactMatchPosition == -1) {
                            exactMatchPosition = index
                        }
                    }
                    if (difference < minDifference) {
                        minDifference = difference
                        closestPosition = index
                    }
                }
            } catch (_: Exception) {}
        }
        val newHighlightedPosition = if (exactMatchPosition != -1) exactMatchPosition else -1
        val oldHighlightedPrice = highlightedPrice
        highlightedPrice = if (newHighlightedPosition != -1) listWithoutGap.getOrNull(newHighlightedPosition)?.price else null

        this.currentPrice = newCurrentPrice
        this.maxValueForScale = calculateMaxValue(listWithoutGap, currentDisplayModeIsTotalAmount)

        submitList(listWithoutGap) {
            if (oldHighlightedPrice != highlightedPrice) {
                val oldIdx = if (oldHighlightedPrice != null) listWithoutGap.indexOfFirst { it.price == oldHighlightedPrice } else -1
                if (oldIdx != -1) notifyItemChanged(oldIdx)
                val newIdx = if (highlightedPrice != null) listWithoutGap.indexOfFirst { it.price == highlightedPrice } else -1
                if (newIdx != -1) notifyItemChanged(newIdx)
            }
        }
        Log.d(TAG, "✅ Final list submitted. Size: ${listWithoutGap.size}, MaxValue: $maxValueForScale")
    }

    fun updateCurrentPrice(newPrice: Float) {
        Log.d(TAG, "🔁 updateCurrentPrice called with newPrice = $newPrice")

        val currentListNoGap = currentList.filter { !it.isGap }
        var closestPosition = -1
        var minDifference = Float.MAX_VALUE
        var exactMatchPosition = -1
        val priceThreshold = 0.001f
        currentListNoGap.forEachIndexed { index, item ->
            try {
                val itemPrice = numberParseFormat.parse(item.price)?.toFloat() ?: 0f
                if (itemPrice > 0) {
                    val difference = kotlin.math.abs(itemPrice - newPrice)
                    if (difference <= priceThreshold) {
                        if (exactMatchPosition == -1) {
                            exactMatchPosition = index
                        }
                    }
                    if (difference < minDifference) {
                        minDifference = difference
                        closestPosition = index
                    }
                }
            } catch (_: Exception) {}
        }
        val newHighlightedPosition = if (exactMatchPosition != -1) exactMatchPosition else -1
        val oldHighlightedPrice = highlightedPrice
        highlightedPrice = if (newHighlightedPosition != -1) currentListNoGap.getOrNull(newHighlightedPosition)?.price else null

        this.currentPrice = newPrice
        if (oldHighlightedPrice != highlightedPrice) {
            val oldIdx = if (oldHighlightedPrice != null) currentListNoGap.indexOfFirst { it.price == oldHighlightedPrice } else -1
            if (oldIdx != -1) notifyItemChanged(oldIdx)
            val newIdx = if (highlightedPrice != null) currentListNoGap.indexOfFirst { it.price == highlightedPrice } else -1
            if (newIdx != -1) notifyItemChanged(newIdx)
        }
    }

    /**
     * 마지막 체결 가격에 맞는 호가만 하이라이트 (price 기준)
     */
    fun updateTradePrice(newTradePrice: Float) {
        lastTradePrice = newTradePrice

        val currentListNoGap = currentList.filter { !it.isGap }
        var foundPrice: String? = null
        currentListNoGap.forEach { item ->
            try {
                val itemPrice = numberParseFormat.parse(item.price)?.toFloat() ?: 0f
                if (itemPrice > 0 && itemPrice == lastTradePrice) {
                    foundPrice = item.price
                }
            } catch (_: Exception) {}
        }
        val oldHighlightedPrice = highlightedPrice
        highlightedPrice = foundPrice
        if (oldHighlightedPrice != highlightedPrice) {
            notifyDataSetChanged()
        }
    }

    fun setDisplayMode(isTotalAmount: Boolean) {
        if (currentDisplayModeIsTotalAmount != isTotalAmount) {
            currentDisplayModeIsTotalAmount = isTotalAmount
            this.maxValueForScale = calculateMaxValue(
                currentList.filter { !it.isGap },
                currentDisplayModeIsTotalAmount
            )
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun getFirstBuyOrderIndex(): Int {
        return currentList.indexOfFirst { it.isBuy }
    }
    
    /**
     * 강조 표시를 완전히 초기화
     */
    fun resetHighlight() {
        val oldHighlightedPrice = highlightedPrice
        highlightedPrice = null
        if (oldHighlightedPrice != null) {
            val idx = currentList.indexOfFirst { it.price == oldHighlightedPrice }
            if (idx != -1) notifyItemChanged(idx)
        }
    }

    // ⛔️ 반드시 아래에 DiffUtil 정의가 있어야 함!
    private class OrderBookDiffCallback : DiffUtil.ItemCallback<OrderBookItem>() {
        override fun areItemsTheSame(oldItem: OrderBookItem, newItem: OrderBookItem): Boolean {
            return oldItem.price == newItem.price && oldItem.isBuy == newItem.isBuy && oldItem.isGap == newItem.isGap
        }

        override fun areContentsTheSame(oldItem: OrderBookItem, newItem: OrderBookItem): Boolean {
            return oldItem == newItem
        }
    }

    private fun calculateMaxValue(list: List<OrderBookItem>, isTotalAmountMode: Boolean): Float {
        val maxValue = list.mapNotNull { item ->
            if (item.isGap || item.price.isBlank() || item.price == "--" || item.quantity.isBlank()) return@mapNotNull null
            try {
                val quantity = numberParseFormat.parse(item.quantity)?.toFloat() ?: 0f
                if (isTotalAmountMode) {
                    val price = numberParseFormat.parse(item.price)?.toFloat() ?: 0f
                    quantity * price
                } else {
                    quantity
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error parsing value for max calc: Q=${item.quantity}, P=${item.price}",
                    e
                )
                null
            }
        }.maxOrNull() ?: 1f
        return if (maxValue <= 0f) 1f else maxValue
    }

    override fun getItemViewType(position: Int): Int {
        return try {
            val item = getItem(position)
            when {
                !item.isBuy -> VIEW_TYPE_SELL
                else -> VIEW_TYPE_BUY
            }
        } catch (e: Exception) {
            Log.e(TAG, "IndexOutOfBounds in getItemViewType for position $position", e)
            -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        // 현재가 테두리 Drawable
        if (borderDrawable == null) {
            borderDrawable = ContextCompat.getDrawable(
                parent.context,
                R.drawable.bg_border_black
            )
        }

        // 매수/매도 항목의 기본 배경 캐싱
        if (defaultBackground == null) {
            val layoutId = if (viewType == VIEW_TYPE_SELL) {
                R.layout.item_order_book_sell
            } else {
                R.layout.item_order_book_buy
            }
            val tempView = inflater.inflate(layoutId, parent, false)
            defaultBackground = tempView.background ?: ColorDrawable(Color.TRANSPARENT)
        }

        return when (viewType) {
            VIEW_TYPE_SELL -> {
                val view = inflater.inflate(R.layout.item_order_book_sell, parent, false)
                OrderBookViewHolder(view, listener, { pos -> getItem(pos) }, this)
            }

            VIEW_TYPE_BUY -> {
                val view = inflater.inflate(R.layout.item_order_book_buy, parent, false)
                OrderBookViewHolder(view, listener, { pos -> getItem(pos) }, this)
            }

            else -> {
                Log.e(TAG, "Invalid viewType in onCreateViewHolder: $viewType")
                val emptyView = View(parent.context)
                object : RecyclerView.ViewHolder(emptyView) {}
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            val item = getItem(position)

            // ✅ 현재가 기준 시장 방향 판단 (상승/하락)
            val isMarketUp = currentPrice >= openPrice
            val textColorResId = if (isMarketUp) {
                R.color.percentage_positive_red
            } else {
                R.color.percentage_negative_blue
            }

            when (holder) {
                is OrderBookViewHolder -> {
                    holder.bind(
                        item = item,
                        currentPrice = currentPrice,
                        maxValueForScale = maxValueForScale,
                        displayModeIsTotalAmount = currentDisplayModeIsTotalAmount,
                        formatter = twoDecimalFormatter,
                        quantityFormatter = quantityFormatter,
                        borderDrawable = borderDrawable,
                        defaultBackground = defaultBackground,
                        openPrice = openPrice,
                        fixedTextColorResId = textColorResId
                        // highlightedPrice는 OrderBookAdapter의 필드로 직접 접근
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error binding view holder at position $position", e)
        }
    }



    class OrderBookViewHolder(
        itemView: View,
        private val listener: OnOrderBookItemClickListener,
        private val getItemForPosition: (Int) -> OrderBookItem?,
        private val adapter: OrderBookAdapter
    ) : RecyclerView.ViewHolder(itemView) {

        private val priceText: TextView? = itemView.findViewById(R.id.text_order_price_v1)
            ?: itemView.findViewById(R.id.text_order_price_v2)
        private val percentText: TextView? = itemView.findViewById(R.id.text_order_percentage_v1)
            ?: itemView.findViewById(R.id.text_order_percentage_v2)
        private val quantityOrTotalText: TextView? =
            itemView.findViewById(R.id.text_order_quantity_v1)
                ?: itemView.findViewById(R.id.text_order_quantity_v2)
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.quantity_progress_bar)
        private val numberParser = DecimalFormat.getNumberInstance(Locale.US)

        init {
            itemView.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    getItemForPosition(currentPosition)?.let { item ->
                        if (!item.isGap && item.price.isNotEmpty() && item.price != "--") {
                            listener.onPriceClicked(item.price)
                        }
                    }
                }
            }
        }

        fun bind(
            item: OrderBookItem,
            currentPrice: Float,
            maxValueForScale: Float,
            displayModeIsTotalAmount: Boolean,
            formatter: DecimalFormat,
            quantityFormatter: DecimalFormat,
            borderDrawable: Drawable?,
            defaultBackground: Drawable?,
            openPrice: Float,
            fixedTextColorResId: Int
        ) {
            itemView.visibility = View.VISIBLE

            if (item.price.isBlank() || item.price == "--") {
                // 빈 아이템은 내용만 숨기고 공간은 유지
                priceText?.text = ""
                percentText?.text = ""
                quantityOrTotalText?.text = ""
                progressBar?.progress = 0
                itemView.visibility = View.VISIBLE
                itemView.background = defaultBackground
                return
            }

            // 🔢 숫자 파싱
            val priceFloat = try {
                val parsedPrice = numberParser.parse(item.price)?.toFloat() ?: currentPrice
                parsedPrice
            } catch (e: Exception) {
                Log.e(TAG, "price 파싱 에러: '${item.price}'", e)
                currentPrice
            }

            val quantityDouble = try {
                numberParser.parse(item.quantity)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                0.0
            }

            priceText?.text = item.price

            val percentValue = if (currentPrice > 0f)
                ((priceFloat - currentPrice) / currentPrice) * 100
            else 0f
            percentText?.text = String.format(Locale.US, "%+.2f%%", percentValue)

            val valueForProgressBar = if (displayModeIsTotalAmount) {
                val total = priceFloat * quantityDouble.toFloat()
                quantityOrTotalText?.text = formatter.format(total)
                total
            } else {
                // 수량을 소수점 3번째 자리까지 표시
                quantityOrTotalText?.text = quantityFormatter.format(quantityDouble)
                quantityDouble.toFloat()
            }

            val textColor = ContextCompat.getColor(itemView.context, fixedTextColorResId)
            priceText?.setTextColor(textColor)
            percentText?.setTextColor(textColor)

            // 현재가 강조 표시: 정확히 하나의 행에만 테두리 적용
            val shouldHighlight = adapter.highlightedPrice != null && item.price == adapter.highlightedPrice
            if (shouldHighlight) {
                itemView.background = borderDrawable
                itemView.setPadding(4, 4, 4, 4)
            } else {
                itemView.background = defaultBackground ?: ColorDrawable(Color.TRANSPARENT)
                itemView.setPadding(0, 0, 0, 0)
            }

            val progressDrawableRes = when {
                item.isCurrentPrice -> R.drawable.progress_bar_current
                item.isBuy -> R.drawable.progress_bar_buy
                else -> R.drawable.progress_bar_sell
            }
            progressBar?.progressDrawable =
                ContextCompat.getDrawable(itemView.context, progressDrawableRes)

            progressBar?.apply {
                val progressPercent = if (maxValueForScale > 0f)
                    ((valueForProgressBar / maxValueForScale) * 100).toInt().coerceIn(0, 100)
                else 0

                progressTintList = null
                indeterminateTintList = null

                val currentProgressAnim = this.progress
                if (isAttachedToWindow && currentProgressAnim != progressPercent) {
                    ObjectAnimator.ofInt(this, "progress", currentProgressAnim, progressPercent).apply {
                        duration = 300
                        interpolator = DecelerateInterpolator()
                    }.start()
                } else {
                    this.progress = progressPercent
                }
            }
        }
    }
}
