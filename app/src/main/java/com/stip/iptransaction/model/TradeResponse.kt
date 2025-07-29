package com.stip.stip.iptransaction.model

// 체결 주문 API 응답
data class TradeListResponse(
    val success: Boolean,
    val message: String,
    val data: List<TradeResponse>
)

// 체결 주문 데이터
data class TradeResponse(
    val id: String?,
    val symbol: String,
    val price: java.math.BigDecimal,
    val quantity: java.math.BigDecimal,
    val tradeAmount: java.math.BigDecimal,
    val feeValue: java.math.BigDecimal,
    val realAmount: java.math.BigDecimal,
    val timestamp: String,
    val orderDateTime: String,
    val marketPairId: String?,
    val buyOrderId: String?,
    val sellOrderId: String?,
    val isSell: Boolean
)

// 마켓 페어 정보
data class MarketPair(
    val id: String,
    val symbol: String,
    val fee: Double = 0.0
) 