package com.stip.stip.order

import com.stip.stip.databinding.FragmentOrderContentBinding
import com.stip.stip.iphome.TradingDataHolder

// import java.text.DecimalFormat // Removed as unused

class OrderDataInitializer(
    private val binding: FragmentOrderContentBinding,
    // fixedTwoDecimalFormatter removed
    private val updateOrderAvailable: () -> Unit,
    private val updateTradingInfoContent: () -> Unit,
    private val updateTradingInfoVisibility: () -> Unit,
    private val getCurrentTicker: () -> String?,
    // getCurrentPrice lambda removed as unused within this specific class
    private val setCurrentPrice: (Float) -> Unit
) {

    fun loadDataAndSetInitialState() {
        val currentTicker = getCurrentTicker()
        val marketItem = TradingDataHolder.ipListingItems.firstOrNull { it.ticker == currentTicker }
        val price = marketItem?.currentPrice?.replace(",", "")?.toFloatOrNull() ?: 0f
        setCurrentPrice(price)

        binding.textUnitQuantity.text = currentTicker ?: "N/A"
        updateOrderAvailable()
        updateTradingInfoContent()
        updateTradingInfoVisibility()
    }
}