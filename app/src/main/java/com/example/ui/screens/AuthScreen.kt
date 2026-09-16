package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.FirebaseStorageHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MandalViewModel,
    isRegisterModeInitial: Boolean = false
) {
    var isRegisterMode by remember { mutableStateOf(isRegisterModeInitial) }
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val standardTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = SaffronPrimary,
        focusedBorderColor = SaffronPrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = SaffronPrimary,
        unfocusedLabelColor = Color(0xFF64748B),
        focusedLeadingIconColor = SaffronPrimary,
        unfocusedLeadingIconColor = SaffronPrimary
    )
    val standardTextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF0F172A)
    )

    // Login Form State
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()
    var loginMobile by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Register Form State
    var regFullName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regGender by remember { mutableStateOf("पुरुष") }
    var regBloodGroup by remember { mutableStateOf("O+") }
    var regDob by remember { mutableStateOf("1998-08-22") }
    var regAddress by remember { mutableStateOf("अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर") }
    var regPhotoUrl by remember { mutableStateOf("") }

    val bloodGroups = listOf("A+", "B+", "AB+", "O+", "A-", "B-", "AB-", "O-")
    val genders = listOf("पुरुष", "स्त्री", "इतर")

    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("auth_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Gradient Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(SaffronPrimary, SaffronLight)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MandalLogoBadge(logoUrl = mandalLogoUrl, size = 82)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "अर्जुनवाड, ता. शिरोळ (कोल्हापूर)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Tab Switcher: लॉगिन vs नवीन नोंदणी
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { isRegisterMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRegisterMode) SaffronPrimary else Color.Transparent,
                            contentColor = if (!isRegisterMode) Color.White else TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = if (!isRegisterMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("सभासद लॉगिन", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { isRegisterMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRegisterMode) SaffronPrimary else Color.Transparent,
                            contentColor = if (isRegisterMode) Color.White else TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = if (isRegisterMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("नवीन नोंदणी", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Form Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (!isRegisterMode) {
                    // LOGIN FORM
                    Text(
                        text = "आपल्या खात्यात प्रवेश करा",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "फक्त ॲडमिनने मंजूर (Approved) केलेले सभासद लॉगिन करू शकतात.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = loginMobile,
                        onValueChange = { loginMobile = it },
                        label = { Text("मोबाईल नंबर") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SaffronPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_mobile_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("पासवर्ड") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SaffronPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "पासवर्ड दाखवा"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isLoggingIn) {
                                viewModel.login(loginMobile, loginPassword) {}
                            }
                        },
                        enabled = !isLoggingIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("तपासत आहे...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("लॉगिन करा (Login)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // REGISTRATION FORM
                    Text(
                        text = "नवीन सभासद नोंदणी अर्ज",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "नोंदणीनंतर आपले खाते 'Pending Approval' मध्ये जाईल. ॲडमिन मंजुरीनंतर लॉगिन करता येईल.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PendingOrange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular Profile Photo Selection via Gallery
                    CircularProfilePicker(
                        selectedImageUrl = regPhotoUrl,
                        onImageSelected = { regPhotoUrl = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("संपूर्ण नाव (उदा. वैभव चौगुले)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SaffronPrimary) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regMobile,
                        onValueChange = { regMobile = it },
                        label = { Text("मोबाईल नंबर (१० अंकी)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SaffronPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_mobile_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gender Selection
                    Text("लिंग निवडा:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genders.forEach { g ->
                            FilterChip(
                                selected = regGender == g,
                                onClick = { regGender = g },
                                label = { Text(g) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Blood Group Selection
                    Text("रक्तगट निवडा:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(bloodGroups) { bg ->
                            FilterChip(
                                selected = regBloodGroup == bg,
                                onClick = { regBloodGroup = bg },
                                label = { Text(bg, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BloodRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MandalDatePickerField(
                        value = regDob,
                        onValueChange = { regDob = it },
                        label = "जन्म तारीख (DOB)",
                        placeholder = "कॅलेंडरमधून जन्मतारीख निवडा",
                        isIsoFormat = true,
                        isDob = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regAddress,
                        onValueChange = { regAddress = it },
                        label = { Text("पत्ता (अर्जुनवाड)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = SaffronPrimary) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("पासवर्ड तयार करा") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SaffronPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = standardTextStyle,
                        colors = standardTextFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.register(
                                fullName = regFullName,
                                mobileNumber = regMobile,
                                password = regPassword,
                                profilePhotoUrl = regPhotoUrl,
                                gender = regGender,
                                bloodGroup = regBloodGroup,
                                dateOfBirth = regDob,
                                address = regAddress,
                                onSuccess = {
                                    isRegisterMode = false
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("reg_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HowToReg, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("नोंदणी पूर्ण करा (Register)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun CircularProfilePicker(
    selectedImageUrl: String?,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val uploadedUrl = FirebaseStorageHelper.uploadImage(context, uri, "profile_photos")
                isUploading = false
                if (uploadedUrl.isNotBlank()) {
                    onImageSelected(uploadedUrl)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(Color.White, CircleShape)
                .clickable(enabled = !isUploading) { galleryLauncher.launch("image/*") }
                .testTag("reg_profile_photo_picker"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                border = BorderStroke(2.5.dp, if (!selectedImageUrl.isNullOrBlank()) SaffronPrimary else Color(0xFFCBD5E1)),
                shadowElevation = 3.dp
            ) {
                if (!selectedImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = selectedImageUrl,
                        contentDescription = "प्रोफाइल फोटो",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Camera badge at bottom-right
            Surface(
                shape = CircleShape,
                color = SaffronPrimary,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "फोटो निवडा",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !isUploading) { galleryLauncher.launch("image/*") }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = SaffronPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (!selectedImageUrl.isNullOrBlank()) "फोटो बदला (गॅलरीतून निवडा)" else "गॅलरीतून प्रोफाइल फोटो निवडा",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SaffronPrimary
            )
        }
    }
}
