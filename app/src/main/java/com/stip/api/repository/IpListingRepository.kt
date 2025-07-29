package com.stip.stip.api.repository

import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.service.IpListingService
import com.stip.stip.api.service.MarketPairsService
import com.stip.stip.api.service.MarketCategoriesService
import com.stip.stip.iphome.model.IpListingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * IP 리스팅 데이터 처리를 위한 Repository
 * API 통신을 통해 데이터를 가져오고 앱에서 사용할 수 있는 형태로 변환
 */
class IpListingRepository {
    private val ipListingService: IpListingService by lazy {
        RetrofitClient.createService(IpListingService::class.java)
    }
    
    private val marketPairsService: MarketPairsService by lazy {
        RetrofitClient.createTapiService(MarketPairsService::class.java)
    }
    
    private val marketCategoriesService: MarketCategoriesService by lazy {
        RetrofitClient.createTapiService(MarketCategoriesService::class.java)
    }

    // 티커와 pairId 매핑을 캐시
    private val tickerToPairIdMap = mutableMapOf<String, String>()

    /**
     * 모든 IP 리스팅 데이터 조회
     * @return IP 리스팅 아이템 목록
     */
    suspend fun getIpListing(): List<IpListingItem> = withContext(Dispatchers.IO) {
        try {
            // Market Pairs API에서 실제 데이터 가져오기
            val response = marketPairsService.getMarketPairs()
            
            if (response.isNotEmpty()) {
                // API 응답을 IpListingItem으로 변환
                val items = response.map { 
                    // 티커-pairId 매핑 저장
                    tickerToPairIdMap[it.baseAsset] = it.id
                    it.toIpListingItem() 
                }
                Log.d("IpListingRepository", "API 호출 성공: ${items.size}개 아이템 로드됨")
                items
            } else {
                // API 응답이 성공하지 않거나 데이터가 비어있을 경우
                Log.w("IpListingRepository", "API 응답이 비어있음")
                emptyList()
            }
        } catch (e: Exception) {
            // 예외 발생 시 빈 리스트 반환
            Log.e("IpListingRepository", "Market Pairs API 호출 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 티커에 해당하는 pairId를 반환
     * @param ticker 티커 심볼
     * @return pairId 또는 null
     */
    suspend fun getPairIdForTicker(ticker: String?): String? {
        if (ticker == null) return null

        // 캐시된 매핑이 있으면 반환
        tickerToPairIdMap[ticker]?.let { return it }

            // 캐시된 매핑이 없으면 API 호출하여 매핑 업데이트
            try {
                val response = marketPairsService.getMarketPairs()
            if (response.isNotEmpty()) {
                response.forEach { pair ->
                    tickerToPairIdMap[pair.baseAsset] = pair.id
                }
            }
        } catch (e: Exception) {
            Log.e("IpListingRepository", "Failed to update ticker-pairId mapping: ${e.message}")
        }

        return tickerToPairIdMap[ticker]
    }

    /**
     * 카테고리별 IP 리스팅 데이터 조회
     * @param category 조회할 카테고리
     * @return 카테고리별 IP 리스팅 아이템 목록
     */
    suspend fun getIpListingByCategory(category: String): List<IpListingItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = ipListingService.getIpListingByCategory(category)
                if (response.isSuccess() && response.data != null) {
                    response.data.items.map { it.toIpListingItem() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * 특정 카테고리의 Market Pairs 데이터 조회
     * @param categoryId 조회할 카테고리 ID
     * @return 카테고리별 IP 리스팅 아이템 목록
     */
    suspend fun getMarketPairsByCategory(categoryId: Int): List<IpListingItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = marketPairsService.getMarketPairs()
                
                if (response.isNotEmpty()) {
                    val items = response.map { 
                        tickerToPairIdMap[it.baseAsset] = it.id
                        it.toIpListingItem() 
                    }
                    Log.d("IpListingRepository", "카테고리별 API 호출 성공: ${items.size}개 아이템 로드됨 (카테고리: $categoryId)")
                    items
                } else {
                    Log.w("IpListingRepository", "카테고리별 API 응답이 비어있음 (카테고리: $categoryId)")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("IpListingRepository", "카테고리별 Market Pairs API 호출 실패: ${e.message}")
                emptyList()
            }
        }

    /**
     * 사용 가능한 카테고리 목록 조회
     * @return 카테고리 목록
     */
    suspend fun getMarketCategories(): List<com.stip.stip.api.model.MarketCategoryResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = marketCategoriesService.getMarketCategories()
                Log.d("IpListingRepository", "카테고리 목록 조회 성공: ${response.size}개 카테고리")
                response
            } catch (e: Exception) {
                Log.e("IpListingRepository", "카테고리 목록 조회 실패: ${e.message}")
                emptyList()
            }
        }

    /**
     * 사용 가능한 baseAsset 목록 조회 (ALL IP 카테고리용)
     * @return baseAsset 목록
     */
    suspend fun getAvailableBaseAssets(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = marketPairsService.getMarketPairs()
            val baseAssets = response.map { it.baseAsset.trim() }.distinct().sorted()
            Log.d("IpListingRepository", "baseAsset 목록 조회 성공: ${baseAssets.size}개")
            baseAssets
        } catch (e: Exception) {
            Log.e("IpListingRepository", "baseAsset 목록 조회 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 키워드로 Market Pairs 데이터 검색
     * @param keyword 검색 키워드 (baseAsset)
     * @return 검색된 IP 리스팅 아이템 목록
     */
    suspend fun searchMarketPairsByKeyword(keyword: String): List<IpListingItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = marketPairsService.getMarketPairs()
                
                // baseAsset으로 필터링
                val filteredResponse = response.filter { 
                    it.baseAsset.trim().equals(keyword.trim(), ignoreCase = true) 
                }
                
                if (filteredResponse.isNotEmpty()) {
                    val items = filteredResponse.map { 
                        tickerToPairIdMap[it.baseAsset] = it.id
                        it.toIpListingItem() 
                    }
                    Log.d("IpListingRepository", "키워드 검색 성공: ${items.size}개 아이템 로드됨 (키워드: $keyword)")
                    items
                } else {
                    Log.w("IpListingRepository", "키워드 검색 결과 없음 (키워드: $keyword)")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("IpListingRepository", "키워드 검색 실패: ${e.message}")
                emptyList()
            }
        }

    /**
     * IP 상세 정보 조회
     * @param ticker IP 티커명
     * @return IP 상세 정보 아이템
     */
    suspend fun getIpDetail(ticker: String): IpListingItem? = withContext(Dispatchers.IO) {
        try {
            val response = ipListingService.getIpDetail(ticker)
            if (response.isSuccess() && response.data != null && response.data.items.isNotEmpty()) {
                response.data.items.first().toIpListingItem()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
