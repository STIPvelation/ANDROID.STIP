package com.stip.ipasset.usd.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * USD 자산 데이터 모델
 */
data class USDAssetData(
    @SerializedName("usdBalance")
    val usdBalance: BigDecimal = BigDecimal.ZERO,           // 총 USD 잔액
    @SerializedName("usdAvailableBalance")
    val usdAvailableBalance: BigDecimal = BigDecimal.ZERO,  // 출금 가능 금액
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal = BigDecimal.ZERO,           // 평가 금액
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal = BigDecimal.ZERO,            // 매수 금액
    @SerializedName("profit")
    val profit: BigDecimal = BigDecimal.ZERO,               // 평가손익
    @SerializedName("profitRate")
    val profitRate: BigDecimal = BigDecimal.ZERO,           // 수익률(%)
    
    // 기존 호환성을 위해 유지 (deprecated)
    val balance: Double = usdBalance?.toDouble() ?: 0.0,            // 총 USD 잔액 (deprecated)
    val withdrawable: Double = usdAvailableBalance?.toDouble() ?: 0.0, // 출금 가능 금액 (deprecated)
    val withdrawalLimit: Double = 498999.00,                // 출금 한도 (deprecated)
    val fee: Double = 3.00                                  // 출금 수수료 (deprecated)
)
