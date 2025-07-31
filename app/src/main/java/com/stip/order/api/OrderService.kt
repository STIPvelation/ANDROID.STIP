package com.stip.order.api

import com.stip.order.data.OrderRequest
import com.stip.order.data.MarketBuyRequest
import com.stip.order.data.MarketSellRequest
import com.stip.stip.order.data.OrderDeleteResponse
import com.stip.stip.order.data.OrderCancelRequest
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
    
    @POST("api/orders/cancel")
    suspend fun cancelOrders(@Body orderCancelRequest: OrderCancelRequest): Response<OrderDeleteResponse>
}

data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: String
)

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