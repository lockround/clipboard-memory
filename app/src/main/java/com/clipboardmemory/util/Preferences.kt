package com.clipboardmemory.util

import android.content.Context

object Preferences {
    private const val PREFS_NAME = "clipboard_memory_prefs"
    private const val KEY_GROQ_API_KEY = "groq_api_key"
    private const val KEY_GROQ_MODEL = "groq_model"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    const val KEY_SERVICE_RUNNING = "service_running"

    const val DEFAULT_GROQ_MODEL = "qwen/qwen3.8-27b"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_GROQ_API_KEY, "").orEmpty()

    fun setApiKey(context: Context, apiKey: String) {
        prefs(context).edit().putString(KEY_GROQ_API_KEY, apiKey).apply()
    }

    fun getModel(context: Context): String =
        prefs(context).getString(KEY_GROQ_MODEL, DEFAULT_GROQ_MODEL)
            ?: DEFAULT_GROQ_MODEL

    fun setModel(context: Context, model: String) {
        prefs(context).edit().putString(KEY_GROQ_MODEL, model).apply()
    }

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_ENABLED, false)

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun isServiceRunning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_RUNNING, false)

    fun setServiceRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_RUNNING, running).apply()
    }
}