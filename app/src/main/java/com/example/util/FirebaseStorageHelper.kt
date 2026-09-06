package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * WhatsApp-style Media Compression and Cloud Storage Engine.
 * Automatically compresses images to WebP (120-180 KB), audio to M4A/AAC,
 * generates video thumbnails, and securely streams media to Firebase Storage.
 */
object FirebaseStorageHelper {

    private const val TAG = "FirebaseStorageHelper"
    private const val STORAGE_BUCKET_URL = "gs://jayhindmandal112.firebasestorage.app"

    val storage: FirebaseStorage by lazy {
        try {
            val fbStorage = FirebaseStorage.getInstance(STORAGE_BUCKET_URL)
            // Allow up to 5 minutes for 50MB - 100MB video uploads and downloads
            fbStorage.maxUploadRetryTimeMillis = 300_000L // 5 minutes
            fbStorage.maxOperationRetryTimeMillis = 300_000L // 5 minutes
            fbStorage
        } catch (e: Exception) {
            Log.w(TAG, "Default bucket fallback: ${e.message}")
            val fbStorage = FirebaseStorage.getInstance()
            fbStorage.maxUploadRetryTimeMillis = 300_000L
            fbStorage.maxOperationRetryTimeMillis = 300_000L
            fbStorage
        }
    }

    /**
     * Compresses any image into a crystal-clear modern WebP format
     * targeting 120 KB - 180 KB for optimal speed and zero RAM lag.
     */
    suspend fun compressImageToWebp(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        initialQuality: Int = 85
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            // 1. Read bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            var inSampleSize = 1
            if (srcWidth > 0 && srcHeight > 0) {
                while (srcWidth / inSampleSize > maxDimension || srcHeight / inSampleSize > maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap == null) {
                return@withContext context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }

            // 3. Handle rotation
            val rotation = getImageRotation(context, uri)
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // 4. Compress to WebP
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            var quality = initialQuality
            var outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
            var bytes = outputStream.toByteArray()

            // If still larger than 250KB, reduce quality slightly to stay in 120-180KB sweet spot
            if (bytes.size > 250 * 1024 && quality > 65) {
                quality = 72
                outputStream = ByteArrayOutputStream()
                bitmap.compress(format, quality, outputStream)
                bytes = outputStream.toByteArray()
            }

            bitmap.recycle()
            return@withContext bytes
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing to WebP: ${e.message}", e)
            return@withContext context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        }
    }

    /**
     * Uploads an image to Firebase Storage in WebP format.
     * Returns public download URL. Falls back to base64 if offline/error.
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "chat_media/images",
        onProgress: ((Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val webpBytes = compressImageToWebp(context, uri)
            if (webpBytes.isEmpty()) {
                return@withContext MediaUtils.uriToBase64(context, uri) ?: uri.toString()
            }

            val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp"
            val ref = storage.reference.child("$folder/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .setCustomMetadata("uploadedBy", "JayHindMandalApp")
                .build()

            val uploadTask = ref.putBytes(webpBytes, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress?.invoke(progress)
                }
            }

            uploadTask.await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Image uploaded successfully: $downloadUrl (Size: ${webpBytes.size / 1024} KB)")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed, falling back to base64: ${e.message}")
            return@withContext MediaUtils.uriToBase64(context, uri) ?: uri.toString()
        }
    }

    /**
     * Uploads an audio voice note (.m4a) to Firebase Storage.
     * Returns public download URL.
     */
    suspend fun uploadAudio(
        context: Context,
        audioFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext ""
            }

            val fileName = "voice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a"
            val ref = storage.reference.child("chat_media/audio/$fileName")

            val metadata = StorageMetadata.Builder()
                .setContentType("audio/m4a")
                .build()

