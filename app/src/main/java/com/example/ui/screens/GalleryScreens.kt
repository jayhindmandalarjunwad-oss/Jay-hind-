package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val photos by viewModel.albumPhotos.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val fullscreenViewerState by viewModel.fullscreenViewerState.collectAsStateWithLifecycle()

    val isAdmin = currentUser?.isAnyAdmin == true

    var selectedTab by remember { mutableStateOf(GalleryTab.PHOTOS) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var activePlayingVideo by remember { mutableStateOf<VideoItem?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = if (selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()) {
                    selectedAlbum!!.title
                } else {
                    "मंडळ गॅलरी (Gallery)"
                },
                subtitle = if (selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()) {
                    "${photos.size} फोटो • ${selectedAlbum!!.category}"
                } else {
                    "छायाचित्रे, ॲल्बम्स व व्हिडिओ संग्रह"
                },
                logoUrl = mandalLogoUrl,
                showBackButton = true,
                onBackClick = {
                    if (selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()) {
                        viewModel.openAlbum(Album("", "", "", "", "", 0, 0))
                    } else {
                        onBack()
                    }
                },
                actions = {
                    if (isAdmin) {
                        if (selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()) {
                            IconButton(
                                onClick = { showAddPhotoDialog = true },
                                modifier = Modifier.testTag("admin_add_photo_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "फोटो जोडा", tint = SaffronPrimary)
                            }
                        } else if (selectedTab == GalleryTab.PHOTOS) {
                            IconButton(
                                onClick = { showCreateAlbumDialog = true },
                                modifier = Modifier.testTag("admin_create_album_button")
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "नवीन ॲल्बम", tint = SaffronPrimary)
                            }
                        } else {
                            IconButton(
                                onClick = { showAddVideoDialog = true },
                                modifier = Modifier.testTag("admin_add_video_button")
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = "व्हिडिओ जोडा", tint = SaffronPrimary)
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
            // When an album is NOT selected, show Tab Bar (Photos / Videos)
            if (selectedAlbum == null || selectedAlbum!!.id.isEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = SurfaceWarm,
                    contentColor = SaffronPrimary,
                    modifier = Modifier.border(1.dp, CardBorderColor)
                ) {
                    Tab(
                        selected = selectedTab == GalleryTab.PHOTOS,
                        onClick = { selectedTab = GalleryTab.PHOTOS },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        text = {
                            Text(
                                text = "फोटो व ॲल्बम्स (${albums.size})",
                                fontWeight = if (selectedTab == GalleryTab.PHOTOS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == GalleryTab.VIDEOS,
                        onClick = { selectedTab = GalleryTab.VIDEOS },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        text = {
                            Text(
                                text = "व्हिडिओ संग्रह (${videos.size})",
                                fontWeight = if (selectedTab == GalleryTab.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (selectedAlbum != null && selectedAlbum!!.id.isNotEmpty()) {
                    // Inside Selected Album - View Photos
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Album Header Bar with Back to Albums & Info
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
                                        .clickable { viewModel.openAlbum(Album("", "", "", "", "", 0, 0)) }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "सर्व ॲल्बम्स",
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "सर्व ॲल्बम्स",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SaffronPrimary
                                    )
                                }

                                if (isAdmin) {
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

                        if (photos.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.PhotoLibrary,
                                title = "या ॲल्बममध्ये फोटो उपलब्ध नाहीत",
                                subtitle = if (isAdmin) "नवीन फोटो जोडण्यासाठी 'फोटो जोडा' बटण वापरा." else "लवकरच फोटो जोडले जातील."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.openFullscreenPhotos(
                                                    photos = photos.map { it.imageUrl },
                                                    titles = photos.map { it.caption },
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

                                        // Bottom gradient & Caption overlay if caption/title is present
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
                                                // Edit Title / Caption button
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

                                                // Delete Photo button
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB CONTENTS
                    when (selectedTab) {
                        GalleryTab.PHOTOS -> {
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
                                                    text = "ॲडमिन: ॲल्बम व्यवस्थापन",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                            }
                                            Button(
                                                onClick = { showCreateAlbumDialog = true },
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

                                if (albums.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Default.PhotoLibrary,
                                        title = "कोणतेही ॲल्बम नाहीत",
                                        subtitle = if (isAdmin) "नवीन ॲल्बम तयार करा." else "मंडळाचे ॲल्बम लवकरच उपलब्ध होतील."
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        contentPadding = PaddingValues(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(albums, key = { it.id }) { album ->
                                            AlbumCard(
                                                album = album,
                                                isAdmin = isAdmin,
                                                onClick = { viewModel.openAlbum(album) },
                                                onDelete = { viewModel.deleteAlbum(album.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        GalleryTab.VIDEOS -> {
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
                                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BloodRed)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "ॲडमिन: व्हिडिओ व्यवस्थापन",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = TextPrimary
                                                )
                                            }
                                            Button(
                                                onClick = { showAddVideoDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("व्हिडिओ जोडा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (videos.isEmpty()) {
                                    EmptyStateView(
                                        icon = Icons.Default.VideoLibrary,
                                        title = "कोणतेही व्हिडिओ उपलब्ध नाहीत",
                                        subtitle = if (isAdmin) "नवीन व्हिडिओ जोडण्यासाठी बटण वापरा." else "लवकरच व्हिडिओ उपलब्ध होतील."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                                    ) {
                                        items(videos, key = { it.id }) { video ->
                                            VideoCard(
                                                video = video,
                                                isAdmin = isAdmin,
                                                onClick = {
                                                    activePlayingVideo = video
                                                },
                                                onDelete = { viewModel.deleteVideo(video.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FULLSCREEN PHOTO VIEWER DIALOG with Swipe & Next/Prev Controls (Admin has download & share access, members view only)
            if (fullscreenViewerState != null && fullscreenViewerState!!.photos.isNotEmpty()) {
                FullscreenPhotoDialog(
                    photos = fullscreenViewerState!!.photos,
                    titles = fullscreenViewerState!!.titles,
                    initialIndex = fullscreenViewerState!!.initialIndex,
                    isAdmin = isAdmin,
                    onDismiss = { viewModel.closeFullscreenPhoto() }
                )
            }

            // CREATE ALBUM DIALOG (Admin)
            if (showCreateAlbumDialog) {
                CreateAlbumDialog(
                    onDismiss = { showCreateAlbumDialog = false },
                    onCreate = { title, cat, cover, desc ->
                        viewModel.createAlbum(title, cat, cover, desc)
                        showCreateAlbumDialog = false
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
                    onDismiss = { showAddVideoDialog = false },
                    onAdd = { title, desc, cat, videoUrl, thumbUrl ->
                        viewModel.addVideo(title, desc, cat, videoUrl, thumbUrl)
                        showAddVideoDialog = false
                    }
                )
            }

            // IN-APP VIDEO PLAYER DIALOG (YouTube Embed & Cloud Video Player with Fullscreen Landscape)
            if (activePlayingVideo != null) {
                VideoPlayerDialog(
                    videoUrl = activePlayingVideo!!.videoUrl,
                    title = activePlayingVideo!!.title,
                    senderName = "जय हिंद तरुण मंडळ • ${activePlayingVideo!!.category}",
                    thumbnailUrl = activePlayingVideo!!.thumbnailUrl,
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
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("album_card_${album.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                UniversalAsyncImage(
                    model = album.coverImageUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SaffronPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = album.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${album.photoCount} फोटो",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
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

@Composable
fun VideoCard(
    video: VideoItem,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                // Play Icon Overlay
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BloodRed.copy(alpha = 0.9f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
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
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "हटवा", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceVariantWarm
                    ) {
                        Text(
                            text = video.category,
                            color = NavySecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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
            }
        }
    }
}

// DIALOG: CREATE ALBUM
@Composable
fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String, coverImage: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("गणेशोत्सव") }
    var coverImage by remember { mutableStateOf("https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80") }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("गणेशोत्सव", "शिवजयंती", "क्रीडा स्पर्धा", "सामाजिक उपक्रम", "आरोग्य शिबिर", "इतर")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नवीन ॲल्बम तयार करा", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ॲल्बम शीर्षक / नाव *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("वर्गवारी (Category):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
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
                    helperText = "मोबाईल गॅलरीतून ॲल्बमसाठी मुख्य फोटो अपलोड करा",
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
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text("तयार करा", fontWeight = FontWeight.Bold)
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

                // GALLERY PICKER COMPONENT
                GalleryImagePicker(
                    selectedImageUrl = photoUrl,
                    onImageSelected = { photoUrl = it },
                    label = "गॅलरीतून फोटो निवडा",
                    helperText = "मोबाईल गॅलरी उघडण्यासाठी येथे क्लिक करा",
                    height = 160.dp
                )

                // Quick Presets Option
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

// DIALOG: ADD VIDEO
@Composable
fun AddVideoDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, category: String, videoUrl: String, thumbUrl: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("गणेशोत्सव") }
    var videoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }
    var thumbUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नवीन व्हिडिओ जोडा", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("व्हिडिओ शीर्षक *") },
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
