package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MandalBanner
import com.example.data.model.MandalEvent
import com.example.data.model.MandalInfo
import com.example.data.model.Post
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MandalViewModel
import com.example.ui.viewmodel.NavigationTab

@Composable
fun HomeScreen(
    viewModel: MandalViewModel,
    onOpenCreatePost: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val mandalInfo by viewModel.mandalInfo.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val todayBirthdays by viewModel.todayBirthdays.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var showIdCardDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP MANDAL HERO / GROUP BANNERS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (banners.isNotEmpty()) {
                    // Group Banners Carousel
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        items(banners, key = { it.id }) { banner ->
                            GroupBannerCard(
                                banner = banner,
                                mandalLogoUrl = mandalLogoUrl,
                                unreadNotifs = unreadNotifs,
                                onNotifClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }
                            )
                        }
                    }
                } else {
                    // Default Fallback Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SaffronPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(165.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.mandal_hero_banner),
                                contentDescription = "Mandal Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                SaffronDark.copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = mandalInfo.locationTitle,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadNotifs > 0) {
                                                    Badge(containerColor = BloodRed) { Text("$unreadNotifs") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "सूचना",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MandalLogoBadge(logoUrl = mandalLogoUrl, size = 46)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mandalInfo.mandalName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mandalInfo.tagline,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.95f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. QUICK ACCESS SHORTCUTS
        item {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = "जलद पर्याय (Quick Access)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton(
                        icon = Icons.Default.Badge,
                        label = "ओळखपत्र",
                        color = SaffronPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { showIdCardDialog = true }
                    )
                    QuickButton(
                        icon = Icons.Default.PhotoLibrary,
                        label = "गॅलरी",
                        color = NavySecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )
                    QuickButton(
                        icon = Icons.Default.Event,
                        label = "कार्यक्रम",
                        color = GoldenTertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.EVENTS) }
                    )
                    QuickButton(
                        icon = Icons.Default.Campaign,
                        label = "सूचना",
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.ANNOUNCEMENTS) }
                    )
                    QuickButton(
                        icon = Icons.Default.Bloodtype,
                        label = "रक्तगट",
                        color = BloodRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setSelectedBloodGroupFilter("सर्व")
                            viewModel.setNavigationTab(NavigationTab.MEMBERS)
                        }
                    )
                    if (currentUser?.isAdmin == true) {
                        QuickButton(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "Admin",
                            color = Color(0xFFDB2777),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) }
                        )
                    }
                }
            }
        }

        // 3. MANDATORY: "ABOUT US" SECTION PLACED DIRECTLY ABOVE "TODAY'S BIRTHDAYS"
        item {
            AboutUsHomeSection(
                mandalInfo = mandalInfo,
                mandalLogoUrl = mandalLogoUrl,
                isAdmin = currentUser?.isAdmin == true,
                onEditClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        // 4. TODAY'S BIRTHDAY MEMBERS SECTION (STRICTLY ON ACTUAL BIRTHDAY ONLY)
        if (todayBirthdays.isNotEmpty()) {
            item {
                BirthdaySection(
                    birthdayMembers = todayBirthdays,
                    onMemberClick = { member -> viewModel.selectMemberForDetail(member) },
                    onChatClick = { member -> viewModel.openChatWith(member) },
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }

        // 5. IMPORTANT ANNOUNCEMENTS TICKER
        if (announcements.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Campaign,
                                    contentDescription = null,
                                    tint = BloodRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "महत्वाच्या सूचना (Notice Board)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        TextButton(onClick = { viewModel.navigateTo(AppScreen.ANNOUNCEMENTS) }) {
                            Text("सर्व पहा", color = NavySecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val topAnn = announcements.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.ANNOUNCEMENTS) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topAnn.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                PriorityBadge(priority = topAnn.priority)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = topAnn.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "सूचनाकर्ते: ${topAnn.author}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = SaffronPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = topAnn.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. UPCOMING EVENTS (आगामी कार्यक्रम)
        if (events.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NavyContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = NavySecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "आगामी कार्यक्रम (Upcoming Events)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        TextButton(onClick = { viewModel.navigateTo(AppScreen.EVENTS) }) {
                            Text("सर्व पहा", color = NavySecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(events, key = { it.id }) { event ->
                            EventCardCompact(
                                event = event,
                                onRegisterClick = { viewModel.toggleEventRegistration(event) }
                            )
                        }
                    }
                }
            }
        }

        // 7. RECENT POSTS / FEED HIGHLIGHTS
        item {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "नवीन पोस्ट्स व अपडेट्स (Recent Feed)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    TextButton(onClick = { viewModel.setNavigationTab(NavigationTab.POSTS) }) {
                        Text("फीड पहा", color = SaffronPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Feed items
        items(posts.take(3), key = { it.id }) { post ->
            PostItemCard(
                post = post,
                currentUser = currentUser,
                onLikeClick = { viewModel.toggleLike(post.id) },
                onCommentClick = { viewModel.openComments(post) },
                onDeleteClick = { viewModel.deletePost(post.id) },
                onEditClick = { postToEdit = post },
                onImageClick = { viewModel.openFullscreenPhoto(it) },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (postToEdit != null) {
        CreatePostDialog(
            currentUser = currentUser,
            initialPost = postToEdit,
            onDismiss = { postToEdit = null },
            onPostCreated = { content, imageUrl, videoUrl ->
                postToEdit?.let { target ->
                    viewModel.updatePost(target.id, content, imageUrl, videoUrl) {
                        postToEdit = null
                    }
                }
            }
        )
    }

    if (showIdCardDialog && currentUser != null) {
        DigitalIdCardDialog(
            user = currentUser!!,
            mandalInfo = mandalInfo,
            mandalLogoUrl = mandalLogoUrl,
            onDismiss = { showIdCardDialog = false }
        )
    }
}

@Composable
fun GroupBannerCard(
    banner: MandalBanner,
    mandalLogoUrl: String?,
    unreadNotifs: Int,
    onNotifClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(340.dp)
            .height(175.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SaffronPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                SaffronDark.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "🚩 मंडळ बॅनर",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onNotifClick,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge(containerColor = BloodRed) { Text("$unreadNotifs") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "सूचना",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column {
                    if (banner.title.isNotBlank()) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (banner.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.95f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutUsHomeSection(
    mandalInfo: MandalInfo,
    mandalLogoUrl: String?,
    isAdmin: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openUrlSafely(url: String) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_us_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MandalLogoBadge(logoUrl = mandalLogoUrl, size = 48)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "आमच्याबद्दल (About Us)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SaffronPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { onEditClick() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "संपादित करा",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SaffronPrimary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = mandalInfo.mandalName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = SaffronPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mandalInfo.aboutDescription,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SOCIAL MEDIA HANDLES HUB
            Text(
                text = "अधिकृत सोशल मीडिया हँडल्स (Official Handles):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // YouTube Handle Button
            SocialHandleRow(
                platformName = "YouTube Channel",
                handle = mandalInfo.youtubeHandle,
                badgeColor = Color(0xFFFF0000),
                icon = Icons.Default.PlayCircleFilled,
                onClick = {
                    val clean = mandalInfo.youtubeHandle.trim()
                    val url = if (clean.startsWith("http")) clean else "https://www.youtube.com/${clean}"
                    openUrlSafely(url)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Facebook Handle Button
            SocialHandleRow(
                platformName = "Facebook Page",
                handle = mandalInfo.facebookHandle,
                badgeColor = Color(0xFF1877F2),
                icon = Icons.Default.ThumbUp,
                onClick = {
                    val clean = mandalInfo.facebookHandle.trim()
                    val url = if (clean.startsWith("http")) clean else "https://www.facebook.com/${clean}"
                    openUrlSafely(url)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Instagram Handle Button
            SocialHandleRow(
                platformName = "Instagram Page",
                handle = mandalInfo.instagramHandle,
                badgeColor = Color(0xFFE1306C),
                icon = Icons.Default.CameraAlt,
                onClick = {
                    val clean = mandalInfo.instagramHandle.trim().removePrefix("@")
                    val url = if (clean.startsWith("http")) clean else "https://www.instagram.com/${clean}"
                    openUrlSafely(url)
                }
            )

            // Optional Admin Web Link if added
            if (mandalInfo.adminWebLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                SocialHandleRow(
                    platformName = "Admin Web Portal Link",
                    handle = mandalInfo.adminWebLink,
                    badgeColor = Color(0xFF0D9488),
                    icon = Icons.Default.Link,
                    onClick = { openUrlSafely(mandalInfo.adminWebLink) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Contact Info Details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mandalInfo.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${mandalInfo.phone}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = SuccessGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mandalInfo.phone,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }
                }

                if (mandalInfo.email.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${mandalInfo.email}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = SaffronPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mandalInfo.email,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NavySecondary
                        )
                    }
                }

                if (mandalInfo.address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NavySecondary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Address",
                                tint = NavySecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mandalInfo.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialHandleRow(
    platformName: String,
    handle: String,
    badgeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = badgeColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = platformName,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = platformName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open",
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuickButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EventCardCompact(
    event: MandalEvent,
    onRegisterClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.width(260.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SaffronPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isRegistered) SuccessGreen else NavySecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (event.isRegistered) Icons.Default.Check else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (event.isRegistered) "नोंदणी झाली (${event.attendeesCount})" else "उपस्थित राहणार (${event.attendeesCount})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
