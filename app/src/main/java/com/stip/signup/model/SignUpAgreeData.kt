package com.stip.stip.signup.model

data class SignUpAgreeData(
    var isCheck: Boolean,
    val title: String,
    val content: String?,
    val contentList: List<String>?
)
