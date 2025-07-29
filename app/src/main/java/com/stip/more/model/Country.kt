package com.stip.stip.more.model

/**
 * 국가 정보를 담기 위한 모델
 * iOS의 Country struct와 동일한 구조로 구현
 */
data class Country(
    val id: String,      // 국가 코드 (예: "KR", "US")
    val name: String,    // 국가 이름
    val emoji: String,   // 국기 이모지
    var isSelected: Boolean = false // 선택 여부
)
