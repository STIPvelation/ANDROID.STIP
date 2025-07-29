package com.stip.stip.ipinfo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewsItem(
    val title: String,
    val date: String,
    val link: String,
    val pubDate: String,
    val imageUrl: String? = null
) : Parcelable