package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName

/**
 * TAPI 시간별 거래 데이터 응답 모델
 * 체결 시각, 체결 가격(USD), 체결 금액 정보를 포함
 */
data class TapiHourlyDataResponse(
    @SerializedName("timestamp")
    val timestamp: String, // 체결 시각 (ISO 8601 형식)
    @SerializedName("price")
    val price: Double, // 체결 가격 (USD)
    @SerializedName("amount")
    val volume: Double, // 체결 금액
    @SerializedName("marketPairId")
    val marketPairId: String? = null // 마켓 페어 ID
) 