package com.stip.stip.signup.api.service

import com.skydoves.sandwich.ApiResponse
import com.stip.stip.model.MemberInfo
import retrofit2.http.GET

interface MemberService {
    @GET("api/members")
    suspend fun getMembers(): ApiResponse<MemberInfo>
} 