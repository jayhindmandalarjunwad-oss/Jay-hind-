package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received boot or package broadcast: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            SystemNotificationHelper.initNotificationChannels(context)
            MandalSyncJobService.scheduleJob(context)
            MandalBackupJobService.scheduleDailyBackup(context)
        }
    }
}
