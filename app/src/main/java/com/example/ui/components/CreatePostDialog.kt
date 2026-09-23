package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.util.FirebaseStorageHelper
import com.example.util.MediaUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreatePostDialog(
    currentUser: User?,
    initialPost: com.example.data.model.Post? = null,
    onDismiss: () -> Unit,
    onPostCreated: (
        content: String,
        imageUrl: String?,
        videoUrl: String?,
        isSponsored: Boolean,
        sponsorBusinessName: String?,
        sponsorContactNumber: String?,
        sponsorCtaText: String?
    ) -> Unit
) {
    val isEdit = initialPost != null
    var postText by remember { mutableStateOf(initialPost?.content ?: "") }
    var selectedImages by remember {
        mutableStateOf<List<String>>(initialPost?.imageUrls?.filter { it.isNotBlank() } ?: emptyList())
    }

    // Sponsored Post state
    val canSponsor = currentUser?.isAnyAdmin == true
    var isSponsored by remember { mutableStateOf(initialPost?.isSponsored ?: false) }
    var sponsorBusinessName by remember { mutableStateOf(initialPost?.sponsorBusinessName ?: "") }
    var sponsorContactNumber by remember { mutableStateOf(initialPost?.sponsorContactNumber ?: "") }
    var sponsorCtaText by remember { mutableStateOf(initialPost?.sponsorCtaText ?: "संपर्क साधा / ऑर्डर द्या") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessingImage by remember { mutableStateOf(false) }

    // Cropper Dialog & Camera State
    var uriToCrop by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun uploadCroppedUri(uri: Uri) {
        coroutineScope.launch {
            try {
                isProcessingImage = true
                val rawAuthor = currentUser?.fullName ?: "Member"
                val cleanAuthor = rawAuthor.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val customName = "JayHind_Post_${cleanAuthor}_${timeStamp}_${selectedImages.size + 1}.webp"
                val uploadedUrl = withContext(Dispatchers.IO) {
                    FirebaseStorageHelper.uploadImage(context, uri, folder = "posts", customFileName = customName)
                }
                if (uploadedUrl.isNotBlank()) {
                    selectedImages = (selectedImages + uploadedUrl).distinct().take(10)
                }
            } catch (t: Throwable) {
                android.util.Log.e("CreatePostDialog", "Upload error: ${t.message}", t)
            } finally {
                isProcessingImage = false
            }
        }
    }

    // Camera Capture Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            uriToCrop = tempCameraUri
        }
    }

    // Single photo picker to open in Cropper
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uriToCrop = uri
        }
    }

    fun launchCamera() {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File.createTempFile("camera_", ".jpg", cacheDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("CreatePostDialog", "Camera launch error: ${e.message}")
        }
    }

    // Multi-photo gallery picker with standardized naming: JayHind_Post_[Author]_[Timestamp]_[Index].webp
    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                uriToCrop = uris[0]
            } else {
                coroutineScope.launch {
                    try {
                        isProcessingImage = true
                        val newImages = mutableListOf<String>()
                        val rawAuthor = currentUser?.fullName ?: "Member"
                        val cleanAuthor = rawAuthor.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
                        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        val startIdx = selectedImages.size

                        withContext(Dispatchers.IO) {
                            for ((idx, uri) in uris.withIndex()) {
                                val customName = "JayHind_Post_${cleanAuthor}_${timeStamp}_${startIdx + idx + 1}.webp"
                                val uploadedUrl = FirebaseStorageHelper.uploadImage(context, uri, folder = "posts", customFileName = customName)
                                if (uploadedUrl.isNotBlank()) {
                                    newImages.add(uploadedUrl)
                                }
                            }
                        }
                        selectedImages = (selectedImages + newImages).distinct().take(10)
                    } catch (t: Throwable) {
                        android.util.Log.e("CreatePostDialog", "Safe catch during image selection: ${t.message}", t)
                    } finally {
                        isProcessingImage = false
                        System.gc()
                    }
                }
            }
        }
    }

    // Cropper Dialog
    if (uriToCrop != null) {
        ImageCropperDialog(
            sourceUri = uriToCrop!!,
            onDismiss = { uriToCrop = null },
            onImageCropped = { croppedUri ->
                uriToCrop = null
                uploadCroppedUri(croppedUri)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "पोस्ट संपादित करा (Edit Post)" else "नवीन पोस्ट तयार करा",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Author Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        photoUrl = currentUser?.profilePhotoUrl ?: "",
                        name = currentUser?.fullName ?: "सभासद",
                        size = 40
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentUser?.fullName ?: "सभासद",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isSponsored) "✨ प्रायोजित जाहिरात (Sponsored Post)" else "सार्वजनिक (मंडळ सभासद)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSponsored) Color(0xFF0D9488) else TextSecondary,
                            fontWeight = if (isSponsored) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }

                // Sponsored Post Toggle for Admins
                if (canSponsor) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSponsored) Color(0xFFF0FDFA) else SurfaceWarm,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSponsored) Color(0xFF14B8A6) else CardBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = if (isSponsored) Color(0xFF0D9488) else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "ही प्रायोजित जाहिरात आहे का? (Sponsored)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "स्थानिक व्यावसायिक जाहिरात व थेट कॉल बटण",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = isSponsored,
                                    onCheckedChange = { isSponsored = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0D9488)
                                    )
                                )
                            }

                            if (isSponsored) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = sponsorBusinessName,
                                    onValueChange = { sponsorBusinessName = it },
                                    label = { Text("दुकानाचे / व्यवसायाचे नाव", fontSize = 12.sp) },
                                    placeholder = { Text("उदा. अर्जुन इलेक्ट्रॉनिक्स", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = sponsorContactNumber,
                                    onValueChange = { sponsorContactNumber = it },
                                    label = { Text("संपर्क / WhatsApp नंबर", fontSize = 12.sp) },
                                    placeholder = { Text("१० अंकी मोबाईल नंबर", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = sponsorCtaText,
                                    onValueChange = { sponsorCtaText = it },
                                    label = { Text("बटणावरील मजकूर (CTA)", fontSize = 12.sp) },
                                    placeholder = { Text("उदा. संपर्क साधा / ऑर्डर द्या", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Content Input
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = {
                        Text(
                            if (isSponsored) "जाहिरातीचा मजकूर, ऑफर्स किंवा नवीन उत्पादनांबद्दल माहिती लिहा..."
                            else "आपल्या मनात काय विचार किंवा मंडळाची माहिती आहे?",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp)
                        .testTag("create_post_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = DividerColor
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Multiple Photos Header with Camera & Cropper buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "फोटो जोडा (${selectedImages.size}/१०):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Row {
                        // Camera Button
                        IconButton(
                            onClick = { launchCamera() },
                            enabled = !isProcessingImage && selectedImages.size < 10
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "कॅमेरा",
                                tint = SaffronPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Gallery Button with Cropper
                        IconButton(
                            onClick = { singlePickerLauncher.launch("image/*") },
                            enabled = !isProcessingImage && selectedImages.size < 10
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "क्रॉप व एडिट",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Multi-gallery picker
                        IconButton(
                            onClick = { multiGalleryLauncher.launch("image/*") },
                            enabled = !isProcessingImage && selectedImages.size < 10
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "गॅलरी",
                                tint = NavySecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Image Selection List & Add Button
                if (isProcessingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceWarm),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("फोटो सुरक्षितपणे लोड होत आहेत...", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                } else if (selectedImages.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { multiGalleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceWarm,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "एका वेळी एकापेक्षा जास्त (Multiple) फोटो निवडा",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                "मोबाईल गॅलरीतून फोटो निवडण्यासाठी येथे टॅप करा",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(selectedImages) { index, imgUrl ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                            ) {
                                UniversalAsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Selected photo ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    targetDimensionPx = 250,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Delete button badge
                                IconButton(
                                    onClick = {
                                        selectedImages = selectedImages.toMutableList().apply { removeAt(index) }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Index badge
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(topEnd = 6.dp),
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Extra Add Tile
                        if (selectedImages.size < 10) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { multiGalleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceWarm,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add More",
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "+ आणखी",
                                            color = SaffronPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Post Submit Button
                Button(
                    onClick = {
                        val joinedImages = if (selectedImages.isNotEmpty()) {
                            selectedImages.joinToString("|||")
                        } else null
                        onPostCreated(
                            postText,
                            joinedImages,
                            null,
                            isSponsored,
                            if (isSponsored) sponsorBusinessName.ifBlank { null } else null,
                            if (isSponsored) sponsorContactNumber.ifBlank { null } else null,
                            if (isSponsored) sponsorCtaText.ifBlank { null } else null
                        )
                    },
                    enabled = !isProcessingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_post_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSponsored) Color(0xFF0D9488) else SaffronPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = if (isEdit) Icons.Default.Check else Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "पोस्ट अपडेट करा (Update Post)"
                        else if (isSponsored) "प्रायोजित जाहिरात प्रसिद्ध करा"
                        else "पोस्ट प्रसिद्ध करा (Post)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

