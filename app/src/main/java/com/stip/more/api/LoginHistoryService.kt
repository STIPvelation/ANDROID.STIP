package com.stip.stip.more.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import java.util.Date

data class LoginHistoryItem(
    val id: String,
    val loginTime: Date,
    val deviceInfo: String,
    val ipAddress: String,
    val location: String,
    val isCurrentDevice: Boolean
)

data class LoginHistoryResponse(
    val success: Boolean,
    val data: List<LoginHistoryItem>,
    val message: String
)

interface LoginHistoryService {
    @GET("/api/v1/security/login-history")
    fun getLoginHistory(
        @Header("Authorization") token: String
    ): Call<LoginHistoryResponse>
}
