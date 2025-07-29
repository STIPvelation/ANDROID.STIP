package com.stip.dummy

/**
 * 수수료 및 입출금 한도 더미 데이터를 관리하는 클래스
 * - 모든 화면에서 일관된 수수료 및 한도 정보를 사용할 수 있도록 함
 * - 실제 구현에서는 API 또는 설정에서 가져와야 함
 */
object FeeAndLimitsDummyData {
    
    /**
     * 티커별 최소 출금 한도
     */
    private val minWithdrawalAmounts = mapOf(
        "USD" to 10.0,
        "JWV" to 1.0,
        "MDM" to 1.0,
        "CDM" to 1.0,
        "IJECT" to 1.0,
        "WETALK" to 1.0,
        "SLEEP" to 1.0,
        "KCOT" to 1.0,
        "MSK" to 1.0,
        "SMT" to 1.0,
        "AXNO" to 1.0,
        "KATV" to 1.0
    )
    
    /**
     * 티커별 최대 출금 한도
     * 모든 티커에 대해 1,000,000으로 고정
     */
    private val maxWithdrawalAmounts = mapOf(
        "USD" to 1000000.0,
        "JWV" to 1000000.0,
        "MDM" to 1000000.0,
        "CDM" to 1000000.0,
        "IJECT" to 1000000.0,
        "WETALK" to 1000000.0,
        "SLEEP" to 1000000.0,
        "KCOT" to 1000000.0,
        "MSK" to 1000000.0,
        "SMT" to 1000000.0,
        "AXNO" to 1000000.0,
        "KATV" to 1000000.0
    )
    
    /**
     * 특정 티커의 최소 출금 한도 조회
     */
    fun getMinWithdrawalAmount(currencyCode: String): Double {
        return minWithdrawalAmounts[currencyCode] ?: 1.0 // 기본 최소 1.0
    }
}
