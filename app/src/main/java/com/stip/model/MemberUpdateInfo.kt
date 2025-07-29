package com.stip.stip.model

data class MemberUpdateInfo(
    val englishFirstName: String,
    val englishLastName: String,
    val address: String,
    val addressDetail: String,
    val postalCode: String,
    val job: String
)