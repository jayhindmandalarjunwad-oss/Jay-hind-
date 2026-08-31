package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MandalInfo
import com.example.ui.theme.*

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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamDialog(
    mandalInfo: MandalInfo,
    onDismiss: () -> Unit,
    onSendReaction: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val videoId = remember(mandalInfo.liveStreamUrl) {
        extractYouTubeId(mandalInfo.liveStreamUrl)
    }

    val streamTitle = mandalInfo.liveStreamTitle.ifEmpty { "श्री गणेश महाआरती थेट प्रक्षेपण" }
    val isLiveActive = mandalInfo.isLiveStreamActive

    // Pulse animation for LIVE badge
    val infiniteTransition = rememberInfiniteTransition(label = "live_badge_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    var lastReaction by remember { mutableStateOf<String?>(null) }
    var reactionCount by remember { mutableIntStateOf(148) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("live_stream_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8E0E00), Color(0xFF1F1C18))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLiveActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BloodRed.copy(alpha = alphaAnim),
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Text(
                                        text = "🔴 LIVE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SaffronPrimary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "व्हिडिओ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isLiveActive) "थेट प्रक्षेपण सुरू आहे" else "थेट दर्शन व व्हिडिओ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_live_stream_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करा",
                            tint = Color.White
                        )
                    }
                }

                // In-App YouTube Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
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
                                                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1" 
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
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("live_webview_player")
                        )
                    } else if (mandalInfo.liveStreamUrl.isNotBlank()) {
                        // If direct URL is provided without parsed 11-char ID (e.g. channel URL)
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
                                    loadUrl(mandalInfo.liveStreamUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
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

                // Stream Details & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title & Viewer count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = streamTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🚩 ${mandalInfo.mandalName}, अर्जुनवाड",
                                fontSize = 12.sp,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isLiveActive) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SaffronPrimary.copy(alpha = 0.15f),
                                contentColor = SaffronDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "👁️ $reactionCount",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Live Reactions & Greetings (जयघोष व प्रतिक्रिया)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🚩 थेट जयघोष व प्रतिक्रिया द्या (Live Reactions):",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "🚩 जय हिंद!",
                                "🙏 बाप्पा मोरया!",
                                "🌺 जय महाराष्ट्र!",
                                "💐 हर हर महादेव!",
                                "❤️ छान!"
                            ).forEach { reaction ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SaffronLight.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clickable {
                                            lastReaction = reaction
                                            reactionCount += 1
                                            onSendReaction(reaction)
                                        }
                                        .testTag("reaction_btn_${reaction.take(4)}")
                                ) {
                                    Text(
                                        text = reaction,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SaffronDark
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = lastReaction != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "✅ आपण '$lastReaction' पाठवले आहे!",
                                fontSize = 12.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons Row: Open in YouTube & Share on WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetUrl = mandalInfo.liveStreamUrl.ifEmpty {
                                    "https://www.youtube.com/@JayHindMandalArjunwad/live"
                                }
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("open_youtube_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YouTube वर पहा",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val liveUrl = mandalInfo.liveStreamUrl.ifEmpty {
                                    "https://www.youtube.com/@JayHindMandalArjunwad/live"
                                }
                                val shareText = "🚩 *जय हिंद मंडळ, अर्जुनवाड*\n🔴 *$streamTitle*\n\nथेट सोहळा व दर्शन घेण्यासाठी खालील लिंकवर क्लिक करा:\n$liveUrl\n\n_जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड_"
                                try {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "थेट प्रक्षेपण शेअर करा"))
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_live_stream_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SuccessGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "शेअर करा",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
