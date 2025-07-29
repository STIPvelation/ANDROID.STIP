package com.stip.ipasset.usd.model

/**
 * USD 반환 트랜잭션 모델 클래스
 */
data class USDReturnTransaction(
    val id: String,
    val date: String,
    val time: String,
    val status: String,
    val usdAmount: Double,
    val krwAmount: Long,
    val statusColorRes: Int
)
