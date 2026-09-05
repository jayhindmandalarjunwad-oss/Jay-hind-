package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MandalFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token received: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "🚩 जय हिंद मंडळ"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: ""
        val channelId = remoteMessage.data["channelId"]
            ?: SystemNotificationHelper.CHANNEL_GENERAL
        val targetRoute = remoteMessage.data["targetRoute"] ?: "NOTIFICATIONS"
        val targetId = remoteMessage.data["targetId"]
        val msgId = remoteMessage.data["msgId"] ?: remoteMessage.messageId ?: "${System.currentTimeMillis()}"

        // Deterministic notification ID so it never duplicates
        val notifId = Math.abs(msgId.hashCode())

        if (body.isNotBlank()) {
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
        } catch (e: Exception) {
            Log.e("FCM", "Failed to save FCM token: ${e.message}", e)
        }
    }
}
