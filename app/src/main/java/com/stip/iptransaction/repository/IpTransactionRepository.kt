package com.stip.stip.iptransaction.repository

import com.stip.order.api.OrderService
import com.stip.order.data.MarketBuyRequest
import com.stip.order.data.MarketSellRequest
import com.stip.stip.order.data.OrderCancelRequest
import com.stip.stip.api.RetrofitClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpTransactionRepository @Inject constructor() {
    private val orderService: OrderService = RetrofitClient.createOrderService()
    
    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            val orderCancelRequest = OrderCancelRequest(orderIds = listOf(orderId))
            val response = orderService.cancelOrders(orderCancelRequest)
            if (response.isSuccessful) {
                val deleteResponse = response.body()
                if (deleteResponse?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(deleteResponse?.message ?: "주문 삭제에 실패했습니다."))
                }
            } else {
                Result.failure(Exception("HTTP 오류: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelOrders(orderIds: List<String>): Result<Unit> {
        return try {
            val orderCancelRequest = OrderCancelRequest(orderIds = orderIds)
            val response = orderService.cancelOrders(orderCancelRequest)
            if (response.isSuccessful) {
                val deleteResponse = response.body()
                if (deleteResponse?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(deleteResponse?.message ?: "주문 삭제에 실패했습니다."))
                }
            } else {
                Result.failure(Exception("HTTP 오류: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun marketBuyOrder(pairId: String, amount: Double): Result<Unit> {
        return try {
            val marketBuyRequest = MarketBuyRequest(
                pairId = pairId,
                amount = amount
            )
            val response = orderService.marketBuyOrder(marketBuyRequest)
            if (response.isSuccessful) {
                val orderResponse = response.body()
                if (orderResponse?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(orderResponse?.message ?: "시장가 매수에 실패했습니다."))
                }
            } else {
                Result.failure(Exception("HTTP 오류: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun marketSellOrder(pairId: String, quantity: Double): Result<Unit> {
        return try {
            val marketSellRequest = MarketSellRequest(
                pairId = pairId,
                quantity = quantity
            )
            val response = orderService.marketSellOrder(marketSellRequest)
            if (response.isSuccessful) {
                val orderResponse = response.body()
                if (orderResponse?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(orderResponse?.message ?: "시장가 매도에 실패했습니다."))
                }
            } else {
                Result.failure(Exception("HTTP 오류: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 