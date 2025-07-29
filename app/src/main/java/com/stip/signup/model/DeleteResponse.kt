package com.stip.stip.signup.model

/**
 * 회원탈퇴 API 응답 모델
 */
data class DeleteResponse(
    val success: Boolean,
    val message: String
) 