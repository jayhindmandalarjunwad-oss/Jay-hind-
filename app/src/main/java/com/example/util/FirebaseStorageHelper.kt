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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
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
     * Uploads a video file (supports up to 100MB for WhatsApp-style sharing),
     * stages into internal local cache to prevent Android content provider stream breakdown,
     * extracts first frame thumbnail, and uploads both to Firebase Storage with resumable retry session.
     * Returns Triple(videoDownloadUrl, thumbnailDownloadUrl, durationFormatted)
     */
    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        senderName: String = "Member",
        chatTarget: String = "Chat",
        onProgress: ((Int) -> Unit)? = null
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        var stagedFile: File? = null
        try {
            // Check file size (max 100MB)
            val fileSize = getFileSize(context, uri)
            if (fileSize > 100 * 1024 * 1024) {
                throw IllegalStateException("व्हिडिओचा आकार १०० MB पेक्षा कमी असावा (Max 100MB allowed).")
            }

            // Step 1: Stage to safe internal cache file
            val stageDir = File(context.cacheDir, "chat_staging_videos").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanSender = senderName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
            val cleanTarget = chatTarget.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Chat" }
            val formattedVideoName = "JayHind_ChatVideo_${cleanSender}_${cleanTarget}_$timeStamp.mp4"
            
            stagedFile = File(stageDir, formattedVideoName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer chunks
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            } ?: throw IllegalStateException("व्हिडिओ फाईल वाचता आली नाही")

            val safeFileUri = Uri.fromFile(stagedFile)

            // Step 2: Extract thumbnail & duration from staged file
            var durationMillis = 0L
            var thumbBytes: ByteArray? = null
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(stagedFile.absolutePath)
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

            // Step 3: Upload thumbnail
            var thumbUrl = ""
            if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                val thumbFileName = "JayHind_Thumb_${cleanSender}_$timeStamp.jpg"
                val thumbRef = storage.reference.child("chat_media/videos/thumbs/$thumbFileName")
                val thumbMeta = StorageMetadata.Builder().setContentType("image/jpeg").build()
                thumbRef.putBytes(thumbBytes, thumbMeta).await()
                thumbUrl = thumbRef.downloadUrl.await().toString()
            }

            // Step 4: Upload video file with Resumable Session and 5-min retry policy
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
            val videoUrl = videoRef.downloadUrl.await().toString()
            Log.d(TAG, "Video uploaded successfully: $videoUrl with name: $formattedVideoName")
            return@withContext Triple(videoUrl, thumbUrl, durationLabel)
        } catch (e: Exception) {
            Log.e(TAG, "Video upload failed: ${e.message}", e)
            throw e
        } finally {
            try {
                stagedFile?.delete()
            } catch (_: Exception) {}
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
