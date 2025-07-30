package com.stip.stip.api.repository

import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.model.TapiHourlyDataResponse
import com.stip.stip.api.service.TapiHourlyDataService
import java.text.SimpleDateFormat
import java.util.*

/**
 * ip 시간별 데이터 Repository
 */
class TapiHourlyDataRepository {
    private val tapiService: TapiHourlyDataService by lazy {
        RetrofitClient.createService(TapiHourlyDataService::class.java)
    }

    /**
     * 시간별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @return 시간별 거래 데이터 리스트
     */
    suspend fun getTodayHourlyData(marketPairId: String): List<TapiHourlyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        val from = "2025-07-01"
        val to = "2025-07-31"
        return try {
            tapiService.getHourlyData(marketPairId, from, to)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 특정 날짜의 시간별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param date 조회할 날짜
     * @return 시간별 거래 데이터 리스트
     */
    suspend fun getHourlyDataByDate(marketPairId: String, date: Date): List<TapiHourlyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        return try {
            tapiService.getHourlyData(marketPairId, dateStr, dateStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 시간 범위의 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param from 시작 시간
     * @param to 종료 시간
     * @return 시간별 거래 데이터 리스트
     */
    suspend fun getHourlyDataByRange(
        marketPairId: String,
        from: Date,
        to: Date
    ): List<TapiHourlyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fromStr = dateFormat.format(from)
        val toStr = dateFormat.format(to)
        return try {
            tapiService.getHourlyData(marketPairId, fromStr, toStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
} 