package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel

enum class AdminTab(val title: String) {
    PENDING_APPROVALS("सभासद मंजुरी"),
    MANAGE_BANNERS("ग्रुप बॅनर"),
    EDIT_ABOUT_US("आमच्याबद्दल व सोशल"),
    MEMBERS_LIST("सर्व सभासद व ॲडमिन"),
    MANAGE_LOGO("मंडळ लोगो"),
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
        viewModel.refreshAllData()
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
                        onClick = { viewModel.refreshAllData() },
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
                AdminTab.MANAGE_BANNERS -> ManageBannersAdminTab(banners, viewModel)
                AdminTab.EDIT_ABOUT_US -> EditAboutUsAdminTab(mandalInfo, viewModel)
                AdminTab.MEMBERS_LIST -> AllMembersAdminTab(allMembers, viewModel)
                AdminTab.MANAGE_LOGO -> ManageLogoAdminTab(mandalLogoUrl, viewModel)
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

            items(pendingList, key = { it.id }) { user ->
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
                            }
                        }

                        if (user.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "पत्ता: ${user.address}",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            items(banners, key = { it.id }) { banner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            AsyncImage(
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

// 4. ALL MEMBERS & ADMIN ROLE MANAGEMENT
@Composable
fun AllMembersAdminTab(members: List<User>, viewModel: MandalViewModel) {
    var memberToManage by remember { mutableStateOf<User?>(null) }
    var memberToChangeRole by remember { mutableStateOf<User?>(null) }

    // Dialog for Delete & Transfer
    if (memberToManage != null) {
        val target = memberToManage!!
        AlertDialog(
            onDismissRequest = { memberToManage = null },
            title = {
                Text(
                    text = "${target.fullName} - सभासद व्यवस्थापन",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "मोबाईल: ${target.mobileNumber} • रक्तगट: ${target.bloodGroup}",
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

    // Dialog for Changing Admin Role
    if (memberToChangeRole != null) {
        val target = memberToChangeRole!!
        val willBeAdmin = !target.isAdmin
        AlertDialog(
            onDismissRequest = { memberToChangeRole = null },
            title = {
                Text(
                    text = if (willBeAdmin) "ॲडमिन अधिकार प्रदान करा (Make Admin)" else "ॲडमिन अधिकार काढून घ्या (Remove Admin)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "सभासद: ${target.fullName} (${target.mobileNumber})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (willBeAdmin)
                            "तुम्हाला या सभासदाला मंडळाचे मुख्य ॲडमिन अधिकार द्यायचे आहेत का? ॲडमिन बनल्यानंतर ते सभासद मंजुरी, बॅनर, सूचना आणि कार्यक्रम व्यवस्थापित करू शकतील."
                        else
                            "तुम्हाला या सभासदाचे ॲडमिन अधिकार काढून त्यांना सामान्य सभासद बनवायचे आहे का?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changeUserRole(target.id, if (willBeAdmin) "ADMIN" else "MEMBER")
                        memberToChangeRole = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (willBeAdmin) Color(0xFFDB2777) else SaffronPrimary
                    )
                ) {
                    Text(if (willBeAdmin) "होय, ॲडमिन बनवा" else "होय, सामान्य सभासद करा")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToChangeRole = null }) {
                    Text("रद्द करा")
                }
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
                        text = "ॲडमिन बदलणे (Change Admin): तुम्ही कोणत्याही पात्र सभासदाला ॲडमिन बनवू शकता किंवा ॲडमिन बदलू शकता.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4C1D95),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Text(
                text = "एकूण नोंदणीकृत सभासद: ${members.size}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
        }

        items(members, key = { it.id }) { member ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MemberAvatar(photoUrl = member.profilePhotoUrl, name = member.fullName, size = 46)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = member.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (member.isAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
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
                            Text(text = "मोबाईल: ${member.mobileNumber}", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = member.status)
                                Spacer(modifier = Modifier.width(6.dp))
                                BloodGroupBadge(bloodGroup = member.bloodGroup)
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
                        // Change Role Button (Make Admin / Remove Admin)
                        OutlinedButton(
                            onClick = { memberToChangeRole = member },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (member.isAdmin) SaffronPrimary else Color(0xFFDB2777)
                            )
                        ) {
                            Icon(
                                imageVector = if (member.isAdmin) Icons.Default.Person else Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (member.isAdmin) "सामान्य सभासद बनवा" else "👑 ॲडमिन बनवा",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!member.isAdmin) {
                                if (member.status == "BLOCKED") {
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
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("दिनांक (Date)") },
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

        items(posts, key = { it.id }) { post ->
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

// 9. MANAGE LOGO (Overhauled with Immediate Live Preview & App-wide simulation)
@Composable
fun ManageLogoAdminTab(
    currentLogoUrl: String?,
    viewModel: MandalViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedLogoInput by remember(currentLogoUrl) { mutableStateOf(currentLogoUrl ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    val hasPendingChanges = selectedLogoInput.trim() != (currentLogoUrl ?: "").trim() && selectedLogoInput.isNotBlank()

    val presetLogos = listOf(
        "भगवा ध्वज मानचिन्ह" to "https://images.unsplash.com/photo-1544717305-2782549b5136?w=600&auto=format&fit=crop&q=80",
        "सुवर्ण कला मानचिन्ह" to "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80",
        "क्रीडा व सांस्कृतिक" to "https://images.unsplash.com/photo-1532375810709-75b1da00537c?w=600&auto=format&fit=crop&q=80"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. STATUS & COMPARISON CARD (Current vs Preview)
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
                    Text(
                        text = "मंडळ लोगो नियंत्रण (Logo Manager)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (hasPendingChanges) {
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
                                Text("बदल प्रलंबित (Preview)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaffronDark)
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
                        tint = if (hasPendingChanges) SaffronPrimary else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )

                    // New Selected Logo Preview
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("नवीन निवडलेला लोगो", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (hasPendingChanges) SaffronDark else TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        MandalLogoBadge(logoUrl = selectedLogoInput.ifBlank { currentLogoUrl }, size = 80)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasPendingChanges) "⚡ लाइव्ह प्रिव्ह्यू" else "कोणताही बदल नाही",
                            fontSize = 11.sp,
                            fontWeight = if (hasPendingChanges) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasPendingChanges) SaffronPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // 2. LIVE APP-WIDE PREVIEWS SIMULATION
        if (hasPendingChanges) {
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

                    // Mini ID Card Watermark Simulation
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedLogoInput,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(54.dp)
                                    .alpha(0.18f)
                            )
                            Text(
                                text = "डिजिटल ओळखपत्र वॉटरमार्क प्रिव्ह्यू",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 3. SELECT LOGO & ACTIONS CARD
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

                // CONFIRM & SAVE BUTTON
                Button(
                    onClick = {
                        if (selectedLogoInput.isNotBlank()) {
                            isSaving = true
                            viewModel.updateMandalLogo(selectedLogoInput.trim()) { success ->
                                isSaving = false
                            }
                        }
                    },
                    enabled = hasPendingChanges && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_save_logo_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
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
                        Text("नक्की करा व सेव्ह करा (Confirm & Save)", fontWeight = FontWeight.Bold)
                    }
                }

                // CANCEL BUTTON (Revert back to current logo)
                if (hasPendingChanges) {
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
                            isResetting = true
                            viewModel.deleteMandalLogo {
                                isResetting = false
                                selectedLogoInput = ""
                            }
                        },
                        enabled = !isResetting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_delete_logo_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isResetting) {
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
