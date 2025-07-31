package com.stip.stip.order.data

/**
 * 주문 삭제 API 응답 모델
 */
data class OrderDeleteResponse(
    val success: Boolean,
    val message: String
)

/**
 * 다중 주문 취소 요청 모델
 */
data class OrderCancelRequest(
    val orderIds: List<String>
) 