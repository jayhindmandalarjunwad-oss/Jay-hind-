package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.MediaUtils
import kotlinx.coroutines.launch

enum class AdminTab(val title: String) {
    PENDING_APPROVALS("सभासद मंजुरी"),
    LIVE_STREAM("🔴 थेट प्रक्षेपण (Live)"),
    MANAGE_BANNERS("ग्रुप बॅनर"),
    EDIT_ABOUT_US("आमच्याबद्दल व सोशल"),
    MEMBERS_LIST("सर्व सभासद व ॲडमिन"),
    MANAGE_LOGO("लोगो व स्वाक्षरी"),
    CREATE_EVENT("नवीन कार्यक्रम"),
    CREATE_ANNOUNCEMENT("सूचना / Broadcast"),
    MANAGE_GALLERY("फोटो व व्हिडिओ"),
    POSTS_MODERATION("पोस्ट्स नियंत्रण")
}

@Composable
fun AdminPanelScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.PENDING_APPROVALS) }

    val pendingMembers by viewModel.pendingMembers.collectAsStateWithLifecycle()
    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val mandalInfo by viewModel.mandalInfo.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshAllData(silent = true)
    }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = "मंडळ ॲडमिन पॅनेल (Admin Panel)",
                subtitle = "सर्व व्यवस्थापन व मंजुरी प्रणाली",
                logoUrl = mandalLogoUrl,
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAllData(silent = false) },
                        modifier = Modifier.testTag("admin_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "रिफ्रेश / Sync",
                            tint = SaffronPrimary
                        )
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
                .testTag("admin_panel_root")
        ) {
            // Scrollable Admin Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SaffronPrimary,
                edgePadding = 12.dp
            ) {
                AdminTab.values().forEach { tab ->
                    val badgeCount = if (tab == AdminTab.PENDING_APPROVALS) pendingMembers.size else 0
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = BloodRed,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$badgeCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                AdminTab.PENDING_APPROVALS -> PendingApprovalsTab(pendingMembers, viewModel)
                AdminTab.LIVE_STREAM -> LiveStreamAdminTab(mandalInfo, viewModel)
                AdminTab.MANAGE_BANNERS -> ManageBannersAdminTab(banners, viewModel)
                AdminTab.EDIT_ABOUT_US -> EditAboutUsAdminTab(mandalInfo, viewModel)
                AdminTab.MEMBERS_LIST -> AllMembersAdminTab(allMembers, viewModel)
                AdminTab.MANAGE_LOGO -> ManageLogoAdminTab(mandalInfo, mandalLogoUrl, viewModel)
                AdminTab.CREATE_EVENT -> CreateEventAdminTab(events, viewModel)
                AdminTab.CREATE_ANNOUNCEMENT -> CreateAnnouncementAdminTab(viewModel)
                AdminTab.MANAGE_GALLERY -> ManageGalleryAdminTab(albums, videos, viewModel)
                AdminTab.POSTS_MODERATION -> PostsModerationAdminTab(posts, viewModel)
            }
        }
    }
}

