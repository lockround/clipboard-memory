package com.clipboardmemory.network

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class GroqResponse(
    val choices: List<Choice>,
    val error: GroqError? = null
)

data class Choice(
    val message: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class GroqError(
    val message: String? = null,
    val type: String? = null
)