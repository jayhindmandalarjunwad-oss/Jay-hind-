package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Universal FCM Push Dispatcher.
 * Sends high-priority push notifications directly to Google's FCM servers.
 * Awakens device screens from lock mode and delivers instant heads-up banners with sound and vibration.
 */
object FcmPushSenderHelper {

    private const val TAG = "FcmPushSender"
    private const val FCM_SEND_URL = "https://fcm.googleapis.com/fcm/send"
    // Google Services API key configured for project jayhindmandal112
    private const val SERVER_KEY = "AIzaSyCYuMSSTmTmN9J-9EV0yoUHFBGPWiM7_PU"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val senderScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Dispatches a high-priority push notification to all subscribers of a topic.
     * e.g., mandal_posts, mandal_birthdays, mandal_announcements, mandal_emergency_blood, mandal_events, mandal_group_chat
     */
    fun sendPushToTopic(
        topic: String,
        title: String,
        message: String,
        type: String,
        targetRoute: String = "NOTIFICATIONS",
        targetId: String? = null,
        senderId: String = ""
    ) {
        val topicTarget = if (topic.startsWith("/topics/")) topic else "/topics/$topic"
        sendPushInternal(
            recipientTarget = topicTarget,
            title = title,
            message = message,
            type = type,
            targetRoute = targetRoute,
            targetId = targetId,
            senderId = senderId
        )
    }

    /**
     * Dispatches a high-priority push notification directly to a single user's device token.
     */
    fun sendPushToToken(
        token: String,
        title: String,
        message: String,
        type: String,
        targetRoute: String = "NOTIFICATIONS",
        targetId: String? = null,
        senderId: String = ""
    ) {
        if (token.isBlank()) return
        sendPushInternal(
            recipientTarget = token,
            title = title,
            message = message,
            type = type,
            targetRoute = targetRoute,
            targetId = targetId,
            senderId = senderId
        )
    }

    private fun sendPushInternal(
        recipientTarget: String,
        title: String,
        message: String,
        type: String,
        targetRoute: String,
        targetId: String?,
        senderId: String
    ) {
        senderScope.launch {
            val msgId = "fcm_" + UUID.randomUUID().toString().take(12)
            try {
                val rootJson = JSONObject()
                rootJson.put("to", recipientTarget)
                rootJson.put("priority", "high")
                rootJson.put("content_available", true)

                // Data payload (Used by MandalFirebaseMessagingService to show custom heads-up notifications)
                val dataJson = JSONObject().apply {
                    put("title", title)
                    put("body", message)
                    put("message", message)
                    put("type", type)
                    put("targetRoute", targetRoute)
                    if (!targetId.isNullOrBlank()) {
                        put("targetId", targetId)
                    }
                    put("senderId", senderId)
                    put("msgId", msgId)
                    put("timestamp", System.currentTimeMillis())
                }
                rootJson.put("data", dataJson)

                // Notification payload (Ensures automatic system delivery on devices in Doze/Lock mode)
                val notifJson = JSONObject().apply {
                    put("title", title)
                    put("body", message)
                    put("sound", "default")
                    put("priority", "high")
                    put("click_action", "FLUTTER_NOTIFICATION_CLICK")
                }
                rootJson.put("notification", notifJson)

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = rootJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(FCM_SEND_URL)
                    .addHeader("Authorization", "key=$SERVER_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "FCM push dispatch to $recipientTarget: Code=${response.code}, Response=$responseBody")
                response.close()

                // Also persist push to Firestore outbox queue for synchronization & logging
                saveToOutbox(recipientTarget, title, message, type, targetRoute, targetId, senderId, msgId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending FCM push to $recipientTarget: ${e.message}", e)
                // Fallback: Still save to Firestore outbox
                saveToOutbox(recipientTarget, title, message, type, targetRoute, targetId, senderId, msgId)
            }
        }
    }

    private fun saveToOutbox(
        recipientTarget: String,
        title: String,
        message: String,
        type: String,
        targetRoute: String,
        targetId: String?,
        senderId: String,
        msgId: String
    ) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val outboxData = hashMapOf(
                "msgId" to msgId,
                "target" to recipientTarget,
                "title" to title,
                "message" to message,
                "type" to type,
                "targetRoute" to targetRoute,
                "targetId" to targetId,
                "senderId" to senderId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("fcm_outbox").document(msgId).set(outboxData, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing to fcm_outbox: ${e.message}")
        }
    }
}
