package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LiveComment
import com.example.data.model.MandalInfo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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

    // Floating reaction bubbles list
    var floatingParticles by remember { mutableStateOf<List<FloatingParticle>>(emptyList()) }

    fun triggerFloatingReaction(reactionText: String) {
        val newParticle = FloatingParticle(
            id = System.currentTimeMillis() + Random.nextLong(1000),
            text = reactionText,
            startOffsetX = Random.nextFloat() * 0.7f + 0.15f
        )
        floatingParticles = floatingParticles + newParticle
        viewerCount += 1
        onSendReaction(reactionText)

        // Auto remove particle after animation
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

    // Restore orientation when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val videoId = remember(mandalInfo.liveStreamUrl) {
        extractYouTubeId(mandalInfo.liveStreamUrl)
    }

    val streamTitle = mandalInfo.liveStreamTitle.ifEmpty { "श्री गणेश महाआरती थेट प्रक्षेपण" }
    val isLiveActive = mandalInfo.isLiveStreamActive

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
                    .fillMaxHeight(0.94f)
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
                // Header Bar (Hidden or Compact in Landscape)
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
                                    text = if (isLiveActive) "🔴 LIVE" else "व्हिडिओ",
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
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        InAppVideoWebView(
                            videoId = videoId,
                            streamUrl = mandalInfo.liveStreamUrl,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating Reaction Overlay in Fullscreen
                        FloatingReactionOverlay(particles = floatingParticles)

                        // Compact Reaction Bar on bottom in Landscape
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
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
                        // 1. VIDEO PLAYER CONTAINER (16:9 Aspect Ratio)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            InAppVideoWebView(
                                videoId = videoId,
                                streamUrl = mandalInfo.liveStreamUrl,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating Reaction Overlay over video
                            FloatingReactionOverlay(particles = floatingParticles)

                            // Overlay viewer badge & rotate hint on top right
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "👁️ $viewerCount",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier.clickable { toggleOrientationFullscreen() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenRotation,
                                            contentDescription = "Rotate",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "आडवा करा",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
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

                        // 3. LIVE CHAT & COMMENTS FEED
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
                                        text = "खालील बॉक्समध्ये टाईप करून सर्व सभासदांना आपला संदेश पाठवा.",
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

                        // 5. ACTION BUTTONS: WHATSAPP SHARE & YOUTUBE / COPY
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
                                    } catch (e: Exception) {
                                        // Fallback to regular chooser if WhatsApp is not directly matched
                                        val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(chooserIntent, "थेट प्रक्षेपण शेअर करा"))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(42.dp)
                                    .testTag("whatsapp_share_live_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WhatsApp वर शेअर करा",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            // Open in YouTube Button
                            OutlinedButton(
                                onClick = {
                                    val targetUrl = mandalInfo.liveStreamUrl.ifEmpty {
                                        "https://www.youtube.com/@JayHindMandalArjunwad/live"
                                    }
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(42.dp)
                                    .testTag("open_youtube_external_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BloodRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = BloodRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "YouTube",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BloodRed,
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

// IN-APP YOUTUBE WEBVIEW COMPONENT
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InAppVideoWebView(
    videoId: String,
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    if (videoId.isNotEmpty()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowContentAccess = true
                    settings.allowFileAccess = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()

                    val embedHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                body { margin: 0; padding: 0; background-color: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; height: 100vh; }
                                iframe { width: 100%; height: 100%; border: 0; }
                            </style>
                        </head>
                        <body>
                            <iframe 
                                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1" 
                                frameborder="0" 
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = modifier.testTag("live_webview_player")
        )
    } else if (streamUrl.isNotBlank()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl(streamUrl)
                }
            },
            modifier = modifier
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = SaffronPrimary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "सध्या कोणतेही थेट प्रक्षेपण चालू नाही.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "मंडळाच्या आगामी आरती किंवा उत्सवाची वेळ लवकरच जाहीर केली जाईल.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
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
                .padding(start = (particle.startOffsetX * 300).dp)
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
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        minutes < 1 -> "आत्ताच"
        minutes < 60 -> "${minutes} मि. पूर्वी"
        hours < 24 -> "${hours} ता. पूर्वी"
        else -> "पूर्वी"
    }
}
