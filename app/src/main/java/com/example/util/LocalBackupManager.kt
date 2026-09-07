package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupItem(
    val fileName: String,
    val file: File,
    val sizeBytes: Long,
    val timestamp: Long,
    val formattedDate: String,
    val formattedSize: String
)

data class BackupStatusInfo(
    val lastBackupDateStr: String,
    val lastBackupFileName: String,
    val lastBackupSizeBytes: Long,
    val totalBackupsCount: Int,
    val isAutoBackupActive: Boolean = true,
    val retentionDays: Int = 7
)

object LocalBackupManager {

    private const val TAG = "LocalBackupManager"
    private const val BACKUP_DIR_NAME = "JayHind_Backups"
    private const val BACKUP_PREFIX = "JayHind_LocalBackup_"
    private const val PREFS_NAME = "jayhind_backup_prefs"
    private const val RETENTION_DAYS = 7

    /**
     * Executes SQLite WAL Checkpoint and copies Room Database to local backup storage.
     */
    fun performBackup(context: Context, isAuto: Boolean = false): Result<File> {
        return try {
            // 1. Force WAL Checkpoint to guarantee full state is committed to SQLite .db file
            AppDatabase.checkpoint(context)

            val dbFile = context.getDatabasePath("jayhind_mandal_db")
            if (!dbFile.exists()) {
                return Result.failure(Exception("स्थानिक डेटाबेस फाइल सापडली नाही."))
            }

            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.ENGLISH).format(Date())
            val backupFileName = "${BACKUP_PREFIX}${timeStamp}.db"
            val destFile = File(backupDir, backupFileName)

            // Copy with buffer
            FileInputStream(dbFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val now = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now))

            // Save status in preferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_backup_timestamp", now)
                .putString("last_backup_date_str", formattedDate)
                .putString("last_backup_file_name", backupFileName)
                .putLong("last_backup_size", destFile.length())
                .putBoolean("last_backup_success", true)
                .apply()

            // 2. Perform 7-Day Auto-Cleanup cycle
            val pruned = cleanupOldBackups(context)
            Log.d(TAG, "Backup created: $backupFileName (${destFile.length()} bytes). Pruned $pruned old backups.")

            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform local backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 7-Day Auto-Cleanup Cycle:
     * Deletes backups older than 7 days, ensuring mobile storage is never clogged.
     * Always safeguards the 2 most recent backups regardless of age.
     */
    fun cleanupOldBackups(context: Context): Int {
        return try {
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists() || !backupDir.isDirectory) return 0

            val backupFiles = backupDir.listFiles { f ->
                f.isFile && f.name.startsWith(BACKUP_PREFIX) && f.name.endsWith(".db")
            } ?: return 0

            val sorted = backupFiles.sortedByDescending { it.lastModified() }
            val now = System.currentTimeMillis()
            val sevenDaysMillis = RETENTION_DAYS * 24 * 60 * 60 * 1000L
            var deletedCount = 0

            sorted.forEachIndexed { index, file ->
                // Always preserve at least the 2 newest backups
                if (index >= 2) {
                    val age = now - file.lastModified()
                    if (age > sevenDaysMillis || index >= RETENTION_DAYS) {
                        try {
                            if (file.delete()) {
                                deletedCount++
                                Log.d(TAG, "Pruned old backup file: ${file.name}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting old backup ${file.name}: ${e.message}")
                        }
                    }
                }
            }
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error in cleanupOldBackups: ${e.message}")
            0
        }
    }

    /**
     * Returns list of all available local backups, ordered by newest first.
     */
    fun getBackupsList(context: Context): List<BackupItem> {
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists() || !backupDir.isDirectory) return emptyList()

        val files = backupDir.listFiles { f ->
            f.isFile && f.name.startsWith(BACKUP_PREFIX) && f.name.endsWith(".db")
        } ?: return emptyList()

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN"))

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val size = file.length()
            val formattedSize = formatFileSize(size)
            val formattedDate = dateFormat.format(Date(file.lastModified()))

            BackupItem(
                fileName = file.name,
                file = file,
                sizeBytes = size,
                timestamp = file.lastModified(),
                formattedDate = formattedDate,
                formattedSize = formattedSize
            )
        }
    }

    /**
     * Retrieves overall backup status info.
     */
    fun getBackupStatusInfo(context: Context): BackupStatusInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDateStr = prefs.getString("last_backup_date_str", null) ?: "अद्याप बॅकअप घेतलेला नाही"
        val lastFileName = prefs.getString("last_backup_file_name", null) ?: "-"
        val lastSize = prefs.getLong("last_backup_size", 0L)
        val backupsList = getBackupsList(context)

        return BackupStatusInfo(
            lastBackupDateStr = lastDateStr,
            lastBackupFileName = lastFileName,
            lastBackupSizeBytes = lastSize,
            totalBackupsCount = backupsList.size,
            isAutoBackupActive = true,
            retentionDays = RETENTION_DAYS
        )
    }

    /**
     * Safely restores the database from a backup file.
     * Closes current connections, clears stale WAL files, and replaces the database file.
     */
    fun restoreBackup(context: Context, backupFile: File): Result<Boolean> {
        return try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                return Result.failure(Exception("बॅकअप फाइल अस्तित्वात नाही अथवा रिकामी आहे."))
            }

            // 1. Close active Room Database connections
            AppDatabase.closeDatabase()

            val dbFile = context.getDatabasePath("jayhind_mandal_db")
            val walFile = File(dbFile.parentFile, "jayhind_mandal_db-wal")
            val shmFile = File(dbFile.parentFile, "jayhind_mandal_db-shm")

            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 2. Overwrite main database file
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Re-initialize database instance
            AppDatabase.getDatabase(context)

            Log.d(TAG, "Database successfully restored from ${backupFile.name}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific backup item manually.
     */
    fun deleteBackup(backupItem: BackupItem): Boolean {
        return try {
            backupItem.file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Exports a backup copy to phone's public Downloads/JayHind_Mandal_Backups folder
     * so user can copy it via USB/computer or File Manager.
     */
    fun exportBackupToDownloads(context: Context, backupFile: File): String? {
        return try {
            val fileName = backupFile.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/JayHind_Mandal_Backups")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(backupFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    "Downloads/JayHind_Mandal_Backups/$fileName"
                } else null
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JayHind_Mandal_Backups")
                if (!dir.exists()) dir.mkdirs()
                val dest = File(dir, fileName)
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                dest.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export backup: ${e.message}")
            null
        }
    }

    /**
     * Shares backup file via standard Android Share sheet (WhatsApp, Drive, Email, etc.)
     */
    fun shareBackup(context: Context, backupFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "जय हिंद मंडळ डेटाबेस बॅकअप (${backupFile.name})")
                putExtra(Intent.EXTRA_TEXT, "जय हिंद तरुण मंडळ अर्जुनवाड - सुरक्षित डेटाबेस बॅकअप:\nफाइल: ${backupFile.name}\nतारीख: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(backupFile.lastModified()))}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "बॅकअप फाइल शेअर करा").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Share failed: ${e.message}")
            Toast.makeText(context, "शेअर करता आले नाही: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.ENGLISH, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
