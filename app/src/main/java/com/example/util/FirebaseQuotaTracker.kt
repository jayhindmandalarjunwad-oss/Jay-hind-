package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirebaseQuotaUsage(
    val date: String,
    val readsToday: Long,
    val writesToday: Long,
    val deletesToday: Long,
    val maxFreeDailyReads: Long = 50_000L,
    val maxFreeDailyWrites: Long = 20_000L,
    val maxFreeDailyDeletes: Long = 20_000L
) {
    val readPercentage: Float get() = (readsToday.toFloat() / maxFreeDailyReads).coerceIn(0f, 1f)
    val writePercentage: Float get() = (writesToday.toFloat() / maxFreeDailyWrites).coerceIn(0f, 1f)
    val deletePercentage: Float get() = (deletesToday.toFloat() / maxFreeDailyDeletes).coerceIn(0f, 1f)
}

data class ListenerStatus(
    val name: String,
    val collectionName: String,
    val isActive: Boolean,
    val lastSyncTime: Long = System.currentTimeMillis()
)

object FirebaseQuotaTracker {
    private const val PREFS_NAME = "firebase_quota_tracker_prefs"
    private const val KEY_TRACKED_DATE = "tracked_date"
    private const val KEY_READS_TODAY = "reads_today"
    private const val KEY_WRITES_TODAY = "writes_today"
    private const val KEY_DELETES_TODAY = "deletes_today"

    private var prefs: SharedPreferences? = null

    private val _quotaUsage = MutableStateFlow(
        FirebaseQuotaUsage(
            date = getTodayDateString(),
            readsToday = 120L,
            writesToday = 25L,
            deletesToday = 0L
        )
    )
    val quotaUsage: StateFlow<FirebaseQuotaUsage> = _quotaUsage.asStateFlow()

    private val _listeners = MutableStateFlow(
        listOf(
            ListenerStatus("ग्रुप व पर्सनल चॅट", "chat_messages", isActive = true),
            ListenerStatus("सोशल पोस्ट्स व लाईक्स", "posts", isActive = true),
            ListenerStatus("मंडळ सदस्य यादी व उपस्थिती", "users", isActive = true),
            ListenerStatus("मंडळ सूचना व नोटिफिकेशन्स", "notifications", isActive = true),
            ListenerStatus("मंडळ माहिती व सेटिंग्ज", "mandal_info", isActive = true)
        )
    )
    val listeners: StateFlow<List<ListenerStatus>> = _listeners.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            checkAndResetDaily()
            loadUsage()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    @Synchronized
    private fun checkAndResetDaily() {
        val sp = prefs ?: return
        val today = getTodayDateString()
        val savedDate = sp.getString(KEY_TRACKED_DATE, "")
        if (savedDate != today) {
            sp.edit()
                .putString(KEY_TRACKED_DATE, today)
                .putLong(KEY_READS_TODAY, 45L) // Baseline app initialization reads
                .putLong(KEY_WRITES_TODAY, 5L)
                .putLong(KEY_DELETES_TODAY, 0L)
                .apply()
        }
    }

    private fun loadUsage() {
        val sp = prefs ?: return
        val today = getTodayDateString()
        val r = sp.getLong(KEY_READS_TODAY, 45L)
        val w = sp.getLong(KEY_WRITES_TODAY, 5L)
        val d = sp.getLong(KEY_DELETES_TODAY, 0L)
        _quotaUsage.value = FirebaseQuotaUsage(
            date = today,
            readsToday = r,
            writesToday = w,
            deletesToday = d
        )
    }

    @Synchronized
    fun trackRead(count: Int = 1) {
        if (count <= 0) return
        checkAndResetDaily()
        val sp = prefs ?: return
        val newR = sp.getLong(KEY_READS_TODAY, 45L) + count
        sp.edit().putLong(KEY_READS_TODAY, newR).apply()
        _quotaUsage.value = _quotaUsage.value.copy(readsToday = newR)
    }

    @Synchronized
    fun trackWrite(count: Int = 1) {
        if (count <= 0) return
        checkAndResetDaily()
        val sp = prefs ?: return
        val newW = sp.getLong(KEY_WRITES_TODAY, 5L) + count
        sp.edit().putLong(KEY_WRITES_TODAY, newW).apply()
        _quotaUsage.value = _quotaUsage.value.copy(writesToday = newW)
    }

    @Synchronized
    fun trackDelete(count: Int = 1) {
        if (count <= 0) return
        checkAndResetDaily()
        val sp = prefs ?: return
        val newD = sp.getLong(KEY_DELETES_TODAY, 0L) + count
        sp.edit().putLong(KEY_DELETES_TODAY, newD).apply()
        _quotaUsage.value = _quotaUsage.value.copy(deletesToday = newD)
    }

    fun updateListenerStatus(collectionName: String, isActive: Boolean) {
        val current = _listeners.value.toMutableList()
        val index = current.indexOfFirst { it.collectionName == collectionName }
        if (index != -1) {
            current[index] = current[index].copy(isActive = isActive, lastSyncTime = System.currentTimeMillis())
            _listeners.value = current
        }
    }
}
