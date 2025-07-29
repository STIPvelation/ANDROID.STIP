package com.stip.ipasset.ticker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 티커 출금 완료 트랜잭션 모델
 */
@Parcelize
data class TickerWithdrawalTransaction(
    val id: Long,
    val tickerAmount: Double,
    val tickerSymbol: String,
    val usdAmount: Double,
    val timestamp: Long,
    val status: String = "출금 완료",
    val txHash: String? = null,
    val recipientAddress: String? = null,
    val fee: Double = 0.0
) : Parcelable {
    fun getFormattedTickerAmount(): String = String.format("%,.0f %s", tickerAmount, tickerSymbol)
    fun getFormattedUsdAmount(): String = String.format("%.2f USD", usdAmount)
    
    fun getFormattedDate(): String {
        val date = Date(timestamp * 1000)
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        }
        return dateFormat.format(date)
    }
    
    fun getFormattedTime(): String {
        val date = Date(timestamp * 1000)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
        }
        return timeFormat.format(date)
    }
    
    fun getFormattedFee(): String = String.format("%.2f %s", fee, tickerSymbol)
}
