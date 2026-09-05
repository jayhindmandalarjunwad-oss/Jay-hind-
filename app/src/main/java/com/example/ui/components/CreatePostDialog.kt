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
import kotlinx.coroutines.launch

@Composable
fun CreatePostDialog(
    currentUser: User?,
    initialPost: com.example.data.model.Post? = null,
    onDismiss: () -> Unit,
    onPostCreated: (content: String, imageUrl: String?, videoUrl: String?) -> Unit
) {
    val isEdit = initialPost != null
    var postText by remember { mutableStateOf(initialPost?.content ?: "") }
    var selectedImages by remember {
        mutableStateOf<List<String>>(initialPost?.imageUrls?.filter { it.isNotBlank() } ?: emptyList())
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessingImage by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("") }
    var uploadCurrentIndex by remember { mutableIntStateOf(0) }
    var uploadTotalCount by remember { mutableIntStateOf(0) }

    // Multi-photo gallery picker
    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlot = 10 - selectedImages.size
            val urisToProcess = uris.take(remainingSlot)
            if (urisToProcess.isNotEmpty()) {
                coroutineScope.launch {
                    isProcessingImage = true
                    uploadTotalCount = urisToProcess.size
                    val newImages = mutableListOf<String>()

                    urisToProcess.forEachIndexed { idx, uri ->
                        uploadCurrentIndex = idx + 1
                        uploadStatusText = "फोटो $uploadCurrentIndex/$uploadTotalCount कॉम्प्रेस व अपलोड होत आहे..."
                        try {
                            val uploadedUrl = FirebaseStorageHelper.uploadImage(
                                context = context,
                                uri = uri,
                                folder = "posts/images",
                                user = currentUser,
                                fileIndex = selectedImages.size + idx + 1
                            )
                            if (uploadedUrl.isNotBlank()) {
                                newImages.add(uploadedUrl)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CreatePostDialog", "Failed to upload photo: ${e.message}")
                        }
                    }

                    selectedImages = (selectedImages + newImages).distinct().take(10)
                    isProcessingImage = false
                    uploadStatusText = ""
                }
            }
        }
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
                            text = "सार्वजनिक (मंडळ सभासद)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Content Input
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = { Text("आपल्या मनात काय विचार किंवा मंडळाची माहिती आहे?", fontSize = 14.sp) },
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

                // Multiple Photos Header
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

                    TextButton(
                        onClick = { multiGalleryLauncher.launch("image/*") },
                        enabled = !isProcessingImage
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = SaffronPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedImages.isEmpty()) "गॅलरीतून निवडा" else "+ आणखी जोडा",
                            color = SaffronPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uploadStatusText.ifEmpty { "फोटो लोड व कॉम्प्रेस होत आहेत..." },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                        onPostCreated(postText, joinedImages, null)
                    },
                    enabled = !isProcessingImage && (postText.isNotBlank() || selectedImages.isNotEmpty()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_post_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isProcessingImage) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "फोटो अपलोड होत आहेत...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(imageVector = if (isEdit) Icons.Default.Check else Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "पोस्ट अपडेट करा (Update Post)" else "पोस्ट प्रसिद्ध करा (Post)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

