package com.stip.stip.signup.api.repository.auth

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.api.AuthService
import com.stip.stip.signup.model.RequestAccountNumber
import com.stip.stip.signup.model.RequestAccountOTP
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.ResponseAccountNumber
import com.stip.stip.signup.model.ResponseAuthLogin
import com.stip.stip.signup.model.ResponseAuthPass
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
): AuthRepository {

    override suspend fun getNiceAuthPass(di: String): ApiResponse<ResponseAuthPass> {
        return authService.getNiceAuthPass(di)
    }

    override suspend fun postNiceAccountOwnerShipVerify(requestAccountOwnerShip: RequestAccountNumber): ApiResponse<ResponseAccountNumber> {
        return authService.postNiceAccountOwnerShipVerify(requestAccountOwnerShip)
    }

    override suspend fun postNiceAccountOwnerShipOTPVerify(requestAccountOTP: RequestAccountOTP): ApiResponse<Unit> {
        return authService.postNiceAccountOwnerShipOTPVerify(requestAccountOTP)
    }

    override suspend fun postAuthLogin(requestAuthLogin: RequestAuthLogin): ApiResponse<ResponseAuthLogin> {
        return authService.postAuthLogin(requestAuthLogin)
    }

}