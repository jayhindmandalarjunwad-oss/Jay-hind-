package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MandalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.example.util.SystemNotificationHelper.initNotificationChannels(this)
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:839410753027:android:cdeafd3dc39c6e0258f836")
                    .setApiKey("AIzaSyCYuMSSTmTmN9J-9EV0yoUHFBGPWiM7_PU")
                    .setProjectId("jayhindmandal112")
                    .setStorageBucket("jayhindmandal112.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MandalApp", "Firebase explicitly initialized with project jayhindmandal112")
            } else {
                Log.d("MandalApp", "Firebase default app already initialized")
            }
        } catch (e: Exception) {
            Log.e("MandalApp", "Failed to initialize Firebase: ${e.message}", e)
        }
    }
}
