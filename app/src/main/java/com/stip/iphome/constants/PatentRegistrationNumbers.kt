package com.stip.stip.iphome.constants

/**
 * 티커별 특허 등록 번호를 관리하는 객체
 * 모든 티커에 대한 등록번호 정보를 제공합니다.
 */
object PatentRegistrationNumbers {
    
    // 기본 등록번호 값 (데이터가 없을 경우)
    const val DEFAULT_VALUE = "정보 없음"
    
    // 티커별 특허 등록번호 매핑 - 모든 티커에 대한 등록번호 추가
    private val tickerToRegistrationNumber = mapOf(
        // 기존 티커
        "CDM" to "특허 제 10-2621090호",
        "IJECT" to "특허 제 10-1377987호",
        "JWV" to "특허 제 10-1912525호",
        "KCOT" to "특허 제 10-2133229호",
        "MDM" to "특허 제 10-1753835호",
        "SMT" to "특허 제 10-6048639호",
        "WETALK" to "특허 제 10-2004315호,10-2317027호,40-1400891호",
        "AXNO" to "Business Model",
        "KATV" to "특허 제 10-2536882호",
        "SLEEP" to "특허 제 10-2762048호,10-2708038호,20-0493828호",
        "MSK" to "특허 제 10-2412492호",
    )
    
    /**
     * 티커에 해당하는 특허 등록번호를 반환
     * 대소문자를 무시하고 검색합니다.
     *
     * @param ticker 티커 이름 (예: "JWV", "AXNO" 등)
     * @param defaultValue 티커에 해당하는 등록번호가 없을 경우 사용할 기본값
     * @return 특허 등록번호 문자열
     */
    fun getRegistrationNumberForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        // 티커 이름에서 앞 부분만 추출 (슬래시가 있는 경우)
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        
        // 대소문자를 무시하고 매핑에서 검색
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToRegistrationNumber.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커 이름 목록을 가져옵니다.
     * @return 모든 등록된 티커 이름 목록
     */
    fun getAllTickers(): List<String> {
        return tickerToRegistrationNumber.keys.toList()
    }
    
    /**
     * 모든 티커와 등록번호 매핑을 가져옵니다.
     * @return 티커 이름과 등록번호의 Map
     */
    fun getAllRegistrationNumbers(): Map<String, String> {
        return tickerToRegistrationNumber.toMap()
    }
}
