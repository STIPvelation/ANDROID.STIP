package com.stip.dummy

import java.util.Date
import java.io.Serializable

/**
 * USD 입금 트랜잭션 더미 모델
 */
data class USDDepositTransaction(
    val id: Long,
    val amount: Double, // USD 금액
    val amountKrw: Int,  // KRW 금액
    val timestamp: Long,  // 타임스탬프 (초 단위)
    val status: String,   // 상태 (입금 완료 등)
    val txHash: String,   // 트랜잭션 해시
    val exchangeRate: Double // 환율 (1 USD = ? KRW)
) : Serializable {
    // 날짜 객체로 변환
    val date: Date
        get() = Date(timestamp * 1000)
}

/**
 * USD 출금 트랜잭션 더미 모델
 */
data class USDWithdrawalTransaction(
    val id: Long,
    val amount: Double, // USD 금액
    val amountKrw: Int,  // KRW 금액
    val timestamp: Long,  // 타임스탬프 (초 단위)
    val status: String,   // 상태 (출금 완료 등)
    val txHash: String,   // 트랜잭션 해시
    val exchangeRate: Double, // 환율 (1 USD = ? KRW)
    val recipientAddress: String, // 수신자 주소
    val fee: Double       // 수수료
) : Serializable {
    // 날짜 객체로 변환
    val date: Date
        get() = Date(timestamp * 1000)
}
