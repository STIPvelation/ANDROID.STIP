package com.stip.stip.signup.model

data class RequestAccountOTP(
    val otp: String,
    val requestNo: String,
    val responseUniqueId: String
)