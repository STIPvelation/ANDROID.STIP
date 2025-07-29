package com.stip.utils

import android.util.Log
import com.stip.api.repository.ExchangeRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 환율을 전역적으로 관리하는 매니저 클래스
 * 앱 전체에서 일관된 환율을 사용할 수 있도록 함
 */
object ExchangeRateManager {
    
    private const val TAG = "ExchangeRateManager"
    private const val DEFAULT_EXCHANGE_RATE = 1300.0
    
    @Volatile
    private var currentExchangeRate: Double = DEFAULT_EXCHANGE_RATE
    
    private val exchangeRateRepository = ExchangeRateRepository()
    
    /**
     * 현재 환율 조회
     * @return 현재 USD-KRW 환율
     */
    fun getCurrentExchangeRate(): Double = currentExchangeRate
    
    /**
     * 환율 업데이트 (API에서 최신 환율 가져오기)
     */
    fun updateExchangeRate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newRate = exchangeRateRepository.getUsdKrwRate()
                currentExchangeRate = newRate
                Log.d(TAG, "환율 업데이트 완료: $newRate")
            } catch (e: Exception) {
                Log.e(TAG, "환율 업데이트 실패: ${e.message}", e)
                // 실패 시 기본값 유지
            }
        }
    }
    
    /**
     * USD 금액을 KRW로 변환 (로컬 환율 사용)
     * @param usdAmount USD 금액
     * @return KRW 금액
     */
    fun convertUsdToKrw(usdAmount: Double): Double {
        return usdAmount * currentExchangeRate
    }
    
    /**
     * KRW 금액을 USD로 변환 (로컬 환율 사용)
     * @param krwAmount KRW 금액
     * @return USD 금액
     */
    fun convertKrwToUsd(krwAmount: Double): Double {
        return krwAmount / currentExchangeRate
    }
    
    /**
     * USD 금액을 KRW로 변환 (API 직접 호출)
     * @param usdAmount USD 금액
     * @return KRW 금액
     */
    suspend fun convertUsdToKrwWithApi(usdAmount: Double): Double {
        return exchangeRateRepository.convertUsdToKrw(usdAmount)
    }
    
    /**
     * KRW 금액을 USD로 변환 (API 직접 호출)
     * @param krwAmount KRW 금액
     * @return USD 금액
     */
    suspend fun convertKrwToUsdWithApi(krwAmount: Double): Double {
        return exchangeRateRepository.convertKrwToUsd(krwAmount)
    }
    
    /**
     * 앱 시작 시 환율 초기화 (동기 방식으로 변경)
     */
    suspend fun initialize() {
        try {
            val newRate = exchangeRateRepository.getUsdKrwRate()
            currentExchangeRate = newRate
            Log.d(TAG, "환율 초기화 완료: $newRate")
        } catch (e: Exception) {
            Log.e(TAG, "환율 초기화 실패: ${e.message}", e)
            // 실패 시 기본값 유지
        }
    }
} 