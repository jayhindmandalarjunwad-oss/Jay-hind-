package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.model.Album
import com.example.data.model.GalleryPhoto
import com.example.data.model.VideoItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FullscreenPhotoDialog
import com.example.ui.components.GalleryImagePicker
import com.example.ui.components.MandalTopHeader
import com.example.ui.components.UniversalAsyncImage
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MandalViewModel

enum class GalleryTab(val title: String) {
    PHOTOS("फोटो व ॲल्बम्स"),
    VIDEOS("व्हिडिओ संग्रह")
}

@Composable
fun GalleryScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val photoAlbums by viewModel.photoAlbums.collectAsStateWithLifecycle()
    val videoAlbums by viewModel.videoAlbums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val selectedVideoAlbum by viewModel.selectedVideoAlbum.collectAsStateWithLifecycle()
    val photos by viewModel.albumPhotos.collectAsStateWithLifecycle()
    val albumVideos by viewModel.albumVideos.collectAsStateWithLifecycle()
    val fullscreenViewerState by viewModel.fullscreenViewerState.collectAsStateWithLifecycle()

    val isAdmin = currentUser?.isAnyAdmin == true

    var selectedTab by remember(currentScreen) {
        mutableStateOf(if (currentScreen == AppScreen.VIDEO_GALLERY) GalleryTab.VIDEOS else GalleryTab.PHOTOS)
    }

    var showCreatePhotoAlbumDialog by remember { mutableStateOf(false) }
    var showCreateVideoAlbumDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showAddVideoDialog by remember { mutableStateOf(false) }

    var editingAlbum by remember { mutableStateOf<Album?>(null) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var editingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var activePlayingVideo by remember { mutableStateOf<VideoItem?>(null) }

    val isPhotoAlbumOpen = selectedTab == GalleryTab.PHOTOS && selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()
    val isVideoAlbumOpen = selectedTab == GalleryTab.VIDEOS && selectedVideoAlbum != null && selectedVideoAlbum!!.id.isNotEmpty()

    // Step-by-Step Back Navigation for Fullscreen Photos, Videos & Albums
    BackHandler(
        enabled = (fullscreenViewerState != null && fullscreenViewerState!!.photos.isNotEmpty()) ||
                  activePlayingVideo != null ||
                  isPhotoAlbumOpen ||
                  isVideoAlbumOpen
    ) {
        when {
            // 1. Close photo viewer if active
            fullscreenViewerState != null && fullscreenViewerState!!.photos.isNotEmpty() -> {
                viewModel.closeFullscreenPhoto()
            }
            // 2. Stop playing video if active
            activePlayingVideo != null -> {
                activePlayingVideo = null
            }
            // 3. Close open photo album and return to photo albums list
            isPhotoAlbumOpen -> {
                viewModel.closeAlbum()
            }
            // 4. Close open video album and return to video albums list
            isVideoAlbumOpen -> {
                viewModel.closeVideoAlbum()
            }
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            val title = when {
                isPhotoAlbumOpen -> selectedAlbum!!.title
                isVideoAlbumOpen -> selectedVideoAlbum!!.title
                selectedTab == GalleryTab.VIDEOS -> "व्हिडिओ संग्रह व ॲल्बम्स"
                else -> "मंडळ गॅलरी (Gallery)"
            }
            val subtitle = when {
                isPhotoAlbumOpen -> "${photos.size} फोटो • ${selectedAlbum!!.category}"
                isVideoAlbumOpen -> "${albumVideos.size} व्हिडिओ • ${selectedVideoAlbum!!.category}"
                selectedTab == GalleryTab.VIDEOS -> "${videoAlbums.size} व्हिडिओ ॲल्बम्स"
                else -> "${photoAlbums.size} फोटो ॲल्बम्स"
            }
            MandalTopHeader(
                title = title,
                subtitle = subtitle,
                logoUrl = mandalLogoUrl,
                showBackButton = true,
                onBackClick = {
                    when {
                        isPhotoAlbumOpen -> viewModel.closeAlbum()
                        isVideoAlbumOpen -> viewModel.closeVideoAlbum()
                        else -> onBack()
                    }
                },
                actions = {
                    if (isAdmin) {
                        when {
                            isPhotoAlbumOpen -> {
                                IconButton(
                                    onClick = { editingAlbum = selectedAlbum },
                                    modifier = Modifier.testTag("admin_edit_photo_album_header")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "शीर्षक व कॅटेगिरी बदला", tint = SaffronPrimary)
                                }
                                IconButton(
                                    onClick = { showAddPhotoDialog = true },
                                    modifier = Modifier.testTag("admin_add_photo_button")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "फोटो जोडा", tint = SaffronPrimary)
                                }
                            }
                            isVideoAlbumOpen -> {
                                IconButton(
                                    onClick = { editingAlbum = selectedVideoAlbum },
                                    modifier = Modifier.testTag("admin_edit_video_album_header")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "शीर्षक व कॅटेगिरी बदला", tint = BloodRed)
                                }
                                IconButton(
                                    onClick = { showAddVideoDialog = true },
                                    modifier = Modifier.testTag("admin_add_video_button")
                                ) {
                                    Icon(Icons.Default.VideoCall, contentDescription = "व्हिडिओ जोडा", tint = BloodRed)
                                }
                            }
                            selectedTab == GalleryTab.PHOTOS -> {
                                IconButton(
                                    onClick = { showCreatePhotoAlbumDialog = true },
                                    modifier = Modifier.testTag("admin_create_album_button")
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "नवीन ॲल्बम", tint = SaffronPrimary)
                                }
                            }
                            else -> {
                                IconButton(
                                    onClick = { showCreateVideoAlbumDialog = true },
                                    modifier = Modifier.testTag("admin_create_video_album_button")
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "नवीन व्हिडिओ ॲल्बम", tint = BloodRed)
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
                .testTag("gallery_root_screen")
        ) {
            // When NO album is currently opened, show Official Banner & Tab Bar (Photos / Videos)
            if (!isPhotoAlbumOpen && !isVideoAlbumOpen) {
                // Official Ribbon Banner Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(SaffronPrimary, GoldenTertiary)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📸 मंडळाचे अधिकृत क्षणचित्रे व व्हिडिओ संग्रह",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = SurfaceWarm,
                    contentColor = SaffronPrimary,
                    modifier = Modifier.border(0.6.dp, CardBorderColor)
                ) {
                    Tab(
                        selected = selectedTab == GalleryTab.PHOTOS,
                        onClick = { selectedTab = GalleryTab.PHOTOS },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        text = {
                            Text(
                                text = "फोटो ॲल्बम्स (${photoAlbums.size})",
                                fontWeight = if (selectedTab == GalleryTab.PHOTOS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == GalleryTab.VIDEOS,
                        onClick = { selectedTab = GalleryTab.VIDEOS },
                        icon = {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selectedTab == GalleryTab.VIDEOS) BloodRed else TextSecondary
                            )
                        },
                        text = {
                            Text(
                                text = "व्हिडिओ ॲल्बम्स (${videoAlbums.size})",
                                fontWeight = if (selectedTab == GalleryTab.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == GalleryTab.VIDEOS) BloodRed else TextSecondary
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    // 1. INSIDE SELECTED PHOTO ALBUM
                    isPhotoAlbumOpen -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = SurfaceVariantWarm,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.closeAlbum() }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "सर्व फोटो ॲल्बम्स",
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "सर्व फोटो ॲल्बम्स",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SaffronPrimary
                                        )
                                    }

                                    if (isAdmin) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { editingAlbum = selectedAlbum },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = SaffronPrimary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("शीर्षक बदला", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                                            }
                                            Button(
                                                onClick = { showAddPhotoDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("फोटो जोडा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (photos.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.PhotoLibrary,
                                    title = "या ॲल्बममध्ये फोटो उपलब्ध नाहीत",
                                    subtitle = if (isAdmin) "नवीन फोटो जोडण्यासाठी 'फोटो जोडा' बटण वापरा." else "लवकरच फोटो जोडले जातील."
                                )
                            } else {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    contentPadding = PaddingValues(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalItemSpacing = 10.dp,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                                        // Pinterest / Instagram dynamic staggered aspect ratios
                                        val itemAspectRatio = when (index % 5) {
                                            0 -> 0.75f // Portrait tall
                                            1 -> 1.25f // Landscape
                                            2 -> 1.0f  // Square
                                            3 -> 0.85f // Medium vertical
                                            else -> 1.15f // Extended landscape
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(itemAspectRatio)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(1.dp, CardBorderColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    viewModel.recordPhotoView(photo.id)
                                                    viewModel.openFullscreenPhotos(
                                                        photos = photos.map { it.imageUrl },
                                                        titles = photos.map { it.caption },
                                                        viewCounts = photos.map { it.viewCount },
                                                        initialIndex = index
                                                    )
                                                }
                                        ) {
                                            UniversalAsyncImage(
                                                model = photo.imageUrl,
                                                contentDescription = photo.caption,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Mandal Watermark Stamp (Top Left)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.45f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "🚩 जय हिंद",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (photo.caption.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.85f)
                                                                )
                                                            )
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = photo.caption,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (isAdmin) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { editingPhoto = photo },
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "शीर्षक बदला",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deletePhoto(photo.id) },
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "हटवा",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }

                                                // Admin Only: Photo View Count Badge
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.Black.copy(alpha = 0.65f),
                                                    modifier = Modifier
                                                        .align(if (photo.caption.isNotBlank()) Alignment.BottomEnd else Alignment.BottomStart)
                                                        .padding(6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = null,
                                                            tint = Color(0xFF34D399),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "${photo.viewCount}",
                                                            color = Color(0xFF34D399),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
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

                    // 2. INSIDE SELECTED VIDEO ALBUM
                    isVideoAlbumOpen -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = SurfaceVariantWarm,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.closeVideoAlbum() }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "सर्व व्हिडिओ ॲल्बम्स",
                                            tint = BloodRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "सर्व व्हिडिओ ॲल्बम्स",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = BloodRed
                                        )
                                    }

                                    if (isAdmin) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { editingAlbum = selectedVideoAlbum },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = BloodRed)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("शीर्षक बदला", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BloodRed)
                                            }
                                            Button(
                                                onClick = { showAddVideoDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("व्हिडिओ जोडा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (albumVideos.isEmpty()) {
                                val isLiveAlbum = selectedVideoAlbum?.id == "album_live_videos" || selectedVideoAlbum?.title?.contains("LIVE VIDEO", ignoreCase = true) == true
                                EmptyStateView(
                                    icon = Icons.Default.VideoLibrary,
                                    title = if (isLiveAlbum) "अद्याप थेट प्रक्षेपण (Live Video) सेव्ह झालेले नाही" else "या व्हिडिओ ॲल्बममध्ये व्हिडिओ उपलब्ध नाहीत",
                                    subtitle = if (isLiveAlbum) "लाईव्ह संपल्यानंतर सर्व थेट प्रक्षेपणे या अल्बममध्ये तारीख व शीर्षकासह आपोआप कायमस्वरूपी सेव्ह होतील." else if (isAdmin) "नवीन व्हिडिओ जोडण्यासाठी 'व्हिडिओ जोडा' बटण वापरा." else "लवकरच व्हिडिओ जोडले जातील."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                                ) {
                                    items(albumVideos, key = { it.id }) { video ->
                                        VideoCard(
                                            video = video,
                                            isAdmin = isAdmin,
                                            onClick = {
                                                viewModel.recordVideoView(video.id)
                                                activePlayingVideo = video
                                            },
                                            onEdit = { editingVideo = video },
                                            onDelete = { viewModel.deleteVideo(video.id, selectedVideoAlbum!!.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. PHOTO ALBUMS GRID (When in Photos tab and no album open)
                    selectedTab == GalleryTab.PHOTOS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isAdmin) {
                                Surface(
                                    color = SurfaceWarm,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SaffronPrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ॲडमिन: फोटो ॲल्बम व्यवस्थापन",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                        }
                                        Button(
                                            onClick = { showCreatePhotoAlbumDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("नवीन ॲल्बम", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (photoAlbums.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.PhotoLibrary,
                                    title = "कोणतेही फोटो ॲल्बम नाहीत",
                                    subtitle = if (isAdmin) "नवीन फोटो ॲल्बम तयार करा." else "मंडळाचे फोटो ॲल्बम लवकरच उपलब्ध होतील."
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(photoAlbums, key = { it.id }) { album ->
                                        AlbumCard(
                                            album = album,
                                            isAdmin = isAdmin,
                                            onClick = { viewModel.openAlbum(album) },
                                            onEdit = { editingAlbum = album },
                                            onDelete = { viewModel.deleteAlbum(album.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. VIDEO ALBUMS GRID (When in Videos tab and no album open)
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isAdmin) {
                                Surface(
                                    color = SurfaceWarm,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f, fill = false),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BloodRed, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "व्हिडिओ ॲल्बम व्यवस्थापन",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { showCreateVideoAlbumDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ नवीन ॲल्बम", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (videoAlbums.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.VideoLibrary,
                                    title = "कोणतेही व्हिडिओ ॲल्बम उपलब्ध नाहीत",
                                    subtitle = if (isAdmin) "नवीन व्हिडिओ ॲल्बम तयार करण्यासाठी बटण वापरा." else "लवकरच व्हिडिओ ॲल्बम उपलब्ध होतील."
                                )
                            } else {
                                val sortedVideoAlbums = remember(videoAlbums) {
                                    videoAlbums.sortedWith(
                                        compareByDescending<Album> { it.id == "album_live_videos" || it.title.contains("LIVE VIDEO", ignoreCase = true) }
                                            .thenByDescending { it.createdAt }
                                    )
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(sortedVideoAlbums, key = { it.id }) { album ->
                                        VideoAlbumCard(
                                            album = album,
                                            isAdmin = isAdmin,
                                            onClick = { viewModel.openVideoAlbum(album) },
                                            onEdit = { editingAlbum = album },
                                            onDelete = { viewModel.deleteAlbum(album.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FULLSCREEN PHOTO VIEWER DIALOG
            if (fullscreenViewerState != null && fullscreenViewerState!!.photos.isNotEmpty()) {
                FullscreenPhotoDialog(
                    photos = fullscreenViewerState!!.photos,
                    titles = fullscreenViewerState!!.titles,
                    viewCounts = fullscreenViewerState!!.viewCounts,
                    initialIndex = fullscreenViewerState!!.initialIndex,
                    isAdmin = isAdmin,
                    onDismiss = { viewModel.closeFullscreenPhoto() }
                )
            }

            // CREATE PHOTO ALBUM DIALOG (Admin)
            if (showCreatePhotoAlbumDialog) {
                CreateAlbumDialog(
                    isPhotoAlbum = true,
                    onDismiss = { showCreatePhotoAlbumDialog = false },
                    onCreate = { title, cat, cover, desc ->
                        viewModel.createAlbum(title, cat, cover, desc)
                        showCreatePhotoAlbumDialog = false
                    }
                )
            }

            // CREATE VIDEO ALBUM DIALOG (Admin)
            if (showCreateVideoAlbumDialog) {
                CreateAlbumDialog(
                    isPhotoAlbum = false,
                    onDismiss = { showCreateVideoAlbumDialog = false },
                    onCreate = { title, cat, cover, desc ->
                        viewModel.createVideoAlbum(title, cat, cover, desc)
                        showCreateVideoAlbumDialog = false
                    }
                )
            }

            // EDIT ALBUM (TITLE, CATEGORY, COVER, DESC) DIALOG (Admin)
            if (editingAlbum != null) {
                EditAlbumDialog(
                    album = editingAlbum!!,
                    isPhotoAlbum = editingAlbum!!.albumType != "VIDEO",
                    onDismiss = { editingAlbum = null },
                    onSave = { title, category, coverImage, desc ->
                        viewModel.updateAlbum(editingAlbum!!.id, title, category, coverImage, desc)
                        editingAlbum = null
                    }
                )
            }

            // ADD PHOTO DIALOG (Admin)
            if (showAddPhotoDialog) {
                AddPhotoDialog(
                    onDismiss = { showAddPhotoDialog = false },
                    onAdd = { url, caption ->
                        viewModel.addPhotoToActiveAlbum(url, caption)
                        showAddPhotoDialog = false
                    }
                )
            }

            // EDIT PHOTO CAPTION / TITLE DIALOG (Admin)
            if (editingPhoto != null) {
                EditPhotoCaptionDialog(
                    photo = editingPhoto!!,
                    onDismiss = { editingPhoto = null },
                    onSave = { newCaption ->
                        viewModel.updatePhotoCaption(editingPhoto!!.id, newCaption)
                        editingPhoto = null
                    }
                )
            }

            // ADD VIDEO DIALOG (Admin)
            if (showAddVideoDialog) {
                AddVideoDialog(
                    albumTitle = selectedVideoAlbum?.title ?: "",
                    defaultCategory = selectedVideoAlbum?.category ?: "गणेशोत्सव",
                    onDismiss = { showAddVideoDialog = false },
                    onAdd = { title, desc, cat, videoUrl, thumbUrl ->
                        viewModel.addVideoToActiveAlbum(title, desc, cat, videoUrl, thumbUrl)
                        showAddVideoDialog = false
                    }
                )
            }

            // EDIT VIDEO DIALOG (Admin)
            if (editingVideo != null) {
                EditVideoDialog(
                    video = editingVideo!!,
                    onDismiss = { editingVideo = null },
                    onSave = { title, desc, category, thumbUrl ->
                        viewModel.updateVideo(editingVideo!!.id, title, desc, category, thumbUrl)
                        editingVideo = null
                    }
                )
            }

            // IN-APP VIDEO PLAYER DIALOG
            if (activePlayingVideo != null) {
                val currentVideoId = activePlayingVideo!!.id
                val latestVideo = albumVideos.find { it.id == currentVideoId } ?: activePlayingVideo!!
                VideoPlayerDialog(
                    videoUrl = latestVideo.videoUrl,
                    title = latestVideo.title,
                    senderName = "जयहिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड • ${latestVideo.category}",
                    thumbnailUrl = latestVideo.thumbnailUrl,
                    viewCount = latestVideo.viewCount,
                    isAdmin = isAdmin,
                    onDismiss = { activePlayingVideo = null }
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("album_card_${album.id}")
    ) {
        // Stack deck back layer for 3D photo album aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(180.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = -6f
                    scaleX = 0.94f
                }
                .clip(RoundedCornerShape(18.dp))
                .background(SaffronPrimary.copy(alpha = 0.25f))
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, CardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                ) {
                    UniversalAsyncImage(
                        model = album.coverImageUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle bottom gradient inside photo container for high legibility
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                )
                            )
                    )

                    // Modern Frosted Glass Category Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.60f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SaffronPrimary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = album.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (isAdmin) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "शीर्षक बदला", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${album.photoCount} फोटो",
                                style = MaterialTheme.typography.bodySmall,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoAlbumCard(
    album: Album,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isLiveAlbum = album.id == "album_live_videos" || album.title.contains("LIVE VIDEO", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_album_card_${album.id}")
    ) {
        // Stack deck back layer for 3D video reel aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(180.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = -6f
                    scaleX = 0.94f
                }
                .clip(RoundedCornerShape(18.dp))
                .background(BloodRed.copy(alpha = if (isLiveAlbum) 0.35f else 0.22f))
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(
                if (isLiveAlbum) 1.2.dp else 0.6.dp,
                if (isLiveAlbum) BloodRed else CardBorderColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isLiveAlbum) 3.dp else 2.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                ) {
                    UniversalAsyncImage(
                        model = album.coverImageUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Modern translucent play badge with subtle glassmorphism overlay
                    // Doesn't obstruct thumbnail text and looks clean & modern
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                            .border(1.2.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Album",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Compact frosted category badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLiveAlbum) BloodRed.copy(alpha = 0.90f) else Color.Black.copy(alpha = 0.50f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                        contentColor = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            if (isLiveAlbum) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(BloodRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = album.category,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isAdmin) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "शीर्षक व कॅटेगिरी बदला", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isLiveAlbum) BloodRed else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = BloodRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveAlbum) "${album.photoCount} थेट प्रक्षेपणे" else "${album.photoCount} व्हिडिओ",
                                style = MaterialTheme.typography.bodySmall,
                                color = BloodRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    video: VideoItem,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isLiveVideo = video.albumId == "album_live_videos" ||
        video.category.contains("LIVE", ignoreCase = true) ||
        video.category.contains("थेट")

    val formattedDate = remember(video.uploadedAt) {
        try {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("mr", "IN"))
            sdf.format(Date(video.uploadedAt))
        } catch (e: Exception) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(video.uploadedAt))
        }
    }

        Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(
            if (isLiveVideo) 1.dp else 0.6.dp,
            if (isLiveVideo) BloodRed.copy(alpha = 0.5f) else CardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                UniversalAsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play Icon Overlay - Semi-transparent glass circle with subtle white border
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Top-Left Badge: Live Replay badge
                if (isLiveVideo) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BloodRed,
                        contentColor = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "🔴 थेट रेकॉर्डिंग (Live Replay)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = video.duration,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "व्हिडिओचे नाव बदला", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // शीर्षक (Title) Display
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                // प्रक्षेपण दिनांक व वेळ (Date and Time)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = BloodRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "प्रक्षेपण: $formattedDate",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${video.viewCount} व्ह्यूज",
                                    color = Color(0xFF059669),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLiveVideo) BloodRed.copy(alpha = 0.1f) else SurfaceVariantWarm
                    ) {
                        Text(
                            text = if (isLiveVideo) "🔴 LIVE VIDEO" else video.category,
                            color = if (isLiveVideo) BloodRed else NavySecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // पुन्हा पहा (Watch Replay) बटन
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("watch_replay_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "पुन्हा पहा (Watch Replay)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// DIALOG: CREATE ALBUM (Supports both Photo and Video Albums)
@Composable
fun CreateAlbumDialog(
    isPhotoAlbum: Boolean = true,
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String, coverImage: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("गणेशोत्सव") }
    var coverImage by remember {
        mutableStateOf(
            if (isPhotoAlbum)
                "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80"
            else
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80"
        )
    }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("गणेशोत्सव", "शिवजयंती", "क्रीडा स्पर्धा", "सामाजिक उपक्रम", "आरोग्य शिबिर", "इतर")
    val themeColor = if (isPhotoAlbum) SaffronPrimary else BloodRed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPhotoAlbum) "नवीन फोटो ॲल्बम तयार करा" else "नवीन व्हिडिओ ॲल्बम तयार करा",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isPhotoAlbum) "फोटो ॲल्बम शीर्षक / नाव *" else "व्हिडिओ ॲल्बम शीर्षक / नाव *") },
                    placeholder = { Text(if (isPhotoAlbum) "उदा. गणेशोत्सव २०२४ फोटो" else "उदा. गणेशोत्सव २०२४ व्हिडिओ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("वर्गवारी (Category):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Text("कव्हर फोटो निवडा (Cover Photo):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                GalleryImagePicker(
                    selectedImageUrl = coverImage,
                    onImageSelected = { coverImage = it },
                    label = "गॅलरीतून कव्हर फोटो निवडा",
                    helperText = "मोबाईल गॅलरीतून ॲल्बमसाठी मुख्य कव्हर फोटो निवडा",
                    height = 120.dp
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("ॲल्बम माहिती / वर्णन (पर्यायी)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && coverImage.isNotBlank()) onCreate(title, category, coverImage, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("तयार करा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

// DIALOG: EDIT ALBUM (TITLE, CATEGORY, COVER, DESC)
@Composable
fun EditAlbumDialog(
    album: Album,
    isPhotoAlbum: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, coverImage: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf(album.title) }
    var category by remember { mutableStateOf(album.category) }
    var coverImage by remember { mutableStateOf(album.coverImageUrl) }
    var desc by remember { mutableStateOf(album.description) }

    val categories = listOf("गणेशोत्सव", "शिवजयंती", "क्रीडा स्पर्धा", "सामाजिक उपक्रम", "आरोग्य शिबिर", "इतर")
    val themeColor = if (isPhotoAlbum) SaffronPrimary else BloodRed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = themeColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPhotoAlbum) "फोटो ॲल्बम शीर्षक व माहिती बदला" else "व्हिडिओ ॲल्बम शीर्षक व माहिती बदला",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ॲल्बमचे शीर्षक / नाव *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("वर्गवारी (Category):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("इतर वर्गवारी टाईप करा") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("कव्हर छायाचित्र (Cover Photo):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                GalleryImagePicker(
                    selectedImageUrl = coverImage,
                    onImageSelected = { coverImage = it },
                    label = "गॅलरीतून नवीन कव्हर फोटो निवडा",
                    helperText = "ॲल्बमचे कव्हर छायाचित्र बदला",
                    height = 120.dp
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("ॲल्बम माहिती / वर्णन") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, category, coverImage, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("जतन करा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

// DIALOG: ADD PHOTO
@Composable
fun AddPhotoDialog(
    onDismiss: () -> Unit,
    onAdd: (url: String, caption: String) -> Unit
) {
    var photoUrl by remember { mutableStateOf<String?>("https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80") }
    var caption by remember { mutableStateOf("") }

    val presetPhotos = listOf(
        Pair("श्री गणेश उत्सव", "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80"),
        Pair("क्रीडा व स्पर्धा", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&auto=format&fit=crop&q=80"),
        Pair("सांस्कृतिक कार्यक्रम", "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?w=800&auto=format&fit=crop&q=80"),
        Pair("मंडळ प्रांगण", "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=800&auto=format&fit=crop&q=80")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = SaffronPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("गॅलरीतून फोटो जोडा", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    text = "मोबाईल गॅलरीतून फोटो निवडा:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                GalleryImagePicker(
                    selectedImageUrl = photoUrl,
                    onImageSelected = { photoUrl = it },
                    label = "गॅलरीतून फोटो निवडा",
                    helperText = "मोबाईल गॅलरी उघडण्यासाठी येथे क्लिक करा",
                    height = 160.dp
                )

                Text(
                    text = "किंवा नमुना फोटो निवडा:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(presetPhotos) { (name, url) ->
                        FilterChip(
                            selected = photoUrl == url,
                            onClick = { photoUrl = url },
                            label = { Text(name, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("फोटोचे शीर्षक / नाव (Title / Caption)") },
                    placeholder = { Text("उदा. महाआरती सोहळा, बक्षीस वितरण") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = photoUrl
                    if (!url.isNullOrBlank()) {
                        onAdd(url, caption)
                    }
                },
                enabled = !photoUrl.isNullOrBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("फोटो जोडा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

// DIALOG: EDIT PHOTO CAPTION / TITLE (Admin)
@Composable
fun EditPhotoCaptionDialog(
    photo: GalleryPhoto,
    onDismiss: () -> Unit,
    onSave: (caption: String) -> Unit
) {
    var caption by remember { mutableStateOf(photo.caption) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SaffronPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("फोटोचे शीर्षक / नाव बदला", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWarm)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                ) {
                    UniversalAsyncImage(
                        model = photo.imageUrl,
                        contentDescription = "Photo Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("फोटोचे शीर्षक / नाव") },
                    placeholder = { Text("उदा. महाआरती सोहळा, बक्षीस वितरण, इत्यादी") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(caption) },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("जतन करा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

// DIALOG: ADD VIDEO (Into Album)
@Composable
fun AddVideoDialog(
    albumTitle: String = "",
    defaultCategory: String = "गणेशोत्सव",
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, category: String, videoUrl: String, thumbUrl: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var videoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }
    var thumbUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80") }

    val categories = listOf("गणेशोत्सव", "शिवजयंती", "क्रीडा स्पर्धा", "सामाजिक उपक्रम", "आरोग्य शिबिर", "इतर")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("नवीन व्हिडिओ जोडा", fontWeight = FontWeight.Bold, color = TextPrimary)
                if (albumTitle.isNotBlank()) {
                    Text("ॲल्बम: $albumTitle", style = MaterialTheme.typography.bodySmall, color = BloodRed)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("व्हिडिओ फाईलचे नाव / शीर्षक *") },
                    placeholder = { Text("उदा. मिरवणूक सोहळा व ढोल पथक") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("वर्गवारी (Category):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("वर्गवारी / उत्सव मॅन्युअली टाईप करा") },
                    placeholder = { Text("उदा. गणेश उत्सव, विसर्जन मिरवणूक") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("माहिती / वर्णन") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("व्हिडिओ लिंक / URL *") },
                    placeholder = { Text("उदा. YouTube किंवा MP4 व्हिडिओ लिंक") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("व्हिडिओ थंबनेल फोटो (गॅलरीतून निवडा):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                GalleryImagePicker(
                    selectedImageUrl = thumbUrl,
                    onImageSelected = { thumbUrl = it },
                    label = "गॅलरीतून थंबनेल निवडा",
                    helperText = "व्हिडिओचे कव्हर छायाचित्र निवडा",
                    height = 110.dp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && videoUrl.isNotBlank()) onAdd(title, desc, category, videoUrl, thumbUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
            ) {
                Text("व्हिडिओ जोडा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

// DIALOG: EDIT VIDEO (TITLE/NAME, DESC, CATEGORY, THUMBNAIL)
@Composable
fun EditVideoDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, category: String, thumbUrl: String) -> Unit
) {
    var title by remember { mutableStateOf(video.title) }
    var desc by remember { mutableStateOf(video.description) }
    var category by remember { mutableStateOf(video.category) }
    var thumbUrl by remember { mutableStateOf(video.thumbnailUrl) }

    val categories = listOf("गणेशोत्सव", "शिवजयंती", "क्रीडा स्पर्धा", "सामाजिक उपक्रम", "आरोग्य शिबिर", "इतर")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = BloodRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("व्हिडिओचे नाव व माहिती बदला", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("व्हिडिओ फाईलचे नाव / शीर्षक *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("वर्गवारी (Category):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("वर्गवारी / उत्सव मॅन्युअली टाईप करा") },
                    placeholder = { Text("उदा. गणेश उत्सव, विसर्जन मिरवणूक") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("माहिती / वर्णन") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("व्हिडिओ थंबनेल फोटो (गॅलरीतून निवडा):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                GalleryImagePicker(
                    selectedImageUrl = thumbUrl,
                    onImageSelected = { thumbUrl = it },
                    label = "गॅलरीतून थंबनेल निवडा",
                    helperText = "व्हिडिओचे कव्हर छायाचित्र निवडा",
                    height = 110.dp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, desc, category, thumbUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("जतन करा", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करा") }
        }
    )
}

