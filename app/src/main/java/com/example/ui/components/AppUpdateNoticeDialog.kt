package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppUpdateInfo
import com.example.ui.theme.*
import com.example.util.AppUpdateHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppUpdateNoticeDialog(
    updateInfo: AppUpdateInfo,
    mandalLogoUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }

    val formattedDate = remember(updateInfo.publishedAt) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("mr", "IN"))
        sdf.format(Date(updateInfo.publishedAt))
    }

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.isForceUpdate && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SaffronPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Mandal Notice Header (मंडळ अधिकृत सूचना फलक शैली)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SaffronPrimary,
                                    SaffronDark
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mandal Emblem / Logo
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, SaffronLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            UniversalAsyncImage(
                                model = mandalLogoUrl ?: R.drawable.ic_jayhind_logo,
                                contentDescription = "Mandal Logo",
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "अर्जुनवाड • अधिकृत सूचना फलक 📢",
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Release Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚀 नवीन ॲप व्हर्जन ${updateInfo.latestVersionName} उपलब्ध",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Body Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Date & Notice Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedDate,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (updateInfo.isForceUpdate) BloodRed.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (updateInfo.isForceUpdate) BloodRed.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (updateInfo.isForceUpdate) "🔴 सक्तीचे अपडेट" else "🟢 नवीन अपडेट",
                                color = if (updateInfo.isForceUpdate) BloodRed else SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notice Content / Release Notes Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, CardBorderColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "नवीन बदल व सुधारणा:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val notesText = if (updateInfo.releaseNotes.isNotBlank()) {
                                updateInfo.releaseNotes
                            } else {
                                "• ॲपची कार्यक्षमता व गती सुधारण्यात आली आहे.\n• काही किरकोळ त्रुटींचे निवारण करण्यात आले आहे.\n• सर्व सभासदांना अखंड व सुरळीत सेवेचा अनुभव मिळेल."
                            }

                            Text(
                                text = notesText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign-off line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "आदेशानुसार: ${updateInfo.publishedBy}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaffronDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Downloading Progress Indicator
                    if (isDownloading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SaffronPrimary,
                                trackColor = SaffronLight.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "नवीन व्हर्जन डाऊनलोड होत आहे... 📥",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Action Buttons
                    Button(
                        onClick = {
                            if (updateInfo.apkDownloadUrl.isNotBlank()) {
                                isDownloading = true
                                AppUpdateHelper.downloadAndInstallApk(
                                    context = context,
                                    rawUrl = updateInfo.apkDownloadUrl,
                                    versionName = updateInfo.latestVersionName,
                                    onStarted = { isDownloading = true },
                                    onFailure = { isDownloading = false }
                                )
                            } else {
                                AppUpdateHelper.openInBrowser(context, updateInfo.apkDownloadUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚀 आत्ताच अपडेट करा (Update Now)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Browser direct link fallback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppUpdateHelper.openInBrowser(context, updateInfo.apkDownloadUrl)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = SaffronDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "थेट ब्राऊझर / गुगल ड्राईव्हमध्ये उघडा",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaffronDark
                        )
                    }

                    // Later button if not force update
                    if (!updateInfo.isForceUpdate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "नंतर करा (Dismiss)",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ हे सक्तीचे अपडेट आहे. ॲप वापरण्यासाठी अपडेट करणे आवश्यक आहे.",
                            fontSize = 11.sp,
                            color = BloodRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
