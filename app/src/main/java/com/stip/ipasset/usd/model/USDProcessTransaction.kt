package com.stip.ipasset.usd.model

/**
 * USD 진행중 트랜잭션 모델 클래스
 */
data class USDProcessTransaction(
    val id: String,
    val date: String,
    val time: String,
    val status: String,
    val usdAmount: Double,
    val krwAmount: Long,
    val statusColorRes: Int
)
