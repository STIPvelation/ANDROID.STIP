package com.stip.stip.api.model

import com.stip.stip.iphome.model.IpListingItem
import com.google.gson.annotations.SerializedName

/**
 * IP 리스팅 API 응답 데이터 모델
 */
data class IpListingResponse(
    @SerializedName("items")
    val items: List<IpListingItemDto>
)

/**
 * IP 리스팅 아이템 DTO (Data Transfer Object)
 * API 응답으로 받은 데이터를 앱 내부 모델로 변환하기 위한 객체
 */
data class IpListingItemDto(
    @SerializedName("ticker")
    val ticker: String,
    
    @SerializedName("current_price")
    val currentPrice: String,
    
    @SerializedName("change_percent")
    val changePercent: String,
    
    @SerializedName("change_absolute")
    val changeAbsolute: String,
    
    @SerializedName("volume")
    val volume: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("company_name")
    val companyName: String,
    
    @SerializedName("high_24h")
    val high24h: String,
    
    @SerializedName("low_24h")
    val low24h: String,
    
    @SerializedName("volume_24h")
    val volume24h: String,
    
    @SerializedName("open")
    val open: String,
    
    @SerializedName("high")
    val high: String,
    
    @SerializedName("low")
    val low: String,
    
    @SerializedName("close")
    val close: String,
    
    @SerializedName("registration_number")
    val registrationNumber: String,
    
    @SerializedName("first_issuance_date")
    val firstIssuanceDate: String,
    
    @SerializedName("total_issuance_limit")
    val totalIssuanceLimit: String,
    
    @SerializedName("link_block")
    val linkBlock: String?,
    
    @SerializedName("link_rating")
    val linkRating: String?,
    
    @SerializedName("link_license")
    val linkLicense: String?,
    
    @SerializedName("link_video")
    val linkVideo: String?,
    
    @SerializedName("current_circulation")
    val currentCirculation: String,
    
    @SerializedName("link_digital_ip_plan")
    val linkDigitalIpPlan: String?,
    
    @SerializedName("link_license_agreement")
    val linkLicenseAgreement: String?,
    
    @SerializedName("digital_ip_link")
    val digitalIpLink: String?,
    
    @SerializedName("business_plan_link")
    val businessPlanLink: String?,
    
    @SerializedName("related_video_link")
    val relatedVideoLink: String?,
    
    @SerializedName("homepage_link")
    val homepageLink: String?,
    
    @SerializedName("patent_grade")
    val patentGrade: String?,
    
    @SerializedName("institutional_values")
    val institutionalValues: List<Float>?,
    
    @SerializedName("stip_values")
    val stipValues: List<Float>?,
    
    @SerializedName("usage_plan_data")
    val usagePlanData: HashMap<String, Float>?,
    
    @SerializedName("representative")
    val representative: String?,
    
    @SerializedName("business_type")
    val businessType: String?,
    
    @SerializedName("contact_email")
    val contactEmail: String?,
    
    @SerializedName("address")
    val address: String?,
    
    @SerializedName("sns_twitter")
    val snsTwitter: String?,
    
    @SerializedName("sns_instagram")
    val snsInstagram: String?,
    
    @SerializedName("sns_kakao_talk")
    val snsKakaoTalk: String?,
    
    @SerializedName("sns_telegram")
    val snsTelegram: String?,
    
    @SerializedName("sns_linked_in")
    val snsLinkedIn: String?,
    
    @SerializedName("sns_we_chat")
    val snsWeChat: String?
) {
    /**
     * DTO 객체를 앱 내부 모델로 변환
     */
    fun toIpListingItem(): IpListingItem {
        return IpListingItem(
            ticker = ticker,
            currentPrice = currentPrice,
            changePercent = changePercent,
            changeAbsolute = changeAbsolute,
            volume = volume,
            category = category,
            companyName = companyName,
            high24h = high24h,
            low24h = low24h,
            volume24h = volume24h,
            open = open,
            high = high,
            low = low,
            close = close,
            registrationNumber = registrationNumber,
            firstIssuanceDate = firstIssuanceDate,
            totalIssuanceLimit = totalIssuanceLimit,
            linkBlock = linkBlock,
            linkRating = linkRating,
            linkLicense = linkLicense,
            linkVideo = linkVideo,
            currentCirculation = currentCirculation,
            linkDigitalIpPlan = linkDigitalIpPlan,
            linkLicenseAgreement = linkLicenseAgreement,
            digitalIpLink = digitalIpLink,
            businessPlanLink = businessPlanLink,
            relatedVideoLink = relatedVideoLink,
            homepageLink = homepageLink,
            patentGrade = patentGrade,
            institutionalValues = institutionalValues,
            stipValues = stipValues,
            usagePlanData = usagePlanData,
            representative = representative,
            businessType = businessType,
            contactEmail = contactEmail,
            address = address,
            snsTwitter = snsTwitter,
            snsInstagram = snsInstagram,
            snsKakaoTalk = snsKakaoTalk,
            snsTelegram = snsTelegram,
            snsLinkedIn = snsLinkedIn,
            snsWeChat = snsWeChat
        )
    }
}
