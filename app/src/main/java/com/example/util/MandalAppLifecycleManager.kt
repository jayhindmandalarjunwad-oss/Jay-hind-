package com.example.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

/**
 * Universal Android Application Lifecycle Manager.
 * Tracks foreground/background transitions across activities using pure Android SDK ActivityLifecycleCallbacks.
 * Zero external dependencies.
 *
 * Automatically notifies listeners when app enters foreground (resumed/started) or background (stopped/paused).
 * When in background: stops expensive Firestore snapshot listeners to save quota and battery.
 * When in foreground: reconnects snapshot listeners and updates presence.
 */
object MandalAppLifecycleManager : Application.ActivityLifecycleCallbacks {

    private const val TAG = "MandalLifecycle"
    private var startedActivityCount = 0
    private var isForeground = false

    private val foregroundCallbacks = mutableListOf<() -> Unit>()
    private val backgroundCallbacks = mutableListOf<() -> Unit>()

    /**
     * Register lifecycle listeners. If app is already in foreground, onForeground is invoked immediately.
     */
    fun registerListener(onForeground: () -> Unit, onBackground: () -> Unit) {
        synchronized(this) {
            foregroundCallbacks.add(onForeground)
            backgroundCallbacks.add(onBackground)
            if (isForeground) {
                try {
                    onForeground()
                } catch (e: Exception) {
                    Log.w(TAG, "Error in onForeground callback: ${e.message}")
                }
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        if (startedActivityCount == 1 && !isForeground) {
            isForeground = true
            Log.d(TAG, "🚀 App entered FOREGROUND (Activities active: $startedActivityCount)")
            synchronized(this) {
                foregroundCallbacks.forEach {
                    try {
                        it()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing foreground callback: ${e.message}", e)
                    }
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount = maxOf(0, startedActivityCount - 1)
        if (startedActivityCount == 0 && isForeground) {
            isForeground = false
            Log.d(TAG, "🌙 App entered BACKGROUND (Activities active: $startedActivityCount)")
            synchronized(this) {
                backgroundCallbacks.forEach {
                    try {
                        it()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing background callback: ${e.message}", e)
                    }
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    fun isAppInForeground(): Boolean = isForeground
}
