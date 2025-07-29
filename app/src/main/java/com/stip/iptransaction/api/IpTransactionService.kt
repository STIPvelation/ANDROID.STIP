package com.stip.stip.iptransaction.api

import android.util.Log
import com.stip.stip.iptransaction.model.DipHoldingitem
import com.stip.stip.iptransaction.model.IpInvestmentItem
import com.stip.stip.iptransaction.model.MyIpHoldingsSummaryItem
import com.stip.stip.iptransaction.model.UnfilledOrder

import com.stip.stip.iptransaction.model.OrderListResponse
import com.stip.stip.iptransaction.model.ApiOrderResponse
import com.stip.stip.iptransaction.model.PortfolioIPResponseDto
import com.stip.stip.iptransaction.model.TradeListResponse
import com.stip.stip.iptransaction.model.TradeResponse
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface IpTransactionApi {
    @GET("api/ip/transactions")
    fun getIpTransactions(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("types") types: List<String>? = null
    ): Call<List<IpInvestmentItem>>

    @GET("api/ip/holdings")
    fun getIpHoldings(): Call<List<DipHoldingitem>>

    @GET("api/ip/holdings/summary")
    fun getIpHoldingsSummary(): Call<MyIpHoldingsSummaryItem>

    @GET("api/ip/unfilled")
    fun getUnfilledOrders(): Call<List<UnfilledOrder>>
    
    @GET("api/orders")
    fun getOrders(
        @Query("userId") userId: String,
        @Query("status") status: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Call<OrderListResponse>
    
    @GET("api/orders")
    fun getOrdersByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Call<OrderListResponse>
    
    @GET("api/orders")
    fun getOrdersByMarketPair(
        @Query("marketPairId") marketPairId: String,
        @Query("status") status: String? = null
    ): Call<OrderListResponse>
    
    @GET("api/trades")
    fun getTrades(
        @Query("marketPairId") marketPairId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Call<TradeListResponse>
    
    @GET("api/trades")
    fun getTradesByStatus(
        @Query("marketPairId") marketPairId: String,
        @Query("status") status: String
    ): Call<TradeListResponse>
    
    // 손익 정보 API
    @GET("api/ip/profit/detail")
    fun getProfitLossItems(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("profitType") profitType: String? = null
    ): Call<List<com.stip.stip.iptransaction.model.ProfitLossItem>>
    
    @GET("api/ip/profit/summary")
    fun getProfitLossSummary(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("profitType") profitType: String? = null
    ): Call<com.stip.stip.iptransaction.model.ProfitLossSummary>
    
    @GET("api/ip/profit/chart")
    fun getProfitLossChartData(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Query("profitType") profitType: String? = null
    ): Call<com.stip.stip.iptransaction.model.ProfitLossChartData>


    
    // 포트폴리오 API
    @GET("api/portfolio")
    fun getPortfolio(@Query("userId") userId: String): Call<PortfolioIPResponseDto>
}

object IpTransactionService {
    private const val TAPI_URL = "https://tapi.sharetheip.com/"
    private const val ENGINE_URL = "http://34.64.197.80:5000/"
    private const val X_API_KEY: String = "AIzaSyAM4J1XFF6SAkXeY78ONDyRtgo3mhk78kE"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("X_API_Key", X_API_KEY)
            
            // JWT 토큰이 있으면 헤더에 추가
            val token = com.stip.stip.signup.utils.PreferenceUtil.getToken()
            if (token != null && token.isNotEmpty()) {
                builder.header("Authorization", "Bearer $token")
            }
            
            val request = builder.method(original.method, original.body).build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(TAPI_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        
    // 엔진 서버용 Retrofit 인스턴스
    private val engineRetrofit = Retrofit.Builder()
        .baseUrl(ENGINE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        
    val ipTransactionApi: IpTransactionApi = retrofit.create(IpTransactionApi::class.java)
    val engineApi: IpTransactionApi = engineRetrofit.create(IpTransactionApi::class.java)
    
    // IP 거래 내역 조회 (투자 기록)
    fun getIpTransactions(
        filterTypes: List<String>? = null,
        startDate: String? = null,
        endDate: String? = null,
        callback: (List<IpInvestmentItem>?, Throwable?) -> Unit
    ) {
        ipTransactionApi.getIpTransactions(startDate, endDate, filterTypes).enqueue(
            object : retrofit2.Callback<List<IpInvestmentItem>> {
                override fun onResponse(
                    call: Call<List<IpInvestmentItem>>,
                    response: retrofit2.Response<List<IpInvestmentItem>>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<List<IpInvestmentItem>>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }
    
    // IP 보유 현황 조회
    fun getIpHoldings(
        callback: (List<DipHoldingitem>?, Throwable?) -> Unit
    ) {
        // 실제 API 호출
        ipTransactionApi.getIpHoldings().enqueue(
            object : retrofit2.Callback<List<DipHoldingitem>> {
                override fun onResponse(
                    call: Call<List<DipHoldingitem>>,
                    response: retrofit2.Response<List<DipHoldingitem>>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<List<DipHoldingitem>>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }
    
    // IP 보유 현황 요약 조회
    fun getIpHoldingsSummary(
        callback: (MyIpHoldingsSummaryItem?, Throwable?) -> Unit
    ) {
        // 실제 API 호출
        ipTransactionApi.getIpHoldingsSummary().enqueue(
            object : retrofit2.Callback<MyIpHoldingsSummaryItem> {
                override fun onResponse(
                    call: Call<MyIpHoldingsSummaryItem>,
                    response: retrofit2.Response<MyIpHoldingsSummaryItem>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<MyIpHoldingsSummaryItem>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }
    
    // 미체결 주문 조회
    fun getApiUnfilledOrders(
        memberId: String,
        page: Int = 1,
        limit: Int = 10,
        callback: (List<ApiOrderResponse>?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "미체결 주문 조회 호출: $memberId, page: $page, limit: $limit")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/orders?userId=$memberId&status=open&page=$page&limit=$limit")
        
        ipTransactionApi.getOrders(memberId, "open", page, limit).enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, records count: ${body.data.size}")
                            val apiOrders = body.data.map { record ->
                                ApiOrderResponse(
                                    id = record.id,
                                    type = record.type,
                                    quantity = record.quantity,
                                    price = record.price,
                                    filledQuantity = record.filledQuantity,
                                    status = record.status,
                                    member = record.member,
                                    marketPair = record.marketPair,
                                    createdAt = record.createdAt,
                                    updatedAt = record.updatedAt,
                                    deletedAt = record.deletedAt
                                )
                            }
                            callback(apiOrders, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 체결 주문 조회
    fun getApiFilledOrders(
        memberId: String,
        page: Int = 1,
        limit: Int = 10,
        callback: (List<ApiOrderResponse>?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "체결 주문 조회 호출: $memberId, page: $page, limit: $limit")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/orders?userId=$memberId&status=filled&page=$page&limit=$limit")
        
        ipTransactionApi.getOrders(memberId, "filled", page, limit).enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, records count: ${body.data.size}")
                            val apiOrders = body.data.map { record ->
                                ApiOrderResponse(
                                    id = record.id,
                                    type = record.type,
                                    quantity = record.quantity,
                                    price = record.price,
                                    filledQuantity = record.filledQuantity,
                                    status = record.status,
                                    member = record.member,
                                    marketPair = record.marketPair,
                                    createdAt = record.createdAt,
                                    updatedAt = record.updatedAt,
                                    deletedAt = record.deletedAt
                                )
                            }
                            callback(apiOrders, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 미체결 주문 조회
    fun getApiUnfilledOrdersByMarketPair(
        marketPairId: String,
        callback: (List<ApiOrderResponse>?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "미체결 주문 조회 호출: marketPairId=$marketPairId, status=open")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/orders?marketPairId=$marketPairId&status=open")
        
        ipTransactionApi.getOrdersByMarketPair(marketPairId, "open").enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, open orders count: ${body.data.size}")
                            val apiOrders = body.data.map { record ->
                                ApiOrderResponse(
                                    id = record.id,
                                    type = record.type,
                                    quantity = record.quantity,
                                    price = record.price,
                                    filledQuantity = record.filledQuantity,
                                    status = record.status,
                                    member = record.member,
                                    marketPair = record.marketPair,
                                    createdAt = record.createdAt,
                                    updatedAt = record.updatedAt,
                                    deletedAt = record.deletedAt
                                )
                            }
                            callback(apiOrders, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 체결된 주문 조회
    fun getApiFilledOrdersByMarketPair(
        marketPairId: String,
        callback: (List<ApiOrderResponse>?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "체결된 주문 조회 호출: marketPairId=$marketPairId, status=filled")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/orders?marketPairId=$marketPairId&status=filled")
        
        ipTransactionApi.getOrdersByMarketPair(marketPairId, "filled").enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, filled orders count: ${body.data.size}")
                            val apiOrders = body.data.map { record ->
                                ApiOrderResponse(
                                    id = record.id,
                                    type = record.type,
                                    quantity = record.quantity,
                                    price = record.price,
                                    filledQuantity = record.filledQuantity,
                                    status = record.status,
                                    member = record.member,
                                    marketPair = record.marketPair,
                                    createdAt = record.createdAt,
                                    updatedAt = record.updatedAt,
                                    deletedAt = record.deletedAt
                                )
                            }
                            callback(apiOrders, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 체결 내역 조회
    fun getApiTradesByMarketPair(
        marketPairId: String,
        status: String = "open",
        callback: (List<TradeResponse>?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "체결 내역 조회 호출: marketPairId=$marketPairId, status=$status")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/trades?marketPairId=$marketPairId&status=$status")
        
        ipTransactionApi.getTradesByStatus(marketPairId, status).enqueue(
            object : retrofit2.Callback<TradeListResponse> {
                override fun onResponse(
                    call: Call<TradeListResponse>,
                    response: retrofit2.Response<TradeListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, trades count: ${body.data.size}")
                            callback(body.data, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<TradeListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 체결 내역 조회 (날짜 범위 기반)
    fun getTrades(
        marketPairId: String,
        startDate: String,
        endDate: String,
        callback: (TradeListResponse?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "새로운 체결 내역 조회 호출: marketPairId=$marketPairId, startDate=$startDate, endDate=$endDate")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/trades?marketPairId=$marketPairId&startDate=$startDate&endDate=$endDate")
        
        ipTransactionApi.getTrades(marketPairId, startDate, endDate).enqueue(
            object : retrofit2.Callback<TradeListResponse> {
                override fun onResponse(
                    call: Call<TradeListResponse>,
                    response: retrofit2.Response<TradeListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, trades count: ${body.data.size}")
                            callback(body, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<TradeListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }

    // 손익 상세 정보 조회
    fun getProfitLossItems(
        year: Int,
        month: Int,
        profitType: String? = null,
        callback: (List<com.stip.stip.iptransaction.model.ProfitLossItem>?, Throwable?) -> Unit
    ) {
        ipTransactionApi.getProfitLossItems(year, month, profitType).enqueue(
            object : retrofit2.Callback<List<com.stip.stip.iptransaction.model.ProfitLossItem>> {
                override fun onResponse(
                    call: Call<List<com.stip.stip.iptransaction.model.ProfitLossItem>>,
                    response: retrofit2.Response<List<com.stip.stip.iptransaction.model.ProfitLossItem>>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<List<com.stip.stip.iptransaction.model.ProfitLossItem>>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }
    
    // 손익 요약 정보 조회
    fun getProfitLossSummary(
        year: Int,
        month: Int,
        profitType: String? = null,
        callback: (com.stip.stip.iptransaction.model.ProfitLossSummary?, Throwable?) -> Unit
    ) {
        ipTransactionApi.getProfitLossSummary(year, month, profitType).enqueue(
            object : retrofit2.Callback<com.stip.stip.iptransaction.model.ProfitLossSummary> {
                override fun onResponse(
                    call: Call<com.stip.stip.iptransaction.model.ProfitLossSummary>,
                    response: retrofit2.Response<com.stip.stip.iptransaction.model.ProfitLossSummary>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<com.stip.stip.iptransaction.model.ProfitLossSummary>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }
    
    // 손익 차트 데이터 조회
    fun getProfitLossChartData(
        year: Int,
        month: Int,
        profitType: String? = null,
        callback: (com.stip.stip.iptransaction.model.ProfitLossChartData?, Throwable?) -> Unit
    ) {
        ipTransactionApi.getProfitLossChartData(year, month, profitType).enqueue(
            object : retrofit2.Callback<com.stip.stip.iptransaction.model.ProfitLossChartData> {
                override fun onResponse(
                    call: Call<com.stip.stip.iptransaction.model.ProfitLossChartData>,
                    response: retrofit2.Response<com.stip.stip.iptransaction.model.ProfitLossChartData>
                ) {
                    if (response.isSuccessful) {
                        callback(response.body(), null)
                    } else {
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<com.stip.stip.iptransaction.model.ProfitLossChartData>, t: Throwable) {
                    callback(null, t)
                }
            }
        )
    }



    // 주문 내역 조회
    fun getOrders(
        memberId: String,
        status: String,
        page: Int = 1,
        limit: Int = 10,
        callback: (OrderListResponse?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "주문 내역 조회 시작: ${TAPI_URL}api/orders?userId=$memberId&status=$status&page=$page&limit=$limit")
        
        ipTransactionApi.getOrders(memberId, status, page, limit).enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "API response received - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "Response body: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "API success: true, records count: ${body.data.size}")
                            callback(body, null)
                        } else {
                            Log.e("IpTransactionService", "API success: false, message: ${body?.message}")
                            callback(null, Exception("API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 기간별 주문 내역 조회 (IP 투자 내역용)
    fun getOrdersByDateRange(
        startDate: String,
        endDate: String,
        callback: (OrderListResponse?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "기간별 주문 내역 조회 시작: ${TAPI_URL}api/orders?startDate=$startDate&endDate=$endDate")
        
        ipTransactionApi.getOrdersByDateRange(startDate, endDate).enqueue(
            object : retrofit2.Callback<OrderListResponse> {
                override fun onResponse(
                    call: Call<OrderListResponse>,
                    response: retrofit2.Response<OrderListResponse>
                ) {
                    Log.d("IpTransactionService", "기간별 주문 내역 API 응답 - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "기간별 주문 내역 응답: $body")
                        
                        if (body?.success == true) {
                            Log.d("IpTransactionService", "기간별 주문 내역 API 성공, 레코드 수: ${body.data.size}")
                            callback(body, null)
                        } else {
                            Log.e("IpTransactionService", "기간별 주문 내역 API 실패, 메시지: ${body?.message}")
                            callback(null, Exception("기간별 주문 내역 API 응답 실패: ${body?.message}"))
                        }
                    } else {
                        Log.e("IpTransactionService", "기간별 주문 내역 HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("기간별 주문 내역 API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<OrderListResponse>, t: Throwable) {
                    Log.e("IpTransactionService", "기간별 주문 내역 API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
    
    // 포트폴리오 조회
    fun getPortfolio(
        userId: String,
        callback: (PortfolioIPResponseDto?, Throwable?) -> Unit
    ) {
        Log.d("IpTransactionService", "포트폴리오 조회 호출: $userId")
        Log.d("IpTransactionService", "API URL: ${TAPI_URL}api/portfolio?userId=$userId")
        
        ipTransactionApi.getPortfolio(userId).enqueue(
            object : retrofit2.Callback<PortfolioIPResponseDto> {
                override fun onResponse(
                    call: Call<PortfolioIPResponseDto>,
                    response: retrofit2.Response<PortfolioIPResponseDto>
                ) {
                    Log.d("IpTransactionService", "포트폴리오 API 응답 - code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("IpTransactionService", "포트폴리오 응답: $body")
                        callback(body, null)
                    } else {
                        Log.e("IpTransactionService", "포트폴리오 API HTTP 오류: ${response.code()}, ${response.message()}")
                        callback(null, Exception("포트폴리오 API 호출 실패: ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<PortfolioIPResponseDto>, t: Throwable) {
                    Log.e("IpTransactionService", "포트폴리오 API 호출 실패", t)
                    callback(null, t)
                }
            }
        )
    }
}
