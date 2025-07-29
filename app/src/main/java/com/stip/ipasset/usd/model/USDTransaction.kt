package com.stip.ipasset.usd.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * USD 트랜잭션 타입 (입금, 출금, 반환)
 */
enum class TransactionType {
    DEPOSIT,    // 입금
    WITHDRAWAL, // 출금
    RETURN      // 반환
}

/**
 * USD 트랜잭션 상태
 */
enum class TransactionStatus {
    COMPLETED,   // 완료
    IN_PROGRESS, // 진행중
    RETURNED     // 반환됨
}

/**
 * USD 트랜잭션 통합 모델
 */
@Parcelize
data class USDTransaction(
    val id: String,
    val date: String,          // 날짜 형식: "2025.06.29"
    val time: String,          // 시간 형식: "10:24"
    val type: TransactionType, // 트랜잭션 타입
    val status: TransactionStatus, // 트랜잭션 상태
    val amountUsd: Double,     // USD 금액
    val amountKrw: Int         // KRW 금액
) : Parcelable
