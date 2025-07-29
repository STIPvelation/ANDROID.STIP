package com.stip.stip.signup.api.repository.kakao

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.api.KakaoLocationService
import com.stip.stip.signup.model.KakaoLocationData
import com.stip.stip.signup.model.KakaoZipCodeData
import javax.inject.Inject

class KakaoLocationRepositoryImpl @Inject constructor(
    private val kakaoLocationService: KakaoLocationService
): KakaoLocationRepository {
    override suspend fun getKakoLocation(
        authorization: String,
        query: String
    ): ApiResponse<KakaoLocationData> {
        return kakaoLocationService.getKakoLocation(authorization, query)
    }

    override suspend fun getKakaoZipCode(
        key: String,
        query: String
    ): ApiResponse<KakaoZipCodeData> {
        return kakaoLocationService.getKakaoZipCode(key, query)
    }
}