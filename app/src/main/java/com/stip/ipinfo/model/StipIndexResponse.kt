package com.stip.stip.ipinfo.model

import com.google.gson.annotations.SerializedName

data class StipIndexResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<StipIndexData>
)

data class StipIndexData(
    @SerializedName("index_name") val indexName: String,
    @SerializedName("current_value") val currentValue: Float,
    @SerializedName("base_value") val baseValue: Float,
    @SerializedName("change_percent") val changePercent: String
)
