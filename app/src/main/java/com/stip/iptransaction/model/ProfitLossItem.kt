package com.stip.stip.iptransaction.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 손익 데이터 모델 클래스
 */
data class ProfitLossItem(
    @SerializedName("date") val date: String,
    @SerializedName("dailyProfit") val dailyProfit: Double,
    @SerializedName("cumulativeProfit") val cumulativeProfit: Double,
    @SerializedName("dailyRate") val dailyRate: Double,
    @SerializedName("cumulativeRate") val cumulativeRate: Double,
    @SerializedName("endingAsset") val endingAsset: Double,
    @SerializedName("startingAsset") val startingAsset: Double,
    @SerializedName("deposit") val deposit: Double,
    @SerializedName("withdrawal") val withdrawal: Double
) {
    // 백엔드에서는 timestamp 형식으로 전달될 수 있으므로 파싱 로직 추가
    @Transient // JSON 직렬화에서 제외
    var dateRaw: Date = java.text.SimpleDateFormat("yyyy-MM-dd").parse(date) ?: Date()
}

/**
 * 손익 요약 데이터 모델 클래스
 */
data class ProfitLossSummary(
    @SerializedName("period") val period: String,         // 기간 (예: "2023-01-01 ~ 2023-01-31")
    @SerializedName("totalProfit") val totalProfit: Double,   // 해당 기간 총 손익
    @SerializedName("profitRate") val profitRate: Double,     // 수익률
    @SerializedName("averageAsset") val averageAsset: Double  // 평균 자산
)

/**
 * 손익 차트 데이터 모델 클래스
 */
data class ProfitLossChartData(
    @SerializedName("dateLabels") val dateLabels: List<String>,     // X축 날짜 라벨
    @SerializedName("dailyValues") val dailyValues: List<Double>,   // 일별 손익 값
    @SerializedName("cumulativeValues") val cumulativeValues: List<Double>  // 누적 손익 값
)
