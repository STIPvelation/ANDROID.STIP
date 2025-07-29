package com.stip.stip.more.fragment.ipentertainment.model

import android.graphics.Color
import java.util.Date

enum class IpType(val displayName: String) {
    COPYRIGHT("저작권"),
    MUSIC("음악"),
    MOVIE("영화"),
    PATENT("특허"),
    TRADEMARK("상표"),
    DESIGN("디자인"),
    CHARACTER("케릭터"),
    FRANCHISE("프랜차이즈"),
    OTHER("기타");
    
    fun getColor(): Int {
        return when(this) {
            PATENT -> Color.parseColor("#2574A9")         // 파랑색 (37, 116, 169)
            TRADEMARK -> Color.parseColor("#28A745")     // 초록색 (40, 167, 69)
            COPYRIGHT, MUSIC, MOVIE -> Color.parseColor("#7952B3")  // 보라색 (121, 82, 179)
            DESIGN -> Color.parseColor("#E15361")        // 분홍색 (225, 83, 97)
            CHARACTER -> Color.parseColor("#F0AD4E")     // 주황색 (240, 173, 78)
            FRANCHISE -> Color.parseColor("#5BC0BE")     // 청록색 (91, 192, 190)
            OTHER -> Color.parseColor("#6C757D")         // 회색 (108, 117, 125)
        }
    }
}

data class AuctionModel(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val currentPrice: Long,
    val endTime: Date,
    val isFeatured: Boolean = false,
    val category: String = "전체",
    val ipType: IpType = IpType.OTHER,
    val registrationNumber: String = "N/A",
    val bidCount: Int = 0
)
