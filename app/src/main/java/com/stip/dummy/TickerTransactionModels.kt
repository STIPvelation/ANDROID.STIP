package com.stip.dummy

import java.util.Date
import java.io.Serializable

/**
 * 티커 입금 트랜잭션 더미 모델
 */
data class TickerDepositTransaction(
    val id: Long,
    val tickerAmount: Double, // 티커 금액
    val tickerSymbol: String, // 티커 심볼
    val usdAmount: Double,    // USD 가치
    val timestamp: Long,      // 타임스탬프 (초 단위)
    val status: String,       // 상태 (입금 완료 등)
    val txHash: String        // 트랜잭션 해시
) : Serializable {
    // 날짜 객체로 변환
    val date: Date
        get() = Date(timestamp * 1000)
}

/**
 * 티커 출금 트랜잭션 더미 모델
 */
data class TickerWithdrawalTransaction(
    val id: Long,
    val tickerAmount: Double, // 티커 금액
    val tickerSymbol: String, // 티커 심볼
    val usdAmount: Double,    // USD 가치
    val timestamp: Long,      // 타임스탬프 (초 단위)
    val status: String,       // 상태 (출금 완료 등)
    val txHash: String,       // 트랜잭션 해시
    val recipientAddress: String, // 수신자 주소
    val fee: Double           // 수수료
) : Serializable {
    // 날짜 객체로 변환
    val date: Date
        get() = Date(timestamp * 1000)
}
