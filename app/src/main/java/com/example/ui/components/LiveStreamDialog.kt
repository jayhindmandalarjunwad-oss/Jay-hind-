package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import android.widget.FrameLayout
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.model.LiveComment
import com.example.data.model.MandalInfo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    // youtu.be/ID
    val youtuBeRegex = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""")
    youtuBeRegex.find(clean)?.let { return it.groupValues[1] }

    // youtube.com/live/ID
    val liveRegex = Regex("""youtube\.com/live/([a-zA-Z0-9_-]{11})""")
    liveRegex.find(clean)?.let { return it.groupValues[1] }

    // [?&]v=ID
    val watchRegex = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
    watchRegex.find(clean)?.let { return it.groupValues[1] }

    // youtube.com/embed/ID
    val embedRegex = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]{11})""")
    embedRegex.find(clean)?.let { return it.groupValues[1] }

    // youtube.com/shorts/ID
    val shortsRegex = Regex("""youtube\.com/shorts/([a-zA-Z0-9_-]{11})""")
    shortsRegex.find(clean)?.let { return it.groupValues[1] }

    // youtube.com/vi?/ID
    val vRegex = Regex("""youtube\.com/vi?/([a-zA-Z0-9_-]{11})""")
    vRegex.find(clean)?.let { return it.groupValues[1] }

    return ""
}

fun detectStreamPlatform(url: String): StreamPlatform {
    val clean = url.trim().lowercase()
    val ytId = extractYouTubeId(url)
    return when {
        ytId.isNotEmpty() || clean.contains("youtu.be") || clean.contains("youtube.com") -> StreamPlatform.YOUTUBE
        clean.contains("facebook.com") || clean.contains("fb.watch") || clean.contains("fb.me") || clean.contains("fb.gg") -> StreamPlatform.FACEBOOK
        clean.contains("instagram.com") || clean.contains("instagr.am") -> StreamPlatform.INSTAGRAM
        clean.contains(".m3u8") || clean.contains(".mp4") || clean.contains("rtmp://") || clean.contains(".webm") -> StreamPlatform.DIRECT_HLS
        clean.startsWith("http://") || clean.startsWith("https://") -> StreamPlatform.CUSTOM
        else -> StreamPlatform.YOUTUBE
    }
}

fun openStreamInExternalApp(context: Context, url: String, platform: StreamPlatform) {
    val cleanUrl = url.trim()
    if (cleanUrl.isBlank()) return
    try {
        when (platform) {
            StreamPlatform.YOUTUBE -> {
                val videoId = extractYouTubeId(cleanUrl)
                val targetUrl = if (videoId.isNotEmpty()) "https://www.youtube.com/watch?v=$videoId" else cleanUrl
                val uri = Uri.parse(targetUrl)

                // Try YouTube App directly first
                val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.youtube")
                }
                try {
                    context.startActivity(appIntent)
                    return
                } catch (_: Exception) {
                    try {
                        val fallbackUri = if (videoId.isNotEmpty()) Uri.parse("vnd.youtube:$videoId") else uri
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                        context.startActivity(fallbackIntent)
                        return
                    } catch (_: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(webIntent)
                        return
                    }
                }
            }
            StreamPlatform.FACEBOOK -> {
                try {
                    val fbIntent = Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=" + URLEncoder.encode(cleanUrl, "UTF-8")))
                    context.startActivity(fbIntent)
                    return
                } catch (_: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                    context.startActivity(webIntent)
                    return
                }
            }
            StreamPlatform.INSTAGRAM -> {
                try {
                    val uri = Uri.parse(cleanUrl)
                    val instaIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.instagram.android")
                    }
                    context.startActivity(instaIntent)
                    return
                } catch (_: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                    context.startActivity(webIntent)
                    return
                }
            }
            else -> {}
        }
        val defaultIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
        context.startActivity(defaultIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "लिंक उघडता आली नाही", Toast.LENGTH_SHORT).show()
    }
}

private data class FloatingParticle(
    val id: Long,
    val text: String,
    val startOffsetX: Float
)

