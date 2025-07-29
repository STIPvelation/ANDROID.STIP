package com.stip.stip.model

data class MemberInfo(
    val id: String,
    val name: String,
    val englishFirstName: String,
    val englishLastName: String,
    val telecomProvider: String,
    val phoneNumber: String,
    val birthdate: String,
    val email: String,
    val bankCode: String,
    val accountNumber: String,
    val address: String,
    val addressDetail: String,
    val postalCode: String,
    val isDirectAccount: Boolean,
    val usagePurpose: String,
    val sourceOfFunds: String,
    val job: String
)