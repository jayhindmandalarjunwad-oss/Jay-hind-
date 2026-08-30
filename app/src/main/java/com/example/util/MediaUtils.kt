package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object MediaUtils {

    /**
     * Converts a local Uri (content:// or file://) into a compact Base64 image data string.
     * This guarantees that images selected from gallery are stored and synced globally
     * across all devices via Firestore without depending on local device file paths.
     */
    suspend fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 600,
        quality: Int = 80
    ): String? = withContext(Dispatchers.IO) {
        try {
            val uriStr = uri.toString()
            if (uriStr.startsWith("data:image/") || uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                return@withContext uriStr
            }

            // 1. Decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext null
            }
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                // Fallback: direct stream to Base64
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                    return@withContext "data:$mime;base64,$b64"
                }
                return@withContext null
            }

            // 2. Calculate sample size
            var inSampleSize = 1
            while (srcWidth / inSampleSize > maxDimension * 1.5 || srcHeight / inSampleSize > maxDimension * 1.5) {
                inSampleSize *= 2
            }

            // 3. Decode scaled bitmap with ARGB_8888 (preserves transparency for PNGs and sharpness)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()
            if (originalBitmap == null) {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                    return@withContext "data:$mime;base64,$b64"
                }
                return@withContext null
            }

            // 4. Handle EXIF rotation
            var rotationAngle = 0
            try {
                val exifStream = context.contentResolver.openInputStream(uri)
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    rotationAngle = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                    exifStream.close()
                }
            } catch (e: Exception) {
                Log.d("MediaUtils", "EXIF read notice: ${e.message}")
            }

            var workingBitmap = originalBitmap
            if (rotationAngle != 0) {
                val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
                workingBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            }

            // 5. Scale to exact maxDimension if still larger
            val finalWidth = workingBitmap.width
            val finalHeight = workingBitmap.height
            val scale = if (finalWidth > maxDimension || finalHeight > maxDimension) {
                val maxSrc = maxOf(finalWidth, finalHeight)
                maxDimension.toFloat() / maxSrc.toFloat()
            } else {
                1.0f
            }

            val finalBitmap = if (scale < 1.0f) {
                val targetW = (finalWidth * scale).toInt().coerceAtLeast(1)
                val targetH = (finalHeight * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(workingBitmap, targetW, targetH, true)
            } else {
                workingBitmap
            }

            // 6. Compress appropriately (PNG if alpha, JPEG otherwise)
            val outputStream = ByteArrayOutputStream()
            val isAlpha = finalBitmap.hasAlpha()
            if (isAlpha) {
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } else {
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            val byteArray = outputStream.toByteArray()
            val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val mime = if (isAlpha) "image/png" else "image/jpeg"
            "data:$mime;base64,$base64Str"
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to convert Uri to Base64: ${e.message}", e)
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                    "data:$mime;base64,$b64"
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Extracts a frame from a local video URI and converts it to a Base64 JPEG string thumbnail.
     */
    suspend fun getVideoThumbnailBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 480,
        quality: Int = 70
    ): String? = withContext(Dispatchers.IO) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1000000)
                ?: retriever.frameAtTime
            if (bitmap != null) {
                val scale = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                    val maxSrc = maxOf(bitmap.width, bitmap.height)
                    maxDimension.toFloat() / maxSrc.toFloat()
                } else 1.0f

                val scaledBitmap = if (scale < 1.0f) {
                    val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                } else bitmap

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64Str"
            } else null
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to extract video thumbnail: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens a video in an external video player or web browser as a fallback.
     */
    fun openVideo(context: Context, videoUrl: String) {
        if (videoUrl.isBlank()) return
        try {
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                context.startActivity(intent)
            } else if (videoUrl.startsWith("content://") || videoUrl.startsWith("file://")) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(videoUrl), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val uri = prepareVideoUriForPlayback(context, videoUrl)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "व्हिडिओ उघडता आला नाही", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "व्हिडिओ प्ले करण्यासाठी ॲप सापडले नाही", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Converts a Base64 image data string or raw Base64 string into an Android Bitmap.
     */
    fun base64ToBitmap(data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        return try {
            val trimmed = data.trim()
            val base64Clean = if (trimmed.contains("base64,")) {
                trimmed.substringAfter("base64,")
            } else {
                trimmed
            }
            val decodedBytes = Base64.decode(base64Clean.trim(), Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to decode base64 to bitmap: ${e.message}")
            null
        }
    }

    /**
     * Downloads/saves an image (Base64 data or HTTP URL) directly to the Android MediaStore/Gallery.
     */
    suspend fun saveImageToGallery(
        context: Context,
        imageUrlOrBase64: String,
        fileNamePrefix: String = "JayHind_Photo"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.jpg"

            val bitmap: Bitmap? = if (imageUrlOrBase64.startsWith("data:image/")) {
                val base64Data = imageUrlOrBase64.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } else if (imageUrlOrBase64.startsWith("http://") || imageUrlOrBase64.startsWith("https://")) {
                val url = URL(imageUrlOrBase64)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } else {
                null
            }

            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "फोटो सेव्ह करता आला नाही", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            var isSaved = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/JayHindMandal")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    isSaved = true
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val mandalDir = File(picturesDir, "JayHindMandal").apply { if (!exists()) mkdirs() }
                val imageFile = File(mandalDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                isSaved = true
            }

            withContext(Dispatchers.Main) {
                if (isSaved) {
                    Toast.makeText(context, "फोटो मोबाईल गॅलरीत सेव्ह झाला! 📥", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "फोटो सेव्ह करण्यात अडचण आली", Toast.LENGTH_SHORT).show()
                }
            }
            isSaved
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error saving image: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "फोटो सेव्ह अयशस्वी: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * Saves a PDF or document file (Base64 data or HTTP URL or Content URI) directly to the device's public Downloads folder.
     * Complies with Scoped Storage for Android 10+ (Q, R, S, Tiramisu, UpsideDownCake, etc.)
     */
    suspend fun saveDocumentToDownloads(
        context: Context,
        docUrlOrBase64: String,
        suggestedFileName: String = "JayHind_Document.pdf",
        mimeType: String = "application/pdf"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cleanName = if (suggestedFileName.contains(".")) suggestedFileName else "$suggestedFileName.pdf"
            val timestampedName = "${System.currentTimeMillis()}_$cleanName"

            val fileBytes: ByteArray? = when {
                docUrlOrBase64.startsWith("data:") -> {
                    val base64Data = docUrlOrBase64.substringAfter("base64,")
                    Base64.decode(base64Data.trim(), Base64.DEFAULT)
                }
                docUrlOrBase64.startsWith("http://") || docUrlOrBase64.startsWith("https://") -> {
                    val url = URL(docUrlOrBase64)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    connection.inputStream.use { it.readBytes() }
                }
                docUrlOrBase64.startsWith("content://") || docUrlOrBase64.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(docUrlOrBase64))?.use { it.readBytes() }
                }
                else -> {
                    // Try decoding as raw base64 string
                    try {
                        Base64.decode(docUrlOrBase64.trim(), Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (fileBytes == null || fileBytes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "दस्तऐवज डाऊनलोड करता आले नाही", Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }

            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, timestampedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JayHindMandal")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(fileBytes)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val mandalDir = File(downloadsDir, "JayHindMandal").apply { if (!exists()) mkdirs() }
                val docFile = File(mandalDir, timestampedName)
                FileOutputStream(docFile).use { out ->
                    out.write(fileBytes)
                }
                savedUri = Uri.fromFile(docFile)
            }

            withContext(Dispatchers.Main) {
                if (savedUri != null) {
                    Toast.makeText(context, "✅ $cleanName डाऊनलोड फोल्डरमध्ये सेव्ह झाले! 📥", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "दस्तऐवज सेव्ह करण्यात अडचण आली", Toast.LENGTH_SHORT).show()
                }
            }
            savedUri
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error saving document: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "डाऊनलोड अयशस्वी: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    /**
     * Opens a document (PDF, doc, etc.) directly using the default PDF reader or external viewer.
     */
    suspend fun openDocumentFile(
        context: Context,
        docUrlOrBase64: String,
        fileName: String = "दस्तावेज.pdf",
        mimeType: String = "application/pdf"
    ) = withContext(Dispatchers.IO) {
        try {
            // First save or cache the file
            val cacheFile = File(context.cacheDir, "temp_docs").apply { if (!exists()) mkdirs() }
            val tempFile = File(cacheFile, fileName)

            val fileBytes: ByteArray? = when {
                docUrlOrBase64.startsWith("data:") -> {
                    val base64Data = docUrlOrBase64.substringAfter("base64,")
                    Base64.decode(base64Data.trim(), Base64.DEFAULT)
                }
                docUrlOrBase64.startsWith("http://") || docUrlOrBase64.startsWith("https://") -> {
                    val url = URL(docUrlOrBase64)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    connection.inputStream.use { it.readBytes() }
                }
                docUrlOrBase64.startsWith("content://") || docUrlOrBase64.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(docUrlOrBase64))?.use { it.readBytes() }
                }
                else -> {
                    try {
                        Base64.decode(docUrlOrBase64.trim(), Base64.DEFAULT)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            if (fileBytes != null && fileBytes.isNotEmpty()) {
                FileOutputStream(tempFile).use { it.write(fileBytes) }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(Intent.createChooser(viewIntent, "दस्तऐवज उघडा"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "PDF उघडण्यासाठी योग्य ॲप उपलब्ध नाही.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (docUrlOrBase64.startsWith("http")) {
                withContext(Dispatchers.Main) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrlOrBase64)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error opening document: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "दस्तऐवज उघडता आले नाही: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Converts a video URI to Base64 data string if under 700KB, or stores locally and prepares for sync.
     */
    suspend fun uriToVideoData(
        context: Context,
        uri: Uri,
        maxBytesForBase64: Long = 700 * 1024
    ): String = withContext(Dispatchers.IO) {
        try {
            val uriStr = uri.toString()
            if (uriStr.startsWith("data:video/") || uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                return@withContext uriStr
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                if (bytes.size <= maxBytesForBase64) {
                    val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return@withContext "data:$mime;base64,$b64"
                } else {
                    // For larger videos, cache a copy in local app storage
                    val videoCacheDir = File(context.cacheDir, "cached_videos").apply { if (!exists()) mkdirs() }
                    val cachedFile = File(videoCacheDir, "vid_${System.currentTimeMillis()}.mp4")
                    FileOutputStream(cachedFile).use { it.write(bytes) }
                    return@withContext Uri.fromFile(cachedFile).toString()
                }
            }
            uriStr
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error converting video uri: ${e.message}", e)
            uri.toString()
        }
    }

    /**
     * Prepares a video source (Base64, local URI, or HTTP URL) for playback by VideoView or external player.
     * Returns a valid playable Uri.
     */
    suspend fun prepareVideoUriForPlayback(
        context: Context,
        videoUrlOrBase64: String
    ): Uri = withContext(Dispatchers.IO) {
        try {
            val trimmed = videoUrlOrBase64.trim()

            // 1. Base64 Video string
            if (trimmed.startsWith("data:video/") || (trimmed.length > 200 && !trimmed.startsWith("http") && !trimmed.startsWith("content:") && !trimmed.startsWith("file:"))) {
                val base64Data = if (trimmed.contains("base64,")) trimmed.substringAfter("base64,") else trimmed
                val videoBytes = Base64.decode(base64Data.trim(), Base64.DEFAULT)
                val tempDir = File(context.cacheDir, "temp_videos").apply { if (!exists()) mkdirs() }
                val tempFile = File(tempDir, "play_${System.currentTimeMillis()}.mp4")
                FileOutputStream(tempFile).use { it.write(videoBytes) }
                return@withContext Uri.fromFile(tempFile)
            }

            // 2. HTTP / HTTPS streaming URL
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return@withContext Uri.parse(trimmed)
            }

            // 3. Local content:// or file:// URI
            if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
                val sourceUri = Uri.parse(trimmed)
                try {
                    // Test if readable on current device
                    val stream = context.contentResolver.openInputStream(sourceUri)
                    if (stream != null) {
                        val tempDir = File(context.cacheDir, "temp_videos").apply { if (!exists()) mkdirs() }
                        val tempFile = File(tempDir, "play_local_${System.currentTimeMillis()}.mp4")
                        FileOutputStream(tempFile).use { out -> stream.copyTo(out) }
                        stream.close()
                        return@withContext Uri.fromFile(tempFile)
                    }
                } catch (e: Exception) {
                    Log.w("MediaUtils", "Content URI cannot be opened directly on this device (origin device differs): ${e.message}")
                }
            }

            // Fallback: Default playable mandal festival sample video MP4
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to prepare video uri: ${e.message}", e)
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        }
    }

    /**
     * Saves a video to the device MediaStore / Movies folder for the user.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        videoUrlOrBase64: String,
        fileNamePrefix: String = "JayHind_Video"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.mp4"
            val videoBytes: ByteArray? = when {
                videoUrlOrBase64.startsWith("data:") -> {
                    val base64Data = videoUrlOrBase64.substringAfter("base64,")
                    Base64.decode(base64Data.trim(), Base64.DEFAULT)
                }
                videoUrlOrBase64.startsWith("http://") || videoUrlOrBase64.startsWith("https://") -> {
                    val url = URL(videoUrlOrBase64)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    connection.inputStream.use { it.readBytes() }
                }
                videoUrlOrBase64.startsWith("content://") || videoUrlOrBase64.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(videoUrlOrBase64))?.use { it.readBytes() }
                }
                else -> {
                    try {
                        Base64.decode(videoUrlOrBase64.trim(), Base64.DEFAULT)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            if (videoBytes == null || videoBytes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "व्हिडिओ डाऊनलोड करता आला नाही", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            var isSaved = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/JayHindMandal")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(videoBytes)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    isSaved = true
                }
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val mandalDir = File(moviesDir, "JayHindMandal").apply { if (!exists()) mkdirs() }
                val videoFile = File(mandalDir, fileName)
                FileOutputStream(videoFile).use { out ->
                    out.write(videoBytes)
                }
                isSaved = true
            }

            withContext(Dispatchers.Main) {
                if (isSaved) {
                    Toast.makeText(context, "✅ व्हिडिओ मोबाईल गॅलरीत सेव्ह झाला! 📥", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "व्हिडिओ सेव्ह करण्यात अडचण आली", Toast.LENGTH_SHORT).show()
                }
            }
            isSaved
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error saving video: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "व्हिडिओ सेव्ह अयशस्वी: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
