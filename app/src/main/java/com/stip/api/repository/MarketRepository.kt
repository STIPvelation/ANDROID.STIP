package com.stip.stip.api.repository

import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.service.MarketService
import com.stip.stip.api.model.MarketResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Market 데이터 처리를 위한 Repository
 * 마켓 정보를 가져오고 앱에서 사용할 수 있는 형태로 변환
 */
class MarketRepository {
    private val marketService: MarketService by lazy {
        RetrofitClient.createTapiService(MarketService::class.java)
    }

    /**
     * 특정 마켓 페어 정보 조회
     * @param marketPairId 마켓 페어 ID
     * @return 마켓 정보
     */
    suspend fun getMarket(marketPairId: String): MarketResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d("MarketRepository", "마켓 정보 조회 시작: marketPairId=$marketPairId")
            val response = marketService.getMarket(marketPairId)
            Log.d("MarketRepository", "마켓 정보 조회 성공: $response")
            response
        } catch (e: Exception) {
            Log.e("MarketRepository", "마켓 정보 조회 실패: ${e.message}", e)
            null
        }
    }
} 