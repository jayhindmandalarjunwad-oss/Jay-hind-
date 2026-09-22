package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SaffronPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("मुक्त", null),
    SQUARE("१:१ प्रोफाइल", 1.0f),
    LANDSCAPE_16_9("१६:९ आडवा", 16f / 9f),
    PORTRAIT_4_5("४:५ उभा", 4f / 5f)
}

/**
 * Modern Jetpack Compose Image Cropper and Editor Dialog.
 * Supports:
 * 1. Pinch to zoom & drag to pan
 * 2. 90-degree image rotation
 * 3. Aspect ratio selection (1:1 Square, 16:9, 4:5, Free)
 * 4. Circular guide overlay option for profile photos
 * 5. High-quality cropped bitmap export to file Uri
 */
@Composable
fun ImageCropperDialog(
    sourceUri: Uri,
    initialRatio: CropAspectRatio = CropAspectRatio.FREE,
    isCircleCrop: Boolean = false,
    onDismiss: () -> Unit,
    onImageCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var selectedRatio by remember { mutableStateOf(initialRatio) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isExporting by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Load original bitmap
    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                var loadedBmp: Bitmap? = null
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    loadedBmp = BitmapFactory.decodeStream(stream, null, opts)
                }

                // Check and correct EXIF orientation if needed
                if (loadedBmp != null) {
                    try {
                        context.contentResolver.openInputStream(sourceUri)?.use { exifStream ->
                            val exif = ExifInterface(exifStream)
                            val orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                            }
                            if (!matrix.isIdentity) {
                                val rotated = Bitmap.createBitmap(
                                    loadedBmp!!, 0, 0,
                                    loadedBmp!!.width, loadedBmp!!.height,
                                    matrix, true
                                )
                                if (rotated != loadedBmp) {
                                    loadedBmp!!.recycle()
                                    loadedBmp = rotated
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // ignore exif parsing errors if not present
                    }
                }

                originalBitmap = loadedBmp
            } catch (e: Exception) {
                android.util.Log.e("ImageCropperDialog", "Failed to decode source bitmap: ${e.message}")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF111827)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, enabled = !isExporting) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "रद्द करा",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "फोटो क्रॉप व एडिट करा",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = originalBitmap ?: return@Button
                            isExporting = true
                            // Export cropped/rotated bitmap
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val cropped = processAndCropBitmap(
                                        src = bmp,
                                        rotationAngle = rotationAngle,
                                        scale = scale,
                                        offset = offset,
                                        selectedRatio = selectedRatio,
                                        canvasW = canvasSize.width,
                                        canvasH = canvasSize.height
                                    )
                                    val tempFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.webp")
                                    FileOutputStream(tempFile).use { out ->
                                        cropped.compress(Bitmap.CompressFormat.WEBP, 90, out)
                                    }
                                    val resultUri = Uri.fromFile(tempFile)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        onImageCropped(resultUri)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ImageCropper", "Export crop failed: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        onImageCropped(sourceUri)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(20.dp),
                        enabled = originalBitmap != null && !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("पूर्ण झाले (Done)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Main Interactive Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = originalBitmap
                    if (bmp == null) {
                        CircularProgressIndicator(color = SaffronPrimary)
                    } else {
                        // Gesture detector for pinch zoom and drag
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageBmp = remember(bmp, rotationAngle) {
                                if (rotationAngle % 360f != 0f) {
                                    val matrix = Matrix().apply { postRotate(rotationAngle) }
                                    Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                } else {
                                    bmp
                                }
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { intSize ->
                                        if (intSize.width > 0 && intSize.height > 0) {
                                            canvasSize = Size(intSize.width.toFloat(), intSize.height.toFloat())
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                if (canvasSize.width != canvasWidth || canvasSize.height != canvasHeight) {
                                    canvasSize = Size(canvasWidth, canvasHeight)
                                }

                                // Draw image with scale & offset
                                val bmpWidth = imageBmp.width.toFloat()
                                val bmpHeight = imageBmp.height.toFloat()

                                val baseScale = min(canvasWidth / bmpWidth, canvasHeight / bmpHeight) * 0.85f
                                val finalScale = baseScale * scale

                                val drawW = bmpWidth * finalScale
                                val drawH = bmpHeight * finalScale

                                val drawLeft = (canvasWidth - drawW) / 2f + offset.x
                                val drawTop = (canvasHeight - drawH) / 2f + offset.y

                                drawImage(
                                    image = imageBmp.asImageBitmap(),
                                    dstOffset = IntOffset(drawLeft.roundToInt(), drawTop.roundToInt()),
                                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())
                                )

                                // Crop Guide Overlay
                                val cropBoxW: Float
                                val cropBoxH: Float
                                val ratio = selectedRatio.ratio
                                if (ratio != null) {
                                    if (ratio >= 1.0f) {
                                        cropBoxW = canvasWidth * 0.85f
                                        cropBoxH = cropBoxW / ratio
                                    } else {
                                        cropBoxH = canvasHeight * 0.65f
                                        cropBoxW = cropBoxH * ratio
                                    }
                                } else {
                                    cropBoxW = canvasWidth * 0.85f
                                    cropBoxH = canvasHeight * 0.65f
                                }

                                val cropBoxLeft = (canvasWidth - cropBoxW) / 2f
                                val cropBoxTop = (canvasHeight - cropBoxH) / 2f

                                // Darken outside crop frame
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    topLeft = Offset.Zero,
                                    size = Size(canvasWidth, cropBoxTop)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    topLeft = Offset(0f, cropBoxTop + cropBoxH),
                                    size = Size(canvasWidth, canvasHeight - (cropBoxTop + cropBoxH))
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    topLeft = Offset(0f, cropBoxTop),
                                    size = Size(cropBoxLeft, cropBoxH)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    topLeft = Offset(cropBoxLeft + cropBoxW, cropBoxTop),
                                    size = Size(canvasWidth - (cropBoxLeft + cropBoxW), cropBoxH)
                                )

                                // White bounding box with corner guides
                                drawRect(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = Offset(cropBoxLeft, cropBoxTop),
                                    size = Size(cropBoxW, cropBoxH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )

                                // Optional Circular Crop Guide for Profile Avatars
                                if (isCircleCrop && selectedRatio == CropAspectRatio.SQUARE) {
                                    val radius = min(cropBoxW, cropBoxH) / 2f
                                    val center = Offset(cropBoxLeft + cropBoxW / 2f, cropBoxTop + cropBoxH / 2f)
                                    drawCircle(
                                        color = SaffronPrimary,
                                        radius = radius,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Tool Bar: Ratio selector & Rotation Controls
                Surface(
                    color = Color(0xFF1F2937),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Ratio Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CropAspectRatio.values().forEach { aspect ->
                                FilterChip(
                                    selected = selectedRatio == aspect,
                                    onClick = { selectedRatio = aspect },
                                    label = { Text(aspect.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SaffronPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF374151),
                                        labelColor = Color(0xFFD1D5DB)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Rotate 90deg, Reset Zoom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    rotationAngle = (rotationAngle + 90f) % 360f
                                }
                            ) {
                                Icon(imageVector = Icons.Default.RotateRight, contentDescription = "फिरवा", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("९०° फिरवा (Rotate)", color = Color.White, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "रीसेट", tint = Color(0xFF9CA3AF))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("रीसेट (Reset)", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun processAndCropBitmap(
    src: Bitmap,
    rotationAngle: Float,
    scale: Float,
    offset: Offset,
    selectedRatio: CropAspectRatio,
    canvasW: Float,
    canvasH: Float
): Bitmap {
    // 1. Rotate if needed
    val rotated = if (rotationAngle % 360f != 0f) {
        val matrix = Matrix().apply { postRotate(rotationAngle) }
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    } else {
        src
    }

    val bmpWidth = rotated.width.toFloat()
    val bmpHeight = rotated.height.toFloat()

    val effCanvasW = if (canvasW > 0f) canvasW else 1000f
    val effCanvasH = if (canvasH > 0f) canvasH else 1000f

    // Crop Guide Box dimensions on canvas
    val cropBoxW: Float
    val cropBoxH: Float
    val ratio = selectedRatio.ratio
    if (ratio != null) {
        if (ratio >= 1.0f) {
            cropBoxW = effCanvasW * 0.85f
            cropBoxH = cropBoxW / ratio
        } else {
            cropBoxH = effCanvasH * 0.65f
            cropBoxW = cropBoxH * ratio
        }
    } else {
        cropBoxW = effCanvasW * 0.85f
        cropBoxH = effCanvasH * 0.65f
    }

    val cropBoxLeft = (effCanvasW - cropBoxW) / 2f
    val cropBoxTop = (effCanvasH - cropBoxH) / 2f

    // Calculate image position on canvas
    val baseScale = min(effCanvasW / bmpWidth, effCanvasH / bmpHeight) * 0.85f
    val finalScale = (baseScale * scale).coerceAtLeast(0.0001f)

    val drawW = bmpWidth * finalScale
    val drawH = bmpHeight * finalScale

    val drawLeft = (effCanvasW - drawW) / 2f + offset.x
    val drawTop = (effCanvasH - drawH) / 2f + offset.y

    // Map crop box corners from Canvas space back into Bitmap pixel space
    val bmpCropLeft = ((cropBoxLeft - drawLeft) / finalScale).roundToInt().coerceIn(0, rotated.width - 1)
    val bmpCropTop = ((cropBoxTop - drawTop) / finalScale).roundToInt().coerceIn(0, rotated.height - 1)
    val bmpCropRight = ((cropBoxLeft + cropBoxW - drawLeft) / finalScale).roundToInt().coerceIn(bmpCropLeft + 1, rotated.width)
    val bmpCropBottom = ((cropBoxTop + cropBoxH - drawTop) / finalScale).roundToInt().coerceIn(bmpCropTop + 1, rotated.height)

    val actualCropW = (bmpCropRight - bmpCropLeft).coerceAtLeast(1)
    val actualCropH = (bmpCropBottom - bmpCropTop).coerceAtLeast(1)

    // Ensure we don't exceed source bounds
    val finalW = actualCropW.coerceAtMost(rotated.width - bmpCropLeft)
    val finalH = actualCropH.coerceAtMost(rotated.height - bmpCropTop)

    return Bitmap.createBitmap(rotated, bmpCropLeft, bmpCropTop, finalW, finalH)
}

