package com.stip.api.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * 환율 API 응답 데이터 모델
 */
data class ExchangeRateResponse(
    @SerializedName("value")
    val value: BigDecimal
) 