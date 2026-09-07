package com.clipboardmemory.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardmemory.ClipboardApp
import com.clipboardmemory.network.ChatMessage
import com.clipboardmemory.network.GroqApiException
import com.clipboardmemory.network.GroqApiService
import com.clipboardmemory.network.GroqRepository
import com.clipboardmemory.util.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ClipboardApp
    private val repository = GroqRepository(GroqApiService.create(), app.database)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasApiKey = MutableStateFlow(Preferences.getApiKey(app).isNotBlank())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    init {
        if (Preferences.getApiKey(app).isNotBlank()) {
            appendMessage(ChatMessage("assistant", welcomeMessage()))
        }
    }

    private fun welcomeMessage() =
        "Hi! I can answer questions about everything you've copied. " +
            "Try asking: \"What did I copy at 3pm today?\" or \"Did I copy any emails?\""

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun setApiKey(key: String) {
        Preferences.setApiKey(app, key.trim())
        _hasApiKey.value = key.isNotBlank()
    }

    fun getApiKey(): String = Preferences.getApiKey(app)

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isSending.value) return

        val apiKey = Preferences.getApiKey(app)
        if (apiKey.isBlank()) {
            _error.value = "Please enter your Groq API key first."
            return
        }

        _inputText.value = ""
        _error.value = null

        val userMessage = ChatMessage("user", text)
        _messages.value = (_messages.value + userMessage)
        _isSending.value = true

        viewModelScope.launch {
            try {
                val context = repository.getClipboardContext(limit = 50)
                val systemPrompt = buildSystemPrompt(context)
                val history = _messages.value.takeLast(10)
                val fullMessages = listOf(
                    ChatMessage("system", systemPrompt)
                ) + history

                val reply = repository.chat(
                    apiKey = apiKey,
                    model = Preferences.getModel(app),
                    messages = fullMessages
                )
                appendMessage(ChatMessage("assistant", reply))
            } catch (e: GroqApiException) {
                _error.value = e.message
            } catch (e: Exception) {
                Log.e(TAG, "Groq call failed", e)
                _error.value = "Could not reach Groq: ${e.message ?: "network error"}"
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun buildSystemPrompt(context: String): String {
        val base = "You are the Clipboard Memory assistant. The user captures their " +
            "clipboard continuously and wants to recall \"what did I copy and when\".\n\n"
        val contextBlock = if (context.isBlank()) {
            "There is no clipboard history recorded yet."
        } else {
            context
        }
        return base +
            "Use the clipboard history below to answer. Cite the date/time when you mention " +
            "an item, and quote the copied text. If the user's question can't be answered from " +
            "the history, say so honestly.\n\n$contextBlock"
    }

    private fun appendMessage(message: ChatMessage) {
        _messages.value = (_messages.value + message).takeLast(MAX_MESSAGES)
    }

    fun clearChat() {
        _messages.value = emptyList()
        appendMessage(ChatMessage("assistant", welcomeMessage()))
    }

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_MESSAGES = 30
    }
}