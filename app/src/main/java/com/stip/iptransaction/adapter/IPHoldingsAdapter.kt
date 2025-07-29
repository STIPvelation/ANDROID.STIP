package com.stip.iptransaction.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.iptransaction.model.DipHoldingitem
import com.stip.stip.R
import java.text.DecimalFormat

/**
 * Adapter for displaying IP Holdings in a RecyclerView
 */
class IPHoldingsAdapter(private val context: Context) :
    RecyclerView.Adapter<IPHoldingsAdapter.ViewHolder>() {

    private var holdingsList: List<DipHoldingitem> = emptyList()

    fun setData(items: List<DipHoldingitem>) {
        holdingsList = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = holdingsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = holdingsList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.item_1_text_asset_name)
        private val returnRateTextView: TextView = itemView.findViewById(R.id.item_1_text_return_rate_value)
        private val quantityTextView: TextView = itemView.findViewById(R.id.item_1_text_quantity_value)
        private val avgPriceTextView: TextView = itemView.findViewById(R.id.item_1_text_avg_price_value)
        private val valuationTextView: TextView = itemView.findViewById(R.id.item_1_text_valuation_value)
        private val purchaseAmountTextView: TextView = itemView.findViewById(R.id.item_1_text_purchase_amount_value)
        private val profitLossTextView: TextView = itemView.findViewById(R.id.item_1_text_pl_value)

        fun bind(item: DipHoldingitem) {
            // Set asset name
            nameTextView.text = item.name

            // Set quantity
            quantityTextView.text = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(item.quantity)

            // Set average buy price
            avgPriceTextView.text = String.format("%,.0f", item.buyPrice)

            // Set valuation price
            val valuationPerUnit = if (item.quantity > 0) item.totalValuation / item.quantity else 0.0
            valuationTextView.text = String.format("%,.0f", valuationPerUnit)

            // Set purchase amount
            purchaseAmountTextView.text = String.format("%,.0f", item.totalBuyAmount)

            // Set profit/loss
            profitLossTextView.text = String.format("%+,.0f", item.profit.toDouble())
            val profitLossColor = if (item.profitRate.toDouble() >= 0)
                Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            profitLossTextView.setTextColor(profitLossColor)

            // Set return rate
            val returnRateText = String.format("%+.1f%%", item.profitRate.toDouble())
            returnRateTextView.text = returnRateText
            returnRateTextView.setTextColor(profitLossColor)
        }
    }
}