package com.stip.stip.api.repository

import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.model.TapiDailyDataResponse
import com.stip.stip.api.service.TapiDailyDataService
import java.text.SimpleDateFormat
import java.util.*

/**
 * ip 일별 데이터 Repository
 */
class TapiDailyDataRepository {
    private val tapiService: TapiDailyDataService by lazy {
        RetrofitClient.createService(TapiDailyDataService::class.java)
    }

    /**
     * 일별 거래 데이터 조회 (기본 30일)
     * @param marketPairId 마켓 페어 ID
     * @return 일별 거래 데이터 리스트
     */
    suspend fun getRecentDailyData(marketPairId: String): List<TapiDailyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val to = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val from = dateFormat.format(calendar.time)
        return try {
            tapiService.getDailyData(marketPairId, from, to)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 7월 1일부터 31일까지의 일별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @return 일별 거래 데이터 리스트
     */
    suspend fun getJulyDailyData(marketPairId: String): List<TapiDailyDataResponse> {
        return try {
            tapiService.getDailyData(marketPairId, "2025-07-01", "2025-07-31")
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 특정 기간의 일별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @return 일별 거래 데이터 리스트
     */
    suspend fun getDailyDataByRange(
        marketPairId: String,
        from: Date,
        to: Date
    ): List<TapiDailyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fromStr = dateFormat.format(from)
        val toStr = dateFormat.format(to)
        return try {
            tapiService.getDailyData(marketPairId, fromStr, toStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 특정 날짜의 일별 거래 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param date 조회할 날짜
     * @return 일별 거래 데이터 리스트
     */
    suspend fun getDailyDataByDate(marketPairId: String, date: Date): List<TapiDailyDataResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        return try {
            tapiService.getDailyData(marketPairId, dateStr, dateStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
} 