package com.stip.stip.api

import android.util.Log
import com.stip.order.api.OrderService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.stip.stip.signup.utils.PreferenceUtil

/**
 * Retrofit 클라이언트 객체
 * API 서버와의 통신을 담당하는 인스턴스를 생성하고 관리
 */
object RetrofitClient {
    private const val ENGINE_URL = "http://34.64.197.80:5000/"
    private const val TAPI_URL = "https://tapi.sharetheip.com/"
    private const val TIMEOUT = 30L // 30초 타임아웃
    
    private val okHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = PreferenceUtil.getToken()

                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }

                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    private val authOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = com.stip.stip.signup.utils.PreferenceUtil.getString(com.stip.stip.signup.Constants.PREF_KEY_AUTH_TOKEN_VALUE)
                val request = original.newBuilder().apply {
                    header("Content-Type", "application/json")
                    header("Accept", "application/json")
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                    android.util.Log.d("APII", "${original.method} : ${original.url} ${original.body} Token : ${token}")
                    method(original.method, original.body)
                }.build()
                chain.proceed(request)
            }
            .build()
    }
    
    // 인증이 필요없는 API 서비스용 Retrofit 인스턴스
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TAPI_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // 인증이 필요한 API 서비스용 Retrofit 인스턴스
    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TAPI_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // 엔진 서버용 OkHttpClient
    private val engineOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = PreferenceUtil.getToken()
                val request = original.newBuilder().apply {
                    header("Content-Type", "application/json")
                    header("Accept", "application/json")
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                    method(original.method, original.body)
                }.build()
                chain.proceed(request)
            }
            .build()
    }
    
    // TAPI 서버용 OkHttpClient
    private val tapiOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = PreferenceUtil.getToken()
                val request = original.newBuilder().apply {
                    header("Content-Type", "application/json")
                    header("Accept", "application/json")
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                    method(original.method, original.body)
                }.build()
                
                Log.d("RetrofitClient", "출금 요청: ${request.method} ${request.url}")
                
                val response = chain.proceed(request)
                
                Log.d("RetrofitClient", "출금 응답 상태 코드: ${response.code}")
                
                // 응답 본문이 비어있는지 확인
                val responseBody = response.body
                if (responseBody != null) {
                    val responseBodyString = responseBody.string()
                    Log.d("RetrofitClient", "응답 본문: $responseBodyString")
                    
                    // 응답 본문을 다시 생성
                    val newResponseBody = okhttp3.ResponseBody.create(
                        responseBody.contentType(),
                        responseBodyString
                    )
                    
                    response.newBuilder()
                        .body(newResponseBody)
                        .build()
                } else {
                    Log.d("RetrofitClient", "출금 응답: 비어있음")
                    response
                }
            }
            .build()
    }
    
    // 엔진 서버용 Retrofit 인스턴스
    private val engineRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ENGINE_URL)
            .client(engineOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // TAPI 서버용 Retrofit 인스턴스
    private val tapiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TAPI_URL)
            .client(tapiOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // API 서비스 인스턴스 생성 (인증 불필요)
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
    
    // API 서비스 인스턴스 생성 (인증 필요)
    fun <T> createAuthService(serviceClass: Class<T>): T {
        return authRetrofit.create(serviceClass)
    }
    
    // 엔진 서버 API 서비스 인스턴스 생성
    fun <T> createEngineService(serviceClass: Class<T>): T {
        return engineRetrofit.create(serviceClass)
    }
    
    // TAPI 서버 API 서비스 인스턴스 생성
    fun <T> createTapiService(serviceClass: Class<T>): T {
        return tapiRetrofit.create(serviceClass)
    }

    fun createOrderService(): OrderService {
        return tapiRetrofit.create(OrderService::class.java)
    }
}
