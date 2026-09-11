package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import com.example.util.AudioPlayerManager
import com.example.util.MediaUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    isSentByMe: Boolean,
    isGroupChat: Boolean = false,
    isAdmin: Boolean = false,
    onImageClick: (String) -> Unit = {},
    onVideoClick: ((ChatMessage) -> Unit)? = null,
    onDeleteClick: ((ChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var isDownloadingImage by remember { mutableStateOf(false) }
    var isDownloadingDoc by remember { mutableStateOf(false) }
    var isOpeningDoc by remember { mutableStateOf(false) }
    var isDownloadingVideo by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showContactActionDialog by remember { mutableStateOf(false) }

    val canDelete = isSentByMe || (isGroupChat && isAdmin)

    val bubbleColor = if (isSentByMe) SaffronContainer else SurfaceWarm
    val textColor = if (isSentByMe) Color(0xFF4A1A00) else TextPrimary
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    val isVoiceMessage = message.attachmentType == "VOICE" || message.attachmentType == "AUDIO"
    val isPlayingThis = AudioPlayerManager.activePlayingMessageId == message.id && AudioPlayerManager.isPlaying

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete",
                    tint = BloodRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "मेसेज हटवा (Delete Message)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isGroupChat && !isSentByMe && isAdmin) {
                        "मंडळ ॲडमिन अधिकार: '${message.senderName}' यांचा हा मेसेज ग्रुपमधून सर्वांसाठी कायमचा हटवायचा आहे का?"
                    } else {
                        "हा संदेश चॅटमधून सर्वांसाठी कायमचा काढून टाकला जाईल. आपण खात्री केली आहे का?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick?.invoke(message)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("हटवा (Delete)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // Contact Details & Action Dialog
    if (showContactActionDialog) {
        val contactName = message.attachmentName ?: "मंडळ संपर्क"
        val contactPhone = message.attachmentExtra ?: message.attachmentUrl ?: ""

        AlertDialog(
            onDismissRequest = { showContactActionDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Contact",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceWarm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("मोबाईल नंबर:", fontSize = 11.sp, color = TextSecondary)
                                Text(contactPhone, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(contactPhone))
                                    Toast.makeText(context, "नंबर कॉपी झाला: $contactPhone", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SaffronPrimary)
                            }
                        }
                    }

                    // Action Buttons: Call, SMS, Save
                    Button(
                        onClick = {
                            showContactActionDialog = false
                            if (contactPhone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("कॉल करा (Call $contactPhone)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showContactActionDialog = false
                            if (contactPhone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contactPhone"))
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SMS संदेश पाठवा")
                    }

                    OutlinedButton(
                        onClick = {
                            showContactActionDialog = false
                            try {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.NAME, contactName)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, contactPhone)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "संपर्क सेव्ह करता आला नाही", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("मोबाईल संपर्कामध्ये सेव्ह करा")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContactActionDialog = false }) {
                    Text("बंद करा")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        // In Group Chat, when message is from another member:
        // Display ONLY sender's profile photo and full name (Strictly NO designation / पद)
        if (!isSentByMe && (isGroupChat || message.receiverId == "GROUP_MANDAL" || message.conversationId == "conv_mandal_group")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 3.dp, start = 4.dp)
            ) {
                if (!message.senderPhotoUrl.isNullOrBlank()) {
                    UniversalAsyncImage(
                        model = message.senderPhotoUrl,
                        contentDescription = message.senderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = SaffronPrimary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = message.senderName.take(1),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = SaffronDark
                )
            }
        }

        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isSentByMe) 14.dp else 2.dp,
                    bottomEnd = if (isSentByMe) 2.dp else 14.dp
                ),
                color = bubbleColor,
                shadowElevation = 1.5.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (canDelete || message.messageText.isNotBlank()) {
                                showOptionsMenu = true
                            }
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {

                // 1. IMAGE ATTACHMENT (Natural Aspect Ratio / Original Size)
                val imageToDisplay = message.attachmentUrl.takeIf { message.attachmentType == "IMAGE" } ?: message.imageUrl
                if (!imageToDisplay.isNullOrBlank()) {
                    val bitmap = remember(imageToDisplay) {
                        if (imageToDisplay.startsWith("data:") || imageToDisplay.length > 80) {
                            MediaUtils.base64ToBitmap(imageToDisplay)
                        } else null
                    }

                    val imageModifier = if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                        val rawRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val safeRatio = rawRatio.coerceIn(0.45f, 2.5f)
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(safeRatio)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(imageToDisplay) }
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 340.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(imageToDisplay) }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        UniversalAsyncImage(
                            model = bitmap ?: imageToDisplay,
                            contentDescription = "Chat Image",
                            contentScale = ContentScale.Fit,
                            modifier = imageModifier
                        )

                        // Direct Download Button for Image (Pictures/JayHind_Mandal_Chat)
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clickable {
                                    if (!isDownloadingImage) {
                                        scope.launch {
                                            isDownloadingImage = true
                                            val cleanSender = message.senderName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
                                            val photoPrefix = "JayHind_ChatPhoto_${cleanSender}"
                                            MediaUtils.saveImageToGallery(
                                                context = context,
                                                imageUrlOrBase64 = imageToDisplay,
                                                fileNamePrefix = photoPrefix,
                                                subFolder = "JayHind_Mandal_Chat"
                                            )
                                            isDownloadingImage = false
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDownloadingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "फोटो गॅलरीमध्ये सेव्ह करा",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 2. VOICE NOTE ATTACHMENT
                if (isVoiceMessage) {
                    VoiceNotePlayerCard(
                        messageId = message.id,
                        audioSource = message.attachmentUrl ?: "",
                        durationLabel = message.attachmentExtra ?: "0:28",
                        isSentByMe = isSentByMe,
                        isPlaying = isPlayingThis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. VIDEO ATTACHMENT
                if (message.attachmentType == "VIDEO") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable {
                                if (onVideoClick != null) {
                                    onVideoClick(message)
                                } else {
                                    MediaUtils.openVideo(context, message.attachmentUrl ?: "")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!message.attachmentExtra.isNullOrBlank()) {
                            UniversalAsyncImage(
                                model = message.attachmentExtra,
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = SaffronPrimary,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Direct Download Button for Video
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .clickable {
                                    val vidUrl = message.attachmentUrl ?: ""
                                    if (vidUrl.isNotBlank() && !isDownloadingVideo) {
                                        scope.launch {
                                            isDownloadingVideo = true
                                            val cleanSender = message.senderName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "Member" }
                                            val videoPrefix = "JayHind_ChatVideo_${cleanSender}"
                                            MediaUtils.saveVideoToGallery(
                                                context = context,
                                                videoUrlOrBase64 = vidUrl,
                                                fileNamePrefix = videoPrefix
                                            )
                                            isDownloadingVideo = false
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDownloadingVideo) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "व्हिडिओ डाऊनलोड करा",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.attachmentName ?: "व्हिडिओ",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 4. DOCUMENT / PDF ATTACHMENT
                if (message.attachmentType == "DOCUMENT") {
                    val docUrl = message.attachmentUrl ?: ""
                    val docName = message.attachmentName ?: "दस्तावेज.pdf"

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (docUrl.isNotBlank() && !isOpeningDoc) {
                                    scope.launch {
                                        isOpeningDoc = true
                                        try {
                                            MediaUtils.openDocumentFile(context, docUrl, docName)
                                        } finally {
                                            isOpeningDoc = false
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BloodRed.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isOpeningDoc) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = BloodRed
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF Document",
                                            tint = BloodRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = docName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isOpeningDoc) "PDF उघडत आहे..." else (message.attachmentExtra ?: "PDF Document • उघडण्यासाठी टॅप करा"),
                                    fontSize = 11.sp,
                                    color = if (isOpeningDoc) BloodRed else TextSecondary
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDownloadingDoc) SaffronPrimary.copy(alpha = 0.2f) else SaffronPrimary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        if (docUrl.isNotBlank() && !isDownloadingDoc) {
                                            scope.launch {
                                                isDownloadingDoc = true
                                                MediaUtils.saveDocumentToDownloads(
                                                    context = context,
                                                    docUrlOrBase64 = docUrl,
                                                    suggestedFileName = docName,
                                                    subFolder = "JayHind_Mandal_Chat"
                                                )
                                                isDownloadingDoc = false
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isDownloadingDoc) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = SaffronPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download to device",
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 5. CONTACT ATTACHMENT
                if (message.attachmentType == "CONTACT") {
                    val contactPhone = message.attachmentExtra ?: message.attachmentUrl ?: ""
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showContactActionDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = SaffronLight.copy(alpha = 0.3f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Contact",
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.attachmentName ?: "मंडळ सदस्य",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = contactPhone,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                IconButton(
                                    onClick = { showContactActionDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Contact Details",
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = DividerColor)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (contactPhone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कॉल करा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showContactActionDialog = true },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(14.dp), tint = SaffronPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("माहिती पहा", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SaffronPrimary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text (if any)
                if (message.messageText.isNotBlank()) {
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Timestamp & Delivery status & Options
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                    if (isSentByMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = if (message.isRead) NavySecondary else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (canDelete || message.messageText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dropdown Menu for message actions (Delete, Copy)
        DropdownMenu(
            expanded = showOptionsMenu,
            onDismissRequest = { showOptionsMenu = false }
        ) {
            if (message.messageText.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("मजकूर कॉपी करा (Copy)") },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showOptionsMenu = false
                        clipboardManager.setText(AnnotatedString(message.messageText))
                        Toast.makeText(context, "मेसेज कॉपी केला", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isGroupChat && !isSentByMe && isAdmin) "ग्रुपमधून हटवा (Admin Delete)" else "मेसेज हटवा (Delete)",
                            color = BloodRed,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = BloodRed,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showOptionsMenu = false
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }
}
}

@Composable
fun VoiceNotePlayerCard(
    messageId: String,
    audioSource: String,
    durationLabel: String,
    isSentByMe: Boolean,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSavingAudio by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveHeight"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSentByMe) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Action Button
            Surface(
                shape = CircleShape,
                color = SaffronPrimary,
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        AudioPlayerManager.playOrToggle(context, messageId, audioSource)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform Graphic / Progress Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.85f, 0.7f, 0.5f, 0.9f, 0.6f, 0.4f)
                    val progress = if (isPlaying) AudioPlayerManager.playbackProgress else 0f
                    val activeBarCount = (barHeights.size * progress).toInt()

                    barHeights.forEachIndexed { index, baseHeight ->
                        val hMultiplier = if (isPlaying) {
                            if (index % 2 == 0) waveAnim else (1.3f - waveAnim)
                        } else {
                            1f
                        }
                        val isActive = index <= activeBarCount

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((18.dp * baseHeight * hMultiplier).coerceIn(4.dp, 20.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isActive) SaffronPrimary else TextSecondary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlaying) {
                            val curSec = AudioPlayerManager.currentPositionSeconds
                            String.format(Locale.getDefault(), "%02d:%02d", curSec / 60, curSec % 60)
                        } else {
                            "🎙️ व्हॉईस मेसेज"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SaffronDark
                    )

                    Text(
                        text = durationLabel,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Direct Download Button for Voice Note (Music/JayHind_Mandal_Chat)
            Surface(
                shape = CircleShape,
                color = SaffronPrimary.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        if (!isSavingAudio && audioSource.isNotBlank()) {
                            scope.launch {
                                isSavingAudio = true
                                MediaUtils.saveVoiceNoteToStorage(
                                    context = context,
                                    voiceUrlOrBase64 = audioSource,
                                    suggestedFileName = "JayHind_Voice_${messageId}.m4a",
                                    subFolder = "JayHind_Mandal_Chat"
                                )
                                isSavingAudio = false
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSavingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = SaffronPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "व्हॉईस नोट सेव्ह करा",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
