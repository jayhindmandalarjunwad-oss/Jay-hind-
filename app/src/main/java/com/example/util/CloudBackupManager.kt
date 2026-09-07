package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupInfo(
    val fileName: String,
    val formattedDate: String,
    val formattedSize: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val downloadUrl: String,
    val locationDescription: String = "Firebase Cloud + Google Drive"
)

object CloudBackupManager {

    private const val TAG = "CloudBackupManager"
    private const val PREFS_NAME = "jayhind_cloud_backup_prefs"
    private const val CLOUD_MASTER_PATH = "backups/jayhind_master_backup.db"

    /**
     * Checks if a Firebase Storage bucket is actually provisioned and reachable on Google Cloud.
     * Prevents noisy StorageException logs if the bucket has not been enabled in Firebase Console.
     */
    private fun findAvailableStorage(): com.google.firebase.storage.FirebaseStorage? {
        val candidates = listOf(
            "gs://jayhindmandal112.firebasestorage.app" to "https://storage.googleapis.com/storage/v1/b/jayhindmandal112.firebasestorage.app",
            "gs://jayhindmandal112.appspot.com" to "https://storage.googleapis.com/storage/v1/b/jayhindmandal112.appspot.com"
        )
        for ((gsUrl, checkUrl) in candidates) {
            try {
                val url = java.net.URL(checkUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                conn.instanceFollowRedirects = false
                val code = conn.responseCode
                conn.disconnect()
                // If code is not 404, the bucket exists in Google Cloud! (e.g. 200, 401, 403)
                if (code != 404) {
                    return com.google.firebase.storage.FirebaseStorage.getInstance(gsUrl)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Storage bucket check skipped: ${e.message}")
            }
        }
        return null
    }

    /**
     * Uploads the local backup SQLite database file to Firebase Cloud Storage.
     * Keeps a master copy at `backups/jayhind_master_backup.db` and an archival copy in `backups/history/`.
     * Also updates Firestore metadata for global status tracking.
     */
    suspend fun uploadBackupToCloud(
        context: Context,
        backupFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<CloudBackupInfo> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                return@withContext Result.failure(Exception("बॅकअप फाइल अस्तित्वात नाही अथवा रिकामी आहे."))
            }

            val storage = findAvailableStorage()
            if (storage == null) {
                val msg = "Firebase Console मध्ये Cloud Storage बकेट अद्याप सक्रिय (Active) नाही. कृपया 'Drive' बटण दाबून थेट Google Drive वर सेव्ह करा."
                Log.i(TAG, msg)
                return@withContext Result.failure(Exception(msg))
            }

            val masterRef = storage.reference.child(CLOUD_MASTER_PATH)
            val historyRef = storage.reference.child("backups/history/${backupFile.name}")

            val metadata = StorageMetadata.Builder()
                .setContentType("application/x-sqlite3")
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .setCustomMetadata("fileName", backupFile.name)
                .setCustomMetadata("mandal", "Jay Hind Tarun Mandal Arjunwad")
                .build()

            val fileUri = Uri.fromFile(backupFile)

            // 1. Upload Master Backup with progress listener
            val uploadTask = masterRef.putFile(fileUri, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                    onProgress(progress)
                }
            }

            uploadTask.await()
            val downloadUrl = masterRef.downloadUrl.await().toString()

            // 2. Upload Archival history in background (non-blocking)
            try {
                historyRef.putFile(fileUri, metadata)
            } catch (e: Exception) {
                Log.w(TAG, "History archive upload warning: ${e.message}")
            }

            val now = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now))
            val size = backupFile.length()
            val formattedSize = LocalBackupManager.formatFileSize(size)

            // 3. Save details locally in SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("cloud_backup_timestamp", now)
                .putString("cloud_backup_date_str", formattedDate)
                .putString("cloud_backup_file_name", backupFile.name)
                .putLong("cloud_backup_size", size)
                .putString("cloud_backup_download_url", downloadUrl)
                .putBoolean("cloud_backup_exists", true)
                .apply()

            // 4. Update Firestore for global admin synchronization
            try {
                val firestore = FirebaseFirestore.getInstance()
                val metaDataMap = mapOf(
                    "lastBackupTimestamp" to now,
                    "lastBackupDateStr" to formattedDate,
                    "lastBackupFileName" to backupFile.name,
                    "lastBackupSizeBytes" to size,
                    "downloadUrl" to downloadUrl,
                    "organization" to "जय हिंद तरुण मंडळ, अर्जुनवाड"
                )
                firestore.collection("system_settings").document("backup_meta")
                    .set(metaDataMap, SetOptions.merge())
                    .await()
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore backup metadata sync note: ${fe.message}")
            }

            val cloudInfo = CloudBackupInfo(
                fileName = backupFile.name,
                formattedDate = formattedDate,
                formattedSize = formattedSize,
                sizeBytes = size,
                timestamp = now,
                downloadUrl = downloadUrl
            )

            Log.d(TAG, "Cloud Backup successfully uploaded: ${backupFile.name} ($formattedSize)")
            Result.success(cloudInfo)
        } catch (e: Exception) {
            val userMsg = if (e.message?.contains("404") == true || e.message?.contains("Object does not exist") == true) {
                "Firebase Console मध्ये Cloud Storage बकेट सक्रिय नाही. कृपया Google Drive ('Drive' बटण) वापरा."
            } else {
                e.localizedMessage ?: "क्लाउड अपलोड त्रुटी"
            }
            Log.w(TAG, "Cloud Backup upload note: $userMsg")
            Result.failure(Exception(userMsg))
        }
    }

    /**
     * Retrieves the latest Cloud Backup details from SharedPreferences.
     * Does not trigger premature StorageException on remote servers when no upload exists.
     */
    suspend fun getLatestCloudBackupInfo(context: Context): CloudBackupInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasCloudBackup = prefs.getBoolean("cloud_backup_exists", false)

        if (hasCloudBackup) {
            val dateStr = prefs.getString("cloud_backup_date_str", null) ?: "माहिती उपलब्ध नाही"
            val fileName = prefs.getString("cloud_backup_file_name", null) ?: "jayhind_master_backup.db"
            val size = prefs.getLong("cloud_backup_size", 0L)
            val timestamp = prefs.getLong("cloud_backup_timestamp", 0L)
            val downloadUrl = prefs.getString("cloud_backup_download_url", "") ?: ""

            return@withContext CloudBackupInfo(
                fileName = fileName,
                formattedDate = dateStr,
                formattedSize = LocalBackupManager.formatFileSize(size),
                sizeBytes = size,
                timestamp = timestamp,
                downloadUrl = downloadUrl
            )
        }
        null
    }

    /**
     * Downloads the master database from Firebase Cloud Storage and restores it locally.
     */
    suspend fun downloadAndRestoreFromCloud(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val storage = findAvailableStorage()
            if (storage == null) {
                return@withContext Result.failure(Exception("Firebase Cloud Storage बकेट उपलब्ध नाही."))
            }

            val masterRef = storage.reference.child(CLOUD_MASTER_PATH)
            val tempFile = File(context.cacheDir, "temp_cloud_backup_restore.db")
            if (tempFile.exists()) tempFile.delete()

            val downloadTask = masterRef.getFile(tempFile)
            downloadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
                    onProgress(progress)
                }
            }
            downloadTask.await()

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(Exception("क्लाउडवरून बॅकअप डाउनलोड करता आला नाही."))
            }

            // Restore using LocalBackupManager
            val restoreResult = LocalBackupManager.restoreBackup(context, tempFile)
            tempFile.delete()

            if (restoreResult.isSuccess) {
                Log.d(TAG, "Successfully restored database from Firebase Cloud Backup!")
                Result.success(true)
            } else {
                Result.failure(restoreResult.exceptionOrNull() ?: Exception("पुनर्संचयित करता आले नाही."))
            }
        } catch (e: Exception) {
            val userMsg = if (e.message?.contains("404") == true || e.message?.contains("Object does not exist") == true) {
                "क्लाउडवर अद्याप कोणताही बॅकअप उपलब्ध नाही."
            } else {
                e.localizedMessage ?: "क्लाउड रिस्टोअर त्रुटी"
            }
            Log.w(TAG, "Download & Restore from Cloud note: $userMsg")
            Result.failure(Exception(userMsg))
        }
    }

    /**
     * Direct Integration: Invokes Google Drive native save activity (`com.google.android.apps.docs`).
     * Opens Google Drive's "Save to Drive" dialog where user selects their account
     * (jayhindmandalarjunwad@gmail.com) and folder, with zero manual setup.
     */
    fun saveToGoogleDrive(context: Context, backupFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            // Try targeting Google Drive app directly
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                `package` = "com.google.android.apps.docs"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, backupFile.name)
                putExtra(Intent.EXTRA_SUBJECT, "जय हिंद तरुण मंडळ डेटाबेस बॅकअप (${backupFile.name})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            val matches = pm.queryIntentActivities(driveIntent, 0)

            if (matches.isNotEmpty()) {
                context.startActivity(driveIntent)
            } else {
                // If specific Google Drive package is not resolved, show system chooser with Drive option
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "जय हिंद तरुण मंडळ बॅकअप (${backupFile.name})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(chooserIntent, "Google Drive किंवा अन्य ॲपमध्ये सेव्ह करा").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Drive save: ${e.message}")
            Toast.makeText(context, "गुगल ड्राईव्ह उघडताना त्रुटी: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
