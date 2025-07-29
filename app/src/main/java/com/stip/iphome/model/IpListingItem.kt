package com.stip.stip.iphome.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IpListingItem(
    val logoResId: Int? = null,               // DIP 로고 (nullable)
    val ticker: String,                       // DIP 티커명 (예: AXNO)
    val symbol: String = "",                  // 심볼 (예: IJECT/USD)
    val name: String = "",                    // 회사명 (예: MediHub)
    val countryImage: String? = null,         // 국가 이미지 URL (nullable)
    val currentPrice: String,                 // 현재가
    val changePercent: String,                // 전일 대비 퍼센트
    val changeAbsolute: String,               // 전일 대비 절대값
    val volume: String,
    val category: String = "Patent",
    val companyName: String,
    val high24h: String = "",
    val low24h: String = "",
    val volume24h: String = "",
    val open: String = "",
    val high: String = "",
    val low: String = "",
    val close: String = "",

    val isTradeTriggered: Boolean = false,
    val isBuy: Boolean = false,
    val type: String = "",

    // 🔽 상세 정보 필드
    val registrationNumber: String,
    val firstIssuanceDate: String,
    val totalIssuanceLimit: String,
    val linkBlock: String?,
    val linkRating: String?,
    val linkLicense: String?,
    val linkVideo: String?,
    val currentCirculation: String,
    val linkDigitalIpPlan: String?,
    val linkLicenseAgreement: String?,
    val digitalIpLink: String?,
    val businessPlanLink: String?,
    val relatedVideoLink: String?,
    val homepageLink: String?,

    // --- 레이더 차트 데이터 필드 추가 ---
    val patentGrade: String? = null, // 예: "A", "B", "C" 등 등급 (nullable)
    // 각 축(권리, 활용, 규제, 거래, 역량, 기술 순서)에 대한 점수 리스트 (nullable)
    val institutionalValues: List<Float>? = null, // 기관 가치 평가 점수 (6개)
    val stipValues: List<Float>? = null, // STIP 가치 평가 점수 (6개)
    // --- 레이더 차트 데이터 필드 끝 ---
    val usagePlanData: HashMap<String, Float>? = null,

    // 🔽 SNS/추가 정보 (nullable)
    val representative: String? = null,
    val businessType: String? = null,
    val contactEmail: String? = null,
    val address: String? = null,
    val snsTwitter: String? = null,
    val snsInstagram: String? = null,
    val snsKakaoTalk: String? = null,
    val snsTelegram: String? = null,
    val snsLinkedIn: String? = null,
    val snsWeChat: String? = null


) : Parcelable {
    fun isBuyType(): Boolean = type.trim() == "매수"
    fun isSellType(): Boolean = type.trim() == "매도"
}