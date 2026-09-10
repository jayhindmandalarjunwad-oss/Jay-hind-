package com.example.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Discontinued Foreground Service.
 * Successfully migrated to battery-friendly, zero-overhead Firebase Cloud Messaging (FCM).
 * This stub safely dismisses any legacy sticky foreground notifications (ID 999)
 * and immediately shuts down without running any background loops or listeners.
 */
class MandalNotificationService : Service() {

    companion object {
        private const val TAG = "MandalNotifService"
        const val NOTIFICATION_ID_FOREGROUND = 999

        fun startService(context: Context) {
            // Discontinued: Dismiss any legacy notification and stop service
            try {
                SystemNotificationHelper.cancelNotification(context, NOTIFICATION_ID_FOREGROUND)
                context.stopService(Intent(context, MandalNotificationService::class.java))
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MandalNotificationService shutting down - Migrated to Google FCM push")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            SystemNotificationHelper.cancelNotification(this, NOTIFICATION_ID_FOREGROUND)
        } catch (_: Exception) {}
    }
}

