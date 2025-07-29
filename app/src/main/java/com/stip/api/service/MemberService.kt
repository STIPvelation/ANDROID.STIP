package com.stip.stip.api.service

import com.stip.stip.model.MemberInfo
import com.stip.stip.model.MemberUpdateInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Query

data class EmailUpdateRequest(
    val email: String
)

data class PhoneNumberUpdateRequest(
    val phoneNumber: String
)

/**
 * DI 응답 모델
 */
data class DiResponse(
    val di: String
)

/**
 * NICE 본인인증 응답 모델
 */
data class NiceAuthResponse(
    val authType: String,
    val birthdate: String,
    val ci: String,
    val di: String,
    val gender: String,
    val name: String,
    val phoneNumber: String,
    val requestNo: String,
    val responseNo: String,
    val telecomProvider: String
)

/**
 * 회원정보 관련 API 서비스 인터페이스
 */
interface MemberService {
    
    /**
     * 회원 정보 조회
     * @return 회원 정보 응답
     */
    @GET("api/v1/members/me")
    suspend fun getMemberInfo(): MemberInfo
    
    /**
     * 회원 정보 수정
     * @param memberInfo 수정할 회원 정보
     * @return 수정된 회원 정보 응답
     */
    @PUT("api/v1/members/me")
    suspend fun updateMemberInfo(@Body memberInfo: MemberUpdateInfo): Response<Unit>

    /**
     * 회원 이메일 정보 수정
     * @param request 수정할 이메일 정보
     * @return Response<Unit> HTTP 상태 코드 포함
     */
    @PATCH("api/v1/members/me/email")
    suspend fun updateEmail(@Body request: EmailUpdateRequest): Response<Unit>
    
    /**
     * 회원 전화번호 수정
     * @param request 수정할 전화번호 정보
     * @return Response<Unit> HTTP 상태 코드 포함
     */
    @PATCH("api/v1/members/me/phone")
    suspend fun updatePhoneNumber(@Body request: PhoneNumberUpdateRequest): Response<Unit>
    
    /**
     * NICE 본인인증 DI로 전화번호 조회
     * @param di NICE 인증 DI 값
     * @return 인증된 사용자 정보 (이름, 생년월일, 전화번호 등)
     */
    @GET("api/v1/auth/di")
    suspend fun getNiceAuthInfo(@Query("di") di: String): Response<NiceAuthResponse>
}
