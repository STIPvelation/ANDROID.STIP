package com.stip.stip.iphome.model // 실제 프로젝트 패키지에 맞게 수정하세요

// PriceChangeStatus Enum은 변경 없음
enum class PriceChangeStatus {
    UP, DOWN, SAME
}

// RecyclerView의 한 항목을 나타내는 데이터 클래스
data class QuoteTick(
    val id: String, // 각 아이템을 고유하게 식별할 ID (DiffUtil에 사용)
    val time: String,      // 시간 (예: "15:01:30")
    val price: Double,     // 가격 (숫자)
    val volume: Double,    // 체결량 (숫자)
    val priceChangeStatus: PriceChangeStatus = PriceChangeStatus.SAME // 가격 변동 상태
)
// 데이터 모델 클래스 내부에 있던 포맷팅 함수들은 제거했습니다.
// 가격 및 체결량 포맷팅은 QuotesAdapter 에서 처리합니다.