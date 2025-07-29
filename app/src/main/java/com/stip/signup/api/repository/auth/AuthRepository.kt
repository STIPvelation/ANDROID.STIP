package com.stip.stip.signup.api.repository.auth

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.model.RequestAccountNumber
import com.stip.stip.signup.model.RequestAccountOTP
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.ResponseAccountNumber
import com.stip.stip.signup.model.ResponseAuthLogin
import com.stip.stip.signup.model.ResponseAuthPass
import retrofit2.http.Body

interface AuthRepository {

    /**
     * (1) NICE PASS 본인 인증 정보 조회
     */
    suspend fun getNiceAuthPass(
        di: String
    ): ApiResponse<ResponseAuthPass>

    /**
     * (2) NICE PASS 인증용 암호화 폼 데이터 조회
     */
    suspend fun postNiceAccountOwnerShipVerify(
        @Body requestAccountOwnerShip: RequestAccountNumber
    ): ApiResponse<ResponseAccountNumber>

    /**
     * (3) NICE PASS 인증용 암호화 폼 데이터 조회
     */
    suspend fun postNiceAccountOwnerShipOTPVerify(
        @Body requestAccountOTP: RequestAccountOTP
    ): ApiResponse<Unit>

    /**
     * (5) 회원 로그인
     */
    suspend fun postAuthLogin(
        @Body requestAuthLogin: RequestAuthLogin
    ): ApiResponse<ResponseAuthLogin>
}