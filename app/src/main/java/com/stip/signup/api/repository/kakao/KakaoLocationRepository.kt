package com.stip.stip.signup.api.repository.kakao

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.model.KakaoLocationData
import com.stip.stip.signup.model.KakaoZipCodeData

interface KakaoLocationRepository {

    /**
     * 카카오 주소 검색 API
     */
    suspend fun getKakoLocation(
        authorization: String,
        query: String
    ): ApiResponse<KakaoLocationData>

    /**
     * 카카오 우편 번호 검색 API
     */
    suspend fun getKakaoZipCode(
        key: String,
        query: String
    ): ApiResponse<KakaoZipCodeData>

}