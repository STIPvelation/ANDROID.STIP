package com.stip.stip.signup.api

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.model.RequestAccountNumber
import com.stip.stip.signup.model.RequestAccountOTP
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.ResponseAccountNumber
import com.stip.stip.signup.model.ResponseAuthLogin
import com.stip.stip.signup.model.ResponseAuthPass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthService {

    /**
     * (1) NICE PASS 본인 인증 정보 조회
     */
    @GET("/api/v1/auth/di")
    suspend fun getNiceAuthPass(
        @Query("di") di: String
    ): ApiResponse<ResponseAuthPass>

    /**
     * (2) NICE 계좌 점유 인증 - 계좌 번호 확인
     */
    @POST("/api/v1/auth/account-ownership/verify")
    suspend fun postNiceAccountOwnerShipVerify(
        @Body requestAccountOwnerShip: RequestAccountNumber
    ): ApiResponse<ResponseAccountNumber>

    /**
     * (3) NICE 계좌 점유 인증 - OTP 번호 확인
     */
    @POST("/api/v1/auth/account-ownership/otp-verify")
    suspend fun postNiceAccountOwnerShipOTPVerify(
        @Body requestAccountOTP: RequestAccountOTP
    ): ApiResponse<Unit>

    /**
     * (5) 회원 로그인
     */
    @POST("/api/v1/auth/login")
    suspend fun postAuthLogin(
        @Body requestAuthLogin: RequestAuthLogin
    ): ApiResponse<ResponseAuthLogin>

}