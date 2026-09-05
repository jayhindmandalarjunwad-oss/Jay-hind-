package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

/**
 * Robust background service that keeps push notifications active
 * even when the app is swiped away or closed by the user.
 */
class MandalNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var chatListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null
    private var bloodAlertListener: ListenerRegistration? = null

    private val shownMessageIds = ConcurrentHashMap<String, Long>()
    private val shownNotifIds = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "MandalNotifService"
        const val NOTIFICATION_ID_FOREGROUND = 999

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MandalNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                // Safely handle ForegroundServiceStartNotAllowedException on Android 12+
                Log.d(TAG, "MandalNotificationService start skipped (background restriction): ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MandalNotificationService onCreate")

        try {
            val notification = SystemNotificationHelper.createForegroundServiceNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID_FOREGROUND,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground: ${e.message}")
        }

        setupFirestorePushListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MandalNotificationService onStartCommand, START_STICKY")
        if (chatListener == null || notificationListener == null) {
            setupFirestorePushListeners()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed (swiped from recents), scheduling native JobService")
        try {
            MandalSyncJobService.scheduleJob(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule native job: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MandalNotificationService onDestroy")
        try {
            chatListener?.remove()
            notificationListener?.remove()
            bloodAlertListener?.remove()
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleanup in onDestroy: ${e.message}")
        }
    }

    private fun getCurrentUserId(): String? {
        val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
        return prefs.getString("logged_user_id", null)
    }

    private fun isCurrentUserAdmin(): Boolean {
        val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("logged_user_role", "MEMBER")
        return role == "ADMIN"
    }

    private fun setupFirestorePushListeners() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val startTimeThreshold = System.currentTimeMillis() - 60_000L // last 1 minute

            // 1. CHAT MESSAGES LISTENER (Group & Direct Chats)
            chatListener?.remove()
            chatListener = firestore.collection("chat_messages")
                .whereGreaterThan("timestamp", startTimeThreshold)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "chat_messages listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    val currentUserId = getCurrentUserId() ?: return@addSnapshotListener

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val msgId = doc.id
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: "सभासद"
                            val receiverId = doc.getString("receiverId") ?: ""
                            val conversationId = doc.getString("conversationId") ?: ""
                            val messageText = doc.getString("messageText") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            // Skip messages sent by self or messages older than 2 minutes
                            if (senderId == currentUserId || (System.currentTimeMillis() - timestamp) > 120_000L) {
                                continue
                            }

                            if (shownMessageIds.putIfAbsent(msgId, timestamp) == null) {
                                val isGroup = receiverId == "GROUP_MANDAL" || conversationId == "conv_mandal_group"
                                if (isGroup) {
                                    val preview = if (messageText.isNotBlank()) messageText else "नवीन संदेश आला आहे"
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
                                    val preview = if (messageText.isNotBlank()) messageText else "नवीन मेसेज आला आहे 💬"
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
                    }
                }

            // 2. GENERAL NOTIFICATIONS & ANNOUNCEMENTS LISTENER
            notificationListener?.remove()
            notificationListener = firestore.collection("notifications")
                .whereGreaterThan("createdAt", startTimeThreshold)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "notifications listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    val currentUserId = getCurrentUserId()
                    val isAdmin = isCurrentUserAdmin()

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val notifId = doc.id
                            val title = doc.getString("title") ?: "🚩 जय हिंद मंडळ"
                            val message = doc.getString("message") ?: ""
                            val type = doc.getString("type") ?: "GENERAL"
                            val targetUserId = doc.getString("targetUserId")
                            val targetRoute = doc.getString("targetRoute") ?: "ANNOUNCEMENTS"
                            val targetId = doc.getString("targetId")
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            // Skip if already seen or older than 3 minutes
                            if ((System.currentTimeMillis() - createdAt) > 180_000L) continue
                            if (shownNotifIds.putIfAbsent(notifId, createdAt) != null) continue

                            val isRelevant = when {
                                targetUserId == null || targetUserId.isBlank() || targetUserId == "ALL" -> true
                                targetUserId == "ADMIN" -> isAdmin
                                else -> targetUserId == currentUserId
                            }

                            if (isRelevant && type != "CHAT") {
                                val channel = if (type == "BLOOD_ALERT") {
                                    SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD
                                } else {
                                    SystemNotificationHelper.CHANNEL_GENERAL
                                }

                                SystemNotificationHelper.showSystemNotification(
                                    context = applicationContext,
                                    title = title,
                                    message = message,
                                    notificationId = Math.abs(notifId.hashCode()),
                                    channelId = channel,
                                    targetRoute = targetRoute,
                                    targetId = targetId
                                )
                            }
                        }
                    }
                }

            // 3. EMERGENCY BLOOD ALERTS LISTENER
            bloodAlertListener?.remove()
            bloodAlertListener = firestore.collection("emergency_blood_alerts")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val currentUserId = getCurrentUserId()

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val alertId = doc.id
                            val bloodGroup = doc.getString("bloodGroup") ?: ""
                            val patientName = doc.getString("patientName") ?: "रुग्ण"
                            val hospital = doc.getString("hospitalName") ?: ""
                            val requesterId = doc.getString("requesterId") ?: ""
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            // Skip if self requested or older than 30 mins
                            if (requesterId == currentUserId || (System.currentTimeMillis() - createdAt) > 30 * 60 * 1000L) continue
                            if (shownNotifIds.putIfAbsent("blood_$alertId", createdAt) != null) continue

                            SystemNotificationHelper.showSystemNotification(
                                context = applicationContext,
                                title = "🚨 तातडीची रक्ताची गरज: गट $bloodGroup",
                                message = "$patientName यांच्यासाठी $hospital येथे $bloodGroup रक्ताची तातडीने गरज आहे. कृपया संपर्क करा!",
                                notificationId = Math.abs(alertId.hashCode()),
                                channelId = SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD,
                                targetRoute = "BLOOD_DONATION",
                                targetId = alertId
                            )
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, "Failed setting up Firestore push listeners: ${e.message}", e)
        }
    }
}
