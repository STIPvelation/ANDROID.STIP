package com.stip.ipasset.usd.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.util.Locale

/**
 * USD 입금 트랜잭션 데이터 클래스
 */
@Parcelize
data class USDDepositTransaction(
    val id: String,
    val date: String, // 날짜 형식: "2025.06.29"
    val time: String, // 시간 형식: "10:24"
    val status: String = "입금 완료",
    val usdAmount: Double,
    val krwAmount: Int,
    val txHash: String? = null,
    val statusColorRes: Int = android.R.color.holo_blue_dark // 기본 상태 색상
) : Parcelable {
    fun getFormattedUsdAmount(): String = "${NumberFormat.getNumberInstance(Locale.US).format(usdAmount)} USD"
    fun getFormattedKrwAmount(): String = "${NumberFormat.getNumberInstance(Locale.US).format(krwAmount)} KRW"
    
    fun getFormattedDate(): String = date
    fun getFormattedTime(): String = time
}