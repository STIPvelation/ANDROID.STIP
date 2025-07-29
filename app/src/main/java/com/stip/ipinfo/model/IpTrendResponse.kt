package com.stip.stip.ipinfo.model

import com.google.gson.annotations.SerializedName

data class IpTrendResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<IpTrendData>
)

data class IpTrendData(
    @SerializedName("ticker") val ticker: String,
    @SerializedName("name") val name: String,
    @SerializedName("current_price") val currentPrice: Float,
    @SerializedName("change_percent") val changePercent: String,
    @SerializedName("change_value") val changeValue: Float
)
