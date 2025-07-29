package com.stip.stip.ipinfo.model

// StipIndexItem 데이터 클래스 정의 (만약 없다면 새로 추가)
data class StipIndexItem(
    val title: String,          // 지수 이름 (예: "STIP 종합지수")
    var percentageString: String, // 화면에 표시될 포맷된 퍼센트 문자열 (예: "+42.14%")
    var currentValue: Float,    // 현재 지수 값 (계산용)
    val baseValue: Float        // 기준 지수 값 (퍼센트 계산 기준)
)