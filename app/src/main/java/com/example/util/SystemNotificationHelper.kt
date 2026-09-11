package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object SystemNotificationHelper {
    const val CHANNEL_GENERAL = "channel_mandal_general"
    const val CHANNEL_CHAT = "channel_mandal_chat"
    const val CHANNEL_GROUP_CHAT = "channel_mandal_group_chat"
    const val CHANNEL_EMERGENCY_BLOOD = "channel_emergency_blood"
    const val CHANNEL_POSTS = "channel_mandal_posts"
    const val CHANNEL_BIRTHDAY = "channel_mandal_birthday"
    const val CHANNEL_BACKGROUND_SERVICE = "channel_mandal_background_service"

    private val recentNotificationTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val DEDUPLICATION_WINDOW_MS = 15_000L // 15 seconds deduplication window

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "मंडळ सूचना व कार्यक्रम (General Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "मंडळातील महत्त्वाच्या सूचना आणि कार्यक्रम नोटिफिकेशन्स"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val postsChannel = NotificationChannel(
                CHANNEL_POSTS,
                "नवीन पोस्ट व उपक्रम (New Posts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "मंडळातील सदस्यांनी केलेल्या नवीन पोस्ट व उपक्रम"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val birthdayChannel = NotificationChannel(
                CHANNEL_BIRTHDAY,
                "वाढदिवस शुभेच्छा (Birthday Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "मंडळातील कार्यकर्त्यांचे व सदस्यांचे वाढदिवस अलर्ट्स"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val chatChannel = NotificationChannel(
                CHANNEL_CHAT,
                "वैयक्तिक चॅट मेसेज (Direct Messages)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "सभासदांचे खाजगी संदेश"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val groupChatChannel = NotificationChannel(
                CHANNEL_GROUP_CHAT,
                "ग्रुप चॅट मेसेज (Group Chat)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "🚩 जय हिंद मंडळ सर्व सदस्य ग्रुप मेसेज"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val bloodChannel = NotificationChannel(
                CHANNEL_EMERGENCY_BLOOD,
                "🚨 आणीबाणी रक्तदान अलर्ट (Emergency Blood SOS)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "तातडीची रक्ताची गरज व जीवनदायी आणीबाणी अलर्ट्स"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450, 150, 450)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val backgroundServiceChannel = NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "बॅकग्राउंड सिंक सेवा (Background Service)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "मंडळातील नवीन मेसेज व सूचना वेळेवर मिळवण्यासाठी बॅकग्राउंड सेवा"
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }

            notificationManager.createNotificationChannels(
                listOf(generalChannel, postsChannel, birthdayChannel, chatChannel, groupChatChannel, bloodChannel, backgroundServiceChannel)
            )
        }
    }

    private fun wakeUpScreenIfNeeded(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            if (powerManager != null && !powerManager.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "MandalApp:NotificationWakeLock"
                )
                wakeLock.acquire(3000L) // Light up screen for 3 seconds on lock screen
            }
        } catch (e: Exception) {
            // Non-critical fallback
        }
    }

    fun createForegroundServiceNotification(context: Context): android.app.Notification {
        initNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚩 जय हिंद मंडळ")
            .setContentText("नवीन मेसेज व सूचनांसाठी बॅकग्राउंड सेवा सक्रिय आहे")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int? = null,
        channelId: String = CHANNEL_GENERAL,
        targetRoute: String = "NOTIFICATIONS",
        targetId: String? = null
    ) {
        val deduplicationKey = "$channelId|$title|$message"
        val now = System.currentTimeMillis()
        val lastShown = recentNotificationTimestamps[deduplicationKey]
        if (lastShown != null && (now - lastShown) < DEDUPLICATION_WINDOW_MS) {
            android.util.Log.d("SystemNotification", "Deduplicated duplicate notification skipped: $title")
            return
        }
        recentNotificationTimestamps[deduplicationKey] = now

        // Housekeeping: clean entries older than 2 minutes
        if (recentNotificationTimestamps.size > 100) {
            recentNotificationTimestamps.entries.removeIf { (now - it.value) > 120_000L }
        }

        initNotificationChannels(context)

        // Generate stable deterministic ID if not explicitly provided
        val effectiveNotificationId = notificationId ?: Math.abs(deduplicationKey.hashCode())

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_ROUTE", targetRoute)
            if (targetId != null) {
                putExtra("EXTRA_TARGET_ID", targetId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            effectiveNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Wake up screen if locked or in sleep mode so user immediately sees incoming alert
        wakeUpScreenIfNeeded(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(if (channelId == CHANNEL_EMERGENCY_BLOOD) longArrayOf(0, 450, 150, 450, 150, 450) else longArrayOf(0, 250, 150, 250))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(effectiveNotificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled when permission not yet granted on Android 13+
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAllNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
