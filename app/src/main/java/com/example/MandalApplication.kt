package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MandalApplication : Application(), coil.ImageLoaderFactory {

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashGuard()
        // Register universal app lifecycle manager to handle quota-safe foreground/background transitions
        registerActivityLifecycleCallbacks(com.example.util.MandalAppLifecycleManager)

        try {
            com.example.util.SystemNotificationHelper.initNotificationChannels(this)
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:839410753027:android:cdeafd3dc39c6e0258f836")
                    .setApiKey("AIzaSyCYuMSSTmTmN9J-9EV0yoUHFBGPWiM7_PU")
                    .setProjectId("jayhindmandal112")
                    .setStorageBucket("jayhindmandal112.firebasestorage.app")
                    .setGcmSenderId("839410753027")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MandalApp", "Firebase explicitly initialized with project jayhindmandal112")
            } else {
                Log.d("MandalApp", "Firebase default app already initialized")
            }

            // On emulators or testing environments, gracefully deactivate FCM auto-init and clean any pending sync queue
            if (isEmulatorEnvironment()) {
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
                    val fcmPrefs = getSharedPreferences("com.google.android.gms.appid", android.content.Context.MODE_PRIVATE)
                    fcmPrefs.edit().clear().apply()
                    val fcmMsgPrefs = getSharedPreferences("com.google.firebase.messaging", android.content.Context.MODE_PRIVATE)
                    fcmMsgPrefs.edit().clear().apply()
                    Log.d("MandalApp", "Emulator environment detected: FCM auto-init disabled to prevent registration errors.")
                } catch (_: Exception) {}
            }

            // Schedule native background sync job & daily database backup
            com.example.util.MandalSyncJobService.scheduleJob(this)
            com.example.util.MandalBackupJobService.scheduleDailyBackup(this)
            com.example.util.GoogleDriveMediaBackupManager.init(this)
        } catch (e: Exception) {
            Log.e("MandalApp", "Failed to initialize Firebase or background services: ${e.message}", e)
        }
    }

    private fun isEmulatorEnvironment(): Boolean {
        val fp = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        return fp.startsWith("generic")
                || fp.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk")
                || product.contains("google_sdk")
                || product.contains("emulator")
                || brand.startsWith("generic")
    }

    private fun setupCrashGuard() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MandalAppCrashGuard", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            if (throwable is OutOfMemoryError) {
                com.example.util.MediaUtils.clearBitmapCache()
                System.gc()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            com.example.util.MediaUtils.clearBitmapCache()
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Throwable) {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            com.example.util.MediaUtils.clearBitmapCache()
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Throwable) {}
    }
}
