package com.stip.stip.more.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class OverseasLoginBlockRequest(
    val block: Boolean, // true: 차단, false: 해제
    val deviceLocale: String, // 현재 기기의 언어 설정
    val restrictToCountryByLanguage: Boolean = true, // 언어별 국가 제한 여부
    val userIpAddress: String? = null // 영어 사용 시 IP 기반으로 국가 확인을 위한 유저 IP
)

data class OverseasLoginBlockResponse(
    val success: Boolean,
    val message: String
)

interface Blockoverseaslogin {
    @POST("/api/v1/security/overseas-login-block")
    fun setOverseasLoginBlock(
        @Body request: OverseasLoginBlockRequest
    ): Call<OverseasLoginBlockResponse>
}
