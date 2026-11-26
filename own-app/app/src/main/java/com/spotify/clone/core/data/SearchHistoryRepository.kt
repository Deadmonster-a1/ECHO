package com.spotify.clone.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Persistent Search History Repository for ECHO.
 * Directly adopted from LastWave-Native SearchHistoryRepository.
 * Manages deduplicated recent queries with reactive StateFlow and SharedPreferences persistence.
 */
class SearchHistoryRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<String>>(loadHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    @Synchronized
    fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _history.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(MAX_HISTORY_ITEMS)
        saveHistory(capped)
        _history.value = capped
    }

    @Synchronized
    fun remove(query: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        saveHistory(current)
        _history.value = current
    }

    @Synchronized
    fun clear() {
        saveHistory(emptyList())
        _history.value = emptyList()
    }

    private fun loadHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "echo_search_history_prefs"
        private const val KEY_HISTORY = "recent_queries"
        private const val MAX_HISTORY_ITEMS = 20

        @Volatile
        private var INSTANCE: SearchHistoryRepository? = null

        fun getInstance(context: Context): SearchHistoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SearchHistoryRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
