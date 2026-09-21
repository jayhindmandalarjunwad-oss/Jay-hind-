package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.UniversalAsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.AppUpdateHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppUpdateAdminTab(
    viewModel: MandalViewModel
) {
    val context = LocalContext.current
    val updateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()

    // Auto-compute next version based on current server state or BuildConfig
    val autoNextVersionName = remember(updateInfo.latestVersionName) {
        AppUpdateHelper.calculateNextVersionName(updateInfo.latestVersionName)
    }
    val autoNextVersionCode = remember(updateInfo.latestVersionCode) {
        val baseCode = maxOf(updateInfo.latestVersionCode, BuildConfig.VERSION_CODE)
        baseCode + 1
    }

    var versionName by remember(autoNextVersionName) { mutableStateOf(autoNextVersionName) }
    var versionCode by remember(autoNextVersionCode) { mutableStateOf(autoNextVersionCode.toString()) }
    var apkDownloadUrl by remember { mutableStateOf("") }
    var releaseNotes by remember {
        mutableStateOf("• नवीन वैशिष्ट्ये व सुधारणांचा समावेश.\n• ॲपची कार्यक्षमता व गती सुधारण्यात आली आहे.\n• काही तांत्रिक त्रुटी दूर केल्या आहेत.")
    }
    var isForceUpdate by remember { mutableStateOf(false) }
    var postAnnouncement by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val formattedPublishedDate = remember(updateInfo.publishedAt) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("mr", "IN"))
        sdf.format(Date(updateInfo.publishedAt))
    }

    val isDriveLink = remember(apkDownloadUrl) {
        apkDownloadUrl.contains("drive.google.com") || apkDownloadUrl.contains("docs.google.com")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER & CURRENT STATUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SaffronPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🚀 ॲप व्हर्जन व अपडेट व्यवस्थापन",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "नवीन ॲप व्हर्जन प्रसिद्ध करा व सदस्यांना थेट अपडेट द्या",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Status details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("सध्या इन्स्टॉल व्हर्जन:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (Code: ${BuildConfig.VERSION_CODE})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("सर्व्हरवर सक्रिय व्हर्जन:", fontSize = 11.sp, color = TextSecondary)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, SuccessGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "v${updateInfo.latestVersionName} (Code: ${updateInfo.latestVersionCode})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (updateInfo.apkDownloadUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "शेवटचे अपडेट: $formattedPublishedDate (${updateInfo.publishedBy})",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. CREATE NEW UPDATE FORM
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, SaffronPrimary.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📢 नवीन व्हर्जन तयार व प्रसिद्ध करा",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SaffronDark
                )
                Text(
                    text = "खालील व्हर्जन क्रमांक आपोआप पुढच्या अंकाने वाढवला आहे:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Version Name & Code Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("नवीन व्हर्जन (Name)") },
                        placeholder = { Text("उदा. 1.2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = SaffronPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = CardBorderColor
                        )
                    )

                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = { versionCode = it },
                        label = { Text("व्हर्जन कोड") },
                        placeholder = { Text("उदा. 2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null, tint = SaffronPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = CardBorderColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // APK Download URL
                OutlinedTextField(
                    value = apkDownloadUrl,
                    onValueChange = { apkDownloadUrl = it },
                    label = { Text("APK फाईल लिंक (Google Drive किंवा थेट लिंक) *") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = SaffronPrimary)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = clipboard?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        apkDownloadUrl = text
                                        Toast.makeText(context, "लिंक पेस्ट केली! 📋", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = SaffronPrimary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = CardBorderColor
                    )
                )

                if (isDriveLink) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreen.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, SuccessGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "गुगल ड्राईव्ह लिंक ओळखली! ॲप ही लिंक थेट १-क्लिक डाऊनलोड लिंकमध्ये आपोआप रूपांतरित करेल.",
                                fontSize = 11.sp,
                                color = SuccessGreen,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Release Notes
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("या व्हर्जनमधील नवीन बदल व सुधारणा (Release Notes)") },
                    placeholder = { Text("काय काय नवीन बदल केले आहेत ते इथे लिहा...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = CardBorderColor
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Post as Official Notice on Mandal Notice Board
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = postAnnouncement,
                        onCheckedChange = { postAnnouncement = it },
                        colors = CheckboxDefaults.colors(checkedColor = SaffronPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "मंडळाच्या अधिकृत सूचना फलकावर (Notice Board) ही सूचना लावा",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "सर्व सदस्यांना सूचना फलकावर नवीन व्हर्जनची माहिती दिसेल",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Force Update switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BloodRed)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "सक्तीचे अपडेट (Force Update)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isForceUpdate) BloodRed else TextPrimary
                        )
                        Text(
                            text = if (isForceUpdate) "सदस्यांना ॲप वापरण्यापूर्वी अपडेट करणे बंधनकारक राहील" else "सदस्य नंतरही अपडेट करू शकतात",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (apkDownloadUrl.isBlank()) {
                            Toast.makeText(context, "कृपया APK फाईलची गुगल ड्राईव्ह किंवा डाऊनलोड लिंक टाका.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showConfirmDialog = true
                    },
                    enabled = !isPublishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("प्रसिद्ध होत आहे...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📢 नवीन व्हर्जन $versionName प्रसिद्ध करा",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // 3. STEP-BY-STEP GUIDE CARD FOR ADMIN
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SaffronPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💡 ॲडमिनसाठी साधे मार्गदर्शक (३ पायऱ्या):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "१. नवीन बदल झाल्यावर AI Studio मधून Export / Download APK करा.\n" +
                            "२. गुगल ड्राईव्हवर (Google Drive) ती APK फाईल अपलोड करा आणि लिंक 'Anyone with the link' करून कॉपी करा.\n" +
                            "३. इथे वर लिंक पेस्ट करा आणि 'नवीन व्हर्जन प्रसिद्ध करा' दाबा.\n\n" +
                            "✅ गावांतील सर्व सदस्यांच्या मोबाईलमध्ये लगेच मंडळाच्या अधिकृत सूचना फलकासारखा सुंदर पॉपअप उघडेल व ते एका क्लिकवर ॲप अपडेट करतील!",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        val parsedCode = versionCode.toIntOrNull() ?: autoNextVersionCode
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "नवीन ॲप व्हर्जन प्रसिद्ध करायचे का? 🚀",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "तुम्ही व्हर्जन $versionName (कोड: $parsedCode) प्रसिद्ध करत आहात.\n\n" +
                            "हे अपडेट सर्व सदस्यांच्या मोबाईलवर अधिकृत सूचना म्हणून दिसेल आणि ते नवीन व्हर्जन डाऊनलोड करू शकतील.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isPublishing = true
                        viewModel.publishAppUpdate(
                            versionName = versionName,
                            versionCode = parsedCode,
                            apkDownloadUrl = apkDownloadUrl,
                            releaseNotes = releaseNotes,
                            isForceUpdate = isForceUpdate,
                            postAnnouncement = postAnnouncement,
                            onSuccess = {
                                isPublishing = false
                                Toast.makeText(context, "नवीन ॲप व्हर्जन प्रसिद्ध झाले! 🎉", Toast.LENGTH_LONG).show()
                            },
                            onError = {
                                isPublishing = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("होय, प्रसिद्ध करा", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("रद्द करा")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
