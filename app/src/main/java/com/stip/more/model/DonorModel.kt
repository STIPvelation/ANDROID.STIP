package com.stip.stip.more.model

data class DonorModel(
    val name: String,
    val donationCount: Int,
    val isCurrentUser: Boolean = false
)
