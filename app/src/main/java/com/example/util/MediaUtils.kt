package com.example.util

import android.content.ContentValues
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object MediaUtils {

    /**
     * Converts a local Uri (content:// or file://) into a compact Base64 JPEG data string.
     * This guarantees that images selected from gallery are stored and synced globally
     * across all devices via Firestore without depending on local device file paths.
     */
    suspend fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 900,
        quality: Int = 78
    ): String? = withContext(Dispatchers.IO) {
        try {
            val uriStr = uri.toString()
            if (uriStr.startsWith("data:image/") || uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                return@withContext uriStr
            }

            // 1. Decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            // 2. Calculate sample size
            var inSampleSize = 1
            while (srcWidth / inSampleSize > maxDimension * 1.5 || srcHeight / inSampleSize > maxDimension * 1.5) {
                inSampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory & size optimization
            }
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            if (originalBitmap == null) return@withContext null

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

            // 6. Compress to JPEG Base64
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            Log.e("MediaUtils", "Failed to convert Uri to Base64: ${e.message}", e)
            null
        }
    }

    /**
     * Converts a Base64 image data string or raw Base64 string into an Android Bitmap.
     */
    fun base64ToBitmap(data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        return try {
            val base64Clean = if (data.contains("base64,")) {
                data.substringAfter("base64,")
            } else {
                data
            }
            val decodedBytes = Base64.decode(base64Clean.trim(), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
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
}
