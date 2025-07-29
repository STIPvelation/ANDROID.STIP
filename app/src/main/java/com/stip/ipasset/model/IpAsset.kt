package com.stip.ipasset.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class IpAsset(
    val id: String,
    val name: String,
    val ticker: String,
    val balance: Double,
    val value: Double,
    val currencyCode: String = ticker,
    val amount: Double = balance,
    
    // 포트폴리오 API 응답에 맞춘 추가 필드들
    val marketPairId: String? = null,
    val walletId: String? = null,
    val address: String? = null,
    val price: BigDecimal = BigDecimal.ZERO,
    val evalAmount: BigDecimal = BigDecimal.valueOf(value),
    val buyAmount: BigDecimal = BigDecimal.ZERO,
    val buyAvgPrice: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val profitRate: BigDecimal = BigDecimal.ZERO
) : Parcelable {
    
    /**
     * 포트폴리오 API의 PortfolioWalletItemDto로 변환
     */
    fun toPortfolioWalletItemDto(): com.stip.api.model.PortfolioWalletItemDto {
        return com.stip.api.model.PortfolioWalletItemDto(
            marketPairId = marketPairId ?: "",
            symbol = ticker,
            walletId = walletId,
            balance = BigDecimal.valueOf(balance),
            address = address,
            price = price,
            evalAmount = evalAmount,
            buyAmount = buyAmount,
            buyAvgPrice = buyAvgPrice,
            profit = profit,
            profitRate = profitRate,
            availableBalance = BigDecimal.valueOf(balance) // 기존 호환성
        )
    }
}
