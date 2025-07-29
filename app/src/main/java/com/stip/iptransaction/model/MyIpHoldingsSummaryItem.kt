package com.stip.stip.iptransaction.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * IP 보유 요약 아이템
 */
data class MyIpHoldingsSummaryItem(
    @SerializedName("usdBalance")
    val usdBalance: BigDecimal,             // 보유 USD
    @SerializedName("usdAvailableBalance")
    val usdAvailableBalance: BigDecimal,    // 사용 가능한 USD
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal,             // 평가 금액
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal,              // 매수 금액
    @SerializedName("profit")
    val profit: BigDecimal,                 // 평가손익
    @SerializedName("profitRate")
    val profitRate: BigDecimal,             // 수익률(%)
    @SerializedName("totalAmount")
    val totalAmount: BigDecimal? = null,    // 총 보유자산
    
    // 기존 호환성을 위해 유지 (deprecated)
    val holdingUsd: Double = usdBalance?.toDouble() ?: 0.0,         // 보유 USD (deprecated)
    val totalBuy: Double = buyAmount?.toDouble() ?: 0.0,            // 총 매수 (deprecated)
    val totalValuation: Double = evalAmount?.toDouble() ?: 0.0,     // 총 평가 (deprecated)
    val valuationProfit: Double = profit?.toDouble() ?: 0.0,        // 평가손익 (deprecated)
    val availableOrder: Double = usdAvailableBalance?.toDouble() ?: 0.0 // 주문 가능 금액 (deprecated)
)