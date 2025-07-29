package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName
import com.stip.stip.iphome.model.OrderBookItem

/**
 * 호가창(Order Book) API 응답 모델
 */
data class OrderBookResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: OrderBookData
)

data class OrderBookData(
    @SerializedName("buy")
    val buy: List<OrderBookOrder>,
    
    @SerializedName("sell")
    val sell: List<OrderBookOrder>
)

data class OrderBookOrder(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("quantity")
    val quantity: Double,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("createdAt")
    val createdAt: String
) {
    /**
     * OrderBookOrder를 OrderBookItem으로 변환
     */
    fun toOrderBookItem(currentPrice: Float, isBuy: Boolean): OrderBookItem {
        val percent = if (currentPrice > 0) {
            ((price - currentPrice) / currentPrice) * 100
        } else {
            0.0
        }
        
        return OrderBookItem(
            price = String.format("%.2f", price),
            quantity = String.format("%.3f", quantity),
            percent = String.format("%+.2f%%", percent),
            isBuy = isBuy,
            isCurrentPrice = false,
            isGap = false
        )
    }
} 