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
 * Decommissioned in favor of official Google FCM (Firebase Cloud Messaging) Push Engine.
 * Saves 30,000+ daily Firestore reads by eliminating 24/7 continuous background listeners.
 * Instant notifications for announcements, emergency blood alerts, events, posts, birthdays,
 * and chats are now delivered directly via Google Play Services (MandalFirebaseMessagingService) with 0 reads.
 */
class MandalNotificationService : Service() {

    companion object {
        private const val TAG = "MandalNotifService"
        const val NOTIFICATION_ID_FOREGROUND = 999

        fun startService(context: Context) {
            // Self-terminating: stops any previous persistent foreground instances
            stopService(context)
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
        Log.d(TAG, "MandalNotificationService onCreate - Gracefully stopping background service in favor of FCM push.")
        try {
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MandalNotificationService onDestroy")
        try {
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
    }
}
