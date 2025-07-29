package com.stip.stip.signup

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/v1/ask_form") // API 엔드포인트 입력(서버)
    fun getFormData(): Call<ApiResponse>

    @POST("api/v1/success")  // 실제 복호화 API 엔드포인트로 변경하세요.
    suspend fun decryptData(@Body request: DecryptionRequest): Response<DecryptionResponse>

}

data class ApiResponse(
    val token_version_id: String,
    val enc_data: String,
    val integrity_value: String
)

data class DecryptionRequest(
    val enc_data: String,
    val integrity_value: String
)

// 서버 응답 데이터 모델
data class DecryptionResponse(
    @SerializedName("responseno") val responseNo: String?,
    @SerializedName("birthdate") val birthDate: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("di") val di: String?,
    @SerializedName("mobileco") val mobileCo: String?,
    @SerializedName("ci") val ci: String?,
    @SerializedName("receivedata") val receiveData: String?,
    @SerializedName("mobileno") val mobileNo: String?,
    @SerializedName("requestno") val requestNo: String?,
    @SerializedName("nationalinfo") val nationalInfo: String?,
    @SerializedName("authtype") val authType: String?,
    @SerializedName("sitecode") val siteCode: String?,
    @SerializedName("utf8name") val utf8Name: String?,
    @SerializedName("enctime") val encTime: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("resultcode") val resultCode: String?,
    @SerializedName("businessno") val businessno: String?
)