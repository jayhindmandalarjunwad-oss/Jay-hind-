package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.SaffronPrimary
import com.example.util.MediaUtils
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * TextureView based Video Player to avoid SurfaceView z-order punch-through issues in Jetpack Compose
 */
class TextureVideoPlayerView(context: Context) : FrameLayout(context), TextureView.SurfaceTextureListener {
    val textureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var pendingUri: Uri? = null

    var onPreparedListener: ((durationMs: Int) -> Unit)? = null
    var onCompletionListener: (() -> Unit)? = null
    var onErrorListener: ((what: Int, extra: Int) -> Unit)? = null

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        addView(textureView, params)
        textureView.surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(st)
        mediaPlayer?.setSurface(surface)
        pendingUri?.let { setVideoUri(it) }
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        surface?.release()
        surface = null
        try { mediaPlayer?.setSurface(null) } catch (_: Exception) {}
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    fun setVideoUri(uri: Uri) {
        pendingUri = uri
        if (surface == null) return

        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setSurface(surface)
                setDataSource(context, uri)
                isLooping = false
                setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                    adjustAspectRatio(vWidth, vHeight)
                }
                setOnPreparedListener { mp ->
                    adjustAspectRatio(mp.videoWidth, mp.videoHeight)
                    onPreparedListener?.invoke(mp.duration)
                    mp.start()
                }
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    onErrorListener?.invoke(what, extra)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            onErrorListener?.invoke(-1, -1)
        }
    }

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) return
        val viewRatio = width.toFloat() / height.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

        val newWidth: Int
        val newHeight: Int
        if (videoRatio > viewRatio) {
            newWidth = width
            newHeight = (width / videoRatio).toInt()
        } else {
            newHeight = height
            newWidth = (height * videoRatio).toInt()
        }
        val lp = textureView.layoutParams as LayoutParams
        lp.width = newWidth
        lp.height = newHeight
        lp.gravity = Gravity.CENTER
        textureView.layoutParams = lp
    }

    fun play() {
        try { mediaPlayer?.start() } catch (_: Exception) {}
    }

    fun pause() {
        try { mediaPlayer?.pause() } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Int) {
        try { mediaPlayer?.seekTo(positionMs) } catch (_: Exception) {}
    }

    val isPlaying: Boolean
        get() = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }

    val currentPosition: Int
        get() = try { mediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }

    val duration: Int
        get() = try { mediaPlayer?.duration ?: 0 } catch (_: Exception) { 0 }

    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

/**
 * Finds the internal WebView inside YouTubePlayerView to control touch events and CSS
 */
private fun findWebViewInViewGroup(viewGroup: ViewGroup): WebView? {
    for (i in 0 until viewGroup.childCount) {
        val child = viewGroup.getChildAt(i)
        if (child is WebView) return child
        if (child is ViewGroup) {
            val found = findWebViewInViewGroup(child)
            if (found != null) return found
        }
    }
    return null
}

/**
 * Injects CSS into YouTube's internal WebView to completely hide the top video title bar,
 * the channel link, the bottom watermark "YouTube", "More videos", and pause overlays.
 */
private fun injectCleanYouTubeCSS(playerView: YouTubePlayerView) {
    playerView.post {
        try {
            val webView = findWebViewInViewGroup(playerView) ?: return@post
            // Disallow internal clicks on web elements so YouTube never shows overlay
            webView.setOnTouchListener { _, _ -> true }
            val css = ".ytp-chrome-top, .ytp-chrome-bottom, .ytp-watermark, .ytp-pause-overlay, .ytp-show-cards-title, .ytp-ce-element, .ytp-button.ytp-share-button, .ytp-title-channel, .ytp-title-link, .ytp-impression-link, .ytp-contextmenu, .ytp-cairo-refresh-signature-moments, .annotation, .ytp-gradient-top, .ytp-gradient-bottom { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; }"
            val js = """
                (function() {
                    var s = document.getElementById('clean_yt_css');
                    if (!s) {
                        s = document.createElement('style');
                        s.id = 'clean_yt_css';
                        s.type = 'text/css';
                        s.innerHTML = '$css';
                        document.head.appendChild(s);
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        } catch (_: Exception) {}
    }
}

/**
 * FrameLayout that intercepts 100% of touches before they can reach children (like YouTube's WebView).
 * This completely prevents YouTube's iframe from opening:
 * - The top title bar
 * - The channel avatar/link (preventing accidental redirection to YouTube app)
 * - The "More videos" overlay
 * - The "YouTube" logo
 * It converts clicks into single-tap events for our custom Compose UI.
 */
class TouchInterceptingFrameLayout(
    context: Context,
    private val onSingleTap: () -> Unit
) : FrameLayout(context) {

    private var downX = 0f
    private var downY = 0f
    private var isClick = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Intercept ALL touches so children (like YouTube's WebView) NEVER receive ANY touch event!
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isClick = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(event.x - downX)
                val dy = Math.abs(event.y - downY)
                if (dx > 30 || dy > 30) {
                    isClick = false
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isClick) {
                    onSingleTap()
                }
                return true
            }
        }
        return true
    }
}

/**
 * Utility to extract direct MP4 streaming URL from YouTube videos.
 * Allows playing YouTube videos directly in native MediaPlayer/TextureView with 0% YouTube UI.
 */
object YouTubeDirectStreamExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.private.coffee",
        "https://pipedapi.tokhmi.xyz"
    )

    suspend fun extractDirectStream(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in pipedInstances) {
            try {
                val req = Request.Builder()
                    .url("$instance/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val videoStreams = json.optJSONArray("videoStreams")
                        if (videoStreams != null && videoStreams.length() > 0) {
                            for (i in 0 until videoStreams.length()) {
                                val item = videoStreams.getJSONObject(i)
                                val format = item.optString("format")
                                val videoOnly = item.optBoolean("videoOnly", false)
                                val url = item.optString("url")
                                if (!videoOnly && (format.contains("mp4", ignoreCase = true) || item.optString("mimeType").contains("mp4"))) {
                                    return@withContext url
                                }
                            }
                            for (i in 0 until videoStreams.length()) {
                                val item = videoStreams.getJSONObject(i)
                                val videoOnly = item.optBoolean("videoOnly", false)
                                val url = item.optString("url")
                                if (!videoOnly && url.isNotEmpty()) {
                                    return@withContext url
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }
}

/**
 * WhatsApp & Gallery In-App Video Player Dialog
 * Supports:
 * 1. Direct In-App YouTube playback using official YouTubePlayerView (No Black Screen!)
 * 2. Full-Screen Edge-to-Edge Landscape Mode
 * 3. MP4 / Cloud video playback with TextureView + MediaPlayer, scrubber, play/pause, time tracker
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
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val youtubeVideoId = remember(videoUrl) { MediaUtils.extractYouTubeVideoId(videoUrl) }
    var directStreamUri by remember { mutableStateOf<Uri?>(null) }
    var isDirectStreamActive by remember { mutableStateOf(false) }
    val isYouTube = (youtubeVideoId != null) && !isDirectStreamActive

    var isPreparing by remember { mutableStateOf(true) }
    var playableUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var ytError by remember { mutableStateOf<String?>(null) }

    var texturePlayerRef by remember { mutableStateOf<TextureVideoPlayerView?>(null) }
    var youTubePlayerViewRef by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var youTubePlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var isYtReady by remember { mutableStateOf(false) }

    var currentPosition by remember { mutableIntStateOf(0) }
    var totalDuration by remember { mutableIntStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Toggle Landscape Fullscreen orientation
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Reset screen orientation & release players safely when dialog is closed
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                texturePlayerRef?.releaseMediaPlayer()
                youTubePlayerViewRef?.let { ypv ->
                    lifecycleOwner.lifecycle.removeObserver(ypv)
                    ypv.release()
                }
            } catch (_: Exception) {}
        }
    }

    // Direct stream extraction & native video preparation
    LaunchedEffect(videoUrl, youtubeVideoId) {
        if (youtubeVideoId != null) {
            isPreparing = true
            hasError = false
            try {
                val directUrl = withTimeoutOrNull(2500) {
                    YouTubeDirectStreamExtractor.extractDirectStream(youtubeVideoId)
                }
                if (directUrl != null) {
                    val uri = Uri.parse(directUrl)
                    directStreamUri = uri
                    playableUri = uri
                    isDirectStreamActive = true
                    isPreparing = false
                    return@LaunchedEffect
                }
            } catch (_: Exception) {}
            isDirectStreamActive = false
            isPreparing = false
            hasError = false
            return@LaunchedEffect
        }
        // Non-YouTube video
        isPreparing = true
        hasError = false
        try {
            val uri = MediaUtils.prepareVideoUriForPlayback(context, videoUrl)
            playableUri = uri
        } catch (e: Exception) {
            hasError = true
            errorMessage = e.localizedMessage ?: "व्हिडिओ लोड करता आला नाही"
        } finally {
            isPreparing = false
        }
    }

    // Auto-hide controls timer (for all players)
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // Progress update ticker for TexturePlayer
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            texturePlayerRef?.let { tp ->
                try {
                    if (tp.isPlaying) {
                        currentPosition = tp.currentPosition
                        val dur = tp.duration
                        if (dur > 0) totalDuration = dur
                    }
                } catch (_: Exception) {}
            }
            delay(300)
        }
    }

    // Periodically enforce Clean YouTube CSS injection during initialization
    LaunchedEffect(isYtReady) {
        if (isYtReady && isYouTube) {
            repeat(8) {
                delay(600)
                youTubePlayerViewRef?.let { injectCleanYouTubeCSS(it) }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDismiss()
        },
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
                .testTag("in_app_video_player_dialog")
        ) {
            // 1. YouTube Native Player (Hardware-Accelerated via androidyoutubeplayer with Touch Interceptor & Edge Crop)
            if (isYouTube && youtubeVideoId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TouchInterceptingFrameLayout(ctx) {
                                isControlsVisible = !isControlsVisible
                            }.apply {
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
                                            override fun onReady(player: YouTubePlayer) {
                                                youTubePlayerRef = player
                                                isYtReady = true
                                                isPreparing = false
                                                player.loadVideo(youtubeVideoId, 0f)
                                                injectCleanYouTubeCSS(this@apply)
                                            }

                                            override fun onCurrentSecond(
                                                player: YouTubePlayer,
                                                second: Float
                                            ) {
                                                currentPosition = (second * 1000).toInt()
                                            }

                                            override fun onVideoDuration(
                                                player: YouTubePlayer,
                                                duration: Float
                                            ) {
                                                if (duration > 0f) {
                                                    totalDuration = (duration * 1000).toInt()
                                                }
                                            }

                                            override fun onStateChange(
                                                player: YouTubePlayer,
                                                state: PlayerConstants.PlayerState
                                            ) {
                                                when (state) {
                                                    PlayerConstants.PlayerState.PLAYING -> {
                                                        isPreparing = false
                                                        isPlaying = true
                                                        ytError = null
                                                        injectCleanYouTubeCSS(this@apply)
                                                    }
                                                    PlayerConstants.PlayerState.PAUSED -> {
                                                        isPlaying = false
                                                        injectCleanYouTubeCSS(this@apply)
                                                    }
                                                    PlayerConstants.PlayerState.ENDED -> {
                                                        isPlaying = false
                                                        isCompleted = true
                                                        injectCleanYouTubeCSS(this@apply)
                                                    }
                                                    else -> {}
                                                }
                                            }

                                            override fun onError(
                                                player: YouTubePlayer,
                                                error: PlayerConstants.PlayerError
                                            ) {
                                                isPreparing = false
                                                when (error) {
                                                    PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> {
                                                        ytError = "व्हिडिओ आढळला नाही किंवा YouTube वरून काढून टाकण्यात आला आहे."
                                                    }
                                                    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> {
                                                        ytError = "YouTube सुरक्षा निर्बंधांमुळे हा व्हिडिओ इन-ॲप प्लेयरमध्ये चालवण्यास मर्यादा आहे. बाह्य YouTube ॲपमध्ये उघडा."
                                                    }
                                                    else -> {
                                                        ytError = "व्हिडिओ प्ले करताना अडचण आली."
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
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .graphicsLayer {
                                // 1.16x edge cropping: pushes YouTube's top video title bar and
                                // bottom "More videos" / "YouTube" logo outside visible area
                                scaleX = 1.16f
                                scaleY = 1.16f
                            }
                    )

                    if (!isYtReady && ytError == null) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "YouTube व्हिडिओ सुरू होत आहे...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (ytError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = ytError ?: "त्रुटी",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { MediaUtils.openVideo(context, videoUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("YouTube ॲपमध्ये उघडा", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (isPreparing) {
                // Loading State
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SaffronPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "व्हिडिओ लोड होत आहे...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (hasError) {
                // Error State
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
                        textAlign = TextAlign.Center
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
                // 2. TextureView based Standard MP4 / Cloud Video Player (Never goes black)
                AndroidView(
                    factory = { ctx ->
                        TextureVideoPlayerView(ctx).apply {
                            onPreparedListener = { dur ->
                                totalDuration = dur
                                isPlaying = true
                                isCompleted = false
                            }
                            onCompletionListener = {
                                isPlaying = false
                                isCompleted = true
                                isControlsVisible = true
                            }
                            onErrorListener = { what, extra ->
                                hasError = true
                                errorMessage = "व्हिडिओ प्लेबॅक त्रुटी आली ($what, $extra)."
                            }
                            playableUri?.let { setVideoUri(it) }
                            texturePlayerRef = this
                        }
                    },
                    update = { view ->
                        texturePlayerRef = view
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // Top Header Bar with Close, Title, Fullscreen, and Options
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onDismiss()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Video",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = title.ifEmpty { if (isYouTube) "मंडळ व्हिडिओ (YouTube)" else "व्हिडिओ संदेश" },
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (senderName.isNotBlank()) {
                                Text(
                                    text = senderName,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Fullscreen Toggle Button (Portrait <-> Landscape)
                        IconButton(onClick = { toggleFullscreen() }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Save to Gallery button (only for non-YouTube media files)
                        if (!isYouTube) {
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
                        }

                        // Open in External Player / Browser / YouTube app (Direct Fallback)
                        IconButton(
                            onClick = {
                                MediaUtils.openVideo(context, videoUrl)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = if (isYouTube) "Open in YouTube" else "Open in External App",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Big Center Play/Pause Button Overlay (Clean Custom Player UI)
            AnimatedVisibility(
                visible = isControlsVisible && !isPreparing && !hasError && ytError == null,
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
                                if (isYouTube) {
                                    youTubePlayerRef?.let { yp ->
                                        if (isCompleted) {
                                            yp.seekTo(0f)
                                            yp.play()
                                            isPlaying = true
                                            isCompleted = false
                                        } else if (isPlaying) {
                                            yp.pause()
                                            isPlaying = false
                                        } else {
                                            yp.play()
                                            isPlaying = true
                                        }
                                    }
                                } else {
                                    texturePlayerRef?.let { tp ->
                                        if (isCompleted) {
                                            tp.seekTo(0)
                                            tp.play()
                                            isPlaying = true
                                            isCompleted = false
                                        } else if (tp.isPlaying) {
                                            tp.pause()
                                            isPlaying = false
                                        } else {
                                            tp.play()
                                            isPlaying = true
                                        }
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

            // Bottom Progress Bar & Time Controls (Clean Custom Player UI)
            AnimatedVisibility(
                visible = isControlsVisible && !isPreparing && !hasError && ytError == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val progress = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                        var sliderPosition by remember(progress) { mutableFloatStateOf(progress) }

                        Slider(
                            value = sliderPosition,
                            onValueChange = { newPos ->
                                sliderPosition = newPos
                                val targetMs = (newPos * totalDuration).toInt()
                                currentPosition = targetMs
                                if (isYouTube) {
                                    youTubePlayerRef?.seekTo(targetMs / 1000f)
                                } else {
                                    texturePlayerRef?.seekTo(targetMs)
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
