package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName
import com.stip.stip.iphome.model.IpListingItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID
import java.util.Locale

/**
 * Market Pairs API 응답 모델
 */
data class MarketPairsResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("countryImage")
    val countryImage: String? = null,
    
    @SerializedName("symbol")
    val symbol: String,
    
    @SerializedName("baseAsset")
    val baseAsset: String,
    
    @SerializedName("quoteAsset")
    val quoteAsset: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("categoryId")
    val categoryId: Int? = null,
    
    @SerializedName("categoryName")
    val categoryName: String? = null,
    
    @SerializedName("lastPrice")
    val lastPrice: BigDecimal? = null, // 현재가
    
    @SerializedName("priceChange")
    val priceChange: BigDecimal? = null, // 전일대비 금액
    
    @SerializedName("changeRate")
    val changeRate: Double? = null, // 등락률 (%)
    
    @SerializedName("volume")
    val volume: BigDecimal? = null, // 거래금액(일간)
    
    @SerializedName("highTicker")
    val highTicker: BigDecimal? = null, // 최고가 (일간)
    
    @SerializedName("lowTicker")
    val lowTicker: BigDecimal? = null // 최저가 (일간)
) {
    /**
     * MarketPairsResponse를 IpListingItem으로 변환
     */
    fun toIpListingItem(): IpListingItem {
        // 공백 제거 및 데이터 정리
        val cleanBaseAsset = baseAsset.trim()
        
        fun truncate2(value: Double?): String =
            if (value == null) "0.00" else String.format(Locale.US, "%.2f", value)
        fun truncate2Signed(value: Double?): String =
            if (value == null) "+0.00%" else (if (value >= 0) "+" else "") + String.format(Locale.US, "%.2f", value) + "%"

        return IpListingItem(
            ticker = cleanBaseAsset,
            symbol = symbol,
            name = name,
            countryImage = countryImage,
            currentPrice = truncate2(lastPrice?.toDouble()),
            changePercent = truncate2Signed(changeRate),
            changeAbsolute = truncate2(priceChange?.toDouble()),
            volume = truncate2(volume?.toDouble()),
            category = categoryName ?: "IP",
            companyName = cleanBaseAsset,
            high24h = String.format(Locale.US, "%.10f", highTicker?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            low24h = String.format(Locale.US, "%.10f", lowTicker?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            volume24h = String.format("%,.0f", volume?.toDouble() ?: 0.0),
            open = String.format(Locale.US, "%.10f", lastPrice?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            high = String.format(Locale.US, "%.10f", highTicker?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            low = String.format(Locale.US, "%.10f", lowTicker?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            close = String.format(Locale.US, "%.10f", lastPrice?.toDouble() ?: 0.0).trimEnd('0').trimEnd('.'),
            isTradeTriggered = false,
            isBuy = false,
            type = "",
            registrationNumber = id,
            firstIssuanceDate = createdAt.split("T")[0], // 날짜 부분만 추출
            totalIssuanceLimit = "1,000,000",
            linkBlock = null,
            linkRating = null,
            linkLicense = null,
            linkVideo = null,
            currentCirculation = "0",
            linkDigitalIpPlan = null,
            linkLicenseAgreement = null,
            digitalIpLink = null,
            businessPlanLink = null,
            relatedVideoLink = null,
            homepageLink = null,
            patentGrade = null,
            institutionalValues = null,
            stipValues = null,
            usagePlanData = null,
            representative = null,
            businessType = null,
            contactEmail = null,
            address = null,
            snsTwitter = null,
            snsInstagram = null,
            snsKakaoTalk = null,
            snsTelegram = null,
            snsLinkedIn = null,
            snsWeChat = null
        )
    }
} 