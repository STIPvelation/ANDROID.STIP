package com.stip.iphome.constants

import com.stip.stip.R

/**
 * 각 티커별 색상 리소스 ID를 관리하는 클래스
 */
object TokenLogos {
    
    // 티커별 색상 리소스 매핑
    private val tickerToColorMap = mapOf<String, Int>(
        // Patent 상장 기업
        "JWV" to R.color.token_jwv,
        "MDM" to R.color.token_mdm,
        "CDM" to R.color.token_cdm,
        "IJECT" to R.color.token_iject,
        "WETALK" to R.color.token_wetalk,
        "SLEEP" to R.color.token_sleep,
        
        // Patent 상장 개인
        "KCOT" to R.color.token_kcot,
        "MSK" to R.color.token_msk,
        "SMT" to R.color.token_smt,
        
        // Business Model 상장 기업
        "AXNO" to R.color.token_axno,
        "KATV" to R.color.token_katv,
        
        // 기본
        "USD" to R.color.token_usd
    )
    
    // 기본 색상
    private val DEFAULT_COLOR = R.color.token_usd
    
    /**
     * 티커에 해당하는 색상 리소스 ID를 반환
     *
     * @param ticker 티커 이름 (예: "JWV", "AXNO" 등)
     * @return 색상 리소스 ID
     */
    fun getColorForTicker(ticker: String?): Int {
        if (ticker == null) return DEFAULT_COLOR
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        return tickerToColorMap[baseTicker] ?: DEFAULT_COLOR
    }
}
