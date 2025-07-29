package com.stip.stip.ipinfo.model

import java.util.Date

// 데이터 클래스 정의 (기존 필드 포함)
data class ProfitItem(
    val date: String,
    val dateRaw: Date, // Date 타입 사용
    val dailyProfit: String,
    val cumulativeProfit: String,
    val dailyRate: String,
    val cumulativeRate: String,
    val endingAsset: String,
    val startingAsset: String,
    val deposit: String,
    val withdrawal: String
)