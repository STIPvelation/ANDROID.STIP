package com.stip.stip.signup.di

import com.stip.stip.BuildConfig
import com.stip.stip.signup.Constants.PREF_KEY_AUTH_TOKEN_VALUE
import com.stip.stip.signup.api.AuthService
import com.stip.stip.signup.api.KakaoLocationService
import com.stip.stip.signup.api.MemberService
import com.stip.stip.signup.utils.PreferenceUtil
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.stip.stip.api.RetrofitClient
import com.stip.stip.api.service.TapiHourlyDataService
import com.stip.stip.api.service.TapiDailyDataService
import com.stip.stip.api.service.MarketPairsService

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    private const val isProduction = true;
    private val API_URL =
        if (isProduction) "https://api.sharetheip.com/" else "https://tapi.sharetheip.com/"

    const val KAKAO_BASE_URL: String = "https://dapi.kakao.com/"
    private const val X_API_Key: String = "AIzaSyAM4J1XFF6SAkXeY78ONDyRtgo3mhk78kE"

    private const val HTTP_CONNECT_TIMEOUT: Long = 20
    private const val HTTP_WRITE_TIMEOUT: Long = 60
    private const val HTTP_READ_TIMEOUT: Long = 60

    @Singleton
    @Provides
    fun provideOkhttpClient() =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(provideLoggingInterceptor())
            .addInterceptor(provideHeaderInterceptor())
            .build()

    @Singleton
    @Provides
    fun provideKakaoOkhttpClient() =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(provideLoggingInterceptor())
            .build()

    @Singleton
    @Provides
    fun provideHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .addHeader("X_API_KEY", X_API_Key)

            val currentToken = PreferenceUtil.getString(PREF_KEY_AUTH_TOKEN_VALUE)
            if (currentToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $currentToken")
            }

            val request = requestBuilder.build()
            android.util.Log.d("ApiModule", "API 요청 URL: ${request.url}")
            android.util.Log.d("ApiModule", "API 요청 메서드: ${request.method}")
            
            chain.proceed(request)
        }
    }

    @Singleton
    @Provides
    fun provideLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthService(): AuthService {
        return Retrofit.Builder()
            .client(provideOkhttpClient())
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideMemberService(): MemberService {
        return Retrofit.Builder()
            .client(provideOkhttpClient())
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(MemberService::class.java)
    }

    @Provides
    @Singleton
    fun provideKakaoLocationService(): KakaoLocationService {
        return Retrofit.Builder()
            .client(provideKakaoOkhttpClient())
            .baseUrl(KAKAO_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(KakaoLocationService::class.java)
    }

    @Provides
    @Singleton
    fun provideTapiHourlyDataService(): TapiHourlyDataService {
        return Retrofit.Builder()
            .client(provideOkhttpClient())
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(TapiHourlyDataService::class.java)
    }

    @Provides
    @Singleton
    fun provideMarketPairsService(): MarketPairsService {
        return Retrofit.Builder()
            .client(provideOkhttpClient())
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(MarketPairsService::class.java)
    }

    @Provides
    @Singleton
    fun provideTapiDailyDataService(): TapiDailyDataService {
        return Retrofit.Builder()
            .client(provideOkhttpClient())
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory.create())
            .build()
            .create(TapiDailyDataService::class.java)
    }

}