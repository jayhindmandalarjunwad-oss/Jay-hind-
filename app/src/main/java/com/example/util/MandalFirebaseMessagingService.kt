package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Modern Google Firebase Cloud Messaging (FCM) push notification engine.
 * Handles incoming data and notification pushes from Google servers with zero background battery drain.
 * Delivers instant heads-up notifications for chats, announcements, and emergency blood alerts.
 */
class MandalFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MandalFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM push token generated: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM push payload received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title
            ?: data["title"]
            ?: "🚩 जय हिंद मंडळ अर्जुनवाड"

        val body = notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: ""

        if (body.isBlank() && notification == null) {
            Log.d(TAG, "Empty push message received, skipping display.")
            return
        }

        val type = data["type"] ?: "GENERAL"
        val senderId = data["senderId"] ?: ""

        // Skip notification if message was authored by current logged-in user
        val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("logged_user_id", null)
        if (!currentUserId.isNullOrBlank() && senderId == currentUserId) {
            Log.d(TAG, "Skipping notification for self-authored message: $senderId")
            return
        }

        // Map channels accurately
        val channelId = when (type) {
            "POST" -> SystemNotificationHelper.CHANNEL_POSTS
            "BIRTHDAY" -> SystemNotificationHelper.CHANNEL_BIRTHDAY
            "CHAT" -> SystemNotificationHelper.CHANNEL_CHAT
            "GROUP_CHAT" -> SystemNotificationHelper.CHANNEL_GROUP_CHAT
            "BLOOD_ALERT" -> SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD
            else -> data["channelId"] ?: SystemNotificationHelper.CHANNEL_GENERAL
        }

        // Map target routes for seamless in-app navigation on tap
        val targetRoute = data["targetRoute"] ?: when (type) {
            "POST" -> "POST"
            "BIRTHDAY" -> "BIRTHDAYS"
            "CHAT", "GROUP_CHAT" -> "CHAT"
            "BLOOD_ALERT" -> "BLOOD_ALERT"
            "EVENT" -> "EVENTS"
            "ANNOUNCEMENT" -> "ANNOUNCEMENTS"
            else -> "NOTIFICATIONS"
        }
        val targetId = data["targetId"] ?: if (type == "CHAT") senderId else null
        val msgId = data["msgId"] ?: remoteMessage.messageId ?: "${System.currentTimeMillis()}"

        // Deterministic notification ID so it never duplicates and can be cleanly dismissed/swiped
        val notifId = Math.abs(msgId.hashCode())

        SystemNotificationHelper.showSystemNotification(
            context = applicationContext,
            title = title,
            message = body,
            notificationId = notifId,
            channelId = channelId,
            targetRoute = targetRoute,
            targetId = targetId
        )
    }

    private fun saveTokenToFirestore(token: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val tokenData = mapOf(
                "token" to token,
                "platform" to "android",
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("fcm_tokens").document(token)
                .set(tokenData, SetOptions.merge())

            val prefs = getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("logged_user_id", null)
            if (!currentUserId.isNullOrBlank()) {
                firestore.collection("users").document(currentUserId)
                    .update("fcmToken", token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token: ${e.message}", e)
        }
    }
}

