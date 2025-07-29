package com.stip.stip.iphome.model

data class OrderBookItem(
    val price: String = "",
    val quantity: String = "",
    val percent: String = "",
    val isBuy: Boolean = false,
    val isCurrentPrice: Boolean = false,
    val isGap: Boolean = false,
    val startPrice: Float = 0f,      // 더미 시작가
    val yesterdayPrice: Float = 0f   // 더미 어제 가격
)