/**
 * 100% In-App Dedicated Live Stream Screen (No Dialog, Seamless Fullscreen Rotation, Reliable Hardware Back Key)
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LiveStreamDialog(
    mandalInfo: MandalInfo,
    mandalLogoUrl: String? = null,
    comments: List<LiveComment> = emptyList(),
    viewerCount: Int = 1,
    currentUserId: String = "",
    isAdmin: Boolean = false,
    onEnterPresence: () -> Unit = {},
    onLeavePresence: () -> Unit = {},
    onDismiss: () -> Unit,
    onSendReaction: (String) -> Unit = {},
    onPostComment: (String) -> Unit = {},
    onEditComment: (String, String) -> Unit = { _, _ -> },
    onDeleteComment: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time live viewer presence (Automatically registers on enter, removes on leave/dismiss)
    DisposableEffect(Unit) {
        onEnterPresence()
        onDispose {
            onLeavePresence()
        }
    }

    var isManualFullscreen by remember { mutableStateOf(false) }
    var typedComment by remember { mutableStateOf("") }
    val displayViewerCount = maxOf(1, viewerCount)

    // Comment Moderation State (Edit / Delete)
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var selectedCommentForMenu by remember { mutableStateOf<LiveComment?>(null) }
    val haptic = LocalHapticFeedback.current

    // Custom Player Controls State
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(true) }
    var isPlayerLoading by remember { mutableStateOf(true) }
    var playbackErrorMsg by remember { mutableStateOf<String?>(null) }
    var showControlsOverlay by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var playerControllerInstance by remember { mutableStateOf<LivePlayerController?>(null) }
    val streamKeyOrUrl by remember(mandalInfo.liveStreamUrl) { mutableStateOf(mandalInfo.liveStreamUrl) }

    // Floating reaction bubbles list
    var floatingParticles by remember { mutableStateOf<List<FloatingParticle>>(emptyList()) }

    // Scaling / Stretch state (Pinch-to-zoom / Stretch to fill screen 4-corner fit)
    var targetScale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "live_video_scale"
    )
    var zoomToastMessage by remember { mutableStateOf<String?>(null) }
    var zoomToastIcon by remember { mutableStateOf<ImageVector?>(null) }
    var lastUserInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun resetControlsTimer() {
        lastUserInteractionTime = System.currentTimeMillis()
    }

    fun toggleZoomFill() {
        if (targetScale > 1.05f) {
            targetScale = 1.0f
            zoomToastMessage = "मूळ आकार (Fit to Screen)"
            zoomToastIcon = Icons.Default.FullscreenExit
        } else {
            targetScale = 1.38f
            zoomToastMessage = "स्क्रीन भरून (Zoomed to Fill)"
            zoomToastIcon = Icons.Default.Fullscreen
        }
    }

    fun onPinchScale(newScale: Float) {
        targetScale = newScale
        if (newScale > 1.05f) {
            zoomToastMessage = "स्क्रीन भरून (Zoomed to Fill)"
            zoomToastIcon = Icons.Default.Fullscreen
        } else {
            zoomToastMessage = "मूळ आकार (Fit to Screen)"
            zoomToastIcon = Icons.Default.FullscreenExit
        }
    }

    LaunchedEffect(zoomToastMessage) {
        if (zoomToastMessage != null) {
            delay(1500)
            zoomToastMessage = null
        }
    }

    val effectiveFullscreen = isLandscape || isManualFullscreen

    fun setFullscreenMode(enable: Boolean) {
        isManualFullscreen = enable
        if (activity != null) {
            val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (enable) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Automatically enforce immersive edge-to-edge system bars in fullscreen / landscape
    LaunchedEffect(effectiveFullscreen) {
        if (activity != null) {
            val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (effectiveFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Toggle Landscape / Fullscreen
    fun toggleOrientationFullscreen() {
        setFullscreenMode(!effectiveFullscreen)
    }

    // 1. In Fullscreen / Landscape: Hardware Back Button exits fullscreen and restores Portrait
    BackHandler(enabled = effectiveFullscreen) {
        setFullscreenMode(false)
    }

    // 2. In Portrait mode: Hardware Back Button closes live stream screen
    BackHandler(enabled = !effectiveFullscreen) {
        setFullscreenMode(false)
        onDismiss()
    }

    // Auto-hide controls overlay after 3.5 seconds in fullscreen
    LaunchedEffect(showControlsOverlay, isPlaying, lastUserInteractionTime, effectiveFullscreen) {
        if (effectiveFullscreen && showControlsOverlay && isPlaying) {
            delay(3500)
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
        onSendReaction(reactionText)

        coroutineScope.launch {
            delay(2200)
            floatingParticles = floatingParticles.filter { it.id != newParticle.id }
        }
    }

    // Toggle Play/Pause
    fun togglePlayPause() {
        if (isPlaying) {
            playerControllerInstance?.pause()
            webViewInstance?.evaluateJavascript("if(window.pauseVideo) window.pauseVideo();", null)
            isPlaying = false
            showControlsOverlay = true
        } else {
            playerControllerInstance?.play()
            webViewInstance?.evaluateJavascript("if(window.playVideo) window.playVideo();", null)
            isPlaying = true
            showControlsOverlay = true
        }
    }

    // Toggle Mute/Unmute
    fun toggleMute() {
        if (isMuted) {
            playerControllerInstance?.unMute()
            webViewInstance?.evaluateJavascript("if(window.unMuteVideo) window.unMuteVideo();", null)
            isMuted = false
        } else {
            playerControllerInstance?.mute()
            webViewInstance?.evaluateJavascript("if(window.muteVideo) window.muteVideo();", null)
            isMuted = true
        }
        showControlsOverlay = true
    }

    // Reload stream in-place
    fun reloadStream() {
        playbackErrorMsg = null
        isPlayerLoading = true
        playerControllerInstance?.reload()
        webViewInstance?.reload()
    }

    // Restore orientation & system bars when view is dismissed
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(act.window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
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
    var lastSeenCommentId by remember { mutableStateOf<String?>(null) }
    val allowedReactionEmojis = setOf("🚩", "🙏", "👍", "🌸")

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottomPadding > 0.dp

    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && comments.isNotEmpty()) {
            delay(100)
            commentsListState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            commentsListState.animateScrollToItem(0)
            val latest = comments.last() // Most recent incoming comment
            if (latest.id != lastSeenCommentId) {
                lastSeenCommentId = latest.id
                val msg = latest.message.trim()
                // Synchronized Floating Reactions: triggers for all viewers when any user sends '🚩', '🙏', '👍', '🌸'
                val matchingEmoji = allowedReactionEmojis.firstOrNull { emoji -> msg.contains(emoji) }
                if (matchingEmoji != null) {
                    val newParticle = FloatingParticle(
                        id = System.currentTimeMillis() + Random.nextLong(1000),
                        text = matchingEmoji,
                        startOffsetX = Random.nextFloat() * 0.7f + 0.15f
                    )
                    floatingParticles = floatingParticles + newParticle
                    coroutineScope.launch {
                        delay(2200)
                        floatingParticles = floatingParticles.filter { it.id != newParticle.id }
                    }
                }
            }
        }
    }

    // TOP-LEVEL DEDICATED IN-APP LAYER (NO MODAL DIALOG CONTAINER)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(if (effectiveFullscreen) Color.Black else MaterialTheme.colorScheme.surface)
            .testTag(if (effectiveFullscreen) "live_stream_fullscreen_view" else "live_stream_portrait_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (effectiveFullscreen) Color.Black else MaterialTheme.colorScheme.surface)
        ) {
            // Top Header Bar (Only shown in Portrait Mode when keyboard is closed for maximum chat real estate like YouTube)
            AnimatedVisibility(
                visible = !effectiveFullscreen && !isKeyboardOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8E0E00), Color(0xFF1F1C18))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = {
                                setFullscreenMode(false)
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
            }

            // Main View Area: True Fullscreen Landscape vs Portrait Layout
            if (effectiveFullscreen) {
                // ==========================================
                // 1. TRUE IMMERSIVE FULLSCREEN MODE (LANDSCAPE)
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable {
                            showControlsOverlay = !showControlsOverlay
                            if (showControlsOverlay) resetControlsTimer()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Smart Multi-Platform Live Player Engine (100% In-App with Pinch-to-Zoom Stretch)
                    SmartMultiPlatformPlayer(
                        streamUrl = streamKeyOrUrl,
                        platform = platform,
                        onWebViewCreated = { webViewInstance = it },
                        onControllerReady = { playerControllerInstance = it },
                        onLoadingChange = { isPlayerLoading = it },
                        onPlayStateChange = { playing -> isPlaying = playing },
                        onErrorChange = { error -> playbackErrorMsg = error },
                        onToggleControls = {
                            showControlsOverlay = !showControlsOverlay
                            if (showControlsOverlay) resetControlsTimer()
                        },
                        videoScale = animatedScale,
                        onDoubleTap = {
                            toggleZoomFill()
                            resetControlsTimer()
                        },
                        onPinchScale = { scale ->
                            onPinchScale(scale)
                            resetControlsTimer()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // TV-Style Corner Watermark Logo (fades out when controls auto-hide for pure video)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControlsOverlay,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                    ) {
                        TvCornerWatermark(
                            logoUrl = mandalLogoUrl,
                            isLive = isLiveActive
                        )
                    }

                    // In-Place Playback Error Fallback (With direct app fallback)
                    if (playbackErrorMsg != null) {
                        PlaybackErrorOverlay(
                            errorMessage = playbackErrorMsg!!,
                            platform = platform,
                            onRetry = { reloadStream() },
                            onOpenInApp = { openStreamInExternalApp(context, streamKeyOrUrl, platform) }
                        )
                    } else if (!isLiveActive || (isPlayerLoading && !isPlaying)) {
                        // Loading / Paused Overlay with Center Mandal Logo
                        MandalLogoPlayerOverlay(
                            logoUrl = mandalLogoUrl,
                            isLoading = isPlayerLoading,
                            isPlaying = isPlaying,
                            onPlayClick = { togglePlayPause() }
                        )
                    }

                    // Floating Unmute Prompt if Muted (only visible when controls are shown)
                    if (isMuted && isLiveActive && !isPlayerLoading && showControlsOverlay) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clickable {
                                    resetControlsTimer()
                                    toggleMute()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Unmute", tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                                Text("🔊 आवाज सुरू करा (Unmute)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Floating Reaction Particles in Fullscreen
                    FloatingReactionOverlay(particles = floatingParticles)

                    // Zoom / Stretch Toast Indicator Pill
                    androidx.compose.animation.AnimatedVisibility(
                        visible = zoomToastMessage != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.8f)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                zoomToastIcon?.let { icon ->
                                    Icon(icon, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = zoomToastMessage ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Semi-transparent Live Comments Ticker in Fullscreen (Bottom-Left)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControlsOverlay && comments.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, bottom = 64.dp)
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            comments.takeLast(3).forEach { c ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = c.userName,
                                            color = SaffronLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = c.message,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom HUD Controls for Fullscreen (Back Icon, Sound, Reload, Exit Fullscreen, Zoom/Fit)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControlsOverlay,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CustomPlayerControlsOverlay(
                            streamTitle = streamTitle,
                            isPlaying = isPlaying,
                            isMuted = isMuted,
                            isLoading = isPlayerLoading,
                            viewerCount = displayViewerCount,
                            isFullscreen = true,
                            isZoomed = targetScale > 1.05f,
                            platform = platform,
                            onBackClick = { setFullscreenMode(false) },
                            onPlayPauseClick = {
                                resetControlsTimer()
                                togglePlayPause()
                            },
                            onMuteToggle = {
                                resetControlsTimer()
                                toggleMute()
                            },
                            onFullscreenToggle = {
                                resetControlsTimer()
                                toggleOrientationFullscreen()
                            },
                            onToggleZoom = {
                                resetControlsTimer()
                                toggleZoomFill()
                            },
                            onReloadClick = {
                                resetControlsTimer()
                                reloadStream()
                            },
                            onOpenInApp = { openStreamInExternalApp(context, streamKeyOrUrl, platform) }
                        )
                    }

                    // Compact Quick Reactions Bar at bottom of Fullscreen View
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControlsOverlay,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
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
                            val quickReactionList = listOf("🚩", "🙏", "👍", "🌸")
                            quickReactionList.forEach { reaction ->
                                Surface(
                                    shape = CircleShape,
                                    color = SaffronPrimary.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            resetControlsTimer()
                                            triggerFloatingReaction(reaction)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = reaction,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // 2. PORTRAIT MODE (VIDEO PLAYER + LIVE CHAT + ACTIONS)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    // 1. BRANDED MULTI-PLATFORM VIDEO PLAYER (16:9 Aspect Ratio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                            .clickable {
                                showControlsOverlay = !showControlsOverlay
                                if (showControlsOverlay) resetControlsTimer()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SmartMultiPlatformPlayer(
                            streamUrl = streamKeyOrUrl,
                            platform = platform,
                            onWebViewCreated = { webViewInstance = it },
                            onControllerReady = { playerControllerInstance = it },
                            onLoadingChange = { isPlayerLoading = it },
                            onPlayStateChange = { playing -> isPlaying = playing },
                            onErrorChange = { error -> playbackErrorMsg = error },
                            onToggleControls = {
                                showControlsOverlay = !showControlsOverlay
                                if (showControlsOverlay) resetControlsTimer()
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // TV-Style Corner Watermark Logo
                        TvCornerWatermark(
                            logoUrl = mandalLogoUrl,
                            isLive = isLiveActive,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                        )

                        // Playback Error Fallback Overlay
                        if (playbackErrorMsg != null) {
                            PlaybackErrorOverlay(
                                errorMessage = playbackErrorMsg!!,
                                platform = platform,
                                onRetry = { reloadStream() },
                                onOpenInApp = { openStreamInExternalApp(context, streamKeyOrUrl, platform) }
                            )
                        } else if (!isLiveActive || (isPlayerLoading && !isPlaying)) {
                            // Loading / Paused Center Mandal Logo Overlay
                            MandalLogoPlayerOverlay(
                                logoUrl = mandalLogoUrl,
                                isLoading = isPlayerLoading,
                                isPlaying = isPlaying,
                                onPlayClick = { togglePlayPause() }
                            )
                        }

                        // Floating Unmute Prompt if Muted
                        if (isMuted && isLiveActive && !isPlayerLoading) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clickable {
                                        resetControlsTimer()
                                        toggleMute()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Unmute", tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                                    Text("🔊 आवाज सुरू करा (Unmute)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Floating Reaction Overlay over video
                        FloatingReactionOverlay(particles = floatingParticles)

                        // Custom Branded HUD Controls Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showControlsOverlay,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CustomPlayerControlsOverlay(
                                streamTitle = streamTitle,
                                isPlaying = isPlaying,
                                isMuted = isMuted,
                                isLoading = isPlayerLoading,
                                viewerCount = displayViewerCount,
                                isFullscreen = false,
                                platform = platform,
                                onBackClick = {
                                    setFullscreenMode(false)
                                    onDismiss()
                                },
                                onPlayPauseClick = {
                                    resetControlsTimer()
                                    togglePlayPause()
                                },
                                onMuteToggle = {
                                    resetControlsTimer()
                                    toggleMute()
                                },
                                onFullscreenToggle = {
                                    resetControlsTimer()
                                    toggleOrientationFullscreen()
                                },
                                onReloadClick = {
                                    resetControlsTimer()
                                    reloadStream()
                                },
                                onOpenInApp = { openStreamInExternalApp(context, streamKeyOrUrl, platform) }
                            )
                        }
                    }



                    // 1.5 DIRECT QUICK ACTION STRIP (Sound, Open in App, Fullscreen) - Hidden when keyboard is open for YouTube style compact space
                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1F1F1F))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SaffronPrimary,
                                    modifier = Modifier.clickable { toggleMute() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Mute Toggle",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isMuted) "आवाज बंद आहे (Tap करा)" else "आवाज चालू आहे",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { openStreamInExternalApp(context, streamKeyOrUrl, platform) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = when (platform) {
                                                StreamPlatform.YOUTUBE -> "YouTube ॲप"
                                                StreamPlatform.FACEBOOK -> "Facebook ॲप"
                                                StreamPlatform.INSTAGRAM -> "Instagram ॲप"
                                                else -> "थेट ॲप"
                                            },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { toggleOrientationFullscreen() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("आडवा", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. QUICK REACTIONS ROW (Hidden when keyboard is open to maximize chat visibility)
                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
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
                                val reactionOptions = listOf("🚩", "🙏", "👍", "🌸")

                                Text(
                                    text = "प्रतिक्रिया:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                                )

                                reactionOptions.forEach { reaction ->
                                    Surface(
                                        shape = CircleShape,
                                        color = SaffronLight.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clickable { triggerFloatingReaction(reaction) }
                                            .testTag("reaction_btn_${reaction}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = reaction,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. LIVE CHAT & COMMENTS FEED - Hidden when keyboard is open to keep full focus on chat messages
                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32))
                                )
                                Text(
                                    text = "थेट संवाद (Real-time Live Chat)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "सर्व प्रेक्षकांना थेट दिसते ⚡",
                                fontSize = 10.sp,
                                color = SaffronDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

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
                                    text = "थेट संवाद व प्रतिक्रिया सुरू करा!",
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
                            val reversedComments = remember(comments) { comments.asReversed() }
                            LazyColumn(
                                state = commentsListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(reversedComments, key = { it.id }) { comment ->
                                    val isOwnComment = currentUserId.isNotBlank() && (comment.userId == currentUserId || comment.userId.isBlank())
                                    val canModerate = isOwnComment || isAdmin

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (editingCommentId == comment.id) SaffronPrimary.copy(alpha = 0.15f) else SurfaceVariantWarm
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (canModerate) {
                                                        selectedCommentForMenu = comment
                                                    }
                                                },
                                                onLongClick = {
                                                    if (canModerate) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectedCommentForMenu = comment
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isOwnComment) SaffronDark else SaffronPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (comment.userPhoto.isNotBlank()) {
                                                    UniversalAsyncImage(
                                                        model = comment.userPhoto,
                                                        contentDescription = comment.userName,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = comment.userName.take(1).ifEmpty { "स" },
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = comment.userName,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SaffronDark
                                                        )
                                                        if (isOwnComment) {
                                                            Text(
                                                                text = "(तुम्ही)",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = SaffronPrimary
                                                            )
                                                        }
                                                        if (comment.edited) {
                                                            Text(
                                                                text = "• संपादित",
                                                                fontSize = 9.sp,
                                                                color = TextMuted
                                                            )
                                                        }
                                                    }
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

                                            if (canModerate) {
                                                Box {
                                                    IconButton(
                                                        onClick = {
                                                            selectedCommentForMenu = comment
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.MoreVert,
                                                            contentDescription = "Options",
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = selectedCommentForMenu?.id == comment.id,
                                                        onDismissRequest = { selectedCommentForMenu = null }
                                                    ) {
                                                        if (isOwnComment) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = SaffronPrimary)
                                                                        Spacer(Modifier.width(8.dp))
                                                                        Text("कमेंट संपादित करा (Edit)", fontSize = 13.sp)
                                                                    }
                                                                },
                                                                onClick = {
                                                                    editingCommentId = comment.id
                                                                    typedComment = comment.message
                                                                    selectedCommentForMenu = null
                                                                }
                                                            )
                                                        }

                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = BloodRed)
                                                                    Spacer(Modifier.width(8.dp))
                                                                    Text(
                                                                        if (isAdmin && !isOwnComment) "कमेंट हटवा (Admin Delete)" else "कमेंट हटवा (Delete)",
                                                                        fontSize = 13.sp,
                                                                        color = BloodRed
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                onDeleteComment(comment.id)
                                                                if (editingCommentId == comment.id) {
                                                                    editingCommentId = null
                                                                    typedComment = ""
                                                                }
                                                                selectedCommentForMenu = null
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorderColor)

                    // 4. TYPE & SEND LIVE COMMENT INPUT + 5. WHATSAPP SHARE CONTAINER
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Editing Banner if a comment is currently being edited
                            if (editingCommentId != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SaffronPrimary.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "कमेंट संपादित करत आहात...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SaffronDark
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingCommentId = null
                                            typedComment = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Edit",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = SaffronPrimary.copy(alpha = 0.2f))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = if (isKeyboardOpen) 4.dp else 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = typedComment,
                                    onValueChange = { typedComment = it },
                                    placeholder = {
                                        Text(
                                            if (editingCommentId != null) "बदललेली कमेंट टाईप करा..." else "✍️ तुमची प्रतिक्रिया येथे टाईप करा...",
                                            fontSize = 13.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_comment_input"),
                                    shape = RoundedCornerShape(20.dp),
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
                                                val currentEditId = editingCommentId
                                                if (currentEditId != null) {
                                                    onEditComment(currentEditId, typedComment.trim())
                                                    editingCommentId = null
                                                } else {
                                                    onPostComment(typedComment.trim())
                                                }
                                                typedComment = ""
                                                focusManager.clearFocus()
                                            }
                                        }
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        if (typedComment.isNotBlank()) {
                                            val currentEditId = editingCommentId
                                            if (currentEditId != null) {
                                                onEditComment(currentEditId, typedComment.trim())
                                                editingCommentId = null
                                            } else {
                                                onPostComment(typedComment.trim())
                                            }
                                            typedComment = ""
                                            focusManager.clearFocus()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(if (editingCommentId != null) SuccessGreen else SaffronPrimary, CircleShape)
                                        .testTag("send_live_comment_btn")
                                ) {
                                    Icon(
                                        imageVector = if (editingCommentId != null) Icons.Default.Check else Icons.Default.Send,
                                        contentDescription = if (editingCommentId != null) "Update" else "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // 5. INSTANT WHATSAPP SHARE (Hidden when keyboard is open so comment input is completely visible)
                            AnimatedVisibility(
                                visible = !isKeyboardOpen,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceWarm)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val liveUrl = mandalInfo.liveStreamUrl.ifEmpty {
                                                "https://www.youtube.com/@JayHindMandalArjunwad/live"
                                            }
                                            val shareText = "🚩 *जयहिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड*\n🔴 *$streamTitle*\n\nथेट आरती व सोहळा पाहण्यासाठी खालील लिंकवर क्लिक करा किंवा जय हिंद ॲप उघडा:\n$liveUrl\n\n_जयहिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड परिवार_"
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
    }
}

// TV-STYLE CORNER WATERMARK LOGO
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

// MANDAL LOGO OVERLAY (When loading or paused)
@Composable
private fun MandalLogoPlayerOverlay(
    logoUrl: String?,
    isLoading: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(2.dp, SaffronPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!logoUrl.isNullOrBlank()) {
                    UniversalAsyncImage(
                        model = logoUrl,
                        contentDescription = "Mandal Logo",
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

            if (isLoading) {
                CircularProgressIndicator(
                    color = SaffronPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "थेट प्रक्षेपण सुरू होत आहे...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onPlayClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize()
                    )
                }
                Text(
                    text = "प्रक्षेपण पाहण्यासाठी क्लिक करा",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// IN-PLACE PLAYBACK ERROR FALLBACK (Pure In-App Retry with Open in App Option)
@Composable
private fun PlaybackErrorOverlay(
    errorMessage: String,
    platform: StreamPlatform,
    onRetry: () -> Unit,
    onOpenInApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = BloodRed.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "Live",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "थेट प्रक्षेपण उपलब्ध आहे",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = errorMessage.ifEmpty { "थेट प्रक्षेपण लोड होत आहे. कृपया पुन्हा प्रयत्न करा." },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("पुन्हा प्रयत्न करा", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onOpenInApp,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open in app", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (platform) {
                            StreamPlatform.YOUTUBE -> "▶️ YouTube ॲपमध्ये पहा"
                            StreamPlatform.FACEBOOK -> "🔵 Facebook ॲपमध्ये पहा"
                            StreamPlatform.INSTAGRAM -> "🟣 Instagram ॲपमध्ये पहा"
                            else -> "🌐 थेट ॲपमध्ये उघडा"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// CUSTOM PLAYER CONTROLS OVERLAY (Overlayed directly over video)
@Composable
private fun CustomPlayerControlsOverlay(
    streamTitle: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    isLoading: Boolean,
    viewerCount: Int,
    isFullscreen: Boolean,
    isZoomed: Boolean = false,
    platform: StreamPlatform,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onToggleZoom: () -> Unit = {},
    onReloadClick: () -> Unit,
    onOpenInApp: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.75f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.82f)
                    )
                )
            )
            .padding(if (isFullscreen) 14.dp else 8.dp)
    ) {
        // Top HUD: Back button (if Fullscreen) + Title + Live Badge + Viewers Count + Sound button + Reload button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (isFullscreen) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .testTag("live_fullscreen_back_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                }

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
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isFullscreen) {
                    Text(
                        text = streamTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$viewerCount उपस्थित",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stretch / Zoom to Fill toggle button in fullscreen
                if (isFullscreen) {
                    Surface(
                        shape = CircleShape,
                        color = if (isZoomed) SaffronPrimary.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.clickable { onToggleZoom() }
                    ) {
                        Icon(
                            imageVector = if (isZoomed) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isZoomed) "मूळ आकार (Fit)" else "स्क्रीन भरून (Fill)",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                }

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

        // Bottom HUD: Fullscreen / Rotation Toggle and Open In App
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚩 जयहिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (platform != StreamPlatform.YOUTUBE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onOpenInApp() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in App",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when (platform) {
                                    StreamPlatform.FACEBOOK -> "Facebook ↗"
                                    StreamPlatform.INSTAGRAM -> "Instagram ↗"
                                    else -> "App ↗"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
                            text = if (isFullscreen) "उभा / Exit" else "आडवा / Fullscreen",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

interface LivePlayerController {
    fun play()
    fun pause()
    fun mute()
    fun unMute()
    fun reload()
}

/**
 * Custom Touch FrameLayout supporting:
 * 1. Two-finger pinch gesture to stretch/zoom video to screen edges
 * 2. Double-tap to toggle between Fit (1.0f) and Fill (1.38f)
 * 3. Single-tap to toggle controls overlay
 * 4. Intercepts all touches so child views cannot leak click events or redirects
 */
class PinchZoomTouchFrameLayout(
    context: Context,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onScaleChanged: (Float) -> Unit
) : FrameLayout(context) {

    private var currentScale = 1.0f

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale *= detector.scaleFactor
            currentScale = currentScale.coerceIn(1.0f, 2.5f)
            onScaleChanged(currentScale)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap()
            return true
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)
        val sHandled = scaleGestureDetector.onTouchEvent(event)
        val gHandled = gestureDetector.onTouchEvent(event)
        return sHandled || gHandled || true
    }
}

// IN-APP SMART MULTI-PLATFORM LIVE PLAYER (100% IN-APP WITHOUT EXTERNAL REDIRECTS)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SmartMultiPlatformPlayer(
    streamUrl: String,
    platform: StreamPlatform,
    onWebViewCreated: (WebView) -> Unit,
    onControllerReady: (LivePlayerController) -> Unit = {},
    onLoadingChange: (Boolean) -> Unit,
    onPlayStateChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit = {},
    onToggleControls: () -> Unit = {},
    videoScale: Float = 1f,
    onDoubleTap: () -> Unit = {},
    onPinchScale: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cleanUrl = streamUrl.trim()
    val ytVideoId = remember(cleanUrl) { extractYouTubeId(cleanUrl) }

    if (platform == StreamPlatform.YOUTUBE && ytVideoId.isNotEmpty()) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
        var youTubePlayerViewRef by remember { mutableStateOf<YouTubePlayerView?>(null) }
        var currentVideoId by remember { mutableStateOf(ytVideoId) }
        var isYtReady by remember { mutableStateOf(false) }

        var directStreamUri by remember { mutableStateOf<Uri?>(null) }
        var isDirectStreamActive by remember { mutableStateOf(false) }
        var texturePlayerRef by remember { mutableStateOf<TextureVideoPlayerView?>(null) }

        // Attempt direct stream extraction (0% YouTube UI, 100% Native stream playback like Gallery)
        LaunchedEffect(ytVideoId) {
            onLoadingChange(true)
            try {
                val directUrl = withTimeoutOrNull(2500) {
                    YouTubeDirectStreamExtractor.extractDirectStream(ytVideoId)
                }
                if (directUrl != null) {
                    val uri = Uri.parse(directUrl)
                    directStreamUri = uri
                    isDirectStreamActive = true
                    onLoadingChange(false)
                    return@LaunchedEffect
                }
            } catch (_: Exception) {}
            isDirectStreamActive = false
        }

        // Periodically enforce Clean YouTube CSS injection
        LaunchedEffect(isYtReady) {
            if (isYtReady) {
                repeat(8) {
                    delay(600)
                    youTubePlayerViewRef?.let { injectCleanYouTubeCSS(it) }
                }
            }
        }

        if (isDirectStreamActive && directStreamUri != null) {
            // Direct Native Player with 0% YouTube UI
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PinchZoomTouchFrameLayout(
                            context = ctx,
                            onSingleTap = onToggleControls,
                            onDoubleTap = onDoubleTap,
                            onScaleChanged = onPinchScale
                        ).apply {
                            val tvp = TextureVideoPlayerView(ctx).apply {
                                texturePlayerRef = this
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                onPreparedListener = { _ ->
                                    onLoadingChange(false)
                                    onPlayStateChange(true)
                                    onErrorChange(null)
                                }
                                onCompletionListener = {
                                    onPlayStateChange(false)
                                }
                                onErrorListener = { _, _ ->
                                    // Seamless fallback to clean YouTubePlayerView on direct stream failure
                                    isDirectStreamActive = false
                                }
                                setVideoUri(directStreamUri!!)
                            }
                            addView(tvp)
                            onControllerReady(object : LivePlayerController {
                                override fun play() { tvp.play(); onPlayStateChange(true) }
                                override fun pause() { tvp.pause(); onPlayStateChange(false) }
                                override fun mute() { tvp.setMute(true) }
                                override fun unMute() { tvp.setMute(false) }
                                override fun reload() { tvp.setVideoUri(directStreamUri!!) }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = videoScale
                            scaleY = videoScale
                        }
                )
            }

            DisposableEffect(directStreamUri) {
                onDispose {
                    texturePlayerRef?.releaseMediaPlayer()
                }
            }
        } else {
            // Embedded YouTube Player with Chromeless CSS + Touch Interception + 1.16x Edge Cropping
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PinchZoomTouchFrameLayout(
                            context = ctx,
                            onSingleTap = onToggleControls,
                            onDoubleTap = onDoubleTap,
                            onScaleChanged = onPinchScale
                        ).apply {
                            val ytView = YouTubePlayerView(ctx).apply {
                                youTubePlayerViewRef = this
                                enableAutomaticInitialization = false
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                lifecycleOwner.lifecycle.addObserver(this)

                                val options = IFramePlayerOptions.Builder(ctx)
                                    .controls(0)
                                    .rel(0)
                                    .ivLoadPolicy(3)
                                    .build()

                                initialize(
                                    object : AbstractYouTubePlayerListener() {
                                        override fun onReady(youTubePlayer: YouTubePlayer) {
                                            youTubePlayerRef = youTubePlayer
                                            isYtReady = true
                                            youTubePlayer.loadVideo(ytVideoId, 0f)
                                            injectCleanYouTubeCSS(this@apply)
                                            onLoadingChange(false)
                                            onPlayStateChange(true)
                                            onErrorChange(null)

                                            onControllerReady(object : LivePlayerController {
                                                override fun play() { youTubePlayer.play() }
                                                override fun pause() { youTubePlayer.pause() }
                                                override fun mute() { youTubePlayer.mute() }
                                                override fun unMute() { youTubePlayer.unMute() }
                                                override fun reload() { youTubePlayer.loadVideo(ytVideoId, 0f) }
                                            })
                                        }

                                        override fun onStateChange(
                                            youTubePlayer: YouTubePlayer,
                                            state: PlayerConstants.PlayerState
                                        ) {
                                            when (state) {
                                                PlayerConstants.PlayerState.PLAYING -> {
                                                    onLoadingChange(false)
                                                    onPlayStateChange(true)
                                                    onErrorChange(null)
                                                    injectCleanYouTubeCSS(this@apply)
                                                }
                                                PlayerConstants.PlayerState.PAUSED -> {
                                                    onPlayStateChange(false)
                                                    injectCleanYouTubeCSS(this@apply)
                                                }
                                                PlayerConstants.PlayerState.BUFFERING -> {
                                                    onLoadingChange(true)
                                                }
                                                PlayerConstants.PlayerState.ENDED -> {
                                                    onPlayStateChange(false)
                                                    injectCleanYouTubeCSS(this@apply)
                                                }
                                                else -> {}
                                            }
                                        }

                                        override fun onError(
                                            youTubePlayer: YouTubePlayer,
                                            error: PlayerConstants.PlayerError
                                        ) {
                                            onLoadingChange(false)
                                            when (error) {
                                                PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> {
                                                    onErrorChange("व्हिडिओ आढळला नाही किंवा काढून टाकला गेला आहे.")
                                                }
                                                PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> {
                                                    onErrorChange("YouTube सुरक्षा निर्बंधांमुळे हा व्हिडिओ इन-ॲप प्लेयरमध्ये चालवण्यास मर्यादा आहे.")
                                                }
                                                else -> {
                                                    onErrorChange("थेट प्रक्षेपण प्लेबॅक त्रुटी आली. कृपया पुन्हा प्रयत्न करा.")
                                                }
                                            }
                                        }
                                    },
                                    true,
                                    options
                                )
                            }
                            addView(ytView)
                        }
                    },
                    update = {
                        if (currentVideoId != ytVideoId) {
                            currentVideoId = ytVideoId
                            youTubePlayerRef?.loadVideo(ytVideoId, 0f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // 1.16x edge cropping: pushes YouTube's top video title bar and
                            // bottom "More videos" / "YouTube" logo outside visible area, multiplied by user zoom scale
                            scaleX = 1.16f * videoScale
                            scaleY = 1.16f * videoScale
                        }
                        .testTag("youtube_native_live_player")
                )
            }

            DisposableEffect(lifecycleOwner) {
                onDispose {
                    youTubePlayerRef?.pause()
                    youTubePlayerViewRef?.let { view ->
                        lifecycleOwner.lifecycle.removeObserver(view)
                        view.release()
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PinchZoomTouchFrameLayout(
                        context = ctx,
                        onSingleTap = onToggleControls,
                        onDoubleTap = onDoubleTap,
                        onScaleChanged = onPinchScale
                    ).apply {
                        val wv = WebView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
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
                    settings.setSupportMultipleWindows(true)
                    settings.javaScriptCanOpenWindowsAutomatically = true

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onPlayerReady() {
                            post {
                                onErrorChange(null)
                                onLoadingChange(false)
                                onPlayStateChange(true)
                            }
                        }

                        @JavascriptInterface
                        fun onPlayerStateChange(state: Int) {
                            post {
                                when (state) {
                                    1 -> { // Playing
                                        onErrorChange(null)
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
                                if (errorCode == 100) {
                                    onErrorChange("व्हिडिओ आढळला नाही किंवा काढून टाकला गेला आहे.")
                                } else if (errorCode == 101 || errorCode == 150 || errorCode == 152) {
                                    onErrorChange("YouTube सुरक्षा निर्बंध किंवा एम्बेडिंग बंद असल्यामुळे हा व्हिडिओ चालण्यास मर्यादा आहे.")
                                } else {
                                    onErrorChange("थेट प्रक्षेपण लोड होत आहे. पुन्हा प्रयत्न करा.")
                                }
                            }
                        }
                    }, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            val newWebView = WebView(ctx)
                            newWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                    val u = req?.url?.toString() ?: return false
                                    openStreamInExternalApp(ctx, u, platform)
                                    return true
                                }
                            }
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = newWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

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

                        // ALLOW EMBEDDED IFRAMES & MEDIA BUT REDIRECT WATCH LINKS DIRECTLY TO APP
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false

                            // If user tapped "Watch video on YouTube" or any watch/live link, launch external app immediately
                            if (targetUrl.contains("youtube.com/watch") || targetUrl.contains("youtu.be/") ||
                                targetUrl.contains("youtube.com/live/") || targetUrl.contains("m.youtube.com/watch") ||
                                targetUrl.contains("youtube.com/shorts")) {
                                openStreamInExternalApp(ctx, targetUrl, StreamPlatform.YOUTUBE)
                                return true
                            }

                            // Never block subframes loading inner scripts/media
                            if (request.isForMainFrame == false) {
                                return false
                            }
                            if (targetUrl == "about:blank" || targetUrl.startsWith("data:")) {
                                return false
                            }
                            // Allow trusted player embed domains
                            if (targetUrl.contains("youtube.com/embed/") || targetUrl.contains("facebook.com/plugins/") ||
                                targetUrl.contains("instagram.com/p/") || targetUrl.contains("/embed/")) {
                                return false
                            }
                            // Handle native app intents or external schemes gracefully
                            if (targetUrl.startsWith("intent://") || targetUrl.startsWith("vnd.youtube:") ||
                                targetUrl.startsWith("fb://") || targetUrl.startsWith("instagram://")) {
                                try {
                                    val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {}
                                return true
                            }
                            // Default fallback: open in app
                            openStreamInExternalApp(ctx, targetUrl, platform)
                            return true
                        }
                    }

                    loadSmartPlayerHtml(this, cleanUrl, platform, ytVideoId)
                    onWebViewCreated(this)
                    onControllerReady(object : LivePlayerController {
                        override fun play() { evaluateJavascript("if(window.playVideo) window.playVideo();", null) }
                        override fun pause() { evaluateJavascript("if(window.pauseVideo) window.pauseVideo();", null) }
                        override fun mute() { evaluateJavascript("if(window.muteVideo) window.muteVideo();", null) }
                        override fun unMute() { evaluateJavascript("if(window.unMuteVideo) window.unMuteVideo();", null) }
                        override fun reload() { reload() }
                    })
                }
                addView(wv)
            }
        },
        update = { webView ->
            (webView as? ViewGroup)?.let { group ->
                findWebViewInViewGroup(group)?.let { wv -> onWebViewCreated(wv) }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = videoScale
                scaleY = videoScale
            }
            .testTag("smart_multiplatform_live_player")
    )
}
}
}

private fun loadSmartPlayerHtml(
    webView: WebView,
    streamUrl: String,
    platform: StreamPlatform,
    ytVideoId: String
) {
    val html = when (platform) {
        StreamPlatform.YOUTUBE -> {
            val validYtId = ytVideoId.ifEmpty { extractYouTubeId(streamUrl) }
            val embedSrc = if (validYtId.isNotEmpty()) {
                "https://www.youtube.com/embed/$validYtId?autoplay=1&mute=1&playsinline=1&controls=1&enablejsapi=1&origin=https://www.youtube.com&rel=0&iv_load_policy=3&modestbranding=1&widget_referrer=https://www.youtube.com"
            } else if (streamUrl.contains("channel/") || streamUrl.contains("@")) {
                val cleanPath = streamUrl.substringAfter("youtube.com/").trim('/')
                "https://www.youtube.com/$cleanPath"
            } else {
                "https://www.youtube.com/embed/$validYtId?autoplay=1&mute=1&playsinline=1&controls=1&enablejsapi=1&origin=https://www.youtube.com&rel=0&widget_referrer=https://www.youtube.com"
            }

            """
            <!DOCTYPE html>
            <html lang="mr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    #player-container { position: relative; width: 100vw; height: 100vh; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: 0; }
                </style>
            </head>
            <body>
                <div id="player-container">
                    <iframe 
                        id="yt-player"
                        src="$embedSrc" 
                        referrerpolicy="strict-origin-when-cross-origin"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                        allowfullscreen="true">
                    </iframe>
                </div>
                <script>
                    window.playVideo = function() {
                        var iframe = document.getElementById('yt-player');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                        }
                    };
                    window.pauseVideo = function() {
                        var iframe = document.getElementById('yt-player');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
                        }
                    };
                    window.muteVideo = function() {
                        var iframe = document.getElementById('yt-player');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"mute","args":""}', '*');
                        }
                    };
                    window.unMuteVideo = function() {
                        var iframe = document.getElementById('yt-player');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                            iframe.contentWindow.postMessage('{"event":"command","func":"setVolume","args":[100]}', '*');
                        }
                    };
                    setTimeout(function() {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }, 800);
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        StreamPlatform.FACEBOOK -> {
            val encodedFb = try { URLEncoder.encode(streamUrl, "UTF-8") } catch (_: Exception) { streamUrl }
            val fbEmbedUrl = "https://www.facebook.com/plugins/video.php?href=$encodedFb&show_text=false&autoplay=true&mute=1&container_width=0"
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
                <iframe id="fb-player" src="$fbEmbedUrl" allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share" allowFullScreen="true" frameborder="0"></iframe>
                <script>
                    setTimeout(function() {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }, 1000);
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        StreamPlatform.INSTAGRAM -> {
            val cleanIg = streamUrl.trim()
            val igEmbed = if (cleanIg.contains("/p/") || cleanIg.contains("/reel/") || cleanIg.contains("/tv/")) {
                val base = cleanIg.substringBefore("?").trimEnd('/')
                "$base/embed/"
            } else {
                cleanIg
            }
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
                <iframe id="ig-player" src="$igEmbed" allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share" allowtransparency="true" frameborder="0" scrolling="no"></iframe>
                <script>
                    setTimeout(function() {
                        if (window.AndroidBridge) window.AndroidBridge.onPlayerReady();
                    }, 1000);
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
                    if (Hls.isSupported() && (videoSrc.indexOf('.m3u8') !== -1 || videoSrc.indexOf('m3u8') !== -1)) {
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = (particle.startOffsetX * 280).dp)
                .offset(y = offsetY.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f * alpha),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = particle.text,
                        fontSize = 22.sp,
                        modifier = Modifier.alpha(alpha)
                    )
                }
            }
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
