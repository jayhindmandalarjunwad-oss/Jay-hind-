package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.util.FirebaseStorageHelper
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UniversalAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1.0f,
    targetDimensionPx: Int = 0,
    placeholder: @Composable (() -> Unit)? = null
) {
    if (model == null || (model is String && model.isBlank())) {
        if (placeholder != null) {
            placeholder()
        }
        return
    }

    when (model) {
        is Bitmap -> {
            Image(
                bitmap = model.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                alpha = alpha,
                modifier = modifier
            )
        }
        is Int -> {
            Image(
                painter = painterResource(id = model),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                alpha = alpha,
                modifier = modifier
            )
        }
        is String -> {
            val trimmed = model.trim()
            val isBase64 = trimmed.startsWith("data:") ||
                    trimmed.contains("base64,") ||
                    trimmed.startsWith("/9j/") ||
                    trimmed.startsWith("iVBOR") ||
                    trimmed.startsWith("R0lGOD") ||
                    trimmed.startsWith("UklGR") ||
                    (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("content://") && !trimmed.startsWith("file://") && trimmed.length > 80)

            if (isBase64) {
                val targetDim = if (targetDimensionPx > 0) targetDimensionPx else 600
                val cached = remember(trimmed, targetDim) { MediaUtils.getCachedBitmap(trimmed, targetDim) }
                val bitmapState = produceState<Bitmap?>(initialValue = cached, trimmed, targetDim) {
                    if (value == null) {
                        value = withContext(Dispatchers.IO) {
                            MediaUtils.base64ToBitmap(trimmed, targetDim)
                        }
                    }
                }
                val bitmap = bitmapState.value
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        alignment = alignment,
                        alpha = alpha,
                        modifier = modifier
                    )
                } else if (placeholder != null) {
                    placeholder()
                } else {
                    Box(
                        modifier = modifier.background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_jayhind_logo),
                            contentDescription = contentDescription,
                            contentScale = contentScale,
                            alignment = alignment,
                            alpha = 0.6f,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                val context = LocalContext.current
                val finalUrl = convertDriveUrlToDirectStreamUrl(trimmed)
                val imageRequestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(finalUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .error(com.example.R.drawable.ic_jayhind_logo)
                    .fallback(com.example.R.drawable.ic_jayhind_logo)

                if (targetDimensionPx > 0) {
                    imageRequestBuilder.size(targetDimensionPx, targetDimensionPx)
                }

                AsyncImage(
                    model = imageRequestBuilder.build(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    alpha = alpha,
                    modifier = modifier
                )
            }
        }
        else -> {
            val context = LocalContext.current
            val imageRequestBuilder = coil.request.ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .error(com.example.R.drawable.ic_jayhind_logo)
                .fallback(com.example.R.drawable.ic_jayhind_logo)

            if (targetDimensionPx > 0) {
                imageRequestBuilder.size(targetDimensionPx, targetDimensionPx)
            }

            AsyncImage(
                model = imageRequestBuilder.build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                alpha = alpha,
                modifier = modifier
            )
        }
    }
}

/**
 * Converts Google Drive shareable URLs or uc?id= into direct high-speed image stream links for Coil.
 * Uses Google Drive's official high-resolution CDN thumbnail endpoint (sz=w1600), ensuring 0 KB Firebase storage usage,
 * 100% free hosting, and bypasses 403 hotlinking restrictions.
 */
fun convertDriveUrlToDirectStreamUrl(url: String, targetSize: Int = 1600): String {
    val trimmed = url.trim()
    if (!trimmed.contains("drive.google.com") && !trimmed.contains("docs.google.com") && !trimmed.contains("googleusercontent.com")) return trimmed

    val fileId = when {
        trimmed.contains("/file/d/") -> trimmed.substringAfter("/file/d/").substringBefore("/").substringBefore("?").substringBefore("&")
        trimmed.contains("id=") -> trimmed.substringAfter("id=").substringBefore("&").substringBefore("#")
        trimmed.contains("/open?id=") -> trimmed.substringAfter("/open?id=").substringBefore("&").substringBefore("#")
        trimmed.contains("/uc?id=") -> trimmed.substringAfter("/uc?id=").substringBefore("&").substringBefore("#")
        trimmed.contains("googleusercontent.com/d/") -> trimmed.substringAfter("/d/").substringBefore("=").substringBefore("/").substringBefore("?")
        else -> null
    }

    return if (!fileId.isNullOrBlank() && fileId.length >= 15) {
        // High-definition thumbnail endpoint (1600px width) directly from Google Drive CDN, 100% free, 0 KB Firebase storage used
        "https://drive.google.com/thumbnail?id=$fileId&sz=w$targetSize"
    } else {
        trimmed
    }
}

@Composable
fun GalleryImagePicker(
    selectedImageUrl: String?,
    onImageSelected: (String) -> Unit,
    label: String = "गॅलरीतून फोटो निवडा",
    helperText: String = "मोबाईल गॅलरीतून फोटो अपलोड करा",
    height: Dp = 140.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    folder: String = "gallery",
    enableCropping: Boolean = false,
    cropRatio: CropAspectRatio = CropAspectRatio.FREE,
    isCircleCrop: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uriToCrop by remember { mutableStateOf<Uri?>(null) }
    var localPreviewUri by remember { mutableStateOf<Uri?>(null) }

    fun proceedUpload(uri: Uri) {
        localPreviewUri = uri
        coroutineScope.launch {
            isProcessing = true
            uploadProgress = 0
            val uploadedUrl = FirebaseStorageHelper.uploadImage(context, uri, folder) { prog ->
                uploadProgress = prog
            }
            isProcessing = false
            if (uploadedUrl.isNotBlank()) {
                localPreviewUri = null
                onImageSelected(uploadedUrl)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (enableCropping) {
                uriToCrop = uri
            } else {
                proceedUpload(uri)
            }
        }
    }

    if (uriToCrop != null) {
        ImageCropperDialog(
            sourceUri = uriToCrop!!,
            initialRatio = cropRatio,
            isCircleCrop = isCircleCrop,
            onDismiss = { uriToCrop = null },
            onImageCropped = { croppedUri ->
                uriToCrop = null
                proceedUpload(croppedUri)
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = !isProcessing) { galleryLauncher.launch("image/*") }
            .testTag("gallery_image_picker"),
        shape = shape,
        color = SurfaceWarm,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (!selectedImageUrl.isNullOrBlank()) SaffronPrimary else CardBorderColor
        )
    ) {
        val displayModel: Any? = localPreviewUri ?: selectedImageUrl
        if (isProcessing && displayModel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                UniversalAsyncImage(
                    model = displayModel,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { (uploadProgress / 100f).coerceIn(0f, 1f) },
                            color = SaffronPrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uploadProgress > 0) "फोटो कॉम्प्रेस व सेव्ह होत आहे... $uploadProgress%" else "फोटो कॉम्प्रेस होत आहे (WebP)...",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { (uploadProgress / 100f).coerceIn(0f, 1f) },
                        color = SaffronPrimary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uploadProgress > 0) "फोटो कॉम्प्रेस व अपलोड होत आहे... $uploadProgress%" else "फोटो कॉम्प्रेस होत आहे (WebP)...",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (!selectedImageUrl.isNullOrBlank() || localPreviewUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                UniversalAsyncImage(
                    model = displayModel ?: "",
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "फोटो निवडला आहे",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SaffronPrimary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "बदला",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SaffronPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "गॅलरी",
                        tint = SaffronPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MandalLogoBadge(
    logoUrl: String? = null,
    size: Int = 42,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = SaffronPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
    ) {
        val cleanUrl = logoUrl?.trim()?.ifEmpty { null }
        if (cleanUrl != null) {
            UniversalAsyncImage(
                model = cleanUrl,
                contentDescription = "मंडळ लोगो",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_jayhind_logo),
                        contentDescription = "मंडळ लोगो",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_jayhind_logo),
                contentDescription = "मंडळ लोगो",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MandalTopHeader(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    logoUrl: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = SurfaceWarm,
        contentColor = TextPrimary,
        shadowElevation = 1.5.dp,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, CardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantWarm)
                        .testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "मागे जा",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                MandalLogoBadge(logoUrl = logoUrl, size = 42)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (showBackButton) TextPrimary else SaffronPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

@Composable
fun BloodGroupBadge(bloodGroup: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BloodRed.copy(alpha = 0.10f),
        contentColor = BloodRed,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, BloodRed.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bloodtype,
                contentDescription = null,
                tint = BloodRed,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = bloodGroup,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BloodRed
            )
        }
    }
}

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, borderColor) = when (role) {
        "अध्यक्ष", "ADMIN", "PRESIDENT" -> Triple(SaffronContainer, SaffronDark, SaffronPrimary.copy(alpha = 0.4f))
        "सचिव", "SECRETARY" -> Triple(NavyContainer, NavySecondary, NavySecondary.copy(alpha = 0.4f))
        "खजिनदार", "TREASURER" -> Triple(GoldContainer, GoldenTertiary, GoldenTertiary.copy(alpha = 0.4f))
        "क्रीडा प्रमुख", "सांस्कृतिक प्रमुख" -> Triple(Color(0xFFDCFCE7), SuccessGreen, SuccessGreen.copy(alpha = 0.4f))
        else -> Triple(SurfaceVariantWarm, TextSecondary, CardBorderColor)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        contentColor = textColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = role,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (text, color, bgColor) = when (status) {
        "APPROVED" -> Triple("मंजूर (Approved)", SuccessGreen, Color(0xFFDCFCE7))
        "PENDING_APPROVAL" -> Triple("प्रतिक्षेत (Pending)", PendingOrange, Color(0xFFFFEDD5))
        "REJECTED" -> Triple("नामंजूर (Rejected)", BloodRed, Color(0xFFFEE2E2))
        "BLOCKED" -> Triple("ब्लॉक (Blocked)", Color.DarkGray, Color(0xFFF1F5F9))
        else -> Triple(status, TextSecondary, SurfaceVariantWarm)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        contentColor = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (text, color, bgColor) = when (priority) {
        "URGENT" -> Triple("तातडीचे", BloodRed, Color(0xFFFEE2E2))
        "HIGH" -> Triple("महत्वाचे", PendingOrange, Color(0xFFFFEDD5))
        else -> Triple("सामान्य", NavySecondary, NavyContainer)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        contentColor = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Checks if a given date string (YYYY-MM-DD or DD/MM/YYYY or DD-MM-YYYY) matches today's month and day.
 */
fun isBirthdayToday(dob: String?): Boolean {
    if (dob.isNullOrBlank()) return false
    val sdfMonthDay = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
    val currentMonthDay = sdfMonthDay.format(java.util.Date())
    return try {
        val trimmed = dob.trim()
        val parts = if (trimmed.contains("-")) trimmed.split("-") else if (trimmed.contains("/")) trimmed.split("/") else emptyList()
        if (parts.size == 3) {
            val (m, d) = if (parts[0].length == 4) {
                parts[1].padStart(2, '0') to parts[2].padStart(2, '0')
            } else {
                parts[1].padStart(2, '0') to parts[0].padStart(2, '0')
            }
            "$m-$d" == currentMonthDay
        } else false
    } catch (e: Exception) {
        false
    }
}

@Composable
fun MemberAvatar(
    photoUrl: String,
    name: String,
    size: Int = 44,
    showBlueRing: Boolean = false,
    showPinkCelebrationRing: Boolean = false,
    dateOfBirth: String = "",
    modifier: Modifier = Modifier
) {
    val isBirthday = showPinkCelebrationRing || (dateOfBirth.isNotBlank() && isBirthdayToday(dateOfBirth))

    val borderModifier = when {
        isBirthday -> Modifier.border(2.dp, BirthdayPink, CircleShape).padding(2.dp)
        showBlueRing -> Modifier.border(2.dp, NavySecondary, CircleShape).padding(2.dp)
        else -> Modifier.border(1.dp, CardBorderColor, CircleShape)
    }

    Box(modifier = modifier.size(size.dp)) {
        if (photoUrl.isNotBlank()) {
            UniversalAsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                targetDimensionPx = (size * 3).coerceIn(96, 240),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(borderModifier),
                placeholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .then(borderModifier)
                            .background(
                                Brush.linearGradient(
                                    listOf(SaffronPrimary, SaffronDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = name.firstOrNull()?.toString() ?: "ज"
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = (size * 0.4).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(borderModifier)
                    .background(
                        Brush.linearGradient(
                            listOf(SaffronPrimary, SaffronDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val initial = name.firstOrNull()?.toString() ?: "ज"
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = (size * 0.4).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Celebratory Birthday Ribbon / Crown Badge
        if (isBirthday) {
            Box(
                modifier = Modifier
                    .size((size * 0.42f).coerceIn(16f, 26f).dp)
                    .align(Alignment.TopEnd)
                    .background(BirthdayPinkDark, CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎂",
                    fontSize = ((size * 0.24f).coerceIn(9f, 15f)).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SaffronContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SaffronPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatTimestampToEnglish(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            "Today, ${sdf.format(Date(timestamp))}"
        }
        diff < 48 * 60 * 60 * 1000 -> {
            val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            "Yesterday, ${sdf.format(Date(timestamp))}"
        }
        else -> {
            val sdf = SimpleDateFormat("dd MMM, yyyy, hh:mm a", Locale.ENGLISH)
            sdf.format(Date(timestamp))
        }
    }
}

fun formatTimestampToMarathi(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60 * 1000 -> "आत्ताच"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} मिनीटांपूर्वी"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} तासांपूर्वी"
        else -> {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("mr", "IN"))
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Animated Shimmer Effect Modifier
 * Creates a smooth sweeping light reflection across skeleton placeholder components.
 * 100% compatible across all Android versions (API 21+).
 */
fun Modifier.shimmerEffect(
    shimmerColors: List<Color> = listOf(
        Color(0xFFE2E8F0).copy(alpha = 0.6f),
        Color(0xFFF8FAFC).copy(alpha = 0.95f),
        Color(0xFFE2E8F0).copy(alpha = 0.6f)
    )
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnim - 500f, y = translateAnim - 500f),
            end = Offset(x = translateAnim, y = translateAnim)
        )
    )
}

/**
 * Skeleton Loader Card for Posts (फेसबुक/इन्स्टाग्राम स्टाईल शिमर सांगाडा)
 */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_skeleton"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Author Row Skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post Content Lines Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Post Image Box Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions Row Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Skeleton Loader Card for Member Directory
 */
@Composable
fun MemberCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_card_skeleton"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
        }
    }
}

