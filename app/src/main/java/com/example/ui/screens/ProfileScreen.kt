package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.IdCardUtils

@Composable
fun ProfileScreen(viewModel: MandalViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalInfo by viewModel.mandalInfo.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showIdCardFullDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFullscreenPhoto by remember { mutableStateOf(false) }
    var isSavingIdCard by remember { mutableStateOf(false) }

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("कृपया प्रथम लॉगिन करा.")
        }
        return
    }

    val user = currentUser!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .testTag("profile_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. OFFICIAL DIGITAL MEMBERSHIP CARD SECTION (New Design)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DigitalIdCardView(
                user = user,
                mandalInfo = mandalInfo,
                mandalLogoUrl = mandalLogoUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIdCardFullDialog = true },
                onQrClick = { showIdCardFullDialog = true },
                onAvatarClick = {
                    if (user.profilePhotoUrl.isNotBlank()) {
                        showFullscreenPhoto = true
                    }
                }
            )

            // Two Quick Action Buttons: Save HD & WhatsApp Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Save HD Image to Mobile Gallery
                Button(
                    onClick = {
                        isSavingIdCard = true
                        IdCardUtils.saveIdCardToGallery(
                            context = context,
                            user = user,
                            mandalInfo = mandalInfo,
                            onSuccess = {
                                isSavingIdCard = false
                                viewModel.showSnackbar("✅ HD ओळखपत्र गॅलरीमध्ये सेव्ह झाले!")
                            },
                            onError = { err ->
                                isSavingIdCard = false
                                viewModel.showSnackbar("❌ $err")
                            }
                        )
                    },
                    enabled = !isSavingIdCard,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("profile_save_hd_id_card_button"),
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
                        text = if (isSavingIdCard) "सेव्ह होत आहे..." else "HD सेव्ह करा",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
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
                        .height(46.dp)
                        .testTag("profile_share_whatsapp_id_card_button"),
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
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ADMIN PANEL BUTTON IF USER IS ADMIN OR CONTENT ADMIN
        if (user.isAnyAdmin) {
            Button(
                onClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_panel_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (user.isAdmin) NavySecondary else Color(0xFF2563EB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (user.isAdmin) "मंडळ ॲडमिन पॅनेल (Admin Panel)" else "मंडळ व्यवस्थापन पॅनेल (Content Panel)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // PROFILE ACTIONS (Strictly Personal Profile Edit & Password Change)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                ProfileOptionRow(
                    icon = Icons.Default.Edit,
                    title = "प्रोफाइल माहिती व फोटो बदला",
                    subtitle = "नाव, फोटो, रक्तगट, जन्म तारीख, पत्ता",
                    onClick = { showEditProfileDialog = true }
                )
                HorizontalDivider(color = DividerColor)
                ProfileOptionRow(
                    icon = Icons.Default.Lock,
                    title = "पासवर्ड बदला",
                    subtitle = "आपला लॉगिन पासवर्ड अद्ययावत करा",
                    onClick = { showChangePasswordDialog = true }
                )
            }
        }

        // MANDAL FEEDBACK & SUGGESTIONS CARD (Direct & Confidential to Super Admin)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFeedbackDialog = true }
                .testTag("profile_feedback_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "मंडळासाठी अभिप्राय व सूचना",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "🔒 ॲडमिनसाठी",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "आपल्या मोलाच्या सूचना थेट मुख्य ॲडमिनकडे नोंदवा",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        // BACKGROUND NOTIFICATION & BATTERY OPTIMIZATION CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                    com.example.util.MandalNotificationService.startService(context)
                    com.example.util.MandalSyncJobService.scheduleJob(context)
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ॲप बंद असताना पुश नोटिफिकेशन्स",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "सक्रिय",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "मोबाईल बंद किंवा लॉक असतानाही त्वरित मेसेज येण्यासाठी बॅटरी मर्यादा हटवा",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        // LOGOUT BUTTON
        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = BloodRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("लॉगआउट करा (Logout)", fontWeight = FontWeight.Bold, color = BloodRed)
        }

        Spacer(modifier = Modifier.height(90.dp))
    }

    // Full Digital ID Card Dialog
    if (showIdCardFullDialog) {
        DigitalIdCardDialog(
            user = user,
            mandalInfo = mandalInfo,
            mandalLogoUrl = mandalLogoUrl,
            onDismiss = { showIdCardFullDialog = false }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, gender, bg, dob, addr, photo ->
                viewModel.updateProfile(name, gender, bg, dob, addr, photo)
                showEditProfileDialog = false
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSave = { oldP, newP ->
                viewModel.changePassword(oldP, newP)
                showChangePasswordDialog = false
            }
        )
    }

    // Fullscreen Profile Photo Viewer
    if (showFullscreenPhoto && user.profilePhotoUrl.isNotBlank()) {
        FullscreenPhotoDialog(
            photos = listOf(user.profilePhotoUrl),
            onDismiss = { showFullscreenPhoto = false }
        )
    }

    // Member Feedback Dialog (Confidential to Super Admin)
    if (showFeedbackDialog) {
        MemberFeedbackDialog(
            user = user,
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { category, rating, message ->
                viewModel.submitFeedback(
                    category = category,
                    rating = rating,
                    message = message,
                    onSuccess = {
                        showFeedbackDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SaffronPrimary.copy(alpha = 0.12f),
            contentColor = SaffronPrimary,
            modifier = Modifier.size(38.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (fullName: String, gender: String, bloodGroup: String, dob: String, address: String, photo: String) -> Unit
) {
    var name by remember { mutableStateOf(user.fullName) }
    var gender by remember { mutableStateOf(user.gender) }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup) }
    var dob by remember { mutableStateOf(user.dateOfBirth) }
    var address by remember { mutableStateOf(user.address) }
    var photoUrl by remember { mutableStateOf(user.profilePhotoUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("प्रोफाइल माहिती व फोटो बदला", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "प्रोफाइल फोटो (गॅलरीतून निवडा):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                GalleryImagePicker(
                    selectedImageUrl = photoUrl,
                    onImageSelected = { photoUrl = it },
                    label = "गॅलरीतून नवीन फोटो निवडा",
                    helperText = "मोबाईल गॅलरीतून स्वतःचा फोटो अपलोड करा",
                    height = 120.dp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("पूर्ण नाव") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("रक्तगट (उदा. O+, A+)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MandalDatePickerField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = "जन्म तारीख (DOB)",
                    placeholder = "कॅलेंडरमधून जन्मतारीख निवडा",
                    isIsoFormat = true,
                    isDob = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पत्ता") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, gender, bloodGroup, dob, address, photoUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text("जतन करा")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (oldPass: String, newPass: String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("पासवर्ड बदला", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("सध्याचा पासवर्ड") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("नवीन पासवर्ड (किमान ४ अक्षरे)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(oldPass, newPass) },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text("बदला")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

@Composable
fun MemberFeedbackDialog(
    user: User,
    onDismiss: () -> Unit,
    onSubmit: (category: String, rating: Int, message: String) -> Unit
) {
    val categories = listOf(
        "सर्वसाधारण सूचना व मार्गदर्शन",
        "मंडळाचे उपक्रम व कार्यक्रम",
        "जय हिंद ॲप सुधारणा / समस्या",
        "आरोग्य, रक्तदान व सामाजिक मदत",
        "क्रीडा व स्पर्धा उपक्रम",
        "तक्रार किंवा अडचण"
    )

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var rating by remember { mutableStateOf(5) }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "मंडळासाठी अभिप्राय व सूचना",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "सभासद: ${user.fullName}",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Confidential Notice Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔒 आपली सूचना पूर्णपणे गोपनीय व सुरक्षित आहे. ही सूचना इतर कोणालाही न दिसता केवळ मंडळाच्या मुख्य ॲडमिनलाच दिसेल.",
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "विषय / प्रकार निवडा:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat,
                                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCategory == cat) SaffronPrimary else TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Star Rating
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "आपला अनुभव / रेटिंग (पर्यायी):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { rating = i },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$i Star",
                                    tint = if (i <= rating) SaffronPrimary else TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Message Text Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "आपला अभिप्राय किंवा सूचना:*",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                "मंडळाच्या प्रगतीसाठी, उपक्रमांसाठी किंवा ॲपसाठी आपल्या मोलाच्या सूचना येथे सविस्तर लिहा...",
                                fontSize = 12.5.sp,
                                color = TextSecondary
                            )
                        },
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_message_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = BloodRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.trim().isBlank()) {
                        errorMessage = "कृपया आपला अभिप्राय किंवा सूचना टाईप करा."
                        return@Button
                    }
                    isSubmitting = true
                    onSubmit(selectedCategory, rating, message.trim())
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_feedback_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("अभिप्राय पाठवा")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("रद्द करा")
            }
        }
    )
}
