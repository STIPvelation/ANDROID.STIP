package com.stip.stip.iptransaction.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.iptransaction.model.DipHoldingitem
import java.text.DecimalFormat

class DipHoldingsAdapter(private var holdings: List<DipHoldingitem>) :
    RecyclerView.Adapter<DipHoldingsAdapter.DipViewHolder>() {

    inner class DipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TODO: 아래 ID들이 실제 사용하는 item_dip.xml 파일의 ID와 정확히 일치하는지 반드시 확인하세요!
        //       (특히 '_1' 접미사 유무)
        private val assetName: TextView = itemView.findViewById(R.id.item_1_text_asset_name)
        private val quantityValue: TextView = itemView.findViewById(R.id.item_1_text_quantity_value)
        private val avgPriceValue: TextView = itemView.findViewById(R.id.item_1_text_avg_price_value)
        private val valuationValue: TextView = itemView.findViewById(R.id.item_1_text_valuation_value)
        private val purchaseAmountValue: TextView = itemView.findViewById(R.id.item_1_text_purchase_amount_value)
        private val plValue: TextView = itemView.findViewById(R.id.item_1_text_pl_value)
        private val returnRateValue: TextView = itemView.findViewById(R.id.item_1_text_return_rate_value)

        fun bind(holding: DipHoldingitem) {
            // ▼▼▼ 이 라인의 주석을 제거하세요 ▼▼▼
            assetName.text = holding.name
            // ▲▲▲ 이 라인의 주석을 제거하세요 ▲▲▲

            quantityValue.text = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(holding.balance.toDouble())
            avgPriceValue.text = "$%,.2f".format(holding.buyAvgPrice.toDouble())
            valuationValue.text = "$%,.2f".format(holding.totalValuation)
            purchaseAmountValue.text = "$%,.2f".format(holding.totalBuyAmount)

            val profitFormatted = if (holding.profit.toDouble() >= 0) "+$%,.2f".format(holding.profit.toDouble()) else "-$%,.2f".format(-holding.profit.toDouble())
            val profitRateFormatted = "%.2f%%".format(holding.profitRate.toDouble())
            val colorRes = if (holding.profit.toDouble() >= 0) R.color.color_rise else R.color.color_fall
            val color = ContextCompat.getColor(itemView.context, colorRes)

            plValue.apply {
                text = profitFormatted
                setTextColor(color)
            }

            returnRateValue.apply {
                text = profitRateFormatted
                setTextColor(color)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DipViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_dip, parent, false)
        return DipViewHolder(view)
        /* ViewBinding 사용 시
        val binding = ItemDipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DipViewHolder(binding)
        */
    }

    override fun onBindViewHolder(holder: DipViewHolder, position: Int) {
        val holding = holdings[position]
        holder.bind(holding)
    }

    override fun getItemCount(): Int {
        return holdings.size
    }

    fun updateData(newHoldings: List<DipHoldingitem>) {
        this.holdings = newHoldings
        notifyDataSetChanged()
    }
}