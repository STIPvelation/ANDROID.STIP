package com.stip.stip.ipinfo.model

import com.google.gson.annotations.SerializedName

data class FxRateResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<FxRateData>
)

data class FxRateData(
    @SerializedName("country") val country: String,
    @SerializedName("code") val code: String,
    @SerializedName("rate") val rate: Float,
    @SerializedName("change") val change: Float,
    @SerializedName("percent") val percent: Float
)
