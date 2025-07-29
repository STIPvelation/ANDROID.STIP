package com.stip.stip.ipinfo.model

data class DipMarketCapItem(
    val logoResId: Int?,  // 로고 리소스 (nullable)
    val name: String,     // DIP 이름
    val number: String,   // 시가총액 숫자
    val order: String     // 순위 (예: "1위")
)