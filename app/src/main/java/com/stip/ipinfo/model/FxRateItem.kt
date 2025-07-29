package com.stip.stip.ipinfo.model

data class FxRateItem(
    val country: String,    // 나라 이름 (ex: 미국)
    val code: String,       // 통화쌍 코드 (ex: USD/KRW)
    val rate: Float,        // 현재 환율
    val change: Float,      // 전일 대비 변동폭
    val percent: Float      // 변동 퍼센트
)