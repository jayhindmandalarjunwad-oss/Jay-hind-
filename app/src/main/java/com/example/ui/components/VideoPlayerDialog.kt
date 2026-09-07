package com.example.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import com.example.ui.theme.SaffronPrimary
import com.example.util.MediaUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/**
 * WhatsApp-style In-App Video Player Dialog for Chat & Group Chat
 */
@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    title: String = "व्हिडिओ",
    senderName: String = "",
    thumbnailUrl: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPreparing by remember { mutableStateOf(true) }
    var playableUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    var currentPosition by remember { mutableIntStateOf(0) }
    var totalDuration by remember { mutableIntStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Prepare video URI (decode base64 to temp mp4 or parse URI/URL)
    LaunchedEffect(videoUrl) {
        isPreparing = true
        hasError = false
        try {
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                // YouTube link: directly open via external intent
                MediaUtils.openVideo(context, videoUrl)
                onDismiss()
                return@LaunchedEffect
            }
            val uri = MediaUtils.prepareVideoUriForPlayback(context, videoUrl)
            playableUri = uri
        } catch (e: Exception) {
            hasError = true
            errorMessage = e.localizedMessage ?: "व्हिडिओ लोड करता आला नाही"
        } finally {
            isPreparing = false
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // Progress update ticker
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            videoViewRef?.let { vv ->
                try {
                    if (vv.isPlaying) {
                        currentPosition = vv.currentPosition
                        val dur = vv.duration
                        if (dur > 0) totalDuration = dur
                    }
                } catch (_: Exception) {}
            }
            delay(300)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    isControlsVisible = !isControlsVisible
                }
                .testTag("whatsapp_video_player_dialog")
        ) {
            // Central Video View
            if (isPreparing) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "व्हिडिओ डाऊनलोड व लोड होत आहे...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (hasError) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "व्हिडिओ प्ले करताना त्रुटी आली",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = {
                                hasError = false
                                isPreparing = true
                                scope.launch {
                                    try {
                                        playableUri = MediaUtils.prepareVideoUriForPlayback(context, videoUrl)
                                    } catch (e: Exception) {
                                        hasError = true
                                        errorMessage = e.localizedMessage ?: "व्हिडिओ लोड करता आला नाही"
                                    } finally {
                                        isPreparing = false
                                    }
                                }
                            }
                        ) {
                            Text("पुन्हा प्रयत्न करा (Retry)", color = SaffronPrimary)
                        }
                        TextButton(
                            onClick = {
                                MediaUtils.openVideo(context, videoUrl)
                            }
                        ) {
                            Text("बाह्य ॲपमध्ये उघडा", color = Color.White)
                        }
                    }
                }
            } else if (playableUri != null) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setZOrderMediaOverlay(true)
                            val uriToPlay = if (playableUri?.scheme == "file") {
                                val file = File(playableUri?.path ?: "")
                                if (file.exists()) {
                                    try {
                                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                                    } catch (_: Exception) {
                                        playableUri
                                    }
                                } else playableUri
                            } else playableUri

                            setVideoURI(uriToPlay)
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                totalDuration = mp.duration
                                start()
                                isPlaying = true
                                isCompleted = false
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                isCompleted = true
                                isControlsVisible = true
                            }
                            setOnErrorListener { _, what, extra ->
                                hasError = true
                                errorMessage = if (videoUrl.startsWith("file://") || videoUrl.startsWith("/")) {
                                    "हा व्हिडिओ स्थानिक फाईलमधून उघडता आला नाही. कृपया प्रेषकाला हा व्हिडिओ पुन्हा पाठवण्यास सांगा."
                                } else {
                                    "व्हिडिओ प्ले करताना अडचण आली (त्रुटी: $what, $extra). 'बाह्य ॲपमध्ये उघडा' बटण वापरून पहा."
                                }
                                true
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // WhatsApp Style Semi-transparent Top Header Controls
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Video",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = title.ifEmpty { "व्हिडिओ संदेश" },
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (senderName.isNotBlank()) {
                                Text(
                                    text = "प्रेषक: $senderName",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Save to Gallery button
                        IconButton(
                            onClick = {
                                if (!isSaving) {
                                    isSaving = true
                                    scope.launch {
                                        val prefix = "JayHind_Video_${senderName.replace(Regex("[^a-zA-Z0-9_]"), "")}"
                                        MediaUtils.saveVideoToGallery(
                                            context = context,
                                            videoUrlOrBase64 = videoUrl,
                                            fileNamePrefix = prefix
                                        )
                                        isSaving = false
                                    }
                                }
                            }
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save to Gallery",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Open in External Player
                        IconButton(
                            onClick = {
                                MediaUtils.openVideo(context, videoUrl)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in External App",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Big Center Play/Pause/Replay Button Overlay
            AnimatedVisibility(
                visible = isControlsVisible && !isPreparing && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    if (isCompleted) {
                                        vv.seekTo(0)
                                        vv.start()
                                        isPlaying = true
                                        isCompleted = false
                                    } else if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isCompleted -> Icons.Default.Replay
                                    isPlaying -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Progress Bar & Time Controls
            AnimatedVisibility(
                visible = isControlsVisible && !isPreparing && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Time & Scrubber Slider
                        val progress = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                        var sliderPosition by remember(progress) { mutableFloatStateOf(progress) }

                        Slider(
                            value = sliderPosition,
                            onValueChange = { newPos ->
                                sliderPosition = newPos
                                videoViewRef?.let { vv ->
                                    val targetMs = (newPos * totalDuration).toInt()
                                    vv.seekTo(targetMs)
                                    currentPosition = targetMs
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = SaffronPrimary,
                                activeTrackColor = SaffronPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currSec = currentPosition / 1000
                            val totalSec = totalDuration / 1000
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", currSec / 60, currSec % 60),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
