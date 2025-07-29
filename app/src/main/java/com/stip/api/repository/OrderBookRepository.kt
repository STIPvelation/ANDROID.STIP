package com.stip.stip.api.repository

import android.util.Log
import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.model.OrderBookResponse
import com.stip.stip.api.service.OrderBookService
import com.stip.stip.iphome.model.OrderBookItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 호가창 데이터 처리를 위한 Repository
 * API 통신을 통해 호가창 데이터를 가져오고 앱에서 사용할 수 있는 형태로 변환
 */
class OrderBookRepository {
    
    private val orderBookService: OrderBookService by lazy {
        RetrofitClient.createTapiService(OrderBookService::class.java)
    }

    /**
     * 특정 마켓 페어의 호가창 데이터 조회
     * @param marketPairId 마켓 페어 ID
     * @param currentPrice 현재 가격 (변동률 계산용)
     * @return 호가창 아이템 목록
     */
    suspend fun getOrderBook(marketPairId: String, currentPrice: Float): List<OrderBookItem> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d("OrderBookRepository", "호가창 데이터 조회 시작: marketPairId=$marketPairId")
                
                val response = orderBookService.getOrderBook(marketPairId)
                
                if (response.success) {
                    val orderBookItems = mutableListOf<OrderBookItem>()
                    
                    // 매도 호가 처리 (높은 가격순으로 정렬)
                    val sellOrders = response.data.sell
                        .sortedByDescending { it.price }
                        .map { it.toOrderBookItem(currentPrice, false) }
                    
                    // 매수 호가 처리 (높은 가격순으로 정렬)
                    val buyOrders = response.data.buy
                        .sortedByDescending { it.price }
                        .map { it.toOrderBookItem(currentPrice, true) }
                    
                    orderBookItems.addAll(sellOrders)
                    orderBookItems.addAll(buyOrders)
                    
                    Log.d("OrderBookRepository", "호가창 데이터 조회 성공: 매도 ${sellOrders.size}개, 매수 ${buyOrders.size}개")
                    orderBookItems
                } else {
                    Log.w("OrderBookRepository", "호가창 API 응답 실패: ${response.message}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("OrderBookRepository", "호가창 데이터 조회 실패", e)
                emptyList()
            }
        }

    /**
     * 매도 호가만 조회
     * @param marketPairId 마켓 페어 ID
     * @param currentPrice 현재 가격
     * @return 매도 호가 아이템 목록
     */
    suspend fun getSellOrders(marketPairId: String, currentPrice: Float): List<OrderBookItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = orderBookService.getOrderBook(marketPairId)
                
                if (response.success) {
                    response.data.sell
                        .sortedByDescending { it.price }
                        .map { it.toOrderBookItem(currentPrice, false) }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("OrderBookRepository", "매도 호가 조회 실패", e)
                emptyList()
            }
        }

    /**
     * 매수 호가만 조회
     * @param marketPairId 마켓 페어 ID
     * @param currentPrice 현재 가격
     * @return 매수 호가 아이템 목록
     */
    suspend fun getBuyOrders(marketPairId: String, currentPrice: Float): List<OrderBookItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = orderBookService.getOrderBook(marketPairId)
                
                if (response.success) {
                    response.data.buy
                        .sortedByDescending { it.price }
                        .map { it.toOrderBookItem(currentPrice, true) }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("OrderBookRepository", "매수 호가 조회 실패", e)
                emptyList()
            }
        }
} 