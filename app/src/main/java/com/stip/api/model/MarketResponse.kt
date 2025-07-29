package com.stip.stip.api.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Market API 응답 모델
 */
data class MarketResponse(
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
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("categoryId")
    val categoryId: Int? = null,
    
    @SerializedName("categoryName")
    val categoryName: String? = null,
    
    @SerializedName("lastPrice")
    val lastPrice: BigDecimal? = null,
    
    @SerializedName("priceChange")
    val priceChange: BigDecimal? = null,
    
    @SerializedName("changeRate")
    val changeRate: Double? = null, // 등락률 (%)
    
    @SerializedName("volume")
    val volume: BigDecimal? = null,
    
    @SerializedName("highTicker")
    val highTicker: BigDecimal? = null,
    
    @SerializedName("lowTicker")
    val lowTicker: BigDecimal? = null,

    @SerializedName("maxValue")
    val maxValue: Double = 0.0,
    
    @SerializedName("fee")
    val fee: Double = 0.0,
    
    @SerializedName("contractAddress")
    val contractAddress: String = "",
    
    @SerializedName("type")
    val type: String = "",
    
    @SerializedName("category")
    val category: MarketCategory? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String = "",
    
    @SerializedName("deletedAt")
    val deletedAt: String? = null
)

/**
 * 마켓 카테고리 정보
 */
data class MarketCategory(
    @SerializedName("categoryId")
    val categoryId: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: String?,
    
    @SerializedName("deletedAt")
    val deletedAt: String?
) 