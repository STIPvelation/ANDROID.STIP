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
    val timestampIso: String,
    val status: String = "출금 완료",
    val txHash: String? = null,
    val recipientAddress: String? = null,
    val fee: Double = 0.0
) : Parcelable {
    fun getFormattedTickerAmount(): String {
        val formatter = java.text.DecimalFormat("#,##0.00").apply { 
            roundingMode = java.math.RoundingMode.DOWN 
            isGroupingUsed = true
        }
        return "${formatter.format(tickerAmount)} $tickerSymbol"
    }
    
    fun getFormattedUsdAmount(): String {
        val formatter = java.text.DecimalFormat("#,##0.00").apply { 
            roundingMode = java.math.RoundingMode.DOWN 
            isGroupingUsed = true
        }
        return "${formatter.format(usdAmount)} USD"
    }
    
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            val date = inputFormat.parse(timestampIso)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                // fallback: 직접 파싱
                val year = timestampIso.substring(0, 4)
                val month = timestampIso.substring(5, 7)
                val day = timestampIso.substring(8, 10)
                "$year.$month.$day"
            } catch (e2: Exception) {
                "----.--.--"
            }
        }
    }
    
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul")
            }
            val date = inputFormat.parse(timestampIso)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                // fallback: 직접 파싱
                timestampIso.substring(11, 16)
            } catch (e2: Exception) {
                "--:--"
            }
        }
    }
    
    fun getFormattedFee(): String = String.format("%.2f %s", fee, tickerSymbol)
}
