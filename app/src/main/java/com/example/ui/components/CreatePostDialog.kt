package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun CreatePostDialog(
    currentUser: User?,
    initialPost: com.example.data.model.Post? = null,
    onDismiss: () -> Unit,
    onPostCreated: (content: String, imageUrl: String?, videoUrl: String?) -> Unit
) {
    val isEdit = initialPost != null
    var postText by remember { mutableStateOf(initialPost?.content ?: "") }
    var selectedImageUrl by remember { mutableStateOf<String?>(initialPost?.imageUrls?.firstOrNull()) }
    var customImageUrl by remember { mutableStateOf("") }
    var showCustomImageInput by remember { mutableStateOf(false) }

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=800&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1608889175123-8ee362201f81?w=800&auto=format&fit=crop&q=80"
    )

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
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = DividerColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Selection Section (from Gallery)
                Text(
                    text = "पोस्टमध्ये फोटो जोडा (गॅलरीतून निवडा):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                GalleryImagePicker(
                    selectedImageUrl = selectedImageUrl,
                    onImageSelected = { selectedImageUrl = it },
                    label = "गॅलरीतून फोटो निवडा",
                    helperText = "मोबाईल गॅलरीतून फोटो अपलोड करण्यासाठी क्लिक करा",
                    height = 130.dp
                )

                if (selectedImageUrl != null) {
                    TextButton(
                        onClick = { selectedImageUrl = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = BloodRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("फोटो काढून टाका", fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "किंवा नमुना फोटो निवडा:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetImages) { imgUrl ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        if (selectedImageUrl == imgUrl) 2.dp else 0.dp,
                                        if (selectedImageUrl == imgUrl) SaffronPrimary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedImageUrl = imgUrl }
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Preset Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post Submit Button
                Button(
                    onClick = {
                        onPostCreated(postText, selectedImageUrl, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_post_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
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
