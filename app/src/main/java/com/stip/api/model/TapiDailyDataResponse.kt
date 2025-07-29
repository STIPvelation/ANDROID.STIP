package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName

/**
 * TAPI 일별 거래 데이터 응답 모델
 * 일자, 종가(USD), 전일 대비 가격 변동, 총 거래량 정보를 포함
 */
data class TapiDailyDataResponse(
    @SerializedName("date")
    val date: String, // 일자 (YYYY-MM-DD 형식)
    @SerializedName("closePrice")
    val closePrice: Double, // 종가 (USD)
    @SerializedName("priceChange")
    val priceChange: Double, // 전일 대비 가격 변동
    @SerializedName("totalVolume")
    val totalVolume: Double, // 총 거래량
    @SerializedName("marketPairId")
    val marketPairId: String? = null // 마켓 페어 ID (선택적)
) 