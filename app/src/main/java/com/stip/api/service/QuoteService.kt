package com.stip.stip.api.service

import com.stip.stip.api.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * IP 호가 데이터를 가져오는 API 서비스 인터페이스
 */
interface QuoteService {
    /**
     * 특정 IP의 시간별 호가 데이터를 가져옴
     * @param ticker IP 티커 심볼
     * @param limit 가져올 데이터 수 (기본값 30)
     */
    @GET("quotes/time/{ticker}")
    suspend fun getTimeQuotes(
        @Path("ticker") ticker: String,
        @Query("limit") limit: Int = 30
    ): ApiResponse<QuoteTimeResponse>
    
    /**
     * 특정 IP의 일별 호가 데이터를 가져옴
     * @param ticker IP 티커 심볼
     * @param limit 가져올 데이터 수 (기본값 20)
     */
    @GET("quotes/daily/{ticker}")
    suspend fun getDailyQuotes(
        @Path("ticker") ticker: String,
        @Query("limit") limit: Int = 20
    ): ApiResponse<QuoteDailyResponse>
}

/**
 * 시간별 호가 데이터 응답 클래스
 */
data class QuoteTimeResponse(
    val items: List<QuoteTimeItem>
)

/**
 * 시간별 호가 데이터 아이템
 */
data class QuoteTimeItem(
    val id: String,
    val ticker: String,
    val price: String,
    val quantity: String,
    val amount: String,
    val timestamp: Long,
    val status: String // "UP", "DOWN", "SAME"
)

/**
 * 일별 호가 데이터 응답 클래스
 */
data class QuoteDailyResponse(
    val items: List<QuoteDailyItem>
)

/**
 * 일별 호가 데이터 아이템
 */
data class QuoteDailyItem(
    val id: String,
    val ticker: String,
    val date: String,
    val openPrice: String,
    val highPrice: String,
    val lowPrice: String,
    val closePrice: String,
    val volume: String,
    val change: String,
    val changePercent: String
)
