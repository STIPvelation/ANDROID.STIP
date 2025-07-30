package com.stip.stip.iphome.constants

import kotlin.to

/**
 * 실시권 범위 정보를 관리하는 데이터 클래스 및 매핑 객체
 * iOS-STIP 프로젝트의 LicenseScopeView.swift에서 추출한 데이터
 */
data class LicenseScope(
    val percentage: String,
    val usageArea: String
)

object LicenseScopeInfo {
    const val DEFAULT_VALUE = "정보 없음"
    
    // 티커별 실시권 범위 매핑
    private val tickerToLicenseScopes = mapOf(
        "AXNO" to listOf(
            LicenseScope("20%", "전 세계에서 스포츠 토토 예측 서비스 운영"),
            LicenseScope("10%", "특정 국가에서 스포츠 토토 예측 서비스 운영"),
            LicenseScope("5%", "로또 월드컵, SAFS(스포츠픽), LAFS(퍼스널로또) 참여 가능"),
            LicenseScope("1%", "스포츠 토토 예측 및 로또 월드컵 홍보 가능")
        ),
        "JWV" to listOf(
            LicenseScope("10%", "대한민국 내에서 민간용 및 정부용(공공기관 포함)을 모두 생산 판매"),
            LicenseScope("8%", "대한민국내에서 제약용으로만 판매 가능"),
            LicenseScope("5%", "대한민국내에서 일반 산업용으로만 판매 가능"),
            LicenseScope("3%", "연구 목적의 활용이며 상업적 판매 불가")
        ),
        "IJECT" to listOf(
            LicenseScope("8%", "국내 전역 병의원 아이젝 제품 판매 권리 부여"),
            LicenseScope("5%", "서울/경기/인천 병의원"),
            LicenseScope("3%", "대전/충청 병의원"),
            LicenseScope("1%", "부산/경남 병의원")
        ),
        "SMT" to listOf(
            LicenseScope("7%", "중국 전역 전국 제조 및 유통 가능"),
            LicenseScope("5%", "중국 전국 확장 기반. 전략적 파트너 대상"),
            LicenseScope("3%", "화동, 화남 등 전략권역 내 확대 운용"),
            LicenseScope("1%", "기술 검증 또는 로컬 유통 업체용 실시권")
        ),
        "WETALK" to listOf(
            LicenseScope("10%", "운영 지점수 : 최대 30 곳 운영 가능"),
            LicenseScope("5%", "최대 15 곳 운영 가능"),
            LicenseScope("3%", "최대 9 곳 운영 가능"),
            LicenseScope("1%", "최대 3 곳 운영 가능")
        ),
        "KCOT" to listOf(
            LicenseScope("5%", "운영 지점수 : 최대 30 곳 운영 가능"),
            LicenseScope("3%", "부분 상업적 사용"),
            LicenseScope("2%", "특정 용도에 한정하여 제품개발"),
            LicenseScope("1%", "연구·개발(R&D) 목적 또는 내부 테스트용")
        ),
        "MDM" to listOf(
            LicenseScope("15%", "모든 국내 상업적 용도 허용"),
            LicenseScope("10%", "특정 업종 한정 사용 (업종 특화 서비스)"),
            LicenseScope("5%", "스타트업 및 중소기업 대상 연구·개발"),
            LicenseScope("1%", "교육 및 비영리 연구 전용")
        ),
        "MSK" to listOf(
            LicenseScope("15%", "모판매처 : 민간용 및 정부용(공공기관 포함) 등 모두"),
            LicenseScope("13%", "공공기관과 정부 대상 판매 제외 국제출원 활용에서 권리자의 추가 허용 필요"),
            LicenseScope("10%", "제약에 제품형태 및 용도구분 제한 추가"),
            LicenseScope("5%", "상업적 판매 불가")
        ),
        "KATV" to listOf(
            LicenseScope("15%", "국내 전 지역에서 자유롭게 수륙양용카트 기반 사업 운영"),
            LicenseScope("10%", "해당 지역 내에서 카트 시스템 구축 및 운영"),
            LicenseScope("5%", "카트 서비스 기능 활용"),
            LicenseScope("1%", "수륙양용카트 기술을 활용한 데이터 분석")
        ),
        "CDM" to listOf(
            LicenseScope("7%", "특정 기업 및 개인이 해당 기술 및 상업적으로 사용"),
            LicenseScope("5%", "해당 기술을 대량 생산하여 국내 판매 가능"),
            LicenseScope("3%", "한국 시장 내에서 일반 소비자 대상으로 제품 판매")
        ),
        "SLEEP" to listOf(
            LicenseScope("15%", "모판매처 : 민간용 및 정부용(공공기관 포함) 등 모두"),
            LicenseScope("13%", "공공기관과 정부 대상 판매 제외 국제출원 활용에서 권리자의 추가 허용 필요"),
            LicenseScope("10%", "제약에 제품형태 및 용도구분 제한 추가"),
            LicenseScope("5%", "상업적 판매 불가")
        )
    )
    
    // 기본 실시권 범위 (매핑에 없는 티커용)
    private val defaultLicenseScopes = listOf(
        LicenseScope("10%", "본 IP에 관한 기술을 상업적인 모든곳에서 활용가능"),
        LicenseScope("5%", "본 IP에 관한 기술을 제한적 상업적 활용가능"),
        LicenseScope("3%", "본 IP에 관한 기술을 연구목적으로만 활용가능"),
        LicenseScope("1%", "대학교 기관 및 연구시설에서 연구 목적으로 사용 가능")
    )
    
    /**
     * 특정 티커의 실시권 범위 목록을 반환
     * @param ticker 티커 심볼
     * @return 해당 티커의 실시권 범위 목록
     */
    fun getLicenseScopesForTicker(ticker: String): List<LicenseScope> {
        return tickerToLicenseScopes[ticker] ?: defaultLicenseScopes
    }
    
    /**
     * 특정 티커가 실시권 범위 매핑에 있는지 확인
     * @param ticker 티커 심볼
     * @return 매핑 존재 여부
     */
    fun hasLicenseScopesForTicker(ticker: String): Boolean {
        return tickerToLicenseScopes.containsKey(ticker)
    }
    
    /**
     * 모든 지원되는 티커 목록 반환
     * @return 지원되는 티커 목록
     */
    fun getSupportedTickers(): Set<String> {
        return tickerToLicenseScopes.keys
    }
}
