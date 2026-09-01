package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.LiveComment
import com.example.data.model.MandalInfo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.random.Random

enum class StreamPlatform(val displayName: String) {
    YOUTUBE("YouTube"),
    FACEBOOK("Facebook Live"),
    INSTAGRAM("Instagram Live"),
    DIRECT_HLS("Direct Stream / HLS"),
    CUSTOM("Custom Stream")
}

fun extractYouTubeId(url: String): String {
    if (url.isBlank()) return ""
    val clean = url.trim()
    if (clean.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        return clean
    }
    val youtuBeRegex = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})")
    youtuBeRegex.find(clean)?.let { return it.groupValues[1] }

    val watchRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
    watchRegex.find(clean)?.let { return it.groupValues[1] }

    val liveRegex = Regex("youtube\\.com/live/([a-zA-Z0-9_-]{11})")
    liveRegex.find(clean)?.let { return it.groupValues[1] }

    val embedRegex = Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})")
    embedRegex.find(clean)?.let { return it.groupValues[1] }

    return ""
}

fun detectStreamPlatform(url: String): StreamPlatform {
    val clean = url.trim().lowercase()
    return when {
        clean.contains("youtu.be") || clean.contains("youtube.com") || extractYouTubeId(url).isNotEmpty() -> StreamPlatform.YOUTUBE
        clean.contains("facebook.com") || clean.contains("fb.watch") || clean.contains("fb.me") -> StreamPlatform.FACEBOOK
        clean.contains("instagram.com") || clean.contains("instagr.am") -> StreamPlatform.INSTAGRAM
        clean.contains(".m3u8") || clean.contains(".mp4") || clean.contains("rtmp://") || clean.contains(".webm") -> StreamPlatform.DIRECT_HLS
        clean.startsWith("http://") || clean.startsWith("https://") -> StreamPlatform.CUSTOM
        else -> StreamPlatform.YOUTUBE
    }
}

private data class FloatingParticle(
    val id: Long,
    val text: String,
    val startOffsetX: Float
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamDialog(
    mandalInfo: MandalInfo,
    mandalLogoUrl: String? = null,
    comments: List<LiveComment> = emptyList(),
    onDismiss: () -> Unit,
    onSendReaction: (String) -> Unit = {},
    onPostComment: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var isManualFullscreen by remember { mutableStateOf(false) }
    var typedComment by remember { mutableStateOf("") }
    var viewerCount by remember { mutableIntStateOf(mandalInfo.liveViewerCount.coerceAtLeast(148)) }

    // Custom Player Controls State
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isPlayerLoading by remember { mutableStateOf(true) }
    var showControlsOverlay by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val streamKeyOrUrl by remember(mandalInfo.liveStreamUrl) { mutableStateOf(mandalInfo.liveStreamUrl) }

    // Floating reaction bubbles list
    var floatingParticles by remember { mutableStateOf<List<FloatingParticle>>(emptyList()) }

    // Auto-hide controls overlay after 4 seconds
    LaunchedEffect(showControlsOverlay, isPlaying) {
        if (showControlsOverlay && isPlaying) {
            delay(4000)
            showControlsOverlay = false
        }
    }

    fun triggerFloatingReaction(reactionText: String) {
        val newParticle = FloatingParticle(
            id = System.currentTimeMillis() + Random.nextLong(1000),
            text = reactionText,
            startOffsetX = Random.nextFloat() * 0.7f + 0.15f
        )
        floatingParticles = floatingParticles + newParticle
        viewerCount += 1
        onSendReaction(reactionText)

        coroutineScope.launch {
            delay(2200)
            floatingParticles = floatingParticles.filter { it.id != newParticle.id }
        }
    }

    // Toggle Landscape / Fullscreen
    fun toggleOrientationFullscreen() {
        if (activity != null) {
            if (isLandscape || isManualFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isManualFullscreen = false
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                isManualFullscreen = true
            }
        } else {
            isManualFullscreen = !isManualFullscreen
        }
    }

    // Toggle Play/Pause via JavaScript
    fun togglePlayPause() {
        if (isPlaying) {
            webViewInstance?.evaluateJavascript("if(window.pauseVideo) window.pauseVideo();", null)
            isPlaying = false
            showControlsOverlay = true
        } else {
            webViewInstance?.evaluateJavascript("if(window.playVideo) window.playVideo();", null)
            isPlaying = true
            showControlsOverlay = true
        }
    }

    // Toggle Mute/Unmute via JavaScript
    fun toggleMute() {
        if (isMuted) {
            webViewInstance?.evaluateJavascript("if(window.unMuteVideo) window.unMuteVideo();", null)
            isMuted = false
        } else {
            webViewInstance?.evaluateJavascript("if(window.muteVideo) window.muteVideo();", null)
            isMuted = true
        }
        showControlsOverlay = true
    }

    // Reload stream
    fun reloadStream() {
        isPlayerLoading = true
        webViewInstance?.reload()
    }

    // Restore orientation when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val streamTitle = mandalInfo.liveStreamTitle.ifEmpty { "श्री गणेश महाआरती थेट प्रक्षेपण" }
    val isLiveActive = mandalInfo.isLiveStreamActive
    val platform = remember(streamKeyOrUrl) { detectStreamPlatform(streamKeyOrUrl) }

    // Pulse animation for LIVE badge
    val infiniteTransition = rememberInfiniteTransition(label = "live_badge_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    val commentsListState = rememberLazyListState()
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            commentsListState.animateScrollToItem(comments.size - 1)
        }
    }

    val effectiveFullscreen = isLandscape || isManualFullscreen

    Dialog(
        onDismissRequest = {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = !effectiveFullscreen
        )
    ) {
        Card(
            modifier = if (effectiveFullscreen) {
                Modifier
                    .fillMaxSize()
                    .testTag("live_stream_dialog_fullscreen")
            } else {
                Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(0.95f)
                    .testTag("live_stream_dialog_portrait")
            },
            shape = if (effectiveFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Top Custom Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8E0E00), Color(0xFF1F1C18))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = if (effectiveFullscreen) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLiveActive) BloodRed.copy(alpha = alphaAnim) else SaffronPrimary,
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLiveActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                                Text(
                                    text = if (isLiveActive) "🔴 थेट दर्शन" else "मंडळ दर्शन",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = streamTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Fullscreen / Rotate Toggle Button
                        IconButton(
                            onClick = { toggleOrientationFullscreen() },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("live_fullscreen_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (effectiveFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (effectiveFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White
                            )
                        }

                        // Close Dialog Button
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("close_live_stream_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "बंद करा",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Main Content Body: Video Player + Chat / Controls
                if (effectiveFullscreen) {
                    // Fullscreen Video View (Landscape Mode)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable { showControlsOverlay = !showControlsOverlay },
                        contentAlignment = Alignment.Center
                    ) {
                        // Smart Multi-Platform Live Player
                        SmartMultiPlatformPlayer(
                            streamUrl = streamKeyOrUrl,
                            platform = platform,
                            onWebViewCreated = { webViewInstance = it },
                            onLoadingChange = { isPlayerLoading = it },
                            onPlayStateChange = { playing -> isPlaying = playing },
                            modifier = Modifier.fillMaxSize()
                        )

                        // TV-Style Corner Watermark Logo (न्यूज चॅनेल प्रमाणे कोपऱ्यात लोगो)
                        TvCornerWatermark(
                            logoUrl = mandalLogoUrl,
                            isLive = isLiveActive,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 12.dp)
                        )

                        // Loading / Paused State Overlay with Center Mandal Logo
                        if (isPlayerLoading || !isPlaying) {
                            MandalLogoPlayerOverlay(
                                logoUrl = mandalLogoUrl,
                                isLoading = isPlayerLoading,
                                isPlaying = isPlaying,
                                onPlayClick = { togglePlayPause() }
                            )
                        }

                        // Floating Reaction Overlay in Fullscreen
                        FloatingReactionOverlay(particles = floatingParticles)

                        // Custom HUD Overlay for Fullscreen
                        if (showControlsOverlay) {
                            CustomPlayerControlsOverlay(
                                isPlaying = isPlaying,
                                isMuted = isMuted,
                                isLoading = isPlayerLoading,
                                viewerCount = viewerCount,
                                isFullscreen = true,
                                platform = platform,
                                onPlayPauseClick = { togglePlayPause() },
                                onMuteToggle = { toggleMute() },
                                onFullscreenToggle = { toggleOrientationFullscreen() },
                                onReloadClick = { reloadStream() }
                            )
                        }

                        // Compact Reaction Bar at bottom in Landscape
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("प्रतिक्रिया:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val quickList = listOf(
                                "🚩 जय हिंद!",
                                "🙏 बाप्पा मोरया!",
                                "🌸 आरती व फुले",
                                "👏 टाळ्या",
                                "🔔 घंटी"
                            )
                            quickList.forEach { reaction ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SaffronPrimary.copy(alpha = 0.9f),
                                    modifier = Modifier.clickable { triggerFloatingReaction(reaction) }
                                ) {
                                    Text(
                                        text = reaction,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Mode: Top Video Player + Bottom Live Chat & Actions
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. BRANDED MULTI-PLATFORM VIDEO PLAYER (16:9 Aspect Ratio)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                                .clickable { showControlsOverlay = !showControlsOverlay },
                            contentAlignment = Alignment.Center
                        ) {
                            SmartMultiPlatformPlayer(
                                streamUrl = streamKeyOrUrl,
                                platform = platform,
                                onWebViewCreated = { webViewInstance = it },
                                onLoadingChange = { isPlayerLoading = it },
                                onPlayStateChange = { playing -> isPlaying = playing },
                                modifier = Modifier.fillMaxSize()
                            )

                            // TV-Style Corner Watermark Logo (न्यूज चॅनेल प्रमाणे कोपऱ्यात लोगो)
                            TvCornerWatermark(
                                logoUrl = mandalLogoUrl,
                                isLive = isLiveActive,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                            )

                            // Loading / Paused Center Mandal Logo Overlay
                            if (isPlayerLoading || !isPlaying) {
                                MandalLogoPlayerOverlay(
                                    logoUrl = mandalLogoUrl,
                                    isLoading = isPlayerLoading,
                                    isPlaying = isPlaying,
                                    onPlayClick = { togglePlayPause() }
                                )
                            }

                            // Floating Reaction Overlay over video
                            FloatingReactionOverlay(particles = floatingParticles)

                            // Custom Branded HUD Controls Overlay
                            if (showControlsOverlay) {
                                CustomPlayerControlsOverlay(
                                    isPlaying = isPlaying,
                                    isMuted = isMuted,
                                    isLoading = isPlayerLoading,
                                    viewerCount = viewerCount,
                                    isFullscreen = false,
                                    platform = platform,
                                    onPlayPauseClick = { togglePlayPause() },
                                    onMuteToggle = { toggleMute() },
                                    onFullscreenToggle = { toggleOrientationFullscreen() },
                                    onReloadClick = { reloadStream() }
                                )
                            }
                        }

                        // 2. QUICK REACTIONS ROW (5 REACTION BUTTONS)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceVariantWarm)
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val reactionOptions = listOf(
                                    "🚩 जय हिंद!",
                                    "🙏 बाप्पा मोरया!",
                                    "🌸 आरती व फुले",
                                    "👏 टाळ्या",
                                    "🔔 घंटी"
                                )

                                reactionOptions.forEach { reaction ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = SaffronLight.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clickable { triggerFloatingReaction(reaction) }
                                            .testTag("reaction_btn_${reaction.take(4)}")
                                    ) {
                                        Text(
                                            text = reaction,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SaffronDark
                                        )
                                    }
                                }
                            }
                        }

                        // 3. LIVE CHAT & COMMENTS FEED (Zero dummy comments!)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (comments.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "थेट प्रतिक्रिया व जयघोष सुरू करा!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "खालील बॉक्समध्ये टाईप करून सर्व सभासदांना आपली प्रतिक्रिया पाठवा.",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = commentsListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(comments, key = { it.id }) { comment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = SurfaceVariantWarm,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(SaffronPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = comment.userName.take(1).ifEmpty { "स" },
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = comment.userName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SaffronDark
                                                    )
                                                    Text(
                                                        text = formatTimeAgoShort(comment.timestamp),
                                                        fontSize = 10.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                                Text(
                                                    text = comment.message,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = CardBorderColor)

                        // 4. TYPE & SEND LIVE COMMENT INPUT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = typedComment,
                                onValueChange = { typedComment = it },
                                placeholder = {
                                    Text(
                                        "✍️ तुमची प्रतिक्रिया येथे टाईप करा...",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("live_comment_input"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SaffronPrimary,
                                    unfocusedBorderColor = CardBorderColor,
                                    focusedContainerColor = SurfaceWarm,
                                    unfocusedContainerColor = SurfaceVariantWarm
                                ),
                                maxLines = 2,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (typedComment.isNotBlank()) {
                                            onPostComment(typedComment)
                                            typedComment = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (typedComment.isNotBlank()) {
                                        onPostComment(typedComment)
                                        typedComment = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SaffronPrimary, CircleShape)
                                    .testTag("send_live_comment_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 5. ACTION BUTTONS: WHATSAPP SHARE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceWarm)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Instant WhatsApp Share
                            Button(
                                onClick = {
                                    val liveUrl = mandalInfo.liveStreamUrl.ifEmpty {
                                        "https://www.youtube.com/@JayHindMandalArjunwad/live"
                                    }
                                    val shareText = "🚩 *जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड*\n🔴 *$streamTitle*\n\nथेट आरती व सोहळा पाहण्यासाठी खालील लिंकवर क्लिक करा किंवा जय हिंद ॲप उघडा:\n$liveUrl\n\n_जय हिंद मंडळ, अर्जुनवाड परिवार_"
                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            setPackage("com.whatsapp")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(chooserIntent, "थेट प्रक्षेपण शेअर करा"))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("whatsapp_share_live_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WhatsApp वर थेट प्रक्षेपण शेअर करा 🟢",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TV-STYLE CORNER WATERMARK LOGO (न्यूज चॅनेल प्रमाणे कोपऱ्यात लोगो)
@Composable
private fun TvCornerWatermark(
    logoUrl: String?,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "watermark_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "watermark_dot_alpha"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mandal Logo Badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, GoldenTertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!logoUrl.isNullOrBlank()) {
                    UniversalAsyncImage(
                        model = logoUrl,
                        contentDescription = "Mandal Logo Watermark",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_jayhind_logo),
                        contentDescription = "Mandal Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column {
                Text(
                    text = "जय हिंद",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 11.sp
                )
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(BloodRed.copy(alpha = dotAlpha))
                        )
                        Text(
                            text = "LIVE",
                            color = Color(0xFFFF4D4D),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// MANDAL LOGO PLAYER OVERLAY (व्हिडिओ लोड किंवा पॉज असताना मध्यभागी मंडळाचा लोगो)
@Composable
private fun MandalLogoPlayerOverlay(
    logoUrl: String?,
    isLoading: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mandal_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Glowing Circular Mandal Logo Frame
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(if (isLoading) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(SaffronPrimary.copy(alpha = 0.4f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, GoldenTertiary, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!logoUrl.isNullOrBlank()) {
                        UniversalAsyncImage(
                            model = logoUrl,
                            contentDescription = "Mandal Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_jayhind_logo),
                            contentDescription = "Mandal Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "अर्जुनवाड • थेट प्रक्षेपण",
                color = GoldenTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "🔴 थेट प्रक्षेपण सुरू होत आहे...",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (!isPlaying) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SaffronPrimary,
                    modifier = Modifier.clickable { onPlayClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "प्रक्षेपण पुन्हा सुरू करा (Play)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// CUSTOM PLAYER CONTROLS OVERLAY (Overlayed directly over video)
@Composable
private fun CustomPlayerControlsOverlay(
    isPlaying: Boolean,
    isMuted: Boolean,
    isLoading: Boolean,
    viewerCount: Int,
    isFullscreen: Boolean,
    platform: StreamPlatform,
    onPlayPauseClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onReloadClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.75f)
                    )
                )
            )
            .padding(10.dp)
    ) {
        // Top HUD: Branded Live Tag + Viewers Count + Sound button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BloodRed
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "थेट प्रक्षेपण (${platform.displayName})",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "👁️ $viewerCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sound Mute/Unmute
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.clickable { onMuteToggle() }
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Sound",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }

                // Reload stream
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.clickable { onReloadClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        // Center HUD: Big Play / Pause Button
        if (!isLoading) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(54.dp)
                        .clickable { onPlayPauseClick() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    )
                }
            }
        }

        // Bottom HUD: Fullscreen / Rotation Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚩 जय हिंद मंडळ, अर्जुनवाड",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onFullscreenToggle() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFullscreen) "लहान करा" else "आडवा / Fullscreen",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// IN-APP SMART MULTI-PLATFORM LIVE PLAYER WEBVIEW
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SmartMultiPlatformPlayer(
    streamUrl: String,
    platform: StreamPlatform,
    onWebViewCreated: (WebView) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onPlayStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanUrl = streamUrl.trim()
    val ytVideoId = remember(cleanUrl) { extractYouTubeId(cleanUrl) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // High compatibility Android WebView settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowContentAccess = true
                settings.allowFileAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                // Modern Android Chrome User-Agent
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onPlayerReady() {
                        post {
                            onLoadingChange(false)
                            onPlayStateChange(true)
                        }
                    }

                    @JavascriptInterface
                    fun onPlayerStateChange(state: Int) {
                        post {
                            when (state) {
                                1 -> { // Playing
                                    onLoadingChange(false)
                                    onPlayStateChange(true)
                                }
                                2 -> { // Paused
                                    onPlayStateChange(false)
                                }
                                3 -> { // Buffering
                                    onLoadingChange(true)
                                }
                                else -> {}
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onPlayerError(errorCode: Int) {
                        post {
                            onLoadingChange(false)
                        }
                    }
                }, "AndroidBridge")

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChange(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        onLoadingChange(false)
                    }
                }

                loadSmartPlayerHtml(this, cleanUrl, platform, ytVideoId)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            onWebViewCreated(webView)
        },
        modifier = modifier.testTag("smart_multiplatform_live_player")
    )
}

private fun loadSmartPlayerHtml(
    webView: WebView,
    streamUrl: String,
    platform: StreamPlatform,
    ytVideoId: String
) {
    val html = when (platform) {
        StreamPlatform.YOUTUBE -> {
            val validYtId = ytVideoId.ifEmpty { "live_stream" }
            """
            <!DOCTYPE html>
            <html lang="mr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    #player-container { position: relative; width: 100vw; height: 100vh; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: 0; }
                    .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-pause-overlay, 
                    .ytp-show-cards-title, .ytp-share-panel, .ytp-button, .ytp-contextmenu {
                        display: none !important; opacity: 0 !important; pointer-events: none !important;
                    }
                </style>
                <script src="https://www.youtube.com/iframe_api"></script>
            </head>
            <body>
                <div id="player-container">
                    <div id="player"></div>
                </div>
                <script>
                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            width: '100%',
                            height: '100%',
                            videoId: '$validYtId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 0,
                                'modestbranding': 1,
                                'rel': 0,
                                'showinfo': 0,
                                'iv_load_policy': 3,
                                'disablekb': 1,
                                'fs': 0,
                                'origin': 'https://www.youtube.com',
                                'enablejsapi': 1
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }
                    function onPlayerReady(event) {
                        event.target.playVideo();
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }
                    function onPlayerStateChange(event) {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerStateChange(event.data);
                    }
                    function onPlayerError(event) {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerError(event.data);
                    }
                    window.playVideo = function() { if (player && player.playVideo) player.playVideo(); };
                    window.pauseVideo = function() { if (player && player.pauseVideo) player.pauseVideo(); };
                    window.muteVideo = function() { if (player && player.mute) player.mute(); };
                    window.unMuteVideo = function() { if (player && player.unMute) player.unMute(); };
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        StreamPlatform.FACEBOOK -> {
            val encodedFb = try { URLEncoder.encode(streamUrl, "UTF-8") } catch (_: Exception) { streamUrl }
            val fbEmbedUrl = "https://www.facebook.com/plugins/video.php?href=$encodedFb&show_text=false&autoplay=true&mute=0"
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    iframe { width: 100vw; height: 100vh; border: 0; }
                </style>
            </head>
            <body>
                <iframe src="$fbEmbedUrl" allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share" allowFullScreen="true"></iframe>
                <script>
                    setTimeout(function() {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }, 1200);
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        StreamPlatform.INSTAGRAM -> {
            val igUrl = if (streamUrl.endsWith("/")) streamUrl else "$streamUrl/"
            val igEmbed = if (igUrl.contains("/embed/")) igUrl else "${igUrl}embed/"
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    iframe { width: 100vw; height: 100vh; border: 0; }
                </style>
            </head>
            <body>
                <iframe src="$igEmbed" allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share" allowtransparency="true" frameborder="0" scrolling="no"></iframe>
                <script>
                    setTimeout(function() {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }, 1200);
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        StreamPlatform.DIRECT_HLS, StreamPlatform.CUSTOM -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    video { width: 100vw; height: 100vh; object-fit: contain; background: #000; }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
            </head>
            <body>
                <video id="videoPlayer" playsinline autoplay controls></video>
                <script>
                    var video = document.getElementById('videoPlayer');
                    var videoSrc = '$streamUrl';
                    if (Hls.isSupported() && videoSrc.includes('.m3u8')) {
                        var hls = new Hls();
                        hls.loadSource(videoSrc);
                        hls.attachMedia(video);
                        hls.on(Hls.Events.MANIFEST_PARSED, function() {
                            video.play();
                            if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                        });
                    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                        video.src = videoSrc;
                        video.addEventListener('loadedmetadata', function() {
                            video.play();
                            if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                        });
                    } else {
                        video.src = videoSrc;
                        video.play();
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }

                    video.onplaying = function() { if (window.AndroidBridge) window.AndroidBridge.onPlayerStateChange(1); };
                    video.onpause = function() { if (window.AndroidBridge) window.AndroidBridge.onPlayerStateChange(2); };
                    video.onwaiting = function() { if (window.AndroidBridge) window.AndroidBridge.onPlayerStateChange(3); };

                    window.playVideo = function() { video.play(); };
                    window.pauseVideo = function() { video.pause(); };
                    window.muteVideo = function() { video.muted = true; };
                    window.unMuteVideo = function() { video.muted = false; };
                </script>
            </body>
            </html>
            """.trimIndent()
        }
    }

    val baseUrl = when (platform) {
        StreamPlatform.YOUTUBE -> "https://www.youtube.com"
        StreamPlatform.FACEBOOK -> "https://www.facebook.com"
        StreamPlatform.INSTAGRAM -> "https://www.instagram.com"
        else -> "https://localhost"
    }

    webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
}

// FLOATING REACTION PARTICLES OVERLAY
@Composable
private fun BoxScope.FloatingReactionOverlay(particles: List<FloatingParticle>) {
    particles.forEach { particle ->
        val transition = rememberInfiniteTransition(label = "particle_${particle.id}")
        val offsetY by transition.animateFloat(
            initialValue = 180f,
            targetValue = -120f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "offsetY"
        )
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.75f * alpha),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = (particle.startOffsetX * 280).dp)
                .offset(y = offsetY.dp)
        ) {
            Text(
                text = particle.text,
                color = Color.White.copy(alpha = alpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun formatTimeAgoShort(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = (diff / 1000) / 60
    val hours = minutes / 60

    return when {
        minutes < 1 -> "आत्ताच"
        minutes < 60 -> "${minutes} मि. पूर्वी"
        hours < 24 -> "${hours} ता. पूर्वी"
        else -> "पूर्वी"
    }
}
