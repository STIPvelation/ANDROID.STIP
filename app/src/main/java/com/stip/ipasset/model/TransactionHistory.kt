package com.stip.ipasset.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionHistory(
    val id: String,
    val timestamp: Long,
    val type: String,
    val amount: Long,
    val ticker: String? = null,
    val currencyCode: String = "USD",
    val status: Status = Status.PROCESSING,
    val txId: String? = null,
    val toAddress: String? = null,
    val executionPrice: Double = 1.0  // Exchange rate to USD
) : Parcelable {
    enum class Status {
        DEPOSIT_COMPLETED,
        WITHDRAWAL_COMPLETED,
        REFUND_COMPLETED,
        PROCESSING
    }
    
    // Adding div operator for calculations
    operator fun div(divisor: Double): Double {
        return amount.toDouble() / divisor
    }
}
