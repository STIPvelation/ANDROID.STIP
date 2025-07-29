package com.stip.api.repository

import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.api.model.ExchangeRateResponse
import com.stip.api.service.ExchangeRateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * 환율 데이터 처리를 위한 Repository
 * API 통신을 통해 환율 데이터를 가져오고 앱에서 사용할 수 있는 형태로 제공
 */
class ExchangeRateRepository {
    
    private val exchangeRateService: ExchangeRateService by lazy {
        RetrofitClient.createTapiService(ExchangeRateService::class.java)
    }

    companion object {
        private const val TAG = "ExchangeRateRepository"
        private const val DEFAULT_EXCHANGE_RATE = 1300.0 // 기본 환율 (API 실패 시 사용)
    }

    /**
     * USD-KRW 환율 조회
     * @return USD-KRW 환율 (API 실패 시 기본값 1300.0 반환)
     */
    suspend fun getUsdKrwRate(): Double = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "USD-KRW 환율 API 호출 시작")
            
            val response = exchangeRateService.getUsdKrwRate()
            val exchangeRate = response.value.toDouble()
            
            Log.d(TAG, "환율 API 응답: $exchangeRate")
            
            exchangeRate
        } catch (e: Exception) {
            Log.e(TAG, "환율 API 호출 실패: ${e.message}", e)
            DEFAULT_EXCHANGE_RATE
        }
    }

    /**
     * USD 금액을 KRW로 변환
     * @param usdAmount USD 금액
     * @return KRW 금액
     */
    suspend fun convertUsdToKrw(usdAmount: Double): Double = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "USD-KRW 변환 API 호출: $usdAmount USD")
            
            val response = exchangeRateService.convertUsdToKrw(usdAmount)
            val krwAmount = response.value.toDouble()
            
            Log.d(TAG, "USD-KRW 변환 결과: $usdAmount USD = $krwAmount KRW")
            
            krwAmount
        } catch (e: Exception) {
            Log.e(TAG, "USD-KRW 변환 API 호출 실패: ${e.message}", e)
            // 실패 시 기본 환율로 계산
            usdAmount * DEFAULT_EXCHANGE_RATE
        }
    }
    
    /**
     * KRW 금액을 USD로 변환
     * @param krwAmount KRW 금액
     * @return USD 금액
     */
    suspend fun convertKrwToUsd(krwAmount: Double): Double = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "KRW-USD 변환 API 호출: $krwAmount KRW")
            
            val response = exchangeRateService.convertKrwToUsd(krwAmount)
            val usdAmount = response.value.toDouble()
            
            Log.d(TAG, "KRW-USD 변환 결과: $krwAmount KRW = $usdAmount USD")
            
            usdAmount
        } catch (e: Exception) {
            Log.e(TAG, "KRW-USD 변환 API 호출 실패: ${e.message}", e)
            // 실패 시 기본 환율로 계산
            krwAmount / DEFAULT_EXCHANGE_RATE
        }
    }
} 