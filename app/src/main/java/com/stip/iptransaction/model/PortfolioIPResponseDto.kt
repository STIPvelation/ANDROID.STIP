package com.stip.stip.iptransaction.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * 포트폴리오 IP 응답 DTO - 실제 API 응답에 맞춤
 */
data class PortfolioIPResponseDto(
    @SerializedName("wallets")
    val wallets: List<PortfolioIPHoldingDto>,           // 보유 자산 리스트
    @SerializedName("usdBalance")
    val usdBalance: BigDecimal,                         // 보유 USD
    @SerializedName("usdAvailableBalance")
    val usdAvailableBalance: BigDecimal,                // 사용 가능한 USD
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal,                         // 평가 금액
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal,                          // 매수 금액
    @SerializedName("profit")
    val profit: BigDecimal,                             // 평가손익
    @SerializedName("profitRate")
    val profitRate: BigDecimal,                         // 수익률(%)
    @SerializedName("portfolioChart")
    val portfolioChart: List<PortfolioIPChartItemDto>   // 비율 그래프
)

/**
 * @deprecated 기존 호환성을 위해 유지
 */
data class PortfolioIPSummaryDto(
    val usdBalance: BigDecimal,         // 보유 USD
    val totalAsset: BigDecimal,         // 총 보유자산(USD 환산)
    val totalBuy: BigDecimal,           // 총 매수
    val totalEval: BigDecimal,          // 총 평가
    val profit: BigDecimal,             // 평가손익
    val profitRate: BigDecimal,         // 수익률(%)
    val holdingCount: Int               // 보유 종목 수
)

data class PortfolioIPChartItemDto(
    @SerializedName("symbol")
    val symbol: String,                 // 종목 심볼
    @SerializedName("name")
    val name: String,                   // 종목명
    @SerializedName("percent")
    val percent: BigDecimal             // 비중 (%)
)

data class PortfolioIPHoldingDto(
    @SerializedName("marketPairId")
    val marketPairId: String,           // 마켓 페어 ID
    @SerializedName("symbol")
    val symbol: String,                 // 종목 심볼
    @SerializedName("walletId")
    val walletId: String?,              // 지갑 ID
    @SerializedName("balance")
    val balance: BigDecimal,            // 보유수량
    @SerializedName("address")
    val address: String?,               // 지갑 주소
    @SerializedName("price")
    val price: BigDecimal,              // 현재가
    @SerializedName("evalAmount")
    val evalAmount: BigDecimal,         // 평가금액
    @SerializedName("buyAmount")
    val buyAmount: BigDecimal,          // 매수금액
    @SerializedName("buyAvgPrice")
    val buyAvgPrice: BigDecimal,        // 매수평균가
    @SerializedName("profit")
    val profit: BigDecimal,             // 평가손익
    @SerializedName("profitRate")
    val profitRate: BigDecimal,         // 수익률(%)
    
    // 기존 호환성을 위해 유지 (deprecated)
    val name: String? = null,           // 종목명 (deprecated)
    val amount: BigDecimal = balance    // 보유수량 (deprecated)
) 