package com.stip.dummy

import android.graphics.Color
import com.stip.ipasset.model.IpAsset
import java.text.NumberFormat
import java.util.*

/**
 * Dummy data class containing sample data for the IP Holdings screen
 * Uses AssetDummyData as the central source of asset information
 */
object IPHoldingDummyData {

    /**
     * Summary section data model
     */
    data class SummaryData(
        val holdingUsd: String = "$25,000.00",
        val totalBuy: String = "$120,000.00",
        val totalValuation: String = "$145,000.00",
        val availableOrder: String = "$20,000.00",
        val totalAsset: String = "$165,000.00",
        val valuationProfit: String = "$25,000.00",
        val profitRate: String = "+20.83%"
    )

    /**
     * Portfolio pie chart data item
     */
    data class PortfolioItem(
        val name: String,
        val value: Float,
        val color: Int
    )

    /**
     * IP Holding list item
     */
    data class IPHoldingItem(
        val id: String,
        val name: String,
        val symbol: String,
        val currentPrice: String,
        val priceChange: Float, // Percentage change
        val holdingQuantity: Float,
        val valueInUsd: String,
        val buyPrice: String,
        val profitLoss: String,
        val profitLossPercentage: Float,
        val color: Int
    )

    // Format numbers for display
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
    }

    // Get USD asset from central data
    private val usdAsset: IpAsset?
        get() = AssetDummyData.getDefaultAssets().find { it.ticker == "USD" }

    // Get total value of all non-USD assets
    private val nonUsdTotalValue: Double
        get() = AssetDummyData.getDefaultAssets()
            .filter { it.ticker != "USD" }
            .sumOf { it.value }

    // Get total buy value (simplified calculation)
    private val totalBuyValue: Double
        get() = nonUsdTotalValue * 0.83 // Assuming ~17% average profit

    // Provide dummy summary data with explicit values matching our holdings data
    fun getSummaryData(): SummaryData {
        // Calculate total values from our explicit holdings data
        val holdingUsd = 25000000.0 // ₩25,000,000
        val totalBuy = 17286250.0 // Sum of all buy values
        val totalValuation = 21286250.0 // Sum of all current values
        val availableOrder = 20000000.0 // Available for orders
        val totalAsset = holdingUsd + totalValuation // Total assets including USD
        val valuationProfit = totalValuation - totalBuy // Profit/loss on investments
        val profitRatePercent = (valuationProfit / totalBuy) * 100 // Percentage profit/loss

        return SummaryData(
            holdingUsd = "₩${formatNumber(holdingUsd)}",
            totalBuy = "₩${formatNumber(totalBuy)}",
            totalValuation = "₩${formatNumber(totalValuation)}",
            availableOrder = "₩${formatNumber(availableOrder)}",
            totalAsset = "₩${formatNumber(totalAsset + availableOrder)}",
            valuationProfit = "₩${formatNumber(valuationProfit)}",
            profitRate = formatProfitRate(profitRatePercent)
        )
    }

    // Provide dummy portfolio data for pie chart using our explicit holdings data
    fun getPortfolioData(): List<PortfolioItem> {
        // Define portfolio items based on our explicit holdings data
        return listOf(
            PortfolioItem("JWV", 10.5f, 0xFF4CAF50.toInt()),
            PortfolioItem("MDM", 14.0f, 0xFF2196F3.toInt()),
            PortfolioItem("CDM", 20.2f, 0xFFFF9800.toInt()),
            PortfolioItem("IJECT", 17.4f, 0xFF9C27B0.toInt()),
            PortfolioItem("WETALK", 13.6f, 0xFF607D8B.toInt()),
            PortfolioItem("SLEEP", 21.0f, 0xFFE91E63.toInt()),
            PortfolioItem("KCOT", 3.3f, 0xFF009688.toInt())
        )
    }

    // Get appropriate color for ticker
    private fun getColorForTicker(ticker: String): Int {
        return when (ticker) {
            "JWV" -> 0xFF4CAF50.toInt()
            "MDM" -> 0xFF2196F3.toInt()
            "CDM" -> 0xFFFF9800.toInt()
            "IJECT" -> 0xFF9C27B0.toInt()
            "WETALK" -> 0xFF607D8B.toInt()
            "SLEEP" -> 0xFFE91E63.toInt()
            "KCOT" -> 0xFF009688.toInt()
            "MSK" -> 0xFF673AB7.toInt()
            "SMT" -> 0xFFFF5722.toInt()
            "AXNO" -> 0xFF795548.toInt()
            "KATV" -> 0xFF3F51B5.toInt()
            else -> Color.GRAY
        }
    }

    // Helper methods
    private fun formatNumber(number: Double): String {
        return String.format(Locale.KOREA, "%,.0f", number)
    }

    private fun formatProfitRate(percentage: Double): String {
        val sign = if (percentage >= 0) "+" else ""
        return "$sign${String.format(Locale.KOREA, "%.2f", percentage)}%"
    }

    // Provide dummy IP holdings data with more explicit and visible values
    fun getHoldingsList(): List<IPHoldingItem> {
        // Create some explicit dummy data instead of relying on AssetDummyData
        return listOf(
            IPHoldingItem(
                id = "JWV",
                name = "JWV",
                symbol = "JWV",
                currentPrice = "₩15,000",
                priceChange = 5.2f,
                holdingQuantity = 125.0f,
                valueInUsd = "₩1,875,000",
                buyPrice = "₩12,500",
                profitLoss = "+₩312,500",
                profitLossPercentage = 20.0f,
                color = 0xFF4CAF50.toInt()
            ),
            IPHoldingItem(
                id = "MDM",
                name = "MDM",
                symbol = "MDM",
                currentPrice = "₩28,500",
                priceChange = -2.3f,
                holdingQuantity = 87.5f,
                valueInUsd = "₩2,493,750",
                buyPrice = "₩32,000",
                profitLoss = "-₩306,250",
                profitLossPercentage = -10.9f,
                color = 0xFF2196F3.toInt()
            ),
            IPHoldingItem(
                id = "CDM",
                name = "CDM",
                symbol = "CDM",
                currentPrice = "₩80,000",
                priceChange = 12.5f,
                holdingQuantity = 45.0f,
                valueInUsd = "₩3,600,000",
                buyPrice = "₩65,000",
                profitLoss = "+₩675,000",
                profitLossPercentage = 23.1f,
                color = 0xFFFF9800.toInt()
            ),
            IPHoldingItem(
                id = "IJECT",
                name = "IJECT",
                symbol = "IJECT",
                currentPrice = "₩50,000",
                priceChange = 3.8f,
                holdingQuantity = 62.0f,
                valueInUsd = "₩3,100,000",
                buyPrice = "₩46,000",
                profitLoss = "+₩248,000",
                profitLossPercentage = 8.7f,
                color = 0xFF9C27B0.toInt()
            ),
            IPHoldingItem(
                id = "WETALK",
                name = "WETALK",
                symbol = "WETALK",
                currentPrice = "₩135,000",
                priceChange = -5.2f,
                holdingQuantity = 18.0f,
                valueInUsd = "₩2,430,000",
                buyPrice = "₩145,000",
                profitLoss = "-₩180,000",
                profitLossPercentage = -6.9f,
                color = 0xFF607D8B.toInt()
            ),
            IPHoldingItem(
                id = "SLEEP",
                name = "SLEEP",
                symbol = "SLEEP",
                currentPrice = "₩18,750",
                priceChange = 15.4f,
                holdingQuantity = 200.0f,
                valueInUsd = "₩3,750,000",
                buyPrice = "₩15,000",
                profitLoss = "+₩750,000",
                profitLossPercentage = 25.0f,
                color = 0xFFE91E63.toInt()
            ),
            IPHoldingItem(
                id = "KCOT",
                name = "KCOT",
                symbol = "KCOT",
                currentPrice = "₩42,500",
                priceChange = 0.8f,
                holdingQuantity = 95.0f,
                valueInUsd = "$4,037,500",
                buyPrice = "$41,000",
                profitLoss = "+$142,500",
                profitLossPercentage = 3.7f,
                color = 0xFF009688.toInt()
            )
        )
    }
}