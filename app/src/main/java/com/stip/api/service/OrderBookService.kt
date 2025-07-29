package com.stip.stip.api.service

import com.stip.stip.api.model.OrderBookResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 호가창(Order Book) API 서비스 인터페이스
 * 호가창 데이터를 조회하기 위한 API 정의
 */
interface OrderBookService {
    
    /**
     * 특정 마켓 페어의 호가창 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @return 호가창 데이터
     */
    @GET("api/orders/orderbook")
    suspend fun getOrderBook(
        @Query("marketPairId") marketPairId: String
    ): OrderBookResponse
} 