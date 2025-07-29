package com.stip.stip.api.service

import com.stip.stip.api.model.MarketCategoryResponse
import retrofit2.http.GET

/**
 * Market Categories API 서비스 인터페이스
 * TAPI 서버에서 Market Categories 데이터를 조회하기 위한 API 정의
 */
interface MarketCategoriesService {
    
    /**
     * Market Categories 데이터 조회
     * @return Market Categories 데이터 배열
     */
    @GET("api/market/categories")
    suspend fun getMarketCategories(): List<MarketCategoryResponse>
} 