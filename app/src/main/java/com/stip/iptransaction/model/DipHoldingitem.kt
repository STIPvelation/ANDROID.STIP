package com.stip.stip.iptransaction.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * DIP 보유 아이템
 */
data class DipHoldingitem(
    @SerializedName("symbol")
    val symbol: String,                     // 종목 심볼 (예: JWV)
    @SerializedName("marketPairId")
    val marketPairId: String? = null,       // 마켓 페어 ID
    @SerializedName("walletId")
    val walletId: String? = null,           // 지갑 ID
    @SerializedName("balance")
    val balance: BigDecimal,                // 보유수량
    @SerializedName("address")
    val address: String? = null,            // 지갑 주소
    @SerializedName("price")
    val price: BigDecimal,                  // 현재가
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal,             // 평가금액
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal,              // 매수금액
    @SerializedName("buyAvgPrice")
    val buyAvgPrice: BigDecimal,            // 매수평균가
    @SerializedName("profit")
    val profit: BigDecimal,                 // 평가손익
    @SerializedName("profitRate")
    val profitRate: BigDecimal,             // 수익률(%)
    
    // 기존 호환성을 위해 유지 (deprecated)
    val name: String = "",                  // 종목명 (deprecated)
    val quantity: Int = balance?.toInt() ?: 0,    // 보유수량 (deprecated)
    val buyPrice: Double = buyAvgPrice?.toDouble() ?: 0.0,          // 매수평균가 (deprecated)
    val totalValuation: Double = evalAmount?.toDouble() ?: 0.0,     // 평가금액 (deprecated)
    val totalBuyAmount: Double = buyAmount?.toDouble() ?: 0.0       // 매수금액 (deprecated)
)