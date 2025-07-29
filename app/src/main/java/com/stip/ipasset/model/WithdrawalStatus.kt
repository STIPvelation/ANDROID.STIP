package com.stip.ipasset.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WithdrawalStatus(
    val amount: Float,
    val address: String,
    val assetName: String,
    val ticker: String,
    val fee: Float,
    val totalAmount: Float
) : Parcelable