// 1. PENDING APPROVALS
@Composable
fun PendingApprovalsTab(pendingList: List<User>, viewModel: MandalViewModel) {
    if (pendingList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.CheckCircle,
            title = "कोणतीही प्रलंबित मंजुरी नाही",
            subtitle = "सर्व नवीन नोंदणीकृत सभासदांची मंजुरी पूर्ण झाली आहे."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ खालील नवीन सभासदांनी नोंदणी केली आहे. मंडळाच्या नियमांनुसार पडताळणी करून मंजुरी द्या.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5A3E00),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            itemsIndexed(pendingList, key = { index, user -> "${user.id}_$index" }) { _, user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PendingOrange.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MemberAvatar(photoUrl = user.profilePhotoUrl, name = user.fullName, size = 52)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "मोबाईल: ${user.mobileNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "रक्तगट: ${user.bloodGroup} • जन्मतारीख: ${user.dateOfBirth}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SaffronPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val safePass = (user.password as String?).orEmpty()
                                if (safePass.isNotBlank()) {
                                    Text(
                                        text = "🔑 पासवर्ड: $safePass",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val safeAddr = (user.address as String?).orEmpty()
                        if (safeAddr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "पत्ता: $safeAddr",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.approveMember(user.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("मंजूर करा (Approve)")
                            }

                            OutlinedButton(
                                onClick = { viewModel.rejectMember(user.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("नामंजूर (Reject)")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. MANAGE GROUP BANNERS (ADD, EDIT, DELETE)
@Composable
fun ManageBannersAdminTab(banners: List<MandalBanner>, viewModel: MandalViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<MandalBanner?>(null) }
    var bannerToDelete by remember { mutableStateOf<MandalBanner?>(null) }

    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }

    // Dialog for Add / Edit Banner
    if (showAddDialog || editingBanner != null) {
        val isEditing = editingBanner != null
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingBanner = null
            },
            title = {
                Text(
                    text = if (isEditing) "मंडळ बॅनर संपादित करा" else "नवीन मंडळ बॅनर जोडा",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "बॅनर फोटो निवडा (Image Selection):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    GalleryImagePicker(
                        selectedImageUrl = bannerImageUrl,
                        onImageSelected = { bannerImageUrl = it },
                        label = "गॅलरीतून बॅनर फोटो निवडा",
                        helperText = "मोबाईलमधून किंवा कॅमेऱ्याने बॅनर इमेज निवडा",
                        height = 130.dp
                    )

                    // Real-time Live Preview inside Dialog
                    if (bannerImageUrl.isNotBlank()) {
                        Text(
                            text = "थेट ॲप प्रिव्ह्यू (Live App Preview):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SaffronPrimary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                UniversalAsyncImage(
                                    model = bannerImageUrl,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    SaffronDark.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "🚩 मंडळ बॅनर प्रिव्ह्यू",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (bannerTitle.isNotBlank()) bannerTitle else "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (bannerSubtitle.isNotBlank()) bannerSubtitle else "अर्जुनवाड • एकता, संस्कृती आणि सामाजिक विकास",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bannerTitle,
                        onValueChange = { bannerTitle = it },
                        label = { Text("बॅनर शीर्षक (Title)") },
                        placeholder = { Text("उदा. जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bannerSubtitle,
                        onValueChange = { bannerSubtitle = it },
                        label = { Text("उपशीर्षक / घोषणा (Subtitle)") },
                        placeholder = { Text("उदा. अर्जुनवाड • एकता, संस्कृती आणि सामाजिक विकास") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bannerImageUrl.isNotBlank()) {
                            if (isEditing) {
                                viewModel.updateBanner(
                                    id = editingBanner!!.id,
                                    imageUrl = bannerImageUrl,
                                    title = bannerTitle,
                                    subtitle = bannerSubtitle
                                )
                            } else {
                                viewModel.addBanner(
                                    imageUrl = bannerImageUrl,
                                    title = bannerTitle,
                                    subtitle = bannerSubtitle
                                )
                            }
                            showAddDialog = false
                            editingBanner = null
                            bannerImageUrl = ""
                            bannerTitle = ""
                            bannerSubtitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isEditing) "अद्यतनित करा (Update)" else "बॅनर जोडा (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingBanner = null
                }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (bannerToDelete != null) {
        AlertDialog(
            onDismissRequest = { bannerToDelete = null },
            title = { Text("बॅनर हटवा (Delete Banner)", fontWeight = FontWeight.Bold) },
            text = { Text("हा बॅनर मुख्य स्क्रीनवरून हटवायचा आहे का?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBanner(bannerToDelete!!.id)
                        bannerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("हटवा (Delete)")
                }
            },
            dismissButton = {
                TextButton(onClick = { bannerToDelete = null }) { Text("रद्द करा") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Add Button
        item {
            Button(
                onClick = {
                    bannerImageUrl = "https://images.unsplash.com/photo-1532375810709-75b1da00537c?w=800&auto=format&fit=crop&q=80"
                    bannerTitle = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ"
                    bannerSubtitle = "अर्जुनवाड • एकता, संस्कृती आणि सामाजिक विकास"
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("नवीन बॅनर जोडा (+ Add New Banner)", fontWeight = FontWeight.Bold)
            }
        }

        // Live Real-Time Banner Preview for Admin
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👀 सभासदांना दिसणारा थेट बॅनर देखावा (Live App Preview)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val activeBanner = banners.firstOrNull()
                    val previewImage = activeBanner?.imageUrl ?: "https://images.unsplash.com/photo-1532375810709-75b1da00537c?w=800&auto=format&fit=crop&q=80"
                    val previewTitle = activeBanner?.title?.ifEmpty { null } ?: "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ"
                    val previewSubtitle = activeBanner?.subtitle?.ifEmpty { null } ?: "अर्जुनवाड • एकता, संस्कृती आणि सामाजिक विकास"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            UniversalAsyncImage(
                                model = previewImage,
                                contentDescription = "Active Banner Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "सध्याचे सक्रिय बॅनर (${banners.size}):",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
        }

        if (banners.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm)
                ) {
                    Text(
                        text = "सध्या कोणताही कस्टम बॅनर जोडलेला नाही. डीफॉल्ट बॅनर प्रदर्शित होत आहे.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            itemsIndexed(banners, key = { index, banner -> "${banner.id}_$index" }) { _, banner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            UniversalAsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.padding(14.dp)) {
                            if (banner.title.isNotBlank()) {
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            if (banner.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = banner.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editingBanner = banner
                                        bannerImageUrl = banner.imageUrl
                                        bannerTitle = banner.title
                                        bannerSubtitle = banner.subtitle
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("संपादित करा (Edit)")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { bannerToDelete = banner },
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("हटवा (Delete)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. EDIT "ABOUT US" & SOCIAL HANDLES
@Composable
fun EditAboutUsAdminTab(mandalInfo: MandalInfo, viewModel: MandalViewModel) {
    var mandalName by remember(mandalInfo) { mutableStateOf(mandalInfo.mandalName) }
    var tagline by remember(mandalInfo) { mutableStateOf(mandalInfo.tagline) }
    var locationTitle by remember(mandalInfo) { mutableStateOf(mandalInfo.locationTitle) }
    var aboutDescription by remember(mandalInfo) { mutableStateOf(mandalInfo.aboutDescription) }
    var youtubeHandle by remember(mandalInfo) { mutableStateOf(mandalInfo.youtubeHandle) }
    var facebookHandle by remember(mandalInfo) { mutableStateOf(mandalInfo.facebookHandle) }
    var instagramHandle by remember(mandalInfo) { mutableStateOf(mandalInfo.instagramHandle) }
    var adminWebLink by remember(mandalInfo) { mutableStateOf(mandalInfo.adminWebLink) }
    var phone by remember(mandalInfo) { mutableStateOf(mandalInfo.phone) }
    var email by remember(mandalInfo) { mutableStateOf(mandalInfo.email) }
    var address by remember(mandalInfo) { mutableStateOf(mandalInfo.address) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏛️ मंडळाची प्राथमिक माहिती (General Info)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = mandalName,
                    onValueChange = { mandalName = it },
                    label = { Text("मंडळाचे नाव (Mandal Name)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("टॅगलाईन (Tagline)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = locationTitle,
                    onValueChange = { locationTitle = it },
                    label = { Text("स्थान लेबल (Location Badge)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aboutDescription,
                    onValueChange = { aboutDescription = it },
                    label = { Text("आमच्याबद्दल सविस्तर माहिती (About Us Description)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            }
        }

        // Social Media Handles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🌐 अधिकृत सोशल मीडिया हँडल्स (Social Media Handles)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = youtubeHandle,
                    onValueChange = { youtubeHandle = it },
                    label = { Text("YouTube Channel Handle / URL") },
                    placeholder = { Text("उदा. @JayHindMandalArjunwad किंवा लिंक") },
                    leadingIcon = {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color(0xFFFF0000))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = facebookHandle,
                    onValueChange = { facebookHandle = it },
                    label = { Text("Facebook Page Handle / URL") },
                    placeholder = { Text("उदा. JayHindMandalArjunwad किंवा लिंक") },
                    leadingIcon = {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF1877F2))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = instagramHandle,
                    onValueChange = { instagramHandle = it },
                    label = { Text("Instagram Page Handle / URL") },
                    placeholder = { Text("उदा. @jayhind_mandal_arjunwad किंवा लिंक") },
                    leadingIcon = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFE1306C))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminWebLink,
                    onValueChange = { adminWebLink = it },
                    label = { Text("ॲडमिन वेब पोर्टल लिंक (Admin Portal Link)") },
                    placeholder = { Text("https://admin.jayhindmandal.org (नंतर ॲड करू शकता)") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF0D9488))
                    },
                    supportingText = { Text("तुम्ही ही लिंक ॲप पूर्ण तयार झाल्यानंतर कधीही जोडू शकता.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Contact Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📞 संपर्क माहिती (Contact Details)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("संपर्क मोबाईल (Contact Phone)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SuccessGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("अधिकृत ईमेल (Email Address)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SaffronPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("मंडळ पत्ता (Address)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = NavySecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }

        Button(
            onClick = {
                viewModel.updateMandalInfo(
                    mandalName = mandalName,
                    tagline = tagline,
                    locationTitle = locationTitle,
                    aboutDescription = aboutDescription,
                    email = email,
                    address = address,
                    phone = phone,
                    youtubeHandle = youtubeHandle,
                    facebookHandle = facebookHandle,
                    instagramHandle = instagramHandle,
                    adminWebLink = adminWebLink
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("माहिती व सोशल लिंक्स सेव्ह करा (Save Changes)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// 4. ALL MEMBERS & ADMIN ROLE / DESIGNATION MANAGEMENT
@Composable
fun AllMembersAdminTab(members: List<User>, viewModel: MandalViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var memberToManage by remember { mutableStateOf<User?>(null) }
    var memberToEditDesignation by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(searchQuery) ||
                it.designation.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Dialog for Delete & Transfer
    if (memberToManage != null) {
        val target = memberToManage!!
        val targetName = (target.fullName as String?).orEmpty().ifBlank { "सभासद" }
        val targetMobile = (target.mobileNumber as String?).orEmpty()
        val targetBlood = (target.bloodGroup as String?).orEmpty().ifBlank { "O+" }
        AlertDialog(
            onDismissRequest = { memberToManage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            title = {
                Text(
                    text = "$targetName - सभासद व्यवस्थापन",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "मोबाईल: $targetMobile • रक्तगट: $targetBlood",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "या सभासदाला मंडळातून काढून टाकायचे आहे का?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "हक्क हस्तांतरण (Rights Transfer): या सभासदाची सर्व जबाबदारी, पोस्ट्स व अधिकार मुख्य ॲडमिन (वैभव चौगुले - 9545791089) यांच्याकडे सुरक्षितपणे वर्ग (Transfer) केले जातील.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5A3E00),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMemberAndTransferRights(target.id)
                        memberToManage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("हक्क ट्रान्सफर करून हटवा")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToManage = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // Comprehensive Dialog for Changing Member Designation (पद) and System Role (अधिकार)
    if (memberToEditDesignation != null) {
        val target = memberToEditDesignation!!
        EditMemberDesignationDialog(
            member = target,
            onDismiss = { memberToEditDesignation = null },
            onSave = { newDesignation, newRole ->
                viewModel.updateMemberDesignationAndRole(target.id, newDesignation, newRole)
                memberToEditDesignation = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEDE9FE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🎖️ सभासद पद व ॲडमिन व्यवस्थापन: सदस्यांचे पद (उदा. कार्यकारणी सदस्य, अध्यक्ष, उपाध्यक्ष, सचिव, सल्लागार इ.) व ॲडमिन अधिकार कधीही बदलू शकता.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4C1D95),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("नाव, पद किंवा मोबाईल नंबरने शोधा...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SaffronPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "एकूण नोंदणीकृत सभासद: ${filteredMembers.size} / ${members.size}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
        }

        itemsIndexed(filteredMembers, key = { index, member -> "${member.id}_$index" }) { _, member ->
            var isPasswordVisible by remember { mutableStateOf(false) }
            val memberDesig = (member.designation as String?).orEmpty().trim()
            val memberPass = (member.password as String?).orEmpty()
            val memberName = (member.fullName as String?).orEmpty().ifBlank { "सभासद" }
            val memberMobile = (member.mobileNumber as String?).orEmpty()
            val memberPhoto = (member.profilePhotoUrl as String?).orEmpty()
            val memberBlood = (member.bloodGroup as String?).orEmpty().ifBlank { "O+" }
            val memberDob = (member.dateOfBirth as String?).orEmpty()
            val memberStatus = (member.status as String?).orEmpty().ifBlank { "APPROVED" }

            val currentDesig = if (memberDesig.isNotBlank()) {
                memberDesig
            } else if (member.isAdmin) {
                "कार्यकारणी सदस्य"
            } else {
                "सभासद"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MemberAvatar(photoUrl = memberPhoto, name = memberName, size = 48)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = memberName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (member.isAdmin) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDB2777).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "👑 ॲडमिन",
                                            color = Color(0xFFDB2777),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Prominent Designation Badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SaffronContainer
                            ) {
                                Text(
                                    text = "🎖️ $currentDesig",
                                    color = SaffronDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = "📱 मोबाईल: $memberMobile", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = memberStatus)
                                Spacer(modifier = Modifier.width(6.dp))
                                BloodGroupBadge(bloodGroup = memberBlood)
                                if (memberDob.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🎂 $memberDob",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PASSWORD & CREDENTIALS INFO CARD (FOR ADMIN ASSISTANCE)
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "पासवर्ड",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "लॉगिन पासवर्ड (Password):",
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isPasswordVisible) {
                                            memberPass.ifBlank { "उपलब्ध नाही" }
                                        } else {
                                            if (memberPass.isNotBlank()) {
                                                "•••••••• (${memberPass.length} अक्षरे)"
                                            } else {
                                                "•••••••• (सेट केलेला नाही)"
                                            }
                                        },
                                        fontSize = 13.sp,
                                        color = Color(0xFF78350F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "पासवर्ड दाखवा",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Password", memberPass)
                                        clipboard?.setPrimaryClip(clip)
                                        viewModel.showSnackbar("$memberName यांचा पासवर्ड कॉपी केला! 📋")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "कॉपी",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Change Role & Designation Button
                        Button(
                            onClick = { memberToEditDesignation = member },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SaffronPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎖️ पद / अधिकार बदला",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!member.isAdmin) {
                                if (memberStatus == "BLOCKED") {
                                    OutlinedButton(
                                        onClick = { viewModel.unblockMember(member.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Unblock", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("अनब्लॉक करा", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    IconButton(onClick = { viewModel.blockMember(member.id) }) {
                                        Icon(Icons.Default.Block, contentDescription = "Block", tint = PendingOrange)
                                    }
                                }
                            }
                            IconButton(onClick = { memberToManage = member }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BloodRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

// DIALOG FOR CHANGING MEMBER DESIGNATION AND SYSTEM ROLE
@Composable
fun EditMemberDesignationDialog(
    member: User,
    onDismiss: () -> Unit,
    onSave: (newDesignation: String, newRole: String) -> Unit
) {
    val safeDesignation = (member.designation as String?).orEmpty().trim()
    val safeRole = (member.role as String?).orEmpty().trim().ifBlank { "MEMBER" }
    val safeName = (member.fullName as String?).orEmpty().ifBlank { "सभासद" }
    val safeMobile = (member.mobileNumber as String?).orEmpty()
    val safePhoto = (member.profilePhotoUrl as String?).orEmpty()

    val initialDesignation = if (safeDesignation.isNotBlank()) {
        safeDesignation
    } else if (member.isAdmin) {
        "कार्यकारणी सदस्य"
    } else {
        "सभासद"
    }

    val presetDesignations = listOf(
        "कार्यकारणी सदस्य",
        "अध्यक्ष",
        "उपाध्यक्ष",
        "सचिव",
        "सहसचिव",
        "खजिनदार",
        "सहखजिनदार",
        "प्रसिद्धी प्रमुख",
        "सांस्कृतिक प्रमुख",
        "क्रीडा प्रमुख",
        "सल्लागार",
        "मार्गदर्शक",
        "संस्थापक सदस्य",
        "आजीवन सभासद",
        "सभासद"
    )

    var selectedDesignation by remember(member.id) { mutableStateOf(initialDesignation) }
    var customDesignationInput by remember(member.id) {
        mutableStateOf(if (initialDesignation !in presetDesignations) initialDesignation else "")
    }
    var selectedRole by remember(member.id) { mutableStateOf(safeRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = SaffronPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "पद व अधिकार व्यवस्थापन",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Member Header Card
                Surface(
                    color = SurfaceWarm,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemberAvatar(photoUrl = safePhoto, name = safeName, size = 48)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(safeName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("📱 $safeMobile", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SaffronContainer
                            ) {
                                Text(
                                    text = "सध्याचे पद: ${safeDesignation.ifBlank { if (member.isAdmin) "कार्यकारणी सदस्य" else "सभासद" }}",
                                    fontSize = 10.sp,
                                    color = SaffronDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "🎖️ नवीन पद निवडा (Select Designation):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                // Quick Preset Chips (Safe chunked rows without FlowRow ABI issue)
                val presetChunks = remember { presetDesignations.chunked(3) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { desig ->
                                val isSelected = (selectedDesignation == desig && customDesignationInput.isBlank())
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SaffronPrimary else SurfaceWarm,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) SaffronDark else CardBorderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDesignation = desig
                                            customDesignationInput = ""
                                        }
                                ) {
                                    Text(
                                        text = desig,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp)
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Custom Designation Input Field
                OutlinedTextField(
                    value = customDesignationInput,
                    onValueChange = {
                        customDesignationInput = it
                        if (it.isNotBlank()) {
                            selectedDesignation = it.trim()
                        }
                    },
                    label = { Text("किंवा इतर कोणतेही कस्टम पद लिहा") },
                    placeholder = { Text("उदा. प्रसिद्धी प्रमुख / महिला समन्वयक") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = DividerColor)

                // Admin Rights Selection
                Text(
                    text = "👑 ॲपमधील सिस्टम अधिकार (System Role):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedRole == "ADMIN") SaffronPrimary.copy(alpha = 0.15f) else SurfaceWarm,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedRole == "ADMIN") SaffronPrimary else CardBorderColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = "ADMIN" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👑 मुख्य ॲडमिन",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selectedRole == "ADMIN") SaffronPrimary else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("सर्व अधिकार व मंजुरी", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedRole != "ADMIN") Color(0xFFE2E8F0) else SurfaceWarm,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedRole != "ADMIN") Color(0xFF64748B) else CardBorderColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = "MEMBER" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👤 सामान्य सभासद",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selectedRole != "ADMIN") Color(0xFF1E293B) else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("फक्त पाहणे व पोस्ट", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDesignation = if (customDesignationInput.isNotBlank()) {
                        customDesignationInput.trim()
                    } else {
                        selectedDesignation.trim().ifBlank { "सभासद" }
                    }
                    onSave(finalDesignation, selectedRole)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("जतन करा (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा")
            }
        }
    )
}

// 5. CREATE EVENT
@Composable
fun CreateEventAdminTab(events: List<MandalEvent>, viewModel: MandalViewModel) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("15 सप्टेंबर 2026") }
    var time by remember { mutableStateOf("सकाळी ९:०० वा.") }
    var location by remember { mutableStateOf("मंडळ प्रांगण, अर्जुनवाड") }
    var desc by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "नवीन कार्यक्रम नोंदणी (Create Event)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("कार्यक्रमाचे नाव (Event Title)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MandalDatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "दिनांक (Date)",
                        placeholder = "तारीख निवडा",
                        isIsoFormat = false,
                        isDob = false,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("वेळ (Time)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("स्थळ (Location)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("तपशील (Description)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                GalleryImagePicker(
                    selectedImageUrl = imageUrl,
                    onImageSelected = { imageUrl = it },
                    label = "कार्यक्रमाचा बॅनर निवडा",
                    helperText = "गॅलरीतून फोटो अपलोड करा किंवा नमुना वापरा",
                    height = 120.dp
                )

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createEvent(
                                title = title,
                                date = date,
                                time = time,
                                location = location,
                                desc = desc,
                                img = imageUrl,
                                cat = "सांस्कृतिक"
                            )
                            title = ""
                            desc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("कार्यक्रम प्रसिद्ध करा (Publish Event)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 6. CREATE ANNOUNCEMENT & BROADCAST
@Composable
fun CreateAnnouncementAdminTab(viewModel: MandalViewModel) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("NORMAL") }
    var alsoBroadcast by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "अधिकृत सूचना प्रसारित करा (Notice Board & Broadcast)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("सूचनेचे शीर्षक (Title)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("सूचनेचा सविस्तर मजकूर (Content)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Text(text = "प्राधान्य (Priority):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NORMAL" to "सामान्य", "IMPORTANT" to "महत्त्वाची", "URGENT" to "तातडीची").forEach { (key, label) ->
                        FilterChip(
                            selected = priority == key,
                            onClick = { priority = key },
                            label = { Text(label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = alsoBroadcast, onCheckedChange = { alsoBroadcast = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("सर्व सभासदांना त्वरित नोटिफिकेशन पाठवा (Push Notification)", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.createAnnouncement(title, content, priority)
                            if (alsoBroadcast) {
                                viewModel.broadcastNotification(title, content)
                            }
                            title = ""
                            content = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("सूचना प्रसिद्ध करा", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 7. MANAGE GALLERY (PHOTOS & VIDEOS)
@Composable
fun ManageGalleryAdminTab(albums: List<Album>, videos: List<VideoItem>, viewModel: MandalViewModel) {
    var albumTitle by remember { mutableStateOf("") }
    var albumDesc by remember { mutableStateOf("") }
    var albumCover by remember { mutableStateOf("https://images.unsplash.com/photo-1544717305-2782549b5136?w=800&auto=format&fit=crop&q=80") }

    var videoTitle by remember { mutableStateOf("") }
    var videoYoutubeId by remember { mutableStateOf("") }
    var videoDesc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Add Photo Album
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "नवीन फोटो अल्बम जोडा (Add Photo Album)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    label = { Text("अल्बमचे नाव (Album Name)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = albumDesc,
                    onValueChange = { albumDesc = it },
                    label = { Text("वर्णन (Description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                GalleryImagePicker(
                    selectedImageUrl = albumCover,
                    onImageSelected = { albumCover = it },
                    label = "अल्बम कव्हर फोटो निवडा",
                    helperText = "गॅलरीतून कव्हर इमेज अपलोड करा",
                    height = 120.dp
                )

                Button(
                    onClick = {
                        if (albumTitle.isNotBlank()) {
                            viewModel.createAlbum(
                                title = albumTitle,
                                category = "सामान्य",
                                coverImage = albumCover,
                                desc = albumDesc
                            )
                            albumTitle = ""
                            albumDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("अल्बम तयार करा", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add Video
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "नवीन व्हिडिओ जोडा (Add Video)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("व्हिडिओ शीर्षक (Title)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = videoYoutubeId,
                    onValueChange = { videoYoutubeId = it },
                    label = { Text("YouTube Video ID किंवा URL") },
                    placeholder = { Text("उदा. dQw4w9WgXcQ किंवा https://youtube.com/...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = videoDesc,
                    onValueChange = { videoDesc = it },
                    label = { Text("वर्णन (Description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (videoTitle.isNotBlank() && videoYoutubeId.isNotBlank()) {
                            viewModel.addVideo(
                                title = videoTitle,
                                desc = videoDesc,
                                category = "सांस्कृतिक",
                                videoUrl = videoYoutubeId,
                                thumbUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=80"
                            )
                            videoTitle = ""
                            videoYoutubeId = ""
                            videoDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("व्हिडिओ जोडा (Add Video)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 8. POSTS MODERATION
@Composable
fun PostsModerationAdminTab(posts: List<Post>, viewModel: MandalViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "प्रसिद्ध केलेल्या पोस्ट्स (${posts.size}):",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
        }

        itemsIndexed(posts, key = { index, post -> "${post.id}_$index" }) { _, post ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = post.content, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "लाईक्स: ${post.likesCount} • कमेंट्स: ${post.commentsCount}", fontSize = 11.sp, color = TextMuted)
                    }

                    IconButton(onClick = { viewModel.deletePost(post.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BloodRed)
                    }
                }
            }
        }
    }
}

// 9. MANAGE LOGO & OFFICIAL SIGNATURE (Overhauled with Immediate Live Preview & App-wide simulation)
@Composable
fun ManageLogoAdminTab(
    mandalInfo: MandalInfo,
    currentLogoUrl: String?,
    viewModel: MandalViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- LOGO STATE ---
    var selectedLogoInput by remember(currentLogoUrl) { mutableStateOf(currentLogoUrl ?: "") }
    var isSavingLogo by remember { mutableStateOf(false) }
    var isResettingLogo by remember { mutableStateOf(false) }
    val hasPendingLogoChanges = selectedLogoInput.trim() != (currentLogoUrl ?: "").trim() && selectedLogoInput.isNotBlank()

    val presetLogos = listOf(
        "भगवा ध्वज मानचिन्ह" to "https://images.unsplash.com/photo-1544717305-2782549b5136?w=600&auto=format&fit=crop&q=80",
        "सुवर्ण कला मानचिन्ह" to "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80",
        "क्रीडा व सांस्कृतिक" to "https://images.unsplash.com/photo-1532375810709-75b1da00537c?w=600&auto=format&fit=crop&q=80"
    )

    // --- OFFICIAL STAMP STATE ---
    var selectedStampInput by remember(mandalInfo.officialStampUrl) { mutableStateOf(mandalInfo.officialStampUrl) }
    var isSavingStamp by remember { mutableStateOf(false) }
    var isProcessingStampPhoto by remember { mutableStateOf(false) }
    var isResettingStamp by remember { mutableStateOf(false) }
    val hasPendingStampChanges = selectedStampInput.trim() != mandalInfo.officialStampUrl.trim()

    val stampPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingStampPhoto = true
                val base64 = MediaUtils.uriToStampBase64(context, uri)
                isProcessingStampPhoto = false
                if (!base64.isNullOrBlank()) {
                    selectedStampInput = base64
                    viewModel.showSnackbar("मंडळ शिक्का फोटो यशस्वीरित्या प्रोसेस झाला! खाली 'अधिकृत शिक्का सेव्ह करा' दाबा. 🏛️")
                } else {
                    viewModel.showSnackbar("❌ शिक्का प्रोसेस करताना त्रुटी आली. कृपया पांढऱ्या कागदावरील शिक्क्याचा स्वच्छ फोटो निवडा.")
                }
            }
        }
    }

    // --- SIGNATURE STATE ---
    var selectedSignatureInput by remember(mandalInfo.presidentSignatureUrl) { mutableStateOf(mandalInfo.presidentSignatureUrl) }
    var presidentNameInput by remember(mandalInfo.presidentName) { mutableStateOf(mandalInfo.presidentName.ifBlank { "अध्यक्ष" }) }
    var isSavingSignature by remember { mutableStateOf(false) }
    var isProcessingSignaturePhoto by remember { mutableStateOf(false) }
    var isResettingSignature by remember { mutableStateOf(false) }
    val hasPendingSignatureChanges = selectedSignatureInput.trim() != mandalInfo.presidentSignatureUrl.trim() || presidentNameInput.trim() != mandalInfo.presidentName.trim()

    val signaturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingSignaturePhoto = true
                val base64 = MediaUtils.uriToSignatureBase64(context, uri)
                isProcessingSignaturePhoto = false
                if (!base64.isNullOrBlank()) {
                    selectedSignatureInput = base64
                    viewModel.showSnackbar("स्वाक्षरी फोटो यशस्वीरित्या प्रोसेस झाला! खाली 'स्वाक्षरी सेव्ह करा' दाबा. ✍️")
                } else {
                    viewModel.showSnackbar("❌ स्वाक्षरी प्रोसेस करताना त्रुटी आली. कृपया स्वच्छ पांढऱ्या कागदावरील स्वाक्षरीचा फोटो निवडा.")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // =========================================================================
        // SECTION 1: PRESIDENT'S SIGNATURE & DESIGNATION (अध्यक्षांची स्वाक्षरी व नाव)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NavySecondary.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = NavySecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "१. अध्यक्षांची स्वाक्षरी व नाव (President Signature)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    if (hasPendingSignatureChanges) {
                        Surface(
                            color = SaffronPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = SaffronDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("बदल प्रलंबित", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaffronDark)
                            }
                        }
                    } else {
                        Surface(
                            color = if (mandalInfo.presidentSignatureUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (mandalInfo.presidentSignatureUrl.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = if (mandalInfo.presidentSignatureUrl.isNotBlank()) SuccessGreen else TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (mandalInfo.presidentSignatureUrl.isNotBlank()) "अपलोड केलेली स्वाक्षरी सक्रीय" else "स्वाक्षरी सेट नाही", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (mandalInfo.presidentSignatureUrl.isNotBlank()) SuccessGreen else TextSecondary)
                            }
                        }
                    }
                }

                // Instructions Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NavySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "पांढऱ्या कोऱ्या कागदावर अध्यक्षांची स्वाक्षरी करून तिचा फोटो काढा. ॲप स्वाक्षरीचा पांढरा भाग पारदर्शक (Transparent) करून तिला अधिकृत निळ्या रंगात सर्व सभासदांच्या ओळखपत्रावर सेट करेल.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF1E3A8A),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Live Side-by-Side Signature Comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Active Signature
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("सध्याची स्वाक्षरी", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.size(width = 115.dp, height = 58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                if (mandalInfo.presidentSignatureUrl.isNotBlank()) {
                                    UniversalAsyncImage(
                                        model = mandalInfo.presidentSignatureUrl,
                                        contentDescription = "स्वाक्षरी",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("स्वाक्षरी नाही", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mandalInfo.presidentName.ifBlank { "अध्यक्ष" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark
                        )
                    }

                    // Arrow Icon
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (hasPendingSignatureChanges) SaffronPrimary else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )

                    // New Selected Signature Preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("नवीन देखावा (Preview)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (hasPendingSignatureChanges) SaffronDark else TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (hasPendingSignatureChanges) SaffronPrimary else CardBorderColor),
                            modifier = Modifier.size(width = 115.dp, height = 58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                val sigToShow = selectedSignatureInput.ifBlank { mandalInfo.presidentSignatureUrl }
                                if (sigToShow.isNotBlank()) {
                                    UniversalAsyncImage(
                                        model = sigToShow,
                                        contentDescription = "नवीन स्वाक्षरी",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("स्वाक्षरी नाही", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = presidentNameInput.ifBlank { "अध्यक्ष" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark
                        )
                    }
                }

                HorizontalDivider(color = DividerColor)

                // President Title / Name input
                OutlinedTextField(
                    value = presidentNameInput,
                    onValueChange = { presidentNameInput = it },
                    label = { Text("अध्यक्षांचे नाव / हुद्दा (President Name / Designation)") },
                    placeholder = { Text("उदा. अध्यक्ष किंवा श्री. सचिन पाटील") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Upload Signature Photo Button
                Button(
                    onClick = {
                        signaturePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingSignaturePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_upload_signature_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isProcessingSignaturePhoto) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("स्वाक्षरी फोटो पारदर्शक करत आहे...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✍️ अध्यक्षांची स्वाक्षरी फोटो निवडा / अपलोड करा", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }

                // Save Signature Button
                Button(
                    onClick = {
                        isSavingSignature = true
                        viewModel.updatePresidentSignature(selectedSignatureInput, presidentNameInput) { success ->
                            isSavingSignature = false
                        }
                    },
                    enabled = hasPendingSignatureChanges && !isSavingSignature,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_save_signature_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSavingSignature) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("स्वाक्षरी सेव्ह होत आहे...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("स्वाक्षरी व नाव सेव्ह करा (Save Signature)", fontWeight = FontWeight.Bold)
                    }
                }

                // Delete / Remove Signature
                if (mandalInfo.presidentSignatureUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            isResettingSignature = true
                            viewModel.deletePresidentSignature {
                                isResettingSignature = false
                                selectedSignatureInput = ""
                            }
                        },
                        enabled = !isResettingSignature,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("admin_delete_signature_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isResettingSignature) {
                            CircularProgressIndicator(color = BloodRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("काढून टाकत आहे...", color = BloodRed)
                        } else {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = BloodRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("स्वाक्षरी काढून टाका (Remove Signature)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BloodRed)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: LIVE PREVIEW ON DIGITAL ID CARD (ओळखपत्रावरील स्वाक्षरी देखावा)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "२. थेट ओळखपत्र स्वाक्षरी देखावा (Live ID Card Preview)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF166534)
                    )
                }

                Text(
                    text = "सर्व सभासदांच्या डिजिटल ओळखपत्रावर स्वाक्षरी खालीलप्रमाणे सुबक दिसेल:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    shadowElevation = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        val activeSig = selectedSignatureInput.ifBlank { mandalInfo.presidentSignatureUrl.ifBlank { null } }
                        val activeStamp = selectedStampInput.ifBlank { mandalInfo.officialStampUrl.ifBlank { null } }
                        if (!activeSig.isNullOrBlank() || !activeStamp.isNullOrBlank()) {
                            PresidentSignatureSection(
                                signatureUrl = activeSig,
                                stampUrl = activeStamp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = presidentNameInput.ifBlank { mandalInfo.presidentName.ifBlank { "अध्यक्ष" } },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "जय हिंद मंडळ, अर्जुनवाड",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 4: MANDAL OFFICIAL LOGO (मंडळ मानचिन्ह / लोगो)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "मंडळ मानचिन्ह / लोगो (Official Logo)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    if (hasPendingLogoChanges) {
                        Surface(
                            color = SaffronPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = SaffronDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("बदल प्रलंबित", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaffronDark)
                            }
                        }
                    } else {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सक्रीय लोगो", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Side-by-side comparison: Active vs Selected Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current Active Logo
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("सध्याचा लोगो", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        MandalLogoBadge(logoUrl = currentLogoUrl, size = 80)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLogoUrl.isNullOrBlank()) "डीफॉल्ट मानचिन्ह" else "कस्टम लोगो",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Arrow Icon
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (hasPendingLogoChanges) SaffronPrimary else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )

                    // New Selected Logo Preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("नवीन निवडलेला लोगो", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (hasPendingLogoChanges) SaffronDark else TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        MandalLogoBadge(logoUrl = selectedLogoInput.ifBlank { currentLogoUrl }, size = 80)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasPendingLogoChanges) "⚡ लाइव्ह प्रिव्ह्यू" else "कोणताही बदल नाही",
                            fontSize = 11.sp,
                            fontWeight = if (hasPendingLogoChanges) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasPendingLogoChanges) SaffronPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // Live Logo Simulation Card
        if (hasPendingLogoChanges) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ॲपमधील थेट देखावा (Live App Simulation):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SaffronDark
                        )
                    }

                    // Mini Top Bar Header simulation
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MandalLogoBadge(logoUrl = selectedLogoInput, size = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("जय हिंद मंडळ अर्जुनवाड", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("ॲप हेडर / Top Bar प्रिव्ह्यू", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Select Logo & Actions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "नवीन लोगो निवडा (Select New Logo):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                GalleryImagePicker(
                    selectedImageUrl = selectedLogoInput,
                    onImageSelected = { selectedLogoInput = it },
                    label = "गॅलरीतून मंडळ लोगो निवडा",
                    helperText = "मोबाईल गॅलरीतून मानचिन्ह / लोगो अपलोड करा (Auto Compression)",
                    height = 140.dp
                )

                Text(
                    text = "किंवा नमुना मानचिन्ह पर्याय (Quick Presets):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetLogos.forEach { (name, url) ->
                        FilterChip(
                            selected = selectedLogoInput == url,
                            onClick = { selectedLogoInput = url },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // CONFIRM & SAVE LOGO BUTTON
                Button(
                    onClick = {
                        if (selectedLogoInput.isNotBlank()) {
                            isSavingLogo = true
                            viewModel.updateMandalLogo(selectedLogoInput.trim()) { success ->
                                isSavingLogo = false
                            }
                        }
                    },
                    enabled = hasPendingLogoChanges && !isSavingLogo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_save_logo_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSavingLogo) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("लोगो अपलोड व सिंक होत आहे...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("नक्की करा व लोगो सेव्ह करा (Confirm & Save Logo)", fontWeight = FontWeight.Bold)
                    }
                }

                // CANCEL BUTTON (Revert back to current logo)
                if (hasPendingLogoChanges) {
                    OutlinedButton(
                        onClick = {
                            selectedLogoInput = currentLogoUrl ?: ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_cancel_logo_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("रद्द करा (Cancel - जुना लोगो ठेवा)", fontWeight = FontWeight.SemiBold)
                    }
                }

                // RESET / DELETE CUSTOM LOGO
                if (!currentLogoUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            isResettingLogo = true
                            viewModel.deleteMandalLogo {
                                isResettingLogo = false
                                selectedLogoInput = ""
                            }
                        },
                        enabled = !isResettingLogo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_delete_logo_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isResettingLogo) {
                            CircularProgressIndicator(color = BloodRed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("रीसेट होत आहे...", color = BloodRed)
                        } else {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = BloodRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("डीफॉल्ट लोगो सेट करा (Reset to Default)", fontWeight = FontWeight.Bold, color = BloodRed)
                        }
                    }
                }
            }
        }
    }
}

// 10. LIVE STREAM MANAGEMENT ADMIN TAB
@Composable
fun LiveStreamAdminTab(
    mandalInfo: MandalInfo,
    viewModel: MandalViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var streamTitle by remember(mandalInfo.liveStreamTitle) {
        mutableStateOf(mandalInfo.liveStreamTitle.ifEmpty { "श्री गणेश महाआरती थेट प्रक्षेपण" })
    }
    var streamUrl by remember(mandalInfo.liveStreamUrl) {
        mutableStateOf(mandalInfo.liveStreamUrl.ifEmpty { "https://www.youtube.com/@JayHindMandalArjunwad/live" })
    }
    var notifyMembers by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    val isLiveActive = mandalInfo.isLiveStreamActive

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("admin_live_stream_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLiveActive) Color(0xFF8E0E00) else SurfaceWarm
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isLiveActive) Color(0xFFFF5252) else CardBorderColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveActive) Color.White else TextMuted)
                            )
                            Text(
                                text = if (isLiveActive) "🔴 थेट प्रक्षेपण चालू आहे (LIVE)" else "⏹️ थेट प्रक्षेपण बंद आहे",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isLiveActive) Color.White else TextPrimary
                            )
                        }

                        if (isLiveActive) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "👁️ ${mandalInfo.liveViewerCount} पाहत आहेत",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isLiveActive)
                            "सध्या सर्व सभासदांना होम स्क्रीनवर थेट प्रक्षेपण अलर्ट दिसत आहे आणि सभासद ॲपमध्ये थेट व्हिडिओ पाहत आहेत."
                        else
                            "येथून आपण YouTube Live लिंक जोडून एका क्लिकवर सर्व सभासदांना थेट प्रक्षेपण दाखवू शकता.",
                        fontSize = 12.sp,
                        color = if (isLiveActive) Color.White.copy(alpha = 0.9f) else TextSecondary
                    )
                }
            }
        }

        // Live Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚙️ थेट प्रक्षेपणाचे तपशील (Live Stream Settings)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    // Title Field
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        label = { Text("प्रक्षेपणाचे नाव / शीर्षक (Title)") },
                        placeholder = { Text("उदा. श्री गणेश महाआरती थेट प्रक्षेपण") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_live_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = CardBorderColor
                        ),
                        singleLine = true
                    )

                    // Stream URL Field (Universal auto-detection for YouTube, Facebook, Instagram, HLS, RTMP)
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("थेट प्रक्षेपण लिंक (YouTube / Facebook / Instagram / HLS / RTMP)") },
                        placeholder = { Text("कोणतीही लाईव्ह लिंक येथे पेस्ट करा (ऑटो-डिटेक्ट होईल)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_live_url_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = CardBorderColor
                        ),
                        trailingIcon = {
                            if (streamUrl.isNotBlank()) {
                                IconButton(onClick = { streamUrl = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Dynamic Detection Indicator
                    if (streamUrl.isNotBlank()) {
                        val detectedPlatform = remember(streamUrl) { detectStreamPlatform(streamUrl) }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (detectedPlatform) {
                                StreamPlatform.YOUTUBE -> SaffronPrimary.copy(alpha = 0.12f)
                                StreamPlatform.FACEBOOK -> Color(0xFF1877F2).copy(alpha = 0.12f)
                                StreamPlatform.INSTAGRAM -> Color(0xFFE1306C).copy(alpha = 0.12f)
                                else -> CardBorderColor.copy(alpha = 0.3f)
                            },
                            border = BorderStroke(1.dp, when (detectedPlatform) {
                                StreamPlatform.YOUTUBE -> SaffronPrimary.copy(alpha = 0.6f)
                                StreamPlatform.FACEBOOK -> Color(0xFF1877F2).copy(alpha = 0.6f)
                                StreamPlatform.INSTAGRAM -> Color(0xFFE1306C).copy(alpha = 0.6f)
                                else -> CardBorderColor
                            })
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ओळखलेले प्लॅटफॉर्म:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = when (detectedPlatform) {
                                        StreamPlatform.YOUTUBE -> "▶️ YouTube Live (युट्युब लाईव्ह)"
                                        StreamPlatform.FACEBOOK -> "🔵 Facebook Live (फेसबुक लाईव्ह)"
                                        StreamPlatform.INSTAGRAM -> "🟣 Instagram Live (इन्स्टाग्राम लाईव्ह)"
                                        StreamPlatform.DIRECT_HLS -> "📡 Direct HLS / RTMP स्ट्रीम"
                                        StreamPlatform.CUSTOM -> "🌐 थेट वेब प्लेयर"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (detectedPlatform) {
                                        StreamPlatform.YOUTUBE -> SaffronPrimary
                                        StreamPlatform.FACEBOOK -> Color(0xFF1877F2)
                                        StreamPlatform.INSTAGRAM -> Color(0xFFE1306C)
                                        else -> TextPrimary
                                    }
                                )
                            }
                        }
                    }

                    // Platform presets indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SaffronPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable {
                                    val cleanHandle = mandalInfo.youtubeHandle.trim()
                                    streamUrl = if (cleanHandle.startsWith("http")) {
                                        if (cleanHandle.contains("live")) cleanHandle else "$cleanHandle/live"
                                    } else {
                                        "https://www.youtube.com/$cleanHandle/live"
                                    }
                                }
                        ) {
                            Text(
                                text = "▶️ YouTube Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1877F2).copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable {
                                    val cleanHandle = mandalInfo.facebookHandle.trim()
                                    streamUrl = if (cleanHandle.startsWith("http")) cleanHandle else "https://www.facebook.com/$cleanHandle/live"
                                }
                        ) {
                            Text(
                                text = "🔵 Facebook Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE1306C).copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable {
                                    val cleanHandle = mandalInfo.instagramHandle.trim()
                                    streamUrl = if (cleanHandle.startsWith("http")) cleanHandle else "https://www.instagram.com/$cleanHandle/live"
                                }
                        ) {
                            Text(
                                text = "📷 Instagram Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE1306C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Push Notification Checkbox (only for starting)
                    if (!isLiveActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notifyMembers = !notifyMembers }
                        ) {
                            Checkbox(
                                checked = notifyMembers,
                                onCheckedChange = { notifyMembers = it },
                                colors = CheckboxDefaults.colors(checkedColor = SaffronPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "सर्व सभासदांना थेट नोटिफिकेशन पाठवा 🔔",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "मोबाईलवर '🔴 थेट आरती/कार्यक्रम सुरू आहे' अशी सूचना जाईल",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Helpful Marathi tip for Live Streaming
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF8E1),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "थेट प्रक्षेपणासाठी उपयुक्त टीप:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "• YouTube वर लाईव्ह करताना YouTube Studio मध्ये 'Allow embedding' (एम्बेडिंग) चालू ठेवावे, जेणेकरून ॲपमध्ये थेट व्हिडिओ दिसेल.",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big Toggle Action Button
                    if (isLiveActive) {
                        // STOP STREAM BUTTON
                        Button(
                            onClick = {
                                isUpdating = true
                                viewModel.setLiveStreamStatus(
                                    isLive = false,
                                    title = streamTitle,
                                    url = streamUrl,
                                    notifyMembers = false
                                ) {
                                    isUpdating = false
                                }
                            },
                            enabled = !isUpdating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_stop_live_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("अपडेट होत आहे...")
                            } else {
                                Icon(Icons.Default.StopCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⏹️ थेट प्रक्षेपण थांबवा (End Live)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // START STREAM BUTTON
                        Button(
                            onClick = {
                                isUpdating = true
                                viewModel.setLiveStreamStatus(
                                    isLive = true,
                                    title = streamTitle,
                                    url = streamUrl,
                                    notifyMembers = notifyMembers
                                ) {
                                    isUpdating = false
                                }
                            },
                            enabled = !isUpdating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_start_live_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("थेट सुरू होत आहे...")
                            } else {
                                Icon(Icons.Default.Videocam, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔴 थेट प्रक्षेपण सुरू करा (Go Live)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Save settings without toggling state
                    OutlinedButton(
                        onClick = {
                            isUpdating = true
                            viewModel.setLiveStreamStatus(
                                isLive = isLiveActive,
                                title = streamTitle,
                                url = streamUrl,
                                notifyMembers = false
                            ) {
                                isUpdating = false
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("फक्त लिंक व शीर्षक सेव्ह करा", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Live Preview Player Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "👀 ॲपमधील थेट देखावा (In-App Player Preview)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "सभासदांच्या मोबाईलवर व्हिडिओ कसा दिसेल हे पाहण्यासाठी खालील बटण दाबा:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openLiveStreamPlayer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("admin_preview_player_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("प्लेयर उघडा", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("YouTube ॲप", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Guide Card (कसे वापरावे - Step by step guide)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📖 मोबाईलवरून थेट प्रक्षेपण (Live Streaming) कसे करावे?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SaffronDark
                    )

                    val steps = listOf(
                        "१. आपल्या मोबाईलमधील **YouTube App** उघडा आणि खालील **'+' चिन्हावर** क्लिक करून **'Go Live'** निवडा.",
                        "२. कॅमेऱ्यासमोर आरती किंवा कार्यक्रम सुरू करा. YouTube स्क्रीनवरील **Share (शेअर)** आयकॉनवर क्लिक करून लिंक **Copy Link** करा.",
                        "३. या ॲडमिन पॅनेलमध्ये येऊन ती लिंक वरील बॉक्समध्ये पेस्ट करा.",
                        "४. **'🔴 थेट प्रक्षेपण सुरू करा'** हे लाल बटण दाबा. सर्व सभासदांना तात्काळ नोटिफिकेशन जाईल आणि ॲपमध्ये आरती थेट दिसेल!",
                        "५. कार्यक्रम संपल्यावर **'⏹️ थेट प्रक्षेपण थांबवा'** बटण दाबा. तो व्हिडिओ आपोआप गॅलरीत सेव्ह होईल."
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

