package com.clipboardmemory.network

import com.clipboardmemory.data.ClipboardDatabase
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroqRepository(
    private val api: GroqApiService,
    private val database: ClipboardDatabase
) {
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

    suspend fun getClipboardContext(limit: Int = 50): String {
        val entries = database.clipboardDao().getRecent(limit)
        if (entries.isEmpty()) return ""

        val sb = StringBuilder("Your Clipboard History (most recent first):\n")
        entries.forEachIndexed { index, entry ->
            val whenText = dateFormat.format(Date(entry.timestamp))
            sb.append("${entries.size - index}. [Copied on $whenText from ${entry.appPackage}]\n")
            sb.append(entry.content.take(1000))
            sb.append("\n")
        }
        return sb.toString()
    }

    suspend fun chat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
    ): String {
        val response = api.chatCompletion(
            authorization = "Bearer $apiKey",
            request = ChatRequest(model = model, messages = messages)
        )

        if (!response.isSuccessful) {
            val serverMessage = response.errorBody()?.string()?.let { parseGroqError(it) }
            throw GroqApiException(
                serverMessage ?: "HTTP ${response.code()}"
            )
        }

        val body = response.body()
        body?.error?.let { throw GroqApiException(it.message ?: "Unknown API error") }

        val content = body?.choices?.firstOrNull()?.message?.content
        return content?.takeIf { it.isNotBlank() }
            ?: throw GroqApiException("Empty response from Groq (model returned no content)")
    }

    private fun parseGroqError(raw: String): String? = try {
        val obj = JsonParser.parseString(raw).asJsonObject
        obj.getAsJsonObject("error")?.get("message")?.asString?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

class GroqApiException(message: String) : Exception(message)