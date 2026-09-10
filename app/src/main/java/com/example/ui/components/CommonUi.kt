package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
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
                val targetDim = if (targetDimensionPx > 0) targetDimensionPx else 1024
                val bitmap = remember(trimmed, targetDim) { MediaUtils.base64ToBitmap(trimmed, targetDim) }
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
                    Image(
                        painter = painterResource(id = com.example.R.drawable.ic_jayhind_logo),
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        alignment = alignment,
                        alpha = alpha,
                        modifier = modifier
                    )
                }
            } else {
                val context = LocalContext.current
                val finalUrl = convertDriveUrlToDirectStreamUrl(trimmed)
                val imageRequestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(finalUrl)
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
 * Converts Google Drive shareable URLs or uc?id= into direct image stream links for Coil.
 */
fun convertDriveUrlToDirectStreamUrl(url: String): String {
    if (!url.contains("drive.google.com")) return url
    val fileId = when {
        url.contains("/file/d/") -> url.substringAfter("/file/d/").substringBefore("/")
        url.contains("id=") -> url.substringAfter("id=").substringBefore("&")
        else -> null
    }
    return if (fileId != null && fileId.isNotBlank()) {
        "https://drive.google.com/uc?export=view&id=$fileId"
    } else {
        url
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                uploadProgress = 0
                val uploadedUrl = FirebaseStorageHelper.uploadImage(context, uri, folder) { prog ->
                    uploadProgress = prog
                }
                isProcessing = false
                if (uploadedUrl.isNotBlank()) {
                    onImageSelected(uploadedUrl)
                }
            }
        }
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
        if (isProcessing) {
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
        } else if (!selectedImageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                UniversalAsyncImage(
                    model = selectedImageUrl,
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
        shadowElevation = 1.dp,
        modifier = Modifier.border(width = 1.dp, color = CardBorderColor)
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
        color = BloodRed.copy(alpha = 0.12f),
        contentColor = BloodRed,
        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                fontWeight = FontWeight.Bold,
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

@Composable
fun MemberAvatar(
    photoUrl: String,
    name: String,
    size: Int = 44,
    showBlueRing: Boolean = false,
    showPinkCelebrationRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderModifier = when {
        showPinkCelebrationRing -> Modifier.border(2.dp, BirthdayPink, CircleShape).padding(2.dp)
        showBlueRing -> Modifier.border(2.dp, NavySecondary, CircleShape).padding(2.dp)
        else -> Modifier.border(1.dp, CardBorderColor, CircleShape)
    }

    if (photoUrl.isNotBlank()) {
        UniversalAsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            targetDimensionPx = (size * 3).coerceIn(96, 240),
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .then(borderModifier),
            placeholder = {
                Box(
                    modifier = modifier
                        .size(size.dp)
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
            modifier = modifier
                .size(size.dp)
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
