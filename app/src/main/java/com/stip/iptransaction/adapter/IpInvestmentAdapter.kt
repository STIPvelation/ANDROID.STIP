package com.stip.stip.iptransaction.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemIpIvestmentFillteredBinding
import com.stip.stip.iptransaction.model.IpInvestmentItem
import java.text.DecimalFormat
import java.util.Locale
import android.util.Log

class IpInvestmentAdapter(
    private var items: List<IpInvestmentItem> = emptyList()
) : RecyclerView.Adapter<IpInvestmentAdapter.InvestmentViewHolder>() {

    private val currencyFormatter = DecimalFormat("#,###")

    inner class InvestmentViewHolder(private val binding: ItemIpIvestmentFillteredBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IpInvestmentItem) {
            val context = binding.textViewItemType.context

            // 색상 처리
            binding.textViewItemType.setTextColor(
                when (item.type) {
                    "매수" -> ContextCompat.getColor(context, R.color.color_buy_red)
                    "매도" -> ContextCompat.getColor(context, R.color.color_sell_blue)
                    "입금" -> ContextCompat.getColor(context, R.color.color_buy_red)
                    "출금" -> ContextCompat.getColor(context, R.color.color_sell_blue)
                    else -> ContextCompat.getColor(context, R.color.color_text_default)
                }
            )

            // 공통 텍스트 컬러 & 배경 적용
            val textColor = ContextCompat.getColor(context, R.color.color_main_text)
            val backgroundColor = ContextCompat.getColor(context, R.color.white)

            binding.root.setBackgroundColor(backgroundColor)

            with(binding) {
                listOf(
                    textViewItemName,
                    textViewItemQuantity,
                    textViewItemUnitPrice,
                    textViewItemAmount,
                    textViewItemFee,
                    textViewItemSettlement,
                    textViewItemOrderTime,
                    textViewItemExecutionTime
                ).forEach {
                    it.setTextColor(textColor)
                    it.setBackgroundColor(backgroundColor)
                }
            }

            // 데이터 바인딩
            binding.textViewItemType.text = item.type
            binding.textViewItemName.text = item.name
            binding.textViewItemQuantity.text = formatNumber(item.quantity)
            binding.textViewItemUnitPrice.text = formatCurrency(item.unitPrice)
            binding.textViewItemAmount.text = formatCurrency(item.amount)
            binding.textViewItemFee.text = formatCurrencyWithCheck(item.fee)
            Log.d("IpInvestmentAdapter", "settlement=${item.settlement}")
            binding.textViewItemSettlement.text = formatCurrency(item.settlement)
            binding.textViewItemOrderTime.text = item.orderTime
            binding.textViewItemExecutionTime.text = item.executionTime
        }

        private fun formatCurrency(value: String): String {
            if (value.isBlank() || value == "-") return "-"
            return try {
                val cleanValue = value.replace("$", "").trim()
                val bigDecimal = java.math.BigDecimal(cleanValue)
                val df = DecimalFormat("#,##0.0000").apply {
                    roundingMode = java.math.RoundingMode.DOWN
                }
                "$${df.format(bigDecimal)}"
            } catch (e: Exception) {
                value
            }
        }

        private fun formatCurrencyWithCheck(value: String): String {
            // 이미 단위가 포함된 경우 ($ 또는 다른 티커 단위) 그대로 반환
            if (value.contains("$") || value.matches(Regex(".*\\s[A-Z]{2,}$"))) {
                return value
            }
            // 단위가 없는 경우에만 $ 붙이기
            return try {
                val bigDecimal = java.math.BigDecimal(value)
                "$${bigDecimal.stripTrailingZeros().toPlainString()}"
            } catch (e: Exception) {
                "$0"
            }
        }

        private fun formatNumber(value: String): String {
            return value.toDoubleOrNull()?.let {
                String.Companion.format(Locale.US, "%,d", it.toInt())
            } ?: value
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val binding = ItemIpIvestmentFillteredBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InvestmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<IpInvestmentItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}