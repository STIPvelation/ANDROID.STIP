package com.stip.stip.iphome.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemIpHomeDailyQuoteBinding
import com.stip.stip.iphome.model.DailyQuote
import java.text.DecimalFormat

class DailyQuotesAdapter(private val context: Context) : ListAdapter<DailyQuote, DailyQuotesAdapter.DailyQuoteViewHolder>(DailyQuoteDiffCallback()) {

    // DecimalFormat 인스턴스
    private val priceFormatter = DecimalFormat("#,##0.00").apply {
        roundingMode = java.math.RoundingMode.DOWN
    } // 가격 포맷터
    private val percentFormatter = DecimalFormat("#,##0.00").apply {
        roundingMode = java.math.RoundingMode.DOWN
    } // 퍼센트 포맷터
    private val volumeFormatter = DecimalFormat("#,##0") // 거래량 포맷터

    inner class DailyQuoteViewHolder(private val binding: ItemIpHomeDailyQuoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dailyQuote: DailyQuote) {
            // 일자 표시 (2025-01-01 -> 2025.01.01 형식으로 변환)
            val displayDate = dailyQuote.date.replace("-", ".")
            binding.itemDateTextView.text = displayDate

            // 종가 표시
            binding.itemLastPriceTextView.text = priceFormatter.format(dailyQuote.close)

            // 전일대비 표시 (양수면 +, 음수면 - 표시)
            val changeAmountText = if (dailyQuote.changeAmount >= 0) {
                "+${priceFormatter.format(dailyQuote.changeAmount)}"
            } else {
                "${priceFormatter.format(dailyQuote.changeAmount)}"
            }
            binding.itemChangePercentTextView.text = changeAmountText
            
            // 거래량 표시
            binding.itemVolumeTextView.text = volumeFormatter.format(dailyQuote.volume)
            
            // 절대값 변화량에 따라 색상 변경
            val textColor = when {
                dailyQuote.changeAmount > 0 -> ContextCompat.getColor(context, R.color.color_rise)
                dailyQuote.changeAmount < 0 -> ContextCompat.getColor(context, R.color.color_fall)
                else -> ContextCompat.getColor(context, R.color.price_same_color)
            }
            binding.itemChangePercentTextView.setTextColor(textColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyQuoteViewHolder {
        val binding = ItemIpHomeDailyQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyQuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyQuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DailyQuoteDiffCallback : DiffUtil.ItemCallback<DailyQuote>() {
    override fun areItemsTheSame(oldItem: DailyQuote, newItem: DailyQuote): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DailyQuote, newItem: DailyQuote): Boolean {
        return oldItem == newItem
    }
}
