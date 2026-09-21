package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object AppUpdateHelper {

    /**
     * Converts Google Drive shareable URLs into direct APK download endpoints.
     */
    fun convertDriveUrlToApkDownloadUrl(url: String): String {
        val trimmed = url.trim()
        if (!trimmed.contains("drive.google.com") && !trimmed.contains("docs.google.com")) return trimmed

        val fileId = when {
            trimmed.contains("/file/d/") -> trimmed.substringAfter("/file/d/").substringBefore("/").substringBefore("?").substringBefore("&")
            trimmed.contains("id=") -> trimmed.substringAfter("id=").substringBefore("&").substringBefore("#")
            trimmed.contains("/open?id=") -> trimmed.substringAfter("/open?id=").substringBefore("&").substringBefore("#")
            trimmed.contains("/uc?id=") -> trimmed.substringAfter("/uc?id=").substringBefore("&").substringBefore("#")
            else -> null
        }

        return if (!fileId.isNullOrBlank() && fileId.length >= 15) {
            "https://drive.google.com/uc?export=download&id=$fileId"
        } else {
            trimmed
        }
    }

    /**
     * Automatically computes the next minor version name:
     * e.g., "1.0" -> "1.1", "1.1" -> "1.2", "1.2" -> "1.3", "2.5" -> "2.6"
     */
    fun calculateNextVersionName(currentVersionName: String): String {
        val trimmed = currentVersionName.trim().removePrefix("v").removePrefix("V")
        val parts = trimmed.split(".")
        return when {
            parts.size >= 2 -> {
                val major = parts[0].toIntOrNull() ?: 1
                val minor = parts[1].toIntOrNull() ?: 0
                "$major.${minor + 1}"
            }
            parts.size == 1 -> {
                val num = parts[0].toIntOrNull() ?: 1
                "$num.1"
            }
            else -> "1.1"
        }
    }

    /**
     * Triggers the installation of an APK file via Android's PackageInstaller.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK फाईल सापडली नाही.", Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateHelper", "Error installing APK: ${e.message}", e)
            Toast.makeText(context, "इन्स्टॉल सुरू करताना अडचण: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens the download or Google Drive link directly in the browser as a fail-safe.
     */
    fun openInBrowser(context: Context, rawUrl: String) {
        try {
            val finalUrl = convertDriveUrlToApkDownloadUrl(rawUrl)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "लिंक उघडता आली नाही: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Downloads the APK file using Android's system DownloadManager and prompts installation.
     */
    fun downloadAndInstallApk(
        context: Context,
        rawUrl: String,
        versionName: String,
        onStarted: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        try {
            val downloadUrl = convertDriveUrlToApkDownloadUrl(rawUrl)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                openInBrowser(context, downloadUrl)
                return
            }

            val fileName = "JayHindMandal_v${versionName.replace(".", "_")}.apk"
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("जयहिंद मंडळ ॲप v$versionName")
                setDescription("मंडळाच्या ॲपचे नवीन अपडेट डाऊनलोड होत आहे...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = dm.enqueue(request)
            onStarted()
            Toast.makeText(context, "नवीन ॲप व्हर्जन डाऊनलोड होत आहे... 📥", Toast.LENGTH_SHORT).show()

            // Broadcast receiver to listen when download finishes
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                    if (id == downloadId) {
                        try {
                            recvContext?.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        if (targetFile.exists()) {
                            Toast.makeText(context, "डाऊनलोड पूर्ण झाले! इन्स्टॉल करत आहे...", Toast.LENGTH_SHORT).show()
                            installApk(context, targetFile)
                        } else {
                            openInBrowser(context, downloadUrl)
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateHelper", "DownloadManager failed, falling back to browser: ${e.message}")
            onFailure(e.localizedMessage ?: "अडचण आली")
            openInBrowser(context, rawUrl)
        }
    }
}
