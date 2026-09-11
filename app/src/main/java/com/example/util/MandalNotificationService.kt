package com.example.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Native Foreground Service that maintains active real-time Firestore listeners
 * for notifications, announcements, events, posts, and chats.
 * Keeps notification alerts instant even when the app is swiped away, in sleep mode, or locked.
 * Runs silently with a minimal ongoing notification to prevent Android OS from killing the process.
 */
class MandalNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestoreListeners = mutableListOf<ListenerRegistration>()
    private val processedNotificationIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val processedChatIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MandalNotificationService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                context.stopService(Intent(context, MandalNotificationService::class.java))
                SystemNotificationHelper.cancelNotification(context, NOTIFICATION_ID_FOREGROUND)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop MandalNotificationService: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MandalNotificationService onCreate - Starting background listeners")
        try {
            val foregroundNotification = SystemNotificationHelper.createForegroundServiceNotification(this)
            startForeground(NOTIFICATION_ID_FOREGROUND, foregroundNotification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification: ${e.message}")
        }

        startRealtimeListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val foregroundNotification = SystemNotificationHelper.createForegroundServiceNotification(this)
            startForeground(NOTIFICATION_ID_FOREGROUND, foregroundNotification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed in onStartCommand startForeground: ${e.message}")
        }
        return START_STICKY
    }

    private fun startRealtimeListeners() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)

            // Real-time listener for Notifications
            val notifListener = firestore.collection("notifications")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "Notifications listener error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    val currentUserId = prefs.getString("logged_user_id", null)
                    val isAdmin = prefs.getString("logged_user_role", "MEMBER") == "ADMIN"
                    val now = System.currentTimeMillis()

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            // Only trigger notification for fresh events (within 3 minutes)
                            if (now - timestamp < 180_000L) {
                                val notifId = doc.id
                                if (processedNotificationIds.add(notifId)) {
                                    val targetUserId = doc.getString("targetUserId")
                                    val authorId = doc.getString("targetExtra")
                                    val type = doc.getString("type") ?: "GENERAL"

                                    // Skip self-authored notifications
                                    if (!authorId.isNullOrBlank() && authorId == currentUserId) {
                                        continue
                                    }

                                    val isRelevant = when {
                                        targetUserId == null || targetUserId.isBlank() || targetUserId == "ALL" -> true
                                        targetUserId == "ADMIN" -> isAdmin
                                        type == "BLOOD_ALERT" -> currentUserId != null
                                        type == "POST" -> currentUserId != null
                                        else -> targetUserId == currentUserId
                                    }

                                    if (isRelevant && type != "CHAT") {
                                        val title = doc.getString("title") ?: "🚩 जय हिंद मंडळ"
                                        val message = doc.getString("message") ?: ""
                                        val channelId = when (type) {
                                            "POST" -> SystemNotificationHelper.CHANNEL_POSTS
                                            "BIRTHDAY" -> SystemNotificationHelper.CHANNEL_BIRTHDAY
                                            "BLOOD_ALERT" -> SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD
                                            else -> SystemNotificationHelper.CHANNEL_GENERAL
                                        }
                                        val targetRoute = doc.getString("targetRoute") ?: when (type) {
                                            "POST" -> "POST"
                                            "BIRTHDAY" -> "BIRTHDAYS"
                                            "BLOOD_ALERT" -> "BLOOD_ALERT"
                                            "EVENT" -> "EVENTS"
                                            else -> "NOTIFICATIONS"
                                        }
                                        val targetId = doc.getString("targetId")

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
                            }
                        }
                    }
                }
            firestoreListeners.add(notifListener)

            // Real-time listener for Chat Messages
            val chatListener = firestore.collection("chat_messages")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e(TAG, "Chat messages listener error: ${e.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    val currentUserId = prefs.getString("logged_user_id", null) ?: return@addSnapshotListener
                    val now = System.currentTimeMillis()

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            if (now - timestamp < 120_000L) {
                                val msgId = doc.id
                                val senderId = doc.getString("senderId") ?: ""
                                if (senderId != currentUserId && processedChatIds.add(msgId)) {
                                    val receiverId = doc.getString("receiverId") ?: ""
                                    val conversationId = doc.getString("conversationId") ?: ""
                                    val senderName = doc.getString("senderName") ?: "सभासद"
                                    val messageText = doc.getString("messageText") ?: ""

                                    val isGroup = receiverId == "GROUP_MANDAL" || conversationId == "conv_mandal_group"
                                    if (isGroup) {
                                        val previewText = if (messageText.isNotBlank()) messageText else "नवीन संदेश आला आहे"
                                        SystemNotificationHelper.showSystemNotification(
                                            context = applicationContext,
                                            title = "🚩 जय हिंद ग्रुप: $senderName",
                                            message = previewText,
                                            notificationId = Math.abs(msgId.hashCode()),
                                            channelId = SystemNotificationHelper.CHANNEL_GROUP_CHAT,
                                            targetRoute = "CHAT",
                                            targetId = "GROUP_MANDAL"
                                        )
                                    } else if (receiverId == currentUserId) {
                                        val previewText = if (messageText.isNotBlank()) messageText else "नवीन संदेश आला आहे"
                                        SystemNotificationHelper.showSystemNotification(
                                            context = applicationContext,
                                            title = "$senderName कडून मेसेज 💬",
                                            message = previewText,
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
                }
            firestoreListeners.add(chatListener)

            Log.d(TAG, "MandalNotificationService active with ${firestoreListeners.size} realtime listeners")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start realtime listeners: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MandalNotificationService onDestroy")
        for (listener in firestoreListeners) {
            try {
                listener.remove()
            } catch (_: Exception) {}
        }
        firestoreListeners.clear()
        serviceScope.cancel()
        try {
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
    }
}
