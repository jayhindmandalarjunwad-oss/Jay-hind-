package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSentByMe: Boolean,
    isGroupChat: Boolean = false,
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloadingDoc by remember { mutableStateOf(false) }
    val bubbleColor = if (isSentByMe) SaffronContainer else SurfaceWarm
    val textColor = if (isSentByMe) Color(0xFF4A1A00) else TextPrimary
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    val isVoiceMessage = message.attachmentType == "VOICE" || message.attachmentType == "AUDIO"
    val isPlayingThis = AudioPlayerManager.activePlayingMessageId == message.id && AudioPlayerManager.isPlaying

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
                    AsyncImage(
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

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isSentByMe) 14.dp else 2.dp,
                bottomEnd = if (isSentByMe) 2.dp else 14.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.5.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {

                // 1. IMAGE ATTACHMENT
                val imageToDisplay = message.attachmentUrl.takeIf { message.attachmentType == "IMAGE" } ?: message.imageUrl
                if (!imageToDisplay.isNullOrBlank()) {
                    UniversalAsyncImage(
                        model = imageToDisplay,
                        contentDescription = "Chat Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(imageToDisplay) }
                    )
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
                                val url = message.attachmentUrl ?: "https://www.youtube.com"
                                try {
                                    val uri = Uri.parse(url)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        if (url.startsWith("content://") || url.startsWith("file://")) {
                                            setDataAndType(uri, "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        } else {
                                            data = uri
                                        }
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://youtube.com"))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {}
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
                                if (docUrl.isNotBlank()) {
                                    scope.launch {
                                        MediaUtils.openDocumentFile(context, docUrl, docName)
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
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "PDF Document",
                                        tint = BloodRed,
                                        modifier = Modifier.size(22.dp)
                                    )
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
                                    text = message.attachmentExtra ?: "PDF Document • उघडण्यासाठी टॅप करा",
                                    fontSize = 11.sp,
                                    color = TextSecondary
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
                                                MediaUtils.saveDocumentToDownloads(context, docUrl, docName)
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
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
                                        text = message.attachmentExtra ?: message.attachmentUrl ?: "",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = DividerColor)
                            Spacer(modifier = Modifier.height(6.dp))

                            val contactPhone = message.attachmentExtra ?: message.attachmentUrl ?: ""
                            Button(
                                onClick = {
                                    if (contactPhone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("कॉल करा (Call)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                // Timestamp & Delivery status
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
        }
    }
}
