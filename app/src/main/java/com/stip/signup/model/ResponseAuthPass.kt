package com.stip.stip.signup.model

data class ResponseAuthPass(
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