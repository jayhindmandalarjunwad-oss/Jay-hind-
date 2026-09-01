package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_id_card_view"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, SaffronPrimary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )

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
                                Text(
                                    text = designationText,
                                    color = SaffronDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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

                        // Right: Official Blue Rubber Stamp with President's Signature Overlay
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OfficialMandalStamp(
                                size = 82,
                                signatureUrl = mandalInfo.presidentSignatureUrl.ifBlank { null }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = mandalInfo.presidentName.ifBlank { "अध्यक्ष" },
                                fontSize = 10.sp,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
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
            }
        }
    }
}

/**
 * Authentic Official Blue Rubber Stamp Composable (अधिकृत डिजिटल गोल निळा शिक्का)
 * with President Signature Overlay.
 */
@Composable
fun OfficialMandalStamp(
    size: Int = 82,
    signatureUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val stampBlue = Color(0xFF1E3A8A) // Official Rubber Stamp Navy/Blue

    Box(
        modifier = modifier
            .size(size.dp)
            .testTag("official_mandal_stamp"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = (this.size.width / 2) - strokeWidth

            // Outer thick ring
            drawCircle(
                color = stampBlue.copy(alpha = 0.88f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth * 1.2f)
            )

            // Inner dashed ring
            drawCircle(
                color = stampBlue.copy(alpha = 0.82f),
                radius = radius - 4.dp.toPx(),
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6f, 3f), 0f
                    )
                )
            )

            // Innermost ring
            drawCircle(
                color = stampBlue.copy(alpha = 0.85f),
                radius = radius - 8.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Stamp Typography (Under the signature)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "★ जय हिंद ★",
                color = stampBlue.copy(alpha = 0.88f),
                fontWeight = FontWeight.Black,
                fontSize = (size * 0.10).sp
            )
            Text(
                text = "अधिकृत शिक्का",
                color = stampBlue.copy(alpha = 0.92f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size * 0.12).sp,
                letterSpacing = 0.3.sp
            )
            Text(
                text = "अर्जुनवाड",
                color = stampBlue.copy(alpha = 0.88f),
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.10).sp
            )
            Text(
                text = "१९९६",
                color = stampBlue.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.09).sp
            )
        }

        // President's Signature Overlay (Over top of stamp with slight rotation)
        if (!signatureUrl.isNullOrBlank()) {
            UniversalAsyncImage(
                model = signatureUrl,
                contentDescription = "अध्यक्षांची स्वाक्षरी",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .padding(2.dp)
            )
        } else {
            // Elegant digital signature vector stroke representation
            Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val w = this@Canvas.size.width
                    val h = this@Canvas.size.height
                    moveTo(w * 0.15f, h * 0.58f)
                    cubicTo(w * 0.3f, h * 0.3f, w * 0.45f, h * 0.7f, w * 0.58f, h * 0.45f)
                    cubicTo(w * 0.68f, h * 0.25f, w * 0.78f, h * 0.65f, w * 0.88f, h * 0.42f)
                    moveTo(w * 0.2f, h * 0.65f)
                    lineTo(w * 0.82f, h * 0.60f)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF0F2D6B).copy(alpha = 0.9f),
                    style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
