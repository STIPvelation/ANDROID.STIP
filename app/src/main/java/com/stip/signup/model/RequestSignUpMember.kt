package com.stip.stip.signup.model

data class RequestSignUpMember(
    val accountNumber: String,
    val address: String,
    val addressDetail: String,
    val bankCode: String,
    val di: String,
    val email: String,
    val englishFirstName: String,
    val englishLastName: String,
    val isDirectAccount: Boolean,
    val job: String,
    val name: String,
    val pin: String,
    val postalCode: String,
    val sourceOfFunds: String,
    val usagePurpose: String
)