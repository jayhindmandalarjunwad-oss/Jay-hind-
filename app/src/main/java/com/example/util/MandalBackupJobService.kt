package com.example.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android Native JobScheduler Service for Daily Automatic Database Backup.
 * Runs in background once every 24 hours.
 * Automatically takes a Room DB snapshot, prunes files older than 7 days,
 * and survives device restarts.
 */
class MandalBackupJobService : JobService() {

    private val jobScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "MandalBackupJob"
        const val BACKUP_JOB_ID = 1008

        fun scheduleDailyBackup(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, MandalBackupJobService::class.java)

                // 24 hours periodic interval (24 * 60 * 60 * 1000L)
                val builder = JobInfo.Builder(BACKUP_JOB_ID, component)
                    .setPeriodic(24 * 60 * 60 * 1000L)
                    .setPersisted(true) // Survives phone restart

                val result = scheduler.schedule(builder.build())
                Log.d(TAG, "MandalBackupJob scheduled successfully with status: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule daily backup job: ${e.message}", e)
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "MandalBackupJobService triggered daily backup execution")

        jobScope.launch {
            try {
                val backupResult = LocalBackupManager.performBackup(applicationContext, isAuto = true)
                if (backupResult.isSuccess) {
                    val file = backupResult.getOrNull()
                    val pruned = LocalBackupManager.cleanupOldBackups(applicationContext)
                    Log.d(TAG, "Daily automatic backup created: ${file?.name}, cleaned up $pruned old backups.")

                    // Also upload to Firebase Cloud Storage automatically
                    if (file != null) {
                        try {
                            val cloudResult = CloudBackupManager.uploadBackupToCloud(applicationContext, file)
                            if (cloudResult.isSuccess) {
                                Log.d(TAG, "Daily cloud backup successfully synced to Firebase Storage!")
                            } else {
                                Log.i(TAG, "Daily cloud sync note: ${cloudResult.exceptionOrNull()?.message}")
                            }
                        } catch (ce: Exception) {
                            Log.i(TAG, "Cloud upload note during daily job: ${ce.message}")
                        }

                        // Daily Google Drive Media Backup (Photos & Voice Messages into structured Year/Month folders)
                        try {
                            val driveMediaResult = GoogleDriveMediaBackupManager.performCompleteMediaBackup(applicationContext, isAutoNightly = true)
                            Log.d(TAG, "Daily Google Drive Media Sync: ${driveMediaResult.getOrNull() ?: driveMediaResult.exceptionOrNull()?.message}")
                        } catch (de: Exception) {
                            Log.i(TAG, "Google Drive media sync note: ${de.message}")
                        }

                        // Automatic 15-day Archival & Purge to Google Drive (Zero Cost, Keeps Firebase Storage < 5GB)
                        try {
                            val archiveResult = GoogleDriveMediaBackupManager.pruneAndArchiveMediaOlderThan15Days(applicationContext)
                            Log.d(TAG, "Daily 15-day media archival: ${archiveResult.getOrNull() ?: archiveResult.exceptionOrNull()?.message}")
                        } catch (ae: Exception) {
                            Log.i(TAG, "Archival error note: ${ae.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "Daily automatic backup failed: ${backupResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in daily backup job: ${e.message}", e)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "MandalBackupJobService stopped by system")
        return true
    }
}
