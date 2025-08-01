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
        "WETALK" to "https://play.google.com/store/apps/details?id=com.hustay.swing.ddd5b0f8cea664c07a746d7ef57cc7724&pcampaignid=web_share",
        "AXNO" to "https://play.google.com/store/apps/details?id=com.axno.safs&pcampaignid=web_share",
        "MDM" to "https://play.google.com/store/apps/details?id=com.modumom.mobileapp.selfnamebase&hl=ko",
        "IJECT" to "https://play.google.com/store/apps/details?id=omni.medi_app&hl=ko",
        "JWV" to "https://drive.google.com/file/d/195ZUd_iC6x9U6tTdfpbzgrHQA5M9oFgi/view?usp=sharing",
        "SMT" to "https://drive.google.com/file/d/1jXWlwGU0Mzfn89H8OZdxJlHWONjFeyYI/view?usp=sharing",
        "CDM" to "https://drive.google.com/file/d/11xRtIEk3Rx43VyKuhPYldrR2V4QTHLlI/view?usp=sharing",
        "KATV" to "https://drive.google.com/file/d/1DNJbCuL33U4lXgWh8Nkv9m4GGO-6LCVB/view?usp=sharing",
        "SLEEP" to "https://drive.google.com/file/d/10-oAZ4HdmQnKjLE1i6_l2BM1jk8wC6vQ/view?usp=sharing",
        "MSK" to "https://drive.google.com/file/d/1DE-xWvUTvrYg245HDyyeAmpXr2N9w-yp/view?usp=sharing"
    )

    /**
     * 홈페이지 링크 정보
     * 티커별 홈페이지 URL 정보를 저장
     */
    private val tickerToHomepage = mapOf(
        "IJECT" to "https://medihub.co.kr/",
        "CDM" to "https://www.kpdp.kr/",
        "WETALK" to "http://dooremall.co.kr/",
        "MDM" to "https://www.modumom.com/",
        "SLEEP" to "https://kokodoc.com/",
        "JWV" to "https://claude.ai/public/artifacts/0acf49fa-fd69-45ab-b236-9e68dec8901a",
        "KCOT" to "https://www.kcotech.com",
        "MSK" to "https://www.jwvision.com",
        "SMT" to "https://www.smtholdings.kr",
        "AXNO" to "https://cafe.naver.com/815foreurope",
        "KATV" to "http://gmigroup.co.kr/index.php"
    )

    /**
     * 사업계획 문서 링크 정보
     * 티커별 사업계획서 URL 정보를 저장
     */
    private val tickerToBusinessPlan = mapOf<String, String>(
        "WETALK" to "https://drive.google.com/file/d/1tsdU2Pc8eMl0cazPNgW-nWJwuCXknmNh/view?usp=sharing",
        "IJECT" to "https://drive.google.com/file/d/1hsZT7pHEckC6w0MM3jkNeRjybitdUuDt/view?usp=sharing",
        "MDM" to "https://drive.google.com/file/d/1WPfcTgrCGdftDrESh4p6_zllR9JDJrqj/view?usp=sharing",
        "KATV" to "https://drive.google.com/file/d/1ajFZTEZrv3zXyCK8NaWIXRUdJILUk2Ea/view?usp=sharing",
        "KCOT" to "https://drive.google.com/file/d/1dcyyt2rq6STBK57yPec-LLivf2_JmhPR/view?usp=sharing",
        "MSK" to "https://www.dropbox.com/scl/fi/xit1fsqnxtunzhpz93qeg/MSK-2507.pdf?rlkey=s3b0lfyv7nh26t4k7ywsae63x&st=3a95u0y0&dl=0",
        "JWV" to "hhttps://drive.google.com/file/d/1hsZT7pHEckC6w0MM3jkNeRjybitdUuDt/view?usp=sharing",
        "AXNO" to "https://docs.google.com/presentation/d/1aGBPC4htTaUGvHqGT3cuy2-QyUSVYwGE/edit?usp=sharing&ouid=114576531580819012864&rtpof=true&sd=true"
    )

    /**
     * 관련 영상 링크 정보
     * 티커별 관련 영상 URL 정보를 저장
     */
    private val tickerToVideo = mapOf<String, String>(
        "JWV" to "https://youtu.be/9nDpKAM-lBY",
        "KATV" to "https://youtu.be/fB7_rg1ueUU",
        "AXNO" to "https://youtu.be/lnSvkIx3LJI",
        "CDM" to "https://youtu.be/R0nOOqy8jr0",
        "MSK" to "https://youtu.be/DOgmEsAMTGc",
        "KCOT" to "https://youtu.be/RdcDhoGqGRY",
        "SMT" to "https://youtu.be/ju0A4AlXuwo",
        "MDM" to "https://youtu.be/2HBBWiiFPSY",
        "SLEEP" to "https://youtu.be/SzthMPIGYe8",
        "IJECT" to "https://youtu.be/IjnXr-XetcM",
        "WETALK" to "https://youtu.be/G_S33LrzdSs"
    )

    /**
     * 관련영상 링크 정보
     * 티커별 관련영상 YouTube URL 정보를 저장
     */
    private val tickerToRelatedVideo = mapOf<String, String>(
        "WETALK" to "https://youtu.be/XsFDoRpoYII?si=dWTqENkLd9FvuzAa",
        "JWV" to "https://youtu.be/zwVU-HmbbeA?si=fSJQKkpXSfdmK2pj",
        "MDM" to "https://youtu.be/pSinruGYXVc?si=JQm2yy8UwzAO4uTy",
        "CDM" to "https://drive.google.com/file/d/1NS5UYf-UniCBaz1eDChVaeykLN61E3xR/view?usp=sharing",
        "IJECT" to "https://youtu.be/03iXowx7I2U",
        "SLEEP" to "https://youtube.com/@idealbio",
        "KATV" to "https://youtu.be/yo1NMK1dkIw?si=3EuB5yal_pihKQTN",
        "KCOT" to "https://youtu.be/BhsVcZz7lpo?si=qlx4cvJxqqmJ8v_h",
        "MSK" to "https://youtu.be/nScxr_-hxIA?si=CE05woBOlgm7Yhwi",
        "AXNO" to "https://www.youtube.com/watch?v=KGbb_CDXQf8"
    )

    /**
     * 실시권 링크 정보
     * 티커별 실시권(License) 관련 Google Drive 문서 URL 정보를 저장
     */
    private val tickerToLicense = mapOf<String, String>(
        "AXNO" to "https://docs.google.com/document/d/10rzjXHKAEEN2QNfjmmBdA2APoCxR2BXv/view?usp=sharing",
        "MSK" to "https://docs.google.com/document/d/1n1ICgtb_2cHhWgesv3_GuiA3HsDGRDYZ/view?usp=sharing",
        "CDM" to "https://docs.google.com/document/d/1n2mfS8HZyMzTKaRS40-l-Loz29H8dXsW/view?usp=sharing",
        "IJECT" to "https://docs.google.com/document/d/11acX5-1tR3ktE3HU8uEu4oi2DEc9rCcW/view?usp=sharing",
        "KATV" to "https://docs.google.com/document/d/16CJZGbLnz7_FB5zWFkj3TP_PKkyR5dso/view?usp=sharing",
        "JWV" to "https://docs.google.com/document/d/1RMooP-Y_6Cn-CTGZQz7rGSZsjd--_Ob-/view?usp=sharing",
        "MDM" to "https://docs.google.com/document/d/1ZewIu7ie99GInWyWyg99bPZo8_vjvOqt/view?usp=sharing",
        "SMT" to "https://docs.google.com/document/d/1rkN24N250phQ38zWt2GSvfGvBP5IQYb-/view?usp=sharing",
        "WETALK" to "https://docs.google.com/document/d/1tsdU2Pc8eMl0cazPNgW-nWJwuCXknmNh/view?usp=sharing",
        "KCOT" to "https://docs.google.com/document/d/1dcyyt2rq6STBK57yPec-LLivf2_JmhPR/view?usp=sharing",
        "SLEEP" to "https://drive.google.com/file/d/1wOPBZlW43O6hpK9_eDU0yjmEO8QhpQE-/view?usp=sharing"
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
        "WETALK" to "두레",
        "AXNO" to "콘테츠웨어전략연구소",
        "KATV" to "GMI그룹",
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
        "KATV" to "이준암",
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
     * 티커에 해당하는 영상보기 URL을 반환
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
     * 티커에 해당하는 관련영상 URL을 반환
     */
    fun getRelatedVideoForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        if (ticker.isNullOrBlank()) return defaultValue ?: DEFAULT_VALUE

        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        return tickerToRelatedVideo.entries.firstOrNull {
            it.key.uppercase() == upperBaseTicker
        }?.value ?: defaultValue ?: DEFAULT_VALUE
    }

    /**
     * 티커에 해당하는 실시권 링크 URL을 반환
     */
    fun getLicenseForTicker(ticker: String?, defaultValue: String? = DEFAULT_VALUE): String {
        android.util.Log.d("IpDetailInfo", "=== getLicenseForTicker START ===")
        android.util.Log.d("IpDetailInfo", "Input ticker: $ticker")

        if (ticker.isNullOrBlank()) {
            android.util.Log.d("IpDetailInfo", "Ticker is null or blank, returning default: ${defaultValue ?: DEFAULT_VALUE}")
            return defaultValue ?: DEFAULT_VALUE
        }

        val baseTicker = ticker.split("/").firstOrNull() ?: ticker
        val upperBaseTicker = baseTicker.uppercase()
        android.util.Log.d("IpDetailInfo", "Base ticker: $baseTicker, Upper ticker: $upperBaseTicker")

        // 모든 키와 값 출력
        android.util.Log.d("IpDetailInfo", "Available license mappings:")
        tickerToLicense.forEach { (key, value) ->
            android.util.Log.d("IpDetailInfo", "  $key -> $value")
        }

        val result = tickerToLicense.entries.firstOrNull {
            it.key.uppercase() == upperBaseTicker
        }?.value

        android.util.Log.d("IpDetailInfo", "Found license URL for $upperBaseTicker: $result")

        // 사업계획 URL과 비교
        val businessPlanUrl = getBusinessPlanForTicker(ticker)
        if (result == businessPlanUrl) {
            android.util.Log.w("IpDetailInfo", "WARNING: License URL matches Business Plan URL!")
        }

        val finalResult = result ?: defaultValue ?: DEFAULT_VALUE
        android.util.Log.d("IpDetailInfo", "Final license URL: $finalResult")
        android.util.Log.d("IpDetailInfo", "=== getLicenseForTicker END ===")

        return finalResult
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
            "license" to getLicenseForTicker(ticker),
            "companyName" to getCompanyNameForTicker(ticker),
            "ceo" to getCEOForTicker(ticker),
            "businessNumber" to getBusinessNumberForTicker(ticker),
            "address" to getAddressForTicker(ticker),
            "businessType" to getBusinessTypeForTicker(ticker)
        )
    }
}
