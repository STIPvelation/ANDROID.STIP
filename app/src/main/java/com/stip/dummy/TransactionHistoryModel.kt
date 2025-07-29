package com.stip.dummy

import java.io.Serializable
import java.text.DecimalFormat

data class TransactionHistory(
    val id: Long,
    val memberNumber: String,
    val amount: Long,               // 예: 1000000 => 1.00 단위 처리
    val timestamp: Long,
    val currencyCode: String,       // 실제 거래 통화 코드 (예: "USD", "POL")
    val usdAmount: Long,            // 환산 금액 (ex: 1000 => 10.00)
    val status: Status,
    val executionPrice: Double = 0.0,
    val heading: String? = null,

    // 티커 전송 상세용
    val ticker: String? = null,
    val toAddress: String? = null,
    val networkName: String? = null,
    val txId: String? = null
) : Serializable {
    // 👉 수량 그대로 (필요시 사용)
    val quantity: Double
        get() = amount / 1_000_000.0

    // 👉 소수점 둘째 자리로 고정된 표시 금액
    val formattedAmount: String
        get() = "${twoDecimalFormat.format(amount / 1_000_000.0)} ${ticker ?: currencyCode}"

    // 👉 소수점 둘째 자리로 고정된 USD 환산 (USD 고정 아님)
    val formattedUsdValue: String
        get() {
            val symbol = if (currencyCode.equals("USD", true)) "USD" else (ticker ?: currencyCode)
            return "${twoDecimalFormat.format(usdAmount / 100.0)} $symbol"
        }

    enum class Status {
        DEPOSIT_COMPLETED,
        WITHDRAWAL_COMPLETED,
        REFUND_COMPLETED,
        PROCESSING
    }

    companion object {
        private val twoDecimalFormat = DecimalFormat("#,##0.00").apply { roundingMode = java.math.RoundingMode.DOWN } // 🔥 소수점 둘째 자리
    }
}
