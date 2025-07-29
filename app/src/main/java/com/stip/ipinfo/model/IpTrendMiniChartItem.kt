package com.stip.stip.ipinfo.model

import com.github.mikephil.charting.data.Entry

data class IpTrendMiniChartItem(
    val stipLabel: String,
    val stipPrice: Float,
    val stipChange: Float,
    val stipPercent: Float,
    val stipEntries: List<Entry>,
    val etipLabel: String,
    val etipPrice: Float,
    val etipChange: Float,
    val etipPercent: Float,
    val etipEntries: List<Entry>
)