package com.stip.ipasset.model

import com.google.gson.annotations.SerializedName

/**
 * 출금 응답 데이터 모델
 * API가 빈 응답을 보내는 경우를 대비하여 기본값 설정
 */
data class WithdrawResponse(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("message")
    val message: String = "출금이 성공적으로 처리되었습니다.",
    @SerializedName("data")
    val data: WithdrawData? = null,
    // 빈 응답 처리를 위한 추가 필드들
    @SerializedName("symbol")
    val symbol: String? = null,
    @SerializedName("balance")
    val balance: Double? = null,
    @SerializedName("availableBalance")
    val availableBalance: Double? = null
)

/**
 * 출금 응답 데이터 내용
 */
data class WithdrawData(
    @SerializedName("transactionId")
    val transactionId: String? = null,
    @SerializedName("txHash")
    val txHash: String? = null,
    @SerializedName("status")
    val status: String? = null
) 