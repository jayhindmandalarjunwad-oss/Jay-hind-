package com.example.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Android Native JobScheduler service that periodically checks for any
 * unread notifications, chats, or emergency blood alerts even when the app is completely closed.
 */
class MandalSyncJobService : JobService() {

    private val jobScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "MandalSyncJob"
        const val JOB_ID = 1005

        fun scheduleJob(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, MandalSyncJobService::class.java)

                // Run periodically every 15 minutes (minimum interval allowed by Android)
                val builder = JobInfo.Builder(JOB_ID, component)
                    .setPeriodic(15 * 60 * 1000L)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true) // Survives phone restart

                val result = scheduler.schedule(builder.build())
                Log.d(TAG, "MandalSyncJob scheduled with result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule job: ${e.message}")
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "MandalSyncJobService started execution in background")

        jobScope.launch {
            try {
                checkRecentNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notifications: ${e.message}")
            } finally {
                jobFinished(params, false)
            }
        }
        return true // Asynchronous execution
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobScope.cancel()
        return true // Reschedule if aborted
    }

    private suspend fun checkRecentNotifications() {
        val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("logged_user_id", null) ?: return
        val isAdmin = prefs.getString("logged_user_role", "MEMBER") == "ADMIN"
        val lastCheckedTime = prefs.getLong("last_background_sync_time", System.currentTimeMillis() - 30 * 60 * 1000L)
        val now = System.currentTimeMillis()

        val firestore = FirebaseFirestore.getInstance()

        // 1. Check notifications
        try {
            val notifsSnapshot = firestore.collection("notifications")
                .whereGreaterThan("timestamp", lastCheckedTime)
                .get()
                .await()

            for (doc in notifsSnapshot.documents) {
                val targetUserId = doc.getString("targetUserId")
                val authorId = doc.getString("targetExtra")
                val type = doc.getString("type") ?: "GENERAL"

                // Skip if authored by self
                if (!authorId.isNullOrBlank() && authorId == currentUserId) {
                    continue
                }

                val isRelevant = when {
                    targetUserId == null || targetUserId.isBlank() || targetUserId == "ALL" -> true
                    targetUserId == "ADMIN" -> isAdmin
                    else -> targetUserId == currentUserId
                }

                if (isRelevant) {
                    val title = doc.getString("title") ?: "🚩 जय हिंद मंडळ"
                    val message = doc.getString("message") ?: ""
                    val notifId = doc.id
                    val targetRoute = doc.getString("targetRoute") ?: when (type) {
                        "POST" -> "POST"
                        "BIRTHDAY" -> "BIRTHDAYS"
                        "BLOOD_ALERT" -> "BLOOD_ALERT"
                        "EVENT" -> "EVENTS"
                        else -> "NOTIFICATIONS"
                    }
                    val targetId = doc.getString("targetId")

                    val channelId = when (type) {
                        "POST" -> SystemNotificationHelper.CHANNEL_POSTS
                        "BIRTHDAY" -> SystemNotificationHelper.CHANNEL_BIRTHDAY
                        "BLOOD_ALERT" -> SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD
                        else -> SystemNotificationHelper.CHANNEL_GENERAL
                    }

                    SystemNotificationHelper.showSystemNotification(
                        context = applicationContext,
                        title = title,
                        message = message,
                        notificationId = Math.abs(notifId.hashCode()),
                        channelId = channelId,
                        targetRoute = targetRoute,
                        targetId = targetId
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications: ${e.message}")
        }

        // 2. Check direct & group chats
        try {
            val chatsSnapshot = firestore.collection("chat_messages")
                .whereGreaterThan("timestamp", lastCheckedTime)
                .get()
                .await()

            for (doc in chatsSnapshot.documents) {
                val senderId = doc.getString("senderId") ?: ""
                val senderName = doc.getString("senderName") ?: "सभासद"
                val receiverId = doc.getString("receiverId") ?: ""
                val conversationId = doc.getString("conversationId") ?: ""
                val messageText = doc.getString("messageText") ?: ""
                val msgId = doc.id

                if (senderId != currentUserId) {
                    if (receiverId == "GROUP_MANDAL" || conversationId == "conv_mandal_group") {
                        val preview = if (messageText.isNotBlank()) messageText else "नवीन ग्रुप संदेश आला आहे"
                        SystemNotificationHelper.showSystemNotification(
                            context = applicationContext,
                            title = "🚩 जय हिंद ग्रुप: $senderName",
                            message = preview,
                            notificationId = Math.abs(msgId.hashCode()),
                            channelId = SystemNotificationHelper.CHANNEL_GROUP_CHAT,
                            targetRoute = "CHAT",
                            targetId = "GROUP_MANDAL"
                        )
                    } else if (receiverId == currentUserId) {
                        val preview = if (messageText.isNotBlank()) messageText else "नवीन संदेश आला आहे"
                        SystemNotificationHelper.showSystemNotification(
                            context = applicationContext,
                            title = "$senderName कडून मेसेज 💬",
                            message = preview,
                            notificationId = Math.abs(msgId.hashCode()),
                            channelId = SystemNotificationHelper.CHANNEL_CHAT,
                            targetRoute = "CHAT",
                            targetId = senderId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat messages: ${e.message}")
        }

        prefs.edit().putLong("last_background_sync_time", now).apply()
    }
}
