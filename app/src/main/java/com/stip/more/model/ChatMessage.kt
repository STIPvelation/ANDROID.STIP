package com.stip.stip.more.model

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val content: String = "",
    val time: String = "",
    val isCurrentUser: Boolean = false
)
