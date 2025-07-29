package com.stip.stip.iptransaction.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.iptransaction.model.PortfolioIPHoldingDto
import com.stip.stip.api.service.MarketPairsService
import com.stip.stip.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.DecimalFormat

class PortfolioHoldingsAdapter(private var holdings: List<PortfolioIPHoldingDto>) :
    RecyclerView.Adapter<PortfolioHoldingsAdapter.PortfolioViewHolder>() {

    // marketPairId와 name, symbol 매핑을 캐시
    private val marketPairDataCache = mutableMapOf<String, Pair<String, String>>()
    private val marketPairsService: MarketPairsService by lazy {
        RetrofitClient.createTapiService(MarketPairsService::class.java)
    }

    inner class PortfolioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val assetName: TextView = itemView.findViewById(R.id.item_1_text_asset_name)
        private val quantityValue: TextView = itemView.findViewById(R.id.item_1_text_quantity_value)
        private val avgPriceValue: TextView = itemView.findViewById(R.id.item_1_text_avg_price_value)
        private val valuationValue: TextView = itemView.findViewById(R.id.item_1_text_valuation_value)
        private val purchaseAmountValue: TextView = itemView.findViewById(R.id.item_1_text_purchase_amount_value)
        private val plValue: TextView = itemView.findViewById(R.id.item_1_text_pl_value)
        private val returnRateValue: TextView = itemView.findViewById(R.id.item_1_text_return_rate_value)

        fun bind(holding: PortfolioIPHoldingDto) {
            // 먼저 symbol로 표시하고, API에서 name과 symbol을 가져온 후 업데이트
            assetName.text = holding.symbol
            
            loadMarketPairName(holding.marketPairId) { name, symbol ->
                val displayText = if (name.isNotBlank()) {
                    "$name\n$symbol"
                } else {
                    symbol
                }
                assetName.text = displayText
            }

            quantityValue.text = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN }.format(holding.balance.toDouble())
            avgPriceValue.text = "$%,.2f".format(holding.buyAvgPrice.toDouble())
            valuationValue.text = "$%,.2f".format(holding.evalAmount.toDouble())
            purchaseAmountValue.text = "$%,.2f".format(holding.buyAmount.toDouble())

            val profitFormatted = if (holding.profit >= BigDecimal.ZERO) "+$%,.2f".format(holding.profit.toDouble()) else "-$%,.2f".format(holding.profit.abs().toDouble())
            val profitRateFormatted = "%.2f%%".format(holding.profitRate.toDouble())
            val colorRes = if (holding.profit >= BigDecimal.ZERO) R.color.color_rise else R.color.color_fall
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

    /**
     * market/pairs API에서 marketPairId에 해당하는 name을 가져오는 함수
     */
    private fun loadMarketPairName(marketPairId: String, onComplete: (String, String) -> Unit) {
        // 캐시된 값이 있으면 사용
        marketPairDataCache[marketPairId]?.let { 
            onComplete(it.first, it.second) // 캐시된 값만 사용
            return
        }

        // API 호출하여 name 가져오기
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val marketPairs = marketPairsService.getMarketPairs()
                val marketPair = marketPairs.find { it.id == marketPairId }
                val name = marketPair?.name ?: ""
                val symbol = marketPair?.symbol ?: ""
                
                // 캐시에 저장
                marketPairDataCache[marketPairId] = Pair(name, symbol)
                
                // UI 업데이트는 메인 스레드에서
                withContext(Dispatchers.Main) {
                    onComplete(name, symbol)
                }
            } catch (e: Exception) {
                // 에러 발생 시 symbol 사용
                withContext(Dispatchers.Main) {
                    onComplete("", "")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_dip, parent, false)
        return PortfolioViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        val holding = holdings[position]
        holder.bind(holding)
    }

    override fun getItemCount(): Int {
        return holdings.size
    }

    fun updateData(newHoldings: List<PortfolioIPHoldingDto>) {
        this.holdings = newHoldings
        notifyDataSetChanged()
    }
} 