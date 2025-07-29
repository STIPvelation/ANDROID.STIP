package com.stip.api.service

import com.stip.api.model.ExchangeRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 환율 관련 API 서비스 인터페이스
 * USD-KRW 환율을 조회하고 변환하기 위한 API 정의
 */
interface ExchangeRateService {
    /**
     * USD-KRW 환율 조회
     * @return 환율 응답 데이터
     */
    @GET("api/exchange-rate/usd-krw")
    suspend fun getUsdKrwRate(): ExchangeRateResponse
    
    /**
     * USD를 KRW로 변환
     * @param usd USD 금액
     * @return 변환된 KRW 금액
     */
    @GET("api/exchange-rate/convert/usd-to-krw")
    suspend fun convertUsdToKrw(@Query("usd") usd: Double): ExchangeRateResponse
    
    /**
     * KRW를 USD로 변환
     * @param krw KRW 금액
     * @return 변환된 USD 금액
     */
    @GET("api/exchange-rate/convert/krw-to-usd")
    suspend fun convertKrwToUsd(@Query("krw") krw: Double): ExchangeRateResponse
} 