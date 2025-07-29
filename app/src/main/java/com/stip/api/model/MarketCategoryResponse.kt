package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName

/**
 * Market Category API 응답 데이터 모델
 * TAPI 서버의 market/categories API 응답을 위한 데이터 클래스
 */
data class MarketCategoryResponse(
    @SerializedName("categoryId")
    val categoryId: Int,
    
    @SerializedName("name")
    val name: String
) 