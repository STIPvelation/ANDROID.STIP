package com.stip.stip.api.service

import com.stip.stip.api.model.MarketResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Market API 서비스 인터페이스
 * 특정 마켓 정보를 조회하기 위한 API 정의
 */
interface MarketService {
    
    /**
     * 특정 마켓 페어 정보 조회
     * @param marketPairId 마켓 페어 ID
     * @return 마켓 정보
     */
    @GET("api/market")
    suspend fun getMarket(
        @Query("marketPairId") marketPairId: String
    ): MarketResponse
} 