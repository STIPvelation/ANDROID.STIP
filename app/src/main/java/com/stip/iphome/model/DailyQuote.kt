package com.stip.stip.iphome.model

import com.stip.stip.api.model.MarketPairsResponse
import java.text.SimpleDateFormat
import java.util.*

data class DailyQuote(
    val id: String,
    val date: String,           // 2025-01-01 형식
    val open: Double,           // 시가
    val high: Double,           // 고가
    val low: Double,            // 저가
    val close: Double,          // 종가
    val volume: Double,         // 거래량
    val changePercent: Double,  // 변동률
    val changeAmount: Double    // 절대값 변화량
) {
    companion object {
        fun fromMarketPairsResponse(marketData: MarketPairsResponse): DailyQuote {
            // 현재 날짜를 2025-01-01 형식으로 포맷팅
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            return DailyQuote(
                id = marketData.id,
                date = currentDate,
                open = marketData.lastPrice?.toDouble() ?: 0.0,
                high = marketData.highTicker?.toDouble() ?: 0.0,
                low = marketData.lowTicker?.toDouble() ?: 0.0,
                close = marketData.lastPrice?.toDouble() ?: 0.0,
                volume = marketData.volume?.toDouble() ?: 0.0,
                changePercent = marketData.changeRate ?: 0.0,
                changeAmount = 0.0 // 기본값
            )
        }
    }
} 