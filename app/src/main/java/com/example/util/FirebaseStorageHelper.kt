package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * WhatsApp-style Media Compression and Cloud Storage Engine.
 * Automatically compresses images to WebP (120-180 KB), audio to M4A/AAC,
 * generates video thumbnails, and securely streams media to Firebase Storage.
 *
 * Folder structure:
 * posts/images/{phone}_{name}/IMG_{timestamp}_{index}.webp
 * chat_media/images/{phone}_{name}/IMG_{timestamp}.webp
 * chat_media/videos/{phone}_{name}/VID_{timestamp}.mp4
 */
object FirebaseStorageHelper {

    private const val TAG = "FirebaseStorageHelper"

    val storage: FirebaseStorage by lazy {
        try {
            val app = FirebaseApp.getInstance()
            FirebaseStorage.getInstance(app)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage initialization fallback: ${e.message}")
            FirebaseStorage.getInstance()
        }
    }

    /**
     * Sanitizes user identifier for Cloud & Google Drive compatibility.
     * E.g. "9822112233_Vaibhav_Chougule"
     */
    fun getUserSubfolder(user: User?): String {
        val rawPhone = user?.mobileNumber.orEmpty()
        val digits = rawPhone.filter { it.isDigit() }
        val phone = if (digits.length >= 10) digits.takeLast(10) else digits.ifBlank { "member" }
        val rawName = user?.fullName.orEmpty().trim().ifBlank { "user" }
        val cleanName = rawName.replace(Regex("[^a-zA-Z0-9_\\u0900-\\u097F]"), "_").take(25)
        return "${phone}_$cleanName"
    }

    /**
     * Generates a precise chronological timestamp for filenames:
     * e.g., "2026-09-05_04-26-15_PM"
     */
    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_hh-mm-ss_a", Locale.US)
        return sdf.format(Date())
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
     * Stores in: {folder}/{userSubfolder}/IMG_{timestamp}_{index}.webp
     * Returns public download URL. Falls back safely if offline.
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "chat_media/images",
        user: User? = null,
        fileIndex: Int = 1,
        onProgress: ((Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val webpBytes = compressImageToWebp(context, uri)
            if (webpBytes.isEmpty()) {
                return@withContext MediaUtils.uriToBase64(context, uri) ?: uri.toString()
            }

            val userFolder = getUserSubfolder(user)
            val timestamp = getFormattedTimestamp()
            val fileName = "IMG_${timestamp}_${String.format(Locale.US, "%02d", fileIndex)}.webp"
            val fullPath = "$folder/$userFolder/$fileName"
            val ref = storage.reference.child(fullPath)

            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .setCustomMetadata("uploadedBy", user?.mobileNumber ?: "member")
                .setCustomMetadata("timestamp", timestamp)
                .build()

            val uploadTask = ref.putBytes(webpBytes, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress?.invoke(progress)
                }
            }

            // Await completion with task snapshot
            val snapshot = uploadTask.await()
            val downloadUrl = snapshot.storage.downloadUrl.await().toString()
            Log.d(TAG, "Image uploaded successfully to $fullPath: $downloadUrl (Size: ${webpBytes.size / 1024} KB)")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage upload failed: ${e.message}", e)
            // If cloud storage fails or offline, provide a high-compression micro-webp string so the post or chat NEVER fails
            try {
                val microBytes = compressImageToWebp(context, uri, maxDimension = 640, initialQuality = 55)
                if (microBytes.isNotEmpty()) {
                    val base64 = android.util.Base64.encodeToString(microBytes, android.util.Base64.NO_WRAP)
                    return@withContext "data:image/webp;base64,$base64"
                }
            } catch (_: Exception) {}
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
        user: User? = null,
        onProgress: ((Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext ""
            }

            val userFolder = getUserSubfolder(user)
            val timestamp = getFormattedTimestamp()
            val fileName = "AUD_${timestamp}.m4a"
            val fullPath = "chat_media/audio/$userFolder/$fileName"
            val ref = storage.reference.child(fullPath)

            val metadata = StorageMetadata.Builder()
                .setContentType("audio/m4a")
                .setCustomMetadata("uploadedBy", user?.mobileNumber ?: "member")
                .build()

            val uploadTask = ref.putFile(Uri.fromFile(audioFile), metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress?.invoke(progress)
                }
            }

            val snapshot = uploadTask.await()
            val downloadUrl = snapshot.storage.downloadUrl.await().toString()
            Log.d(TAG, "Audio uploaded successfully: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Audio upload failed, fallback to base64: ${e.message}")
            return@withContext AudioRecorderHelper.fileToBase64(audioFile)
        }
    }

    /**
     * Uploads a video file (auto-compresses large videos WhatsApp-style to ~5-8MB),
     * extracts first frame thumbnail as WebP/JPEG, and uploads both to Firebase Storage.
     * Stores in: chat_media/videos/{phone}_{name}/VID_{timestamp}.mp4
     * Returns Triple(videoDownloadUrl, thumbnailDownloadUrl, durationFormatted)
     */
    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        user: User? = null,
        onProgress: ((Int) -> Unit)? = null
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        try {
            val originalSize = getFileSize(context, uri)
            // Allow videos up to 100MB because VideoCompressor will shrink them down
            if (originalSize > 100 * 1024 * 1024) {
                throw IllegalStateException("व्हिडिओचा मूळ आकार खूप मोठा आहे (कमाल 100MB पर्यंत अनुमती आहे).")
            }

            // WhatsApp-style automatic compression step
            Log.d(TAG, "Starting video auto-compression for size: ${originalSize / 1024 / 1024} MB")
            val compressedVideoFile = VideoCompressor.compressVideo(context, uri) { compressionProgress ->
                // Map compression to 0-40% of overall progress
                val mappedProgress = (compressionProgress * 0.4f).toInt()
                onProgress?.invoke(mappedProgress)
            }
            val uploadUri = Uri.fromFile(compressedVideoFile)

            // Extract thumbnail & duration
            var durationMillis = 0L
            var thumbBytes: ByteArray? = null
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uploadUri)
                val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMillis = timeStr?.toLongOrNull() ?: 0L
                val frameBitmap = retriever.getFrameAtTime(1000000) // 1 second
                if (frameBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    frameBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    thumbBytes = stream.toByteArray()
                    frameBitmap.recycle()
                }
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract video thumbnail: ${e.message}")
            }

            val durationSeconds = (durationMillis / 1000).toInt()
            val durationLabel = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)

            val userFolder = getUserSubfolder(user)
            val timestamp = getFormattedTimestamp()

            // Upload thumbnail safely (if thumb fails, do not block main video)
            var thumbUrl = ""
            if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                try {
                    val thumbFileName = "THUMB_${timestamp}.jpg"
                    val thumbRef = storage.reference.child("chat_media/videos/$userFolder/thumbs/$thumbFileName")
                    val thumbMeta = StorageMetadata.Builder().setContentType("image/jpeg").build()
                    val thumbSnapshot = thumbRef.putBytes(thumbBytes, thumbMeta).await()
                    thumbUrl = thumbSnapshot.storage.downloadUrl.await().toString()
                } catch (te: Exception) {
                    Log.w(TAG, "Thumbnail upload skipped: ${te.message}")
                }
            }

            // Upload compressed video file
            val videoFileName = "VID_${timestamp}.mp4"
            val videoFullPath = "chat_media/videos/$userFolder/$videoFileName"
            val videoRef = storage.reference.child(videoFullPath)
            val videoMeta = StorageMetadata.Builder()
                .setContentType("video/mp4")
                .setCustomMetadata("uploadedBy", user?.mobileNumber ?: "member")
                .setCustomMetadata("duration", durationLabel)
                .build()

            val uploadTask = videoRef.putFile(uploadUri, videoMeta)
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    // Map upload to 40-100% of overall progress
                    val uploadPercent = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    val overallProgress = 40 + (uploadPercent * 0.6f).toInt()
                    onProgress?.invoke(overallProgress)
                }
            }

            val videoSnapshot = uploadTask.await()
            val videoUrl = videoSnapshot.storage.downloadUrl.await().toString()
            Log.d(TAG, "Video uploaded successfully to $videoFullPath: $videoUrl")

            // Clean up temporary compressed file in cache
            try {
                if (compressedVideoFile.exists() && compressedVideoFile.absolutePath.contains("cache")) {
                    compressedVideoFile.delete()
                }
            } catch (_: Exception) {}

            return@withContext Triple(videoUrl, thumbUrl, durationLabel)
        } catch (e: Exception) {
            Log.e(TAG, "Video upload failed: ${e.message}", e)
            throw e
        }
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
        try {
            val fileSize = getFileSize(context, uri)
            if (fileSize > 10 * 1024 * 1024) {
                throw IllegalStateException("कागदपत्राचा आकार १० MB पेक्षा कमी असावा (Max 10MB allowed).")
            }

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
            Log.e(TAG, "Document upload failed: ${e.message}", e)
            throw e
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
