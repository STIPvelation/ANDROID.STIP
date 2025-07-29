package com.stip.api.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * 포트폴리오 API 응답 데이터 모델
 */
data class PortfolioResponse(
    @SerializedName("wallets")
    val wallets: List<PortfolioWalletItemDto>,
    @SerializedName("totalAmount")
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("usdBalance")
    val usdBalance: BigDecimal = BigDecimal.ZERO,
    @SerializedName("usdAvailableBalance")
    val usdAvailableBalance: BigDecimal = BigDecimal.ZERO,
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("profit")
    val profit: BigDecimal = BigDecimal.ZERO,
    @SerializedName("profitRate")
    val profitRate: BigDecimal = BigDecimal.ZERO,
    @SerializedName("portfolioChart")
    val portfolioChart: List<PortfolioChartItemDto> = emptyList(),
    
    // 기존 호환성을 위해 유지 (deprecated)
    @SerializedName("total")
    val total: BigDecimal = BigDecimal.ZERO,
    @SerializedName("totalEval")
    val totalEval: BigDecimal = BigDecimal.ZERO,
    @SerializedName("totalBuy")
    val totalBuy: BigDecimal = BigDecimal.ZERO,
    @SerializedName("buyingAmount")
    val buyingAmount: BigDecimal = BigDecimal.ZERO
)

/**
 * 포트폴리오 지갑 아이템
 */
data class PortfolioWalletItemDto(
    @SerializedName("marketPairId")
    val marketPairId: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("walletId")
    val walletId: String?,
    @SerializedName("balance")
    val balance: BigDecimal,
    @SerializedName("address")
    val address: String?,
    @SerializedName("price")
    val price: BigDecimal = BigDecimal.ZERO,
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("buyAvgPrice")
    val buyAvgPrice: BigDecimal = BigDecimal.ZERO,
    @SerializedName("profit")
    val profit: BigDecimal = BigDecimal.ZERO,
    @SerializedName("profitRate")
    val profitRate: BigDecimal = BigDecimal.ZERO,
    
    // 기존 호환성을 위해 유지 (deprecated)
    @SerializedName("availableBalance")
    val availableBalance: BigDecimal = BigDecimal.ZERO
)

/**
 * 포트폴리오 차트 아이템
 */
data class PortfolioChartItemDto(
    @SerializedName("symbol")
    val symbol: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("percent")
    val percent: BigDecimal? = BigDecimal.ZERO
)

/**
 * 기존 호환성을 위한 PortfolioAsset 클래스
 */
data class PortfolioAsset(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("walletId")
    val walletId: String,
    @SerializedName("balance")
    val balance: Double,
    @SerializedName("availableBalance")
    val availableBalance: Double,
    @SerializedName("address")
    val address: String
) {
    fun toPortfolioWalletItemDto(): PortfolioWalletItemDto {
        return PortfolioWalletItemDto(
            marketPairId = "",
            symbol = symbol,
            walletId = walletId,
            balance = BigDecimal.valueOf(balance),
            availableBalance = BigDecimal.valueOf(availableBalance),
            address = address
        )
    }
} 