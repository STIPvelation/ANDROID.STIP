package com.stip.stip.iphome.model

data class QuoteTickDaily(
    val date: String,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val closePrice: Double,
    val volume: Double,
    val changeFromPrevious: Double,
    val changePercent: Double
)