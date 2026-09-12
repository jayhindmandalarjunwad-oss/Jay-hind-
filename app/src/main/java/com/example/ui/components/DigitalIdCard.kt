package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MandalInfo
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.util.IdCardUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DigitalIdCardView(
    user: User,
    mandalInfo: MandalInfo,
    mandalLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onQrClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    val memberId = remember(user.id, user.createdAt) { IdCardUtils.formatMemberId(user) }
    val qrBitmap = remember(user, mandalInfo) {
        val payload = IdCardUtils.getVerificationPayload(user, mandalInfo)
        IdCardUtils.generateQrBitmap(payload, 300)
    }

    val holographicGoldBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                GoldenTertiary,
                Color(0xFFFFE082),
                SaffronPrimary,
                Color(0xFFFFF8E1),
                GoldenTertiary
            )
        )
    }

    var isFlipped by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cardFlipRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isFlipped = !isFlipped
            }
            .graphicsLayer {
                rotationY = flipRotation
                cameraDistance = 14f * density
            }
            .testTag("digital_id_card_view")
    ) {
        if (flipRotation <= 90f) {
            // ==========================================
            // ओळखपत्राची पुढील बाजू (FRONT SIDE OF ID CARD)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.5.dp, holographicGoldBrush),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. TOP HEADER BANNER (Saffron & Gold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        SaffronPrimary,
                                        SaffronDark,
                                        GoldenTertiary
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MandalLogoBadge(logoUrl = mandalLogoUrl, size = 46)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "जय हिंद मंडळ, अर्जुनवाड",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "कला, क्रीडा व सांस्कृतिक मंडळ (स्था. १९९६)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Formatted Member ID Badge (JHM - 26 - 001)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = memberId,
                                    color = SaffronDark,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

            // Sub-header title
            Surface(
                color = Color(0xFFFFF7ED),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "अधिकृत डिजिटल सभासद ओळखपत्र",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark
                    )
                    Text(
                        text = "ता. शिरोळ • जि. कोल्हापूर",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // 2. CARD BODY (With Subtle Watermark Background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Subtle Center Watermark Logo
                val cleanWatermarkLogo = mandalLogoUrl?.trim()?.ifEmpty { null }
                if (cleanWatermarkLogo != null) {
                    UniversalAsyncImage(
                        model = cleanWatermarkLogo,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        alpha = 0.09f,
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.Center),
                        placeholder = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_jayhind_logo),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(160.dp)
                                    .align(Alignment.Center)
                                    .alpha(0.09f)
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_jayhind_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.Center)
                            .alpha(0.09f)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Profile Photo + Details Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = onAvatarClick != null) {
                                onAvatarClick?.invoke()
                            }
                        ) {
                            MemberAvatar(
                                photoUrl = user.profilePhotoUrl,
                                name = user.fullName,
                                size = 76,
                                showBlueRing = user.isAdmin
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            BloodGroupBadge(bloodGroup = user.bloodGroup)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextPrimary
                                )
                                if (user.isAdmin || (user.designation.isNotBlank() && user.designation != "सभासद")) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GleamingVerifiedBadge(size = 17.dp)
                                }
                            }

                            val designationText = if (user.designation.isNotBlank()) {
                                user.designation
                            } else if (user.isAdmin) {
                                "कार्यकारिणी सदस्य"
                            } else {
                                "आजीवन सभासद"
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SaffronContainer,
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    if (user.isAdmin || (user.designation.isNotBlank() && user.designation != "सभासद")) {
                                        GleamingVerifiedBadge(size = 12.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = designationText,
                                        color = SaffronDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user.mobileNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .padding(top = 2.dp)
                                        .align(Alignment.Top)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user.address.ifBlank { "अर्जुनवाड, ता. शिरोळ" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. BOTTOM ROW: QR CODE (Left) & OFFICIAL DIGITAL BLUE STAMP + SIGNATURE (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Verification QR Code
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onQrClick?.invoke() }
                                .padding(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                shadowElevation = 1.dp
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "व्हेरिफिकेशन QR कोड",
                                        modifier = Modifier
                                            .size(76.dp)
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(76.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "QR Code",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "व्हेरिफिकेशन QR कोड",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Middle: 3D Holographic Security Stamp (QR कोड आणि शिक्का यांच्या बरोबर मध्ये - ५४dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            HolographicSecuritySeal(size = 54.dp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "सुरक्षा होलोग्राम",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }

                        // Right: President's Authorized Signature (अध्यक्षांची स्वाक्षरी)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(min = 120.dp)
                        ) {
                            if (!mandalInfo.presidentSignatureUrl.isNullOrBlank() || !mandalInfo.officialStampUrl.isNullOrBlank()) {
                                PresidentSignatureSection(
                                    signatureUrl = mandalInfo.presidentSignatureUrl.ifBlank { null },
                                    stampUrl = mandalInfo.officialStampUrl.ifBlank { null }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = mandalInfo.presidentName.ifBlank { "अध्यक्ष" },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "जय हिंद मंडळ, अर्जुनवाड",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // 4. CARD FOOTER BAR
            Surface(
                color = SurfaceVariantWarm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val regDate = SimpleDateFormat("dd/MM/yyyy", Locale("mr", "IN")).format(Date(user.createdAt))
                        Text(
                            text = "नोंदणी: $regDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "वैधता: आजीवन (Lifetime)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "🔄 मागची बाजू पाहण्यासाठी टॅप करा (Tap to flip)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = SaffronDark,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} else {
    // ==========================================
    // ओळखपत्राची मागची बाजू (BACK SIDE OF ID CARD)
    // ==========================================
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationY = 180f }
            .testTag("digital_id_card_back_view"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.5.dp, holographicGoldBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        DigitalIdCardBackView(
            user = user,
            mandalInfo = mandalInfo,
            mandalLogoUrl = mandalLogoUrl
        )
    }
}
}
}

/**
 * Authentic President's Signature Section Composable (अध्यक्षांची अधिकृत स्वाक्षरी)
 * Displays the President's signature photo cleanly (without any default circular stamp).
 */
@Composable
fun PresidentSignatureSection(
    signatureUrl: String? = null,
    stampUrl: String? = null,
    showFallbackSignature: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(135.dp)
            .height(72.dp)
            .testTag("president_signature_box"),
        contentAlignment = Alignment.Center
    ) {
        // If an authentic custom physical stamp photo was explicitly uploaded, show it in background
        if (!stampUrl.isNullOrBlank()) {
            UniversalAsyncImage(
                model = stampUrl,
                contentDescription = "मंडळाचा शिक्का",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(54.dp)
                    .alpha(0.85f)
            )
        }

        // President's Signature (Only display when uploaded)
        if (!signatureUrl.isNullOrBlank()) {
            UniversalAsyncImage(
                model = signatureUrl,
                contentDescription = "अध्यक्षांची स्वाक्षरी",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Backward compatibility alias for OfficialMandalStamp without default round stamp.
 */
@Composable
fun OfficialMandalStamp(
    size: Int = 82,
    stampUrl: String? = null,
    signatureUrl: String? = null,
    showFallbackSignature: Boolean = false,
    modifier: Modifier = Modifier
) {
    PresidentSignatureSection(
        signatureUrl = signatureUrl,
        stampUrl = stampUrl,
        showFallbackSignature = showFallbackSignature,
        modifier = modifier
    )
}

/**
 * 3D Holographic Security Stamp (चकाकणारा होलोग्राफिक स्टॅम्प)
 * Simulates high-security government smart card hologram with rotating iridescent metallic gradient.
 * Default size is 54dp, placed prominently between QR code and President signature.
 */
@Composable
fun HolographicSecuritySeal(
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologramTransition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hologramAngle"
    )

    val holographicBrush = remember(angle) {
        val rad = Math.toRadians(angle.toDouble())
        val x = (Math.cos(rad) * 60).toFloat()
        val y = (Math.sin(rad) * 60).toFloat()
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700), // Gold
                Color(0xFF67E8F9), // Iridescent Cyan
                Color(0xFFF472B6), // Iridescent Pink
                Color(0xFFFBBF24), // Amber
                Color(0xFFA78BFA), // Lavender
                Color(0xFF34D399), // Emerald
                Color(0xFFFFD700)  // Gold
            ),
            start = Offset(27f - x, 27f - y),
            end = Offset(27f + x, 27f + y)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(holographicBrush)
            .border(1.5.dp, Color(0xFFFFD700), CircleShape)
            .padding(2.dp)
            .testTag("holographic_security_seal"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.8.dp, Color.White.copy(alpha = 0.95f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "★ जय हिंद ★",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "अधिकृत",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF991B1B)
                )
                Text(
                    text = "SECURE",
                    fontSize = 5.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

/**
 * Golden Gleaming Verified Badge (गोल्डन व्हेरिफाईड चमकणारा बॅज)
 * Displayed next to office bearers / verified members with an animated diagonal light glint.
 */
@Composable
fun GleamingVerifiedBadge(
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gleamTransition")
    val glintOffset by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glintOffset"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("gleaming_verified_badge"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "पदाधिकारी व्हेरिफाईड",
            tint = Color(0xFFF59E0B), // Golden Amber
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val currentX = glintOffset * width

            val glintBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.85f),
                    Color.Transparent
                ),
                start = Offset(currentX - width * 0.4f, 0f),
                end = Offset(currentX + width * 0.4f, height)
            )

            drawCircle(
                brush = glintBrush,
                radius = width * 0.48f,
                center = Offset(width / 2f, height / 2f)
            )
        }
    }
}

/**
 * Back Side of the Digital ID Card (ओळखपत्राची मागची बाजू)
 * Displayed when the user taps on the card to flip it in 3D.
 */
@Composable
fun DigitalIdCardBackView(
    user: User,
    mandalInfo: MandalInfo,
    mandalLogoUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. TOP HEADER BANNER (With "॥ सत्यमेव जयते ॥" and Mandal Name)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SaffronPrimary,
                            SaffronDark,
                            GoldenTertiary
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenTertiary.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "॥ सत्यमेव जयते ॥",
                        color = Color(0xFFFFF0B3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर • स्था. १९९६",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Subheader
        Surface(
            color = Color(0xFFFFF7ED),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "अधिकृत नियमावली व आपत्कालीन संपर्क",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaffronDark
                )
                Text(
                    text = "नोंदणीकृत संस्था",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
            }
        }

        // 2. BODY: Objectives, Rules, Emergency Helplines (Dynamic from Admin Settings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val objectivesText = mandalInfo.idCardObjectives.ifBlank {
                "• गावातील कला, क्रीडा, शिक्षण व सांस्कृतिक वारशाचे संवर्धन करणे.\n• सामाजिक बांधिलकी, रक्तदान चळवळ व आपत्कालीन मदतकार्य.\n• युवकांना विधायक दिशा देणे व गावाचा सर्वांगीण विकास साधणे."
            }
            val rulesText = mandalInfo.idCardRules.ifBlank {
                "• हे ओळखपत्र मंडळाच्या सर्व अधिकृत कार्यक्रमांसाठी वैध राहील.\n• ओळखपत्र अहस्तांतरणीय असून गैरवापर कायद्याने गुन्हा आहे.\n• गहाळ झाल्यास तात्काळ ॲडमिनशी संपर्क साधावा."
            }
            val emergencyText = mandalInfo.emergencyContacts.ifBlank {
                "रुग्णवाहिका: १०८ • संपर्क: ${mandalInfo.phone.ifBlank { "+91 98765 43210" }}"
            }

            // Objectives
            Text(
                text = "📌 मंडळाची ध्येये व उद्दिष्टे:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SaffronDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = objectivesText,
                fontSize = 9.5.sp,
                color = TextPrimary,
                lineHeight = 13.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Rules & Helplines Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚖️ महत्त्वाचे नियम:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaffronDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rulesText,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🚑 आपत्कालीन मदत:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BloodRed
                    )
                    Text(
                        text = emergencyText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // President's Signature & Seal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 100.dp)
                ) {
                    if (!mandalInfo.presidentSignatureUrl.isNullOrBlank() || !mandalInfo.officialStampUrl.isNullOrBlank()) {
                        PresidentSignatureSection(
                            signatureUrl = mandalInfo.presidentSignatureUrl.ifBlank { null },
                            stampUrl = mandalInfo.officialStampUrl.ifBlank { null }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = mandalInfo.presidentName.ifBlank { "अध्यक्ष" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "अधिकृत डिजिटल स्वाक्षरी",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 3. FOOTER
        Surface(
            color = SurfaceVariantWarm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔄 पुढील बाजू पाहण्यासाठी टॅप करा (Tap to flip)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = SaffronDark,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Full-screen / Interactive Digital ID Card Dialog with Save HD & WhatsApp Share actions
 */
@Composable
fun DigitalIdCardDialog(
    user: User,
    mandalInfo: MandalInfo,
    mandalLogoUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showQrDetailDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "डिजिटल ओळखपत्र",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantWarm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करा",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Card View
                DigitalIdCardView(
                    user = user,
                    mandalInfo = mandalInfo,
                    mandalLogoUrl = mandalLogoUrl,
                    onQrClick = { showQrDetailDialog = true }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5. ACTION BUTTONS: HD Save & WhatsApp Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Save HD Image to Gallery
                    Button(
                        onClick = {
                            isSaving = true
                            IdCardUtils.saveIdCardToGallery(
                                context = context,
                                user = user,
                                mandalInfo = mandalInfo,
                                onSuccess = { isSaving = false },
                                onError = { isSaving = false }
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_hd_id_card_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaving) "सेव्ह होत आहे..." else "HD सेव्ह करा",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Button 2: WhatsApp Share Button
                    Button(
                        onClick = {
                            IdCardUtils.shareIdCard(
                                context = context,
                                user = user,
                                mandalInfo = mandalInfo,
                                onlyWhatsApp = true
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_whatsapp_id_card_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp शेअर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hint row for QR Verification
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showQrDetailDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = NavySecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "QR कोड पडताळणी तपशील पहा",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavySecondary
                    )
                }
            }
        }
    }

    // QR Details preview modal
    if (showQrDetailDialog) {
        AlertDialog(
            onDismissRequest = { showQrDetailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("डिजिटल QR पडताळणी माहिती", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "हा QR कोड कोणत्याही मोबाईल कॅमेऱ्याने किंवा QR स्कॅनरने स्कॅन केल्यास खालील माहिती पडताळता येते:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = SurfaceVariantWarm,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = IdCardUtils.getVerificationDisplaySummary(user, mandalInfo),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDetailDialog = false }) {
                    Text("ठीक आहे", fontWeight = FontWeight.Bold, color = SaffronPrimary)
                }
            }
        )
    }
}
