package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private val viewModel: MandalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                MandalApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dataString = intent?.dataString
        if (!dataString.isNullOrBlank()) {
            viewModel.handleScannedQrPayload(dataString)
        }
        val targetRoute = intent?.getStringExtra("EXTRA_TARGET_ROUTE")
        val targetId = intent?.getStringExtra("EXTRA_TARGET_ID")
        if (!targetRoute.isNullOrBlank()) {
            viewModel.handleNotificationRoute(targetRoute, targetId)
        }
    }
}

@Composable
fun MandalApp(viewModel: MandalViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()
    val scannedResult by viewModel.scannedVerificationResult.collectAsStateWithLifecycle()
    val mandalInfo by viewModel.mandalInfo.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val fullscreenPhotoUrl by viewModel.fullscreenPhotoUrl.collectAsStateWithLifecycle()
    val fullscreenViewerState by viewModel.fullscreenViewerState.collectAsStateWithLifecycle()
    val showLiveStreamPlayer by viewModel.showLiveStreamPlayer.collectAsStateWithLifecycle()
    val liveComments by viewModel.liveComments.collectAsStateWithLifecycle()
    val sessionSecurityNotice by viewModel.sessionSecurityNotice.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var memberForIdCardDialog by remember { mutableStateOf<User?>(null) }

    // Request Notification Permission immediately when app is installed / opened (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showSnackbar("मंडळाच्या सूचना (Notifications) परवानगी यशस्वीरित्या दिली आहे! 🔔")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(sessionSecurityNotice) {
        if (!sessionSecurityNotice.isNullOrBlank()) {
            viewModel.navigateTo(AppScreen.LOGIN)
        }
    }

    // Hardware Back Button Handling
    BackHandler(enabled = currentScreen != AppScreen.SPLASH) {
        when (currentScreen) {
            AppScreen.MAIN -> {
                if (currentTab != NavigationTab.HOME) {
                    viewModel.setNavigationTab(NavigationTab.HOME)
                }
            }
            AppScreen.CHAT_DETAIL -> viewModel.closeChat()
            AppScreen.USER_POSTS -> viewModel.closeUserPosts()
            AppScreen.LOGIN, AppScreen.REGISTER -> {
                // Keep on screen or close
            }
            else -> viewModel.navigateTo(AppScreen.MAIN)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NavySecondary,
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            }
        },
        topBar = {
            if (currentScreen == AppScreen.MAIN) {
                val tabTitle = when (currentTab) {
                    NavigationTab.HOME -> "जय हिंद मंडळ अर्जुनवाड"
                    NavigationTab.QUICK_ACCESS -> "जलद मेनू व माहिती (Quick Access)"
                    NavigationTab.MEMBERS -> "सभासद सूची व रक्तगट"
                    NavigationTab.POSTS -> "मंडळ सोशल फीड"
                    NavigationTab.CHAT -> "सभासद मेसेंजर"
                    NavigationTab.PROFILE -> "माझे सभासद प्रोफाइल"
                }

                val tabSubtitle = when (currentTab) {
                    NavigationTab.HOME -> "कला, क्रीडा व सांस्कृतिक मंडळ"
                    NavigationTab.QUICK_ACCESS -> "सर्व सेवा व आमच्याबद्दल माहिती"
                    NavigationTab.MEMBERS -> "अर्जुनवाड • ता. शिरोळ"
                    NavigationTab.POSTS -> "नवीन घडामोडी व उपक्रम"
                    NavigationTab.CHAT -> "थेट संवाद"
                    NavigationTab.PROFILE -> "अधिकृत ओळखपत्र"
                }

                MandalTopHeader(
                    title = tabTitle,
                    subtitle = tabSubtitle,
                    showBackButton = false,
                    logoUrl = mandalLogoUrl,
                    actions = {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = BloodRed) { Text("$unreadNotifs") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen == AppScreen.MAIN) {
                MandalBottomNavigation(
                    currentTab = currentTab,
                    unreadChatCount = unreadChatCount,
                    onTabSelected = { viewModel.setNavigationTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
        ) {
            when (currentScreen) {
                AppScreen.SPLASH -> {
                    SplashScreen(
                        isLoggedIn = currentUser != null,
                        mandalLogoUrl = mandalLogoUrl,
                        onNavigateNext = {
                            if (currentUser != null) {
                                viewModel.navigateTo(AppScreen.MAIN)
                                viewModel.setNavigationTab(NavigationTab.POSTS)
                            } else {
                                viewModel.navigateTo(AppScreen.LOGIN)
                            }
                        }
                    )
                }

                AppScreen.LOGIN -> {
                    AuthScreen(viewModel = viewModel, isRegisterModeInitial = false)
                }

                AppScreen.REGISTER -> {
                    AuthScreen(viewModel = viewModel, isRegisterModeInitial = true)
                }

                AppScreen.MAIN -> {
                    when (currentTab) {
                        NavigationTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onOpenCreatePost = { showCreatePostDialog = true }
                        )
                        NavigationTab.QUICK_ACCESS -> QuickAccessScreen(
                            viewModel = viewModel
                        )
                        NavigationTab.MEMBERS -> MembersScreen(viewModel = viewModel)
                        NavigationTab.POSTS -> PostsScreen(
                            viewModel = viewModel,
                            onOpenCreatePost = { showCreatePostDialog = true }
                        )
                        NavigationTab.CHAT -> ChatListScreen(viewModel = viewModel)
                        NavigationTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }

                AppScreen.ADMIN_PANEL -> {
                    if (currentUser?.isAdmin == true) {
                        AdminPanelScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(AppScreen.MAIN) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.MAIN)
                    }
                }

                AppScreen.GALLERY,
                AppScreen.PHOTO_GALLERY,
                AppScreen.VIDEO_GALLERY -> {
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN) }
                    )
                }

                AppScreen.EVENTS -> {
                    EventsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN) }
                    )
                }

                AppScreen.ANNOUNCEMENTS -> {
                    AnnouncementsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN) }
                    )
                }

                AppScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.MAIN) }
                    )
                }

                AppScreen.CHAT_DETAIL -> {
                    val partner by viewModel.activeChatPartner.collectAsStateWithLifecycle()
                    if (partner != null) {
                        ChatDetailScreen(
                            viewModel = viewModel,
                            partner = partner!!,
                            onBack = { viewModel.closeChat() }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.MAIN)
                    }
                }

                AppScreen.USER_POSTS -> {
                    val selectedUser by viewModel.selectedUserForPosts.collectAsStateWithLifecycle()
                    if (selectedUser != null) {
                        UserPostsScreen(
                            user = selectedUser!!,
                            viewModel = viewModel,
                            onBack = { viewModel.closeUserPosts() }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.MAIN)
                    }
                }
            }

            // Global Create Post Dialog
            if (showCreatePostDialog) {
                CreatePostDialog(
                    currentUser = currentUser,
                    onDismiss = { showCreatePostDialog = false },
                    onPostCreated = { content, img, vid ->
                        viewModel.createPost(content, img, vid) {
                            showCreatePostDialog = false
                        }
                    }
                )
            }

            // Member Verification Detail Modal (shows verified info after external camera scanning)
            scannedResult?.let { result ->
                MemberVerificationDetailDialog(
                    result = result,
                    onDismiss = { viewModel.clearVerificationResult() },
                    onViewFullIdCard = { member ->
                        memberForIdCardDialog = member
                    },
                    onOpenChat = { member ->
                        viewModel.clearVerificationResult()
                        viewModel.openChatWith(member)
                    }
                )
            }

            // Full Digital ID Card Modal
            memberForIdCardDialog?.let { member ->
                DigitalIdCardDialog(
                    user = member,
                    mandalInfo = mandalInfo,
                    mandalLogoUrl = mandalLogoUrl,
                    onDismiss = { memberForIdCardDialog = null }
                )
            }

            // Global Fullscreen Photo Viewer Modal with Swipe, Titles, and Admin Download/Share functionality
            if (fullscreenViewerState != null) {
                FullscreenPhotoDialog(
                    photos = fullscreenViewerState!!.photos,
                    titles = fullscreenViewerState!!.titles,
                    initialIndex = fullscreenViewerState!!.initialIndex,
                    isAdmin = currentUser?.isAdmin == true,
                    onDismiss = { viewModel.closeFullscreenPhoto() }
                )
            } else if (!fullscreenPhotoUrl.isNullOrBlank()) {
                FullscreenPhotoDialog(
                    photoUrl = fullscreenPhotoUrl,
                    isAdmin = currentUser?.isAdmin == true,
                    onDismiss = { viewModel.closeFullscreenPhoto() }
                )
            }

            // Global In-App Live Stream Dialog (Smart Multi-Platform Player with Watermark & Logo)
            if (showLiveStreamPlayer) {
                LiveStreamDialog(
                    mandalInfo = mandalInfo,
                    mandalLogoUrl = mandalLogoUrl,
                    comments = liveComments,
                    onDismiss = { viewModel.closeLiveStreamPlayer() },
                    onSendReaction = { reaction ->
                        viewModel.sendLiveReaction(reaction)
                    },
                    onPostComment = { commentText ->
                        viewModel.postLiveComment(commentText)
                    }
                )
            }

            // Single Device Active Session Security Alert Dialog
            if (!sessionSecurityNotice.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearSessionSecurityNotice() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "सुरक्षा सूचना",
                            tint = BloodRed,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "सुरक्षा सूचना (Security Alert)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = sessionSecurityNotice ?: "आपले खाते दुसऱ्या मोबाईलमध्ये लॉगिन झाल्यामुळे या मोबाईलमधील लॉगिन सुरक्षिततेसाठी बंद (लॉगआऊट) करण्यात आले आहे.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearSessionSecurityNotice() },
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text("समजले (OK)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = SurfaceWarm,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

data class BottomNavItem(
    val tab: NavigationTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val testTag: String
)

@Composable
fun MandalBottomNavigation(
    currentTab: NavigationTab,
    unreadChatCount: Int = 0,
    onTabSelected: (NavigationTab) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            tab = NavigationTab.HOME,
            label = "Main",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_tab_home"
        ),
        BottomNavItem(
            tab = NavigationTab.MEMBERS,
            label = "Members",
            selectedIcon = Icons.Filled.Group,
            unselectedIcon = Icons.Outlined.Group,
            testTag = "nav_tab_members"
        ),
        BottomNavItem(
            tab = NavigationTab.POSTS,
            label = "Post",
            selectedIcon = Icons.Filled.DynamicFeed,
            unselectedIcon = Icons.Outlined.DynamicFeed,
            testTag = "nav_tab_posts"
        ),
        BottomNavItem(
            tab = NavigationTab.CHAT,
            label = if (unreadChatCount > 0) "Chat ($unreadChatCount)" else "Chat",
            selectedIcon = Icons.Filled.Chat,
            unselectedIcon = Icons.Outlined.Chat,
            badgeCount = unreadChatCount,
            testTag = "nav_tab_chat"
        ),
        BottomNavItem(
            tab = NavigationTab.PROFILE,
            label = "Profile",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle,
            testTag = "nav_tab_profile"
        )
    )

    NavigationBar(
        containerColor = SurfaceWarm,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(width = 1.dp, color = CardBorderColor)
            .testTag("bottom_navigation_bar")
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = BloodRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "${item.badgeCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SaffronPrimary,
                    selectedTextColor = SaffronPrimary,
                    indicatorColor = SaffronContainer,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
