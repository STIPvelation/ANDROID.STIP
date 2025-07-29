package com.stip.stip.more.fragment.ipentertainment.model

data class WebtoonModel(
    val id: String,
    val title: String,
    val author: String,
    val rating: Float,
    val imageUrl: String,
    val isNew: Boolean = false,
    val isUpdated: Boolean = false,
    val isCompleted: Boolean = false,
    val category: String = "전체"
)
