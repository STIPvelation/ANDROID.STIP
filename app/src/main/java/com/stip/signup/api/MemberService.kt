package com.stip.stip.signup.api

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.signup.model.MemberData
import com.stip.stip.signup.model.RequestSignUpMember
import com.stip.stip.signup.model.RequestPinNumber
import com.stip.stip.signup.model.RequestAuthLogin
import com.stip.stip.signup.model.ResponseExistMember
import com.stip.stip.signup.model.ResponseSignUpMember
import com.stip.stip.signup.model.DeleteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface MemberService {

    /**
     * (4) 회원 가입
     */
    @POST("/api/v1/members")
    suspend fun postSignUpMembers(
        @Body requestSignUpMember: RequestSignUpMember
    ): ApiResponse<ResponseSignUpMember>


    /**
     * 로그인 화면 PIN 번호 수정
     */
    @PATCH("/api/v1/members/me/pin")
    suspend fun patchMemberPin(
        @Query("di") di: String,
        @Body requestPinNumber: RequestPinNumber
    ): ApiResponse<Unit>

    /**
     * PIN 번호 확인
     */
    @POST("/api/v1/members/me/pin/verify")
    suspend fun verifyMemberPin(
        @Query("di") di: String,
        @Body requestPinNumber: RequestPinNumber
    ): ApiResponse<Unit>


    /**
     * 회원 조회
     */
    @GET("/api/v1/members/me")
    suspend fun getMembers(
    ): ApiResponse<MemberData>


    /**
     * 로그인 화면 회원 존재 여부
     */
    @GET("/api/v1/members/di/existence")
    suspend fun getExistenceMember(
        @Query("di") di: String
    ): ApiResponse<ResponseExistMember>


    /**
     * 로그인 화면 회원 삭제
     */
    @DELETE("/api/v1/members/di")
    suspend fun deleteMembersByDi(
        @Query("di") di: String
    ): ApiResponse<Unit>


    /**
     * 회원 탈퇴
     */
    @DELETE("/api/v1/members/me")
    suspend fun deleteMembers(
        @Header("Authorization") authorization: String
    ): Response<DeleteResponse>

}