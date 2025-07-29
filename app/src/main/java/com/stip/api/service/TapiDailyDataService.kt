package com.stip.stip.api.service

import com.stip.stip.api.model.TapiDailyDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Daily Data API 서비스 인터페이스
 * 서버에서 일별 거래 데이터를 조회하기 위한 API 정의
 */
interface TapiDailyDataService {
    /**
     * 일별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param from 시작 날짜 (YYYY-MM-DD 형식)
     * @param to 종료 날짜 (YYYY-MM-DD 형식)
     * @return 일별 거래 데이터 배열
     */
    @GET("api/tickers/daily")
    suspend fun getDailyData(
        @Query("marketPairId") marketPairId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<TapiDailyDataResponse>
} 