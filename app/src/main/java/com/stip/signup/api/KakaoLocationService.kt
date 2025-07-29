package com.stip.stip.signup.api

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.model.KakaoLocationData
import com.stip.stip.signup.model.KakaoZipCodeData
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocationService {

    /**
     * 카카오 주소 검색 API
     */
    @GET("v2/local/search/keyword.json")
    suspend fun getKakoLocation(
        @Header("Authorization") key: String,
        @Query("query") query: String
    ): ApiResponse<KakaoLocationData>

    /**
     * 카카오 우편 번호 검색 API
     */
    @GET("v2/local/search/address.json")
    suspend fun getKakaoZipCode(
        @Header("Authorization") key: String,
        @Query("query") query: String
    ): ApiResponse<KakaoZipCodeData>

}
