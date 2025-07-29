package com.stip.order.data

data class OrderRequest(
    val pairId: String,
    val quantity: Double,
    val price: Double
)

data class MarketBuyRequest(
    val pairId: String,
    val amount: Double
)

data class MarketSellRequest(
    val pairId: String,
    val quantity: Double
) 