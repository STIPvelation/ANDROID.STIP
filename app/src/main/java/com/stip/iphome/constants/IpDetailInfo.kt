package com.stip.stip.iphome.constants

/**
 * IP 상세 정보를 관리하는 객체
 * IP링크, 홈페이지, 사업계획, 관련영상, 법인명, 대표자, 사업자등록번호, 주소, 업종 정보를 포함합니다.
 */
object IpDetailInfo {
    
    // 기본값 상수
    const val DEFAULT_VALUE = "정보 없음"
    
    /**
     * IP 링크 정보
     * 티커별 IP 관련 링크 정보를 저장
     */
    private val tickerToIpLink = mapOf<String, String>(
        // 실제 링크 정보로 채워질 예정
        "WETALK" to "https://play.google.com/store/apps/details?id=com.hustay.swing.ddd5b0f8cea664c07a746d7ef57cc7724&pcampaignid=web_share",
        "AXNO" to "https://play.google.com/store/apps/details?id=com.axno.safs&pcampaignid=web_share",
        "MDM" to "https://play.google.com/store/apps/details?id=com.modumom.mobileapp.selfnamebase&hl=ko",
        "IJECT" to "https://play.google.com/store/apps/details?id=omni.medi_app&hl=ko"
    )
    
    /**
     * 홈페이지 링크 정보
     * 티커별 홈페이지 URL 정보를 저장
     */
    private val tickerToHomepage = mapOf(
        "SLEEP" to "https://kokodoc.com/",
        "KATV" to "http://gmigroup.co.kr/index.php",
        "MDM" to "https://www.modumom.com/",
        "IJECT" to "https://medihub.co.kr/",
        "CDM" to "https://www.kpdp.kr/",
        "WETALK" to "http://dooremall.co.kr/"
    )
    
    /**
     * 사업계획 문서 링크 정보
     * 티커별 사업계획서 URL 정보를 저장
     */
    private val tickerToBusinessPlan = mapOf<String, String>(
        // 실제 사업계획 링크 정보로 채워질 예정
        "WETALK" to "https://drive.google.com/file/d/1Is-QZqGok8oT6VEpvYkfreicbpX0Dnej/view?usp=sharing"
    )
    
    /**
     * 관련 영상 링크 정보
     * 티커별 관련 영상 URL 정보를 저장
     */
    private val tickerToVideo = mapOf<String, String>(
        // 실제 관련 영상 링크 정보로 채워질 예정
        "WETALK" to "https://youtu.be/XsFDoRpoYII?si=a2TnGPTR1q5LSgtk"
    )
    
    /**
     * 법인명 정보
     * 티커별 법인명 정보를 저장
     */
    private val tickerToCompanyName = mapOf(
        "CDM" to "한국생산자직거래본부",
        "IJECT" to "메디허브",
        "JWV" to "준원지비아이",
        "KCOT" to "코이코어",
        "MDM" to "에프티엔씨",
        "SMT" to "개인",
        "WETALK" to "개인",
        "AXNO" to "콘테츠웨어전략연구소",
        "KATV" to "GMI 그룹",
        "SLEEP" to "수면과건강",
        "MSK" to "개인"
    )
    
    /**
     * 대표자 정보
     * 티커별 대표자명 정보를 저장
     */
    private val tickerToCEO = mapOf(
        "CDM" to "신재희",
        "IJECT" to "염현철",
        "JWV" to "안경진",
        "KCOT" to "김범수",
        "MDM" to "황영오",
        "SMT" to "김주회",
        "WETALK" to "김병호",
        "AXNO" to "이한순",
        "KATV" to "이준암", // KATV 대표자명 수정 김준암 -> 이준암
        "SLEEP" to "황청풍",
        "MSK" to "윤성은"
    )
    