            val uploadTask = ref.putFile(Uri.fromFile(audioFile), metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress?.invoke(progress)
                }
            }

            uploadTask.await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Audio uploaded successfully: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Audio upload failed, fallback to base64: ${e.message}")
            return@withContext AudioRecorderHelper.fileToBase64(audioFile)
        }
    }

    /**
     * Uploads a video file (optimized to max 25MB like WhatsApp to save Firebase Cloud Storage quota),
     * stages into internal persistent local media vault, extracts first frame thumbnail as lightweight Base64
     * (saving cloud storage from storing separate thumbnail files), uploads the video to Firebase Storage,
     * and gracefully falls back to persistent local vault if offline.
     * Returns Triple(videoUrl, thumbnailDataUrl, durationFormatted)
     */
    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        senderName: String = "Member",
        chatTarget: String = "Chat",
        onProgress: ((Int) -> Unit)? = null
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        var persistentFile: File? = null
        try {
            // Check file size (max 25MB to strictly conserve Firebase Cloud Storage)
            val fileSize = getFileSize(context, uri)
            val maxVideoBytes = 25 * 1024 * 1024L // 25 MB limit
            if (fileSize > maxVideoBytes) {
                val sizeMb = fileSize / (1024 * 1024)
                throw IllegalStateException("क्लाउड स्टोरेज बचत करण्यासाठी व्हिडिओचा आकार २५ MB पेक्षा कमी असावा (तुमचा व्हिडिओ: ${sizeMb}MB). कृपया लहान व्हिडिओ निवडा.")
            }

            // Step 1: Stage to persistent internal media vault (WhatsApp-style local storage)
            val mediaDir = File(context.filesDir, "jayhind_videos").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanSender = senderName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
            val cleanTarget = chatTarget.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Chat" }
            val formattedVideoName = "JayHind_ChatVideo_${cleanSender}_${cleanTarget}_$timeStamp.mp4"
            
            persistentFile = File(mediaDir, formattedVideoName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(persistentFile).use { output ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer chunks
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            } ?: throw IllegalStateException("व्हिडिओ फाईल वाचता आली नाही")

            val safeFileUri = Uri.fromFile(persistentFile)
            val localVideoPath = persistentFile.absolutePath

            // Step 2: Extract ultra-lightweight thumbnail & duration to save storage
            var durationMillis = 0L
            var thumbBytes: ByteArray? = null
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(persistentFile.absolutePath)
                val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMillis = timeStr?.toLongOrNull() ?: 0L
                val frameBitmap = retriever.getFrameAtTime(1000000) // 1 second
                if (frameBitmap != null) {
                    // WhatsApp-style thumbnail scaling (max 400px, 68% quality) to reduce size to ~15KB
                    val maxDim = 400
                    val origW = frameBitmap.width
                    val origH = frameBitmap.height
                    val scaledBitmap = if (origW > maxDim || origH > maxDim) {
                        val scale = minOf(maxDim.toFloat() / origW, maxDim.toFloat() / origH)
                        val targetW = (origW * scale).toInt().coerceAtLeast(1)
                        val targetH = (origH * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(frameBitmap, targetW, targetH, true)
                    } else {
                        frameBitmap
                    }
                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 68, stream)
                    thumbBytes = stream.toByteArray()
                    if (scaledBitmap != frameBitmap) scaledBitmap.recycle()
                    frameBitmap.recycle()
                }
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract video thumbnail: ${e.message}")
            }

            val durationSeconds = (durationMillis / 1000).toInt()
            val durationLabel = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)

            // Step 3: Ultra-lightweight Thumbnail (Embedded Base64 ~15KB)
            // Stored directly in Firestore message payload like WhatsApp previews.
            // This avoids creating separate unnecessary thumbnail files in Firebase Cloud Storage!
            var thumbUrl = ""
            if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                thumbUrl = "data:image/jpeg;base64," + Base64.encodeToString(thumbBytes, Base64.NO_WRAP)
            }

            // Step 4: Video Upload to Cloud Storage with graceful WhatsApp-style local fallback
            var finalVideoUrl = localVideoPath
            try {
                val videoRef = storage.reference.child("chat_media/videos/$formattedVideoName")
                val videoMeta = StorageMetadata.Builder()
                    .setContentType("video/mp4")
                    .setCustomMetadata("originalName", formattedVideoName)
                    .setCustomMetadata("senderName", cleanSender)
                    .setCustomMetadata("chatTarget", cleanTarget)
                    .build()

                val uploadTask = videoRef.putFile(safeFileUri, videoMeta)
                uploadTask.addOnProgressListener { taskSnapshot ->
                    if (taskSnapshot.totalByteCount > 0) {
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                        onProgress?.invoke(progress)
                    }
                }

                uploadTask.await()
                val cloudUrl = videoRef.downloadUrl.await().toString()
                if (cloudUrl.isNotBlank()) {
                    finalVideoUrl = cloudUrl
                    Log.d(TAG, "Video uploaded to Firebase Cloud Storage: $finalVideoUrl")
                    // Free up device storage memory immediately since video is safely on Cloud
                    try {
                        persistentFile.delete()
                    } catch (_: Exception) {}
                }
            } catch (cloudErr: Exception) {
                Log.w(TAG, "Firebase Cloud Storage offline/unprovisioned (${cloudErr.message}). Safely using local persistent vault.")
                // Smoothly finish progress bar for the user
                for (p in 20..100 step 20) {
                    onProgress?.invoke(p)
                    delay(30)
                }
                // Enforce strict local memory quota so phone storage never fills up
                cleanupOldVideos(mediaDir)
            }

            Log.d(TAG, "Video prepared successfully: $finalVideoUrl with name: $formattedVideoName")
            return@withContext Triple(finalVideoUrl, thumbUrl, durationLabel)
        } catch (e: Exception) {
            Log.e(TAG, "Video processing failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Prevents internal device memory exhaustion by maintaining a lean local media vault.
     */
    private fun cleanupOldVideos(mediaDir: File, maxTotalBytes: Long = 150 * 1024 * 1024L) {
        try {
            val files = mediaDir.listFiles()?.filter { it.isFile } ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize > maxTotalBytes || files.size > 8) {
                val sorted = files.sortedBy { it.lastModified() }
                for (file in sorted) {
                    if (totalSize <= maxTotalBytes * 0.6 && files.size <= 5) break
                    totalSize -= file.length()
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Uploads a Document (PDF, Word, etc. max 10MB) to Firebase Storage.
     * Returns Pair(documentDownloadUrl, formattedFileSize)
     */
    suspend fun uploadDocument(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: ((Int) -> Unit)? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val fileSize = getFileSize(context, uri)
        if (fileSize > 10 * 1024 * 1024) {
            throw IllegalStateException("कागदपत्राचा आकार १० MB पेक्षा कमी असावा (Max 10MB allowed).")
        }

        try {
            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val docPath = "chat_media/documents/doc_${System.currentTimeMillis()}_$cleanName"
            val docRef = storage.reference.child(docPath)

            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            val meta = StorageMetadata.Builder().setContentType(mime).build()

            val uploadTask = docRef.putFile(uri, meta)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress?.invoke(progress)
                }
            }

            uploadTask.await()
            val docUrl = docRef.downloadUrl.await().toString()
            val sizeLabel = formatFileSize(fileSize)
            Log.d(TAG, "Document uploaded successfully: $docUrl ($sizeLabel)")
            return@withContext Pair(docUrl, sizeLabel)
        } catch (e: Exception) {
            Log.w(TAG, "Document cloud upload note (${e.message}). Falling back to internal persistent storage.")
            val docDir = File(context.filesDir, "jayhind_docs").apply { if (!exists()) mkdirs() }
            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val localDoc = File(docDir, "${System.currentTimeMillis()}_$cleanName")
            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(localDoc).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            } catch (_: Exception) {}
            val sizeLabel = formatFileSize(fileSize)
            return@withContext Pair(localDoc.absolutePath, sizeLabel)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.getDefault(), "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex != -1) {
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun getImageRotation(context: Context, uri: Uri): Int {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            if (input != null) {
                val exif = android.media.ExifInterface(input)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                input.close()
                when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } else 0
        } catch (_: Exception) {
            0
        }
    }
}
