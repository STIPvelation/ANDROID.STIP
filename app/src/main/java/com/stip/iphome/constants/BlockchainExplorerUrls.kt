package com.stip.stip.iphome.constants

/**
 * 티커별 블록체인 탐색기 URL을 관리하는 객체
 * 각 티커에 해당하는 Polygonscan URL을 제공합니다.
 */
object BlockchainExplorerUrls {
    
    private val tickerUrlMap = mapOf(
        // Patent 상장 기업
        "JWV" to "https://polygonscan.com/token/0xf79d09fbc12030724825af9cecda3cf5db2e678f",
        "MDM" to "https://polygonscan.com/token/0x68f371e4771e24e88b72191ce5ea41c4b583cf46",
        "CDM" to "https://polygonscan.com/token/0xf0013f75f6a214c9f3a93b48fd744ab29f4a0ba6",
        "IJECT" to "https://polygonscan.com/token/0x68c59dc6cb6b3c6b61c7b9431ae27ab369340002",
        "WETALK" to "https://polygonscan.com/token/0x0ce52622aae6b00524d818f251b04ad8d59ee04f",
        "SLEEP" to "https://polygonscan.com/token/0x1a546b0c0634629c60ae7411d8b90d0c1d257ca4",
        
        // Patent 상장 개인
        "KCOT" to "https://polygonscan.com/token/0x53aed237fa6c022437ac570b5b353e9e02ed6af2",
        "MSK" to "https://polygonscan.com/token/0x8af4b7bff86108e0796059d6d8f49ddfb1b718a4",
        "SMT" to "https://polygonscan.com/token/0x5ca9f6987c43b1d96f3641dd271e901b7d283d5b",
        
        // Business Model 상장 기업
        "AXNO" to "https://polygonscan.com/token/0x373450b5488e0420a9b05b142d21b9d03511c217",
        "KATV" to "https://polygonscan.com/token/0x651f4b2d42313cb6044363d9441343bad4788346"
    )
    
    private const val DEFAULT_URL = "https://stipvelation.com/block"
    
    /**
     * 티커에 해당하는 블록체인 탐색기 URL을 반환
     * 
     * @param ticker 티커 이름 (예: "JWV", "AXNO" 등)
     * @param fallbackUrl 티커에 해당하는 URL이 없을 경우 사용할 기본 URL
     * @return 블록체인 탐색기 URL
     */
    fun getUrlForTicker(ticker: String?, fallbackUrl: String? = null): String {
        return if (ticker != null) {
            // 티커에서 USD 부분을 제거하고 매핑된 URL을 찾음
            val baseTicker = ticker.split("/").firstOrNull() ?: ticker
            tickerUrlMap[baseTicker] ?: fallbackUrl ?: DEFAULT_URL
        } else {
            fallbackUrl ?: DEFAULT_URL
        }
    }
}
