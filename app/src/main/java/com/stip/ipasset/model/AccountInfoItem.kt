package com.stip.ipasset.model

data class AccountInfoItem(
    val id: String,
    val name: String,
    val accountNumber: String,
    val bankName: String,
    val isSelected: Boolean = false
)
