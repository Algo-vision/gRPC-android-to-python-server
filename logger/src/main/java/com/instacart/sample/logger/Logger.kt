package com.instacart.sample.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val MAX_LOGS = 100

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun debug(tag: String, message: String) {
        Log.d(tag, message)
        addLog("[DEBUG]", message, tag)
    }

    fun info(tag: String, message: String) {
        Log.i(tag, message)
        addLog("[INFO] ", message, tag)
    }

    fun warn(tag: String, message: String) {
        Log.w(tag, message)
        addLog("[WARN] ", message, tag)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        addLog("[ERROR]", message, tag)
    }

    private fun addLog(level: String, message: String, tag: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $level [$tag] $message"

        _logs.value = (listOf(logEntry) + _logs.value).take(MAX_LOGS)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