    /**
     * 티커에 해당하는 IP 링크를 반환
     */
    fun getIpLinkForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToIpLink.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커에 해당하는 홈페이지 URL을 반환
     */
    fun getHomepageForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToHomepage.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커에 해당하는 사업계획 URL을 반환
     */
    fun getBusinessPlanForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToBusinessPlan.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커에 해당하는 관련 영상 URL을 반환
     */
    fun getVideoForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToVideo.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커에 해당하는 법인명을 반환
     */
    fun getCompanyNameForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToCompanyName.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 사업자등록번호 정보
     * 티커별 사업자등록번호 정보를 저장
     */
    private val tickerToBusinessNumber = mapOf(
        "CDM" to "320-87-02053",
        "IJECT" to "856-81-00663",
        "JWV" to "616-86-00925",
        "KCOT" to "",
        "MDM" to "103-86-00910",
        "SMT" to "",
        "WETALK" to "",
        "AXNO" to "408-86-16942",
        "KATV" to "605-86-24761",
        "SLEEP" to "402-81-93252",
        "MSK" to ""
    )
    
    /**
     * 주소 정보
     * 티커별 주소 정보를 저장
     */
    private val tickerToAddress = mapOf(
        "CDM" to "강원특별자치도 원주시 미래로 37 ",
        "IJECT" to "경기도 군포시 엘에스로 175",
        "JWV" to "제주 제주시 신대로 124",
        "KCOT" to "",
        "MDM" to "경기도 성남시 분당구 황새울로200번길 ",
        "SMT" to "",
        "WETALK" to "",
        "AXNO" to "서울특별시 강남구 테헤란로 625, 1745호(삼성동, 덕명빌딩)",
        "KATV" to "부산 동구 중앙대로 270, 6층 681호",
        "SLEEP" to "서울시 송파구 중대로 150 백암빌딩 7층 ",
        "MSK" to ""
    )
    
    /**
     * 업종 정보
     * 티커별 업종 정보를 저장
     */
    private val tickerToBusinessType = mapOf(
        "CDM" to "",
        "IJECT" to "의료기기임대업",
        "JWV" to "수산물 가공업, 제조업",
        "KCOT" to "",
        "MDM" to "",
        "SMT" to "",
        "WETALK" to "",
        "AXNO" to "",
        "KATV" to "그 외 자동차용 신품 부품 제조업",
        "SLEEP" to "기타",
        "MSK" to ""
    )
    
    /**
     * 티커에 해당하는 대표자명을 반환
     */
    fun getCEOForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToCEO.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }
    
    /**
     * 티커에 해당하는 사업자등록번호를 반환
     */
    fun getBusinessNumberForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        val result = tickerToBusinessNumber.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: ""
        
        return if (result.isBlank()) defaultValue ?: DEFAULT_VALUE else result
    }
    
    /**
     * 티커에 해당하는 주소를 반환
     */
    fun getAddressForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        val result = tickerToAddress.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: ""
        
        return if (result.isBlank()) defaultValue ?: DEFAULT_VALUE else result
    }
    
    /**
     * 티커에 해당하는 업종을 반환
     */
    fun getBusinessTypeForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE
        
        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        val result = tickerToBusinessType.entries.firstOrNull { 
            it.key.uppercase() == upperBaseTicker 
        }?.value ?: ""
        
        return if (result.isBlank()) defaultValue ?: DEFAULT_VALUE else result
    }
    
    /**
     * 특정 티커의 모든 정보를 Map으로 반환
     */
    fun getAllInfoForTicker(ticker: String?): Map<String, String> {
        if (ticker.isNullOrBlank()) return mapOf()
        
        return mapOf(
            "ipLink" to getIpLinkForTicker(ticker),
            "homepage" to getHomepageForTicker(ticker),
            "businessPlan" to getBusinessPlanForTicker(ticker),
            "video" to getVideoForTicker(ticker),
            "companyName" to getCompanyNameForTicker(ticker),
            "ceo" to getCEOForTicker(ticker),
            "businessNumber" to getBusinessNumberForTicker(ticker),
            "address" to getAddressForTicker(ticker),
            "businessType" to getBusinessTypeForTicker(ticker)
        )
    }
}
