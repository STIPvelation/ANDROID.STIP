package com.stip.order.api

import com.stip.order.data.OrderRequest
import com.stip.order.data.MarketBuyRequest
import com.stip.order.data.MarketSellRequest
import com.stip.stip.order.data.OrderDeleteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface OrderService {
    @POST("api/orders/buy")
    suspend fun buyOrder(@Body orderRequest: OrderRequest): Response<OrderResponse>

    @POST("api/orders/sell")
    suspend fun sellOrder(@Body orderRequest: OrderRequest): Response<OrderResponse>
    
    @POST("api/orders/market/buy")
    suspend fun marketBuyOrder(@Body marketBuyRequest: MarketBuyRequest): Response<OrderResponse>
    
    @POST("api/orders/market/sell")
    suspend fun marketSellOrder(@Body marketSellRequest: MarketSellRequest): Response<OrderResponse>
    
    @DELETE("api/orders/{orderId}")
    suspend fun deleteOrder(@Path("orderId") orderId: String): Response<OrderDeleteResponse>
}

data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: Any? // OrderData 또는 String 등 다양한 타입을 받을 수 있도록
) {
    /**
     * data 필드가 OrderData 타입인지 확인
     */
    fun getOrderData(): OrderData? {
        return if (data is OrderData) data else null
    }
    
    /**
     * data 필드가 String 타입인지 확인
     */
    fun getDataString(): String? {
        return if (data is String) data else null
    }
}

data class OrderData(
    val id: String,
    val type: String,
    val quantity: Double,
    val price: Double,
    val filledQuantity: Double,
    val status: String,
    val member: Any?,
    val marketPair: Any?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
) 