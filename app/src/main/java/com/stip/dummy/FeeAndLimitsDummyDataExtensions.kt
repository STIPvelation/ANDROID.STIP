package com.stip.dummy

/**
 * FeeAndLimitsDummyData를 위한 확장 함수
 */

/**
 * 특정 통화의 최소 출금 한도를 반환합니다.
 */
fun FeeAndLimitsDummyData.getMinWithdrawalAmount(currency: String): Double {
    return when(currency) {
        "USD" -> 10.0
        else -> 1.0
    }
}
