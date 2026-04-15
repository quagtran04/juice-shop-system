package com.example.juiceshop.model

data class ChatMessage(
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: String = "",
    val isTyping: Boolean = false  // true = hiện typing indicator
)