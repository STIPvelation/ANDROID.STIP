package com.stip.stip.signup.model

import java.io.Serializable

data class BankData(
    val icon: Int,
    val name: String,
    val code: String
): Serializable
