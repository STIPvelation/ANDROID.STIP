package com.stip.stip.api.repository

import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.model.TapiDailyDataResponse
import com.stip.stip.api.model.TapiHourlyDataResponse
import com.stip.stip.api.service.TapiDailyDataService
import com.stip.stip.api.service.TapiHourlyDataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * IP 호가 데이터 처리를 위한 Repository
 */
class QuoteRepository {
    private val dailyDataService: TapiDailyDataService by lazy {
        RetrofitClient.createService(TapiDailyDataService::class.java)
    }
    private val hourlyDataService: TapiHourlyDataService by lazy {
        RetrofitClient.createService(TapiHourlyDataService::class.java)
    }

    /**
     * 일별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param from 시작 날짜 (YYYY-MM-DD)
     * @param to 종료 날짜 (YYYY-MM-DD)
     */
    suspend fun getDailyData(marketPairId: String, from: String, to: String): List<TapiDailyDataResponse> = withContext(Dispatchers.IO) {
        try {
            dailyDataService.getDailyData(marketPairId, from, to)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 시간별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param from 시작 시각 (ISO 8601)
     * @param to 종료 시각 (ISO 8601)
     */
    suspend fun getHourlyData(marketPairId: String, from: String, to: String): List<TapiHourlyDataResponse> = withContext(Dispatchers.IO) {
        try {
            hourlyDataService.getHourlyData(marketPairId, from, to)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
