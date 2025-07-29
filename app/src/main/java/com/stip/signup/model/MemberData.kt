package com.stip.stip.signup.model

data class MemberData(
    val createdAt: Long,
    val id: String,
    val isPhoneNumberVerified: Boolean,
    val name: String,
    val phoneNumber: String,
    val telecomProvider: String,
    val memberNumber: String // 9자리 회원번호(공통번호)
)