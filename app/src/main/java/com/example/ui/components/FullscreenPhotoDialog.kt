package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SaffronPrimary
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

@Composable
fun FullscreenPhotoDialog(
    photos: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val validPhotos = remember(photos) { photos.filter { it.isNotBlank() } }
    if (validPhotos.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    val safeInitialIndex = remember(validPhotos, initialIndex) {
        initialIndex.coerceIn(0, (validPhotos.size - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { validPhotos.size }
    )

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
                .testTag("fullscreen_photo_dialog")
        ) {
            // Horizontal Pager for smooth left/right swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 56.dp)
            ) { page ->
                val currentUrl = validPhotos.getOrNull(page)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUrl != null) {
                        UniversalAsyncImage(
                            model = currentUrl,
                            contentDescription = "Full Screen Photo ${page + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Left / Right Quick Navigation Arrows when multiple photos exist
            if (validPhotos.size > 1) {
                // Previous button (Left)
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "मागील फोटो",
                            tint = Color.White
                        )
                    }
                }

                // Next button (Right)
                if (pagerState.currentPage < validPhotos.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "पुढील फोटो",
                            tint = Color.White
                        )
                    }
                }
            }

            // Top Bar
            Surface(
                color = Color.Black.copy(alpha = 0.70f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करा",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "फोटो दर्शक",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (validPhotos.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${validPhotos.size} (डावीकडे/उजवीकडे स्वाइप करा)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Download Button in Top Bar (Share button removed per request)
                    IconButton(
                        onClick = {
                            val activePhotoUrl = validPhotos.getOrNull(pagerState.currentPage)
                            if (activePhotoUrl != null && !isDownloading) {
                                coroutineScope.launch {
                                    isDownloading = true
                                    MediaUtils.saveImageToGallery(context, activePhotoUrl)
                                    isDownloading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SaffronPrimary)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "डाऊनलोड करा",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Bar with Dots & Download CTA
            Surface(
                color = Color.Black.copy(alpha = 0.80f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page indicator dots for multi-photos
                    if (validPhotos.size > 1 && validPhotos.size <= 12) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            for (i in validPhotos.indices) {
                                val isSelected = pagerState.currentPage == i
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SaffronPrimary else Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val activePhotoUrl = validPhotos.getOrNull(pagerState.currentPage)
                            if (activePhotoUrl != null && !isDownloading) {
                                coroutineScope.launch {
                                    isDownloading = true
                                    MediaUtils.saveImageToGallery(context, activePhotoUrl)
                                    isDownloading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "मोबाईल गॅलरीमध्ये सेव्ह करा",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single photo backwards-compatible overload
 */
@Composable
fun FullscreenPhotoDialog(
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    if (photoUrl.isNullOrBlank()) return
    FullscreenPhotoDialog(
        photos = listOf(photoUrl),
        initialIndex = 0,
        onDismiss = onDismiss
    )
}
