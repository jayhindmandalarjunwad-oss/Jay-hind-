package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

data class FirebaseStorageUsageStats(
    val totalQuotaBytes: Long = 5L * 1024L * 1024L * 1024L, // 5.0 GB Free Spark Plan Quota
    val usedBytes: Long = 0L,
    val remainingBytes: Long = 5L * 1024L * 1024L * 1024L,
    val usedPercentage: Float = 0f,
    val photosBytes: Long = 0L,
    val voiceNotesBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val totalFilesCount: Int = 0,
    val isNearLimit: Boolean = false,
    val formattedTotal: String = "५.०० GB",
    val formattedUsed: String = "० MB",
    val formattedRemaining: String = "५.०० GB"
)

object FirebaseMemoryManager {

    private const val SPARK_STORAGE_LIMIT_BYTES: Long = 5L * 1024L * 1024L * 1024L // 5 GB

    suspend fun calculateStorageUsage(context: Context): FirebaseStorageUsageStats = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)

            var photosBytes = 0L
            var voiceBytes = 0L
            var docsBytes = 0L
            var videosBytes = 0L
            var filesCount = 0

            // 1. Post images
            val posts = db.postDao().getAllPostsDirect()
            for (p in posts) {
                val urls = p.imageUrlsJson ?: ""
                if (urls.isNotBlank()) {
                    val count = urls.split("http").size - 1
                    if (count > 0) {
                        // Average compressed WebP post image on Firebase is ~250 KB
                        photosBytes += count * 250L * 1024L
                        filesCount += count
                    }
                }
            }

            // 2. Banner images
            val banners = db.bannerDao().getAllBannersDirect()
            for (b in banners) {
                if (!b.imageUrl.isNullOrBlank()) {
                    photosBytes += 350L * 1024L // ~350 KB
                    filesCount++
                }
            }

            // 3. Event banners
            val events = db.eventDao().getAllEventsDirect()
            for (e in events) {
                if (!e.imageUrl.isNullOrBlank()) {
                    photosBytes += 300L * 1024L // ~300 KB
                    filesCount++
                }
            }

            // 4. Gallery Photos
            val photos = db.galleryDao().getAllPhotosDirect()
            for (ph in photos) {
                if (ph.imageUrl.isNotBlank()) {
                    photosBytes += 400L * 1024L // ~400 KB
                    filesCount++
                }
            }

            // 5. Chat Messages (Photos, Voice Notes, Documents/PDF, Videos)
            val chats = db.chatDao().getAllChatMessagesDirect()
            for (c in chats) {
                val url = c.attachmentUrl ?: ""
                if (url.isNotBlank()) {
                    filesCount++
                    when (c.attachmentType) {
                        "VOICE" -> voiceBytes += 120L * 1024L // ~120 KB audio note
                        "DOCUMENT" -> docsBytes += 600L * 1024L // ~600 KB PDF
                        "VIDEO" -> videosBytes += 3L * 1024L * 1024L // ~3 MB compressed video
                        else -> photosBytes += 220L * 1024L // ~220 KB chat image
                    }
                }
            }

            // 6. Local SQLite DB File size
            var dbSize = 0L
            try {
                val dbFile = context.getDatabasePath("jayhind_mandal_db")
                if (dbFile.exists()) {
                    dbSize = dbFile.length()
                }
            } catch (_: Exception) {}

            val totalUsed = photosBytes + voiceBytes + docsBytes + videosBytes + dbSize
            val remaining = (SPARK_STORAGE_LIMIT_BYTES - totalUsed).coerceAtLeast(0L)
            val percentage = ((totalUsed.toDouble() / SPARK_STORAGE_LIMIT_BYTES.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)

            FirebaseStorageUsageStats(
                totalQuotaBytes = SPARK_STORAGE_LIMIT_BYTES,
                usedBytes = totalUsed,
                remainingBytes = remaining,
                usedPercentage = percentage,
                photosBytes = photosBytes,
                voiceNotesBytes = voiceBytes,
                documentsBytes = docsBytes,
                videosBytes = videosBytes,
                databaseBytes = dbSize,
                totalFilesCount = filesCount,
                isNearLimit = percentage >= 80f,
                formattedTotal = "५.०० GB",
                formattedUsed = formatBytes(totalUsed),
                formattedRemaining = formatBytes(remaining)
            )
        } catch (e: Exception) {
            FirebaseStorageUsageStats()
        }
    }

    fun formatBytes(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1024L * 1024L * 1024L -> "${df.format(bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024L * 1024L -> "${df.format(bytes.toDouble() / (1024.0 * 1024.0))} MB"
            bytes >= 1024L -> "${df.format(bytes.toDouble() / 1024.0)} KB"
            else -> "$bytes Bytes"
        }
    }
}
