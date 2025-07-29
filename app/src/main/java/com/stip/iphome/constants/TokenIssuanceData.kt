package com.stip.stip.iphome.constants

/**
 * 토큰 발행 관련 데이터를 관리하는 상수 클래스
 */
object TokenIssuanceData {
    
    // 공통 발행 한도
    const val DEFAULT_TOTAL_ISSUANCE_LIMIT = "1,000,000"
    
    // 티커별 첫 발행일 매핑
    private val tickerToFirstIssuanceDate = mapOf(
        "AXNO" to "May-28-2025",
        "KATV" to "May-28-2025",
        "SLEEP" to "Jun-09-2025",
        "WETALK" to "May-26-2025",
        "IJECT" to "May-28-2025",
        "CDM" to "May-28-2025",
        "MDM" to "May-28-2025",
        "JWV" to "May-28-2025",
        "SMT" to "May-28-2025",
        "MSK" to "May-28-2025",
        "KCOT" to "May-28-2025"
    )
    
    /**
     * 티커에 해당하는 첫 발행일을 반환
     *
     * @param ticker 티커 이름 (예: "JWV", "AXNO" 등)
     * @param defaultValue 티커에 해당하는 발행일이 없을 경우 사용할 기본값
     * @return 발행일 문자열
     */
    fun getFirstIssuanceDateForTicker(ticker: String?, defaultValue: String? = null): String? {
        if (ticker == null) return defaultValue
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        return tickerToFirstIssuanceDate[baseTicker] ?: defaultValue
    }
    
    /**
     * 티커에 해당하는 총 발행 한도를 반환
     * 모든 티커는 동일한 발행 한도(1,000,000)를 가짐
     *
     * @return 발행 한도 문자열
     */
    fun getTotalIssuanceLimit(): String {
        return DEFAULT_TOTAL_ISSUANCE_LIMIT
    }
}
