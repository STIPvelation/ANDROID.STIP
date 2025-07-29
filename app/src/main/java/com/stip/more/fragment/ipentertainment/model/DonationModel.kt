package com.stip.stip.more.fragment.ipentertainment.model

import java.util.Date

data class DonationModel(
    val id: String,
    val title: String,
    val organizer: String,
    val description: String,
    val imageUrl: String,
    val currentAmount: Long,
    val goalAmount: Long,
    val endDate: Date,
    val isUrgent: Boolean = false,
    val category: String = "전체"
) {
    fun getProgress(): Int {
        return ((currentAmount.toDouble() / goalAmount.toDouble()) * 100).toInt()
    }
}
