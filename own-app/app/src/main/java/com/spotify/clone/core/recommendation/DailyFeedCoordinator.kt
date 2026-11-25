package com.spotify.clone.core.recommendation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Coordinates ECHO's automatic daily home refresh across calendar-day rollovers,
 * foreground resumes, and midnight timers.
 */
object DailyFeedCoordinator {

    private const val PREFS_NAME = "echo_daily_feed_prefs"
    private const val KEY_LAST_DATE = "last_daily_refresh_date"
    private const val KEY_LAST_TS = "last_daily_refresh_ts"

    private var prefs: SharedPreferences? = null
    @Volatile private var inMemoryLastDate: String? = null
    @Volatile private var inMemoryLastTs: Long = 0L

    private val _currentDateKey = MutableStateFlow(formatDateKey(System.currentTimeMillis()))
    val currentDateKeyFlow: StateFlow<String> = _currentDateKey.asStateFlow()

    private val _lastUpdatedTimestampMs = MutableStateFlow(0L)
    val lastUpdatedTimestampMs: StateFlow<Long> = _lastUpdatedTimestampMs.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            inMemoryLastDate = p.getString(KEY_LAST_DATE, null)
            inMemoryLastTs = p.getLong(KEY_LAST_TS, 0L)
            _lastUpdatedTimestampMs.value = inMemoryLastTs
            _currentDateKey.value = formatDateKey(System.currentTimeMillis())
        }
    }

    fun formatDateKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun friendlyDateLabel(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(nowMs))
    }

    fun dailySeed(dateKey: String = formatDateKey()): Long {
        var h = 1125899906842597L
        for (ch in dateKey) {
            h = 31L * h + ch.code.toLong()
        }
        return h
    }

    fun needsDailyRefresh(nowMs: Long = System.currentTimeMillis()): Boolean {
        val today = formatDateKey(nowMs)
        val lastDate = prefs?.getString(KEY_LAST_DATE, inMemoryLastDate) ?: inMemoryLastDate
        if (lastDate != today) return true
        val lastTs = prefs?.getLong(KEY_LAST_TS, inMemoryLastTs) ?: inMemoryLastTs
        return lastTs <= 0L || (nowMs - lastTs) >= 12L * 60L * 60L * 1000L
    }

    fun checkDayRollover(nowMs: Long = System.currentTimeMillis()): Boolean {
        val today = formatDateKey(nowMs)
        val changed = _currentDateKey.value != today
        if (changed) {
            _currentDateKey.value = today
        }
        return changed || needsDailyRefresh(nowMs)
    }

    fun markDailyRefreshed(nowMs: Long = System.currentTimeMillis()) {
        val today = formatDateKey(nowMs)
        inMemoryLastDate = today
        inMemoryLastTs = nowMs
        _currentDateKey.value = today
        _lastUpdatedTimestampMs.value = nowMs
        prefs?.edit()
            ?.putString(KEY_LAST_DATE, today)
            ?.putLong(KEY_LAST_TS, nowMs)
            ?.apply()
    }

    fun msUntilNextMidnight(nowMs: Long = System.currentTimeMillis()): Long {
        val nextMidnight = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }
        return (nextMidnight.timeInMillis - nowMs).coerceAtLeast(60_000L)
    }

    fun resetForTests() {
        prefs?.edit()?.clear()?.apply()
        inMemoryLastDate = null
        inMemoryLastTs = 0L
        _lastUpdatedTimestampMs.value = 0L
        _currentDateKey.value = formatDateKey(System.currentTimeMillis())
    }
}
