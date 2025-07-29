package com.stip.stip.api.service

import com.stip.stip.api.model.MarketPairsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Market Pairs API 서비스 인터페이스
 * TAPI 서버에서 Market Pairs 데이터를 조회하기 위한 API 정의
 */
interface MarketPairsService {
    
    /**
     * Market Pairs 데이터 조회
     * @return Market Pairs 데이터 배열
     */
    @GET("api/market/pairs")
    suspend fun getMarketPairs(): List<MarketPairsResponse>
    
    /**
     * 키워드와 카테고리로 Market Pairs 데이터 조회
     * @param keyword 검색 키워드
     * @param categoryId 카테고리 ID
     * @return Market Pairs 데이터 배열
     */
    @GET("api/market/pairs")
    suspend fun getMarketPairs(
        @Query("keyword") keyword: String? = null,
        @Query("categoryId") categoryId: Int? = null
    ): List<MarketPairsResponse>
} 