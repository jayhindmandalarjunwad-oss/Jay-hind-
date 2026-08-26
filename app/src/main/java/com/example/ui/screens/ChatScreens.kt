package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel

@Composable
fun ChatListScreen(viewModel: MandalViewModel) {
    val summaries by viewModel.chatSummaries.collectAsStateWithLifecycle()
    val approvedMembers by viewModel.approvedMembers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: All Members
    var showNewChatDialog by remember { mutableStateOf(false) }

    val otherMembers = remember(approvedMembers, currentUser) {
        approvedMembers.filter { it.id != currentUser?.id }
    }

    // Filter active chat summaries by Name, Mobile Number, or Message text
    val filteredSummaries = remember(summaries, searchQuery) {
        if (searchQuery.isBlank()) {
            summaries
        } else {
            val q = searchQuery.trim()
            summaries.filter {
                it.otherUser.fullName.contains(q, ignoreCase = true) ||
                        it.otherUser.mobileNumber.contains(q) ||
                        it.lastMessage.contains(q, ignoreCase = true)
            }
        }
    }

    // Filter ALL mandal members by Name or Mobile Number
    val filteredAllMembers = remember(otherMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            otherMembers
        } else {
            val q = searchQuery.trim()
            otherMembers.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                        it.mobileNumber.contains(q) ||
                        it.bloodGroup.contains(q, ignoreCase = true)
            }
        }
    }

    // Members who don't have an active conversation yet matching search
    val matchingMembersWithoutSummary = remember(filteredAllMembers, summaries) {
        val existingChatUserIds = summaries.map { it.otherUser.id }.toSet()
        filteredAllMembers.filter { it.id !in existingChatUserIds }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("chat_list_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar (Searches by Name or Mobile Number)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("नाव किंवा मोबाईल नंबर टाकून शोधा...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SaffronPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("chat_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = true
                    )

                    // Secondary Tab Row: [चॅट्स] [सर्व सदस्य (Total)]
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SaffronPrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("चॅट्स (${summaries.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("सर्व सदस्य (${otherMembers.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }
            }

            // Tab 0: Active Chats & Quick Contact Search
            if (selectedTab == 0) {
                if (filteredSummaries.isEmpty() && searchQuery.isNotBlank()) {
                    // When searching and no previous conversation matched, show all matching members directly
                    if (filteredAllMembers.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronLight.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🔍 नाव किंवा नंबरनुसार सापडलेले एकूण सदस्य (${filteredAllMembers.size}):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SaffronDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            items(filteredAllMembers, key = { it.id }) { member ->
                                ChatMemberPickCard(
                                    member = member,
                                    onClick = { viewModel.openChatWith(member) }
                                )
                            }
                        }
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.PersonSearch,
                            title = "'$searchQuery' साठी कोणीही सदस्य आढळले नाही",
                            subtitle = "कृपया अचूक नाव किंवा मोबाईल नंबर टाकून पुन्हा प्रयत्न करा."
                        )
                    }
                } else if (filteredSummaries.isEmpty() && searchQuery.isBlank()) {
                    // No active chats yet -> Show all members to start chat right away
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SaffronLight.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = SaffronPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "मंडळातील एकूण सदस्य: ${otherMembers.size}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "चॅट सुरू करण्यासाठी कोणत्याही सदस्यावर टॅप करा किंवा नाव/नंबरने शोधा.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(otherMembers, key = { it.id }) { member ->
                                ChatMemberPickCard(
                                    member = member,
                                    onClick = { viewModel.openChatWith(member) }
                                )
                            }
                        }
                    }
                } else {
                    // List active conversation rows + any extra matching members if searching
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                    ) {
                        items(filteredSummaries, key = { it.otherUser.id }) { summary ->
                            ChatConversationRow(
                                summary = summary,
                                onClick = { viewModel.openChatWith(summary.otherUser) }
                            )
                            HorizontalDivider(
                                color = DividerColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 74.dp)
                            )
                        }

                        // If searching, also append any other matching members who don't have a chat thread yet
                        if (searchQuery.isNotBlank() && matchingMembersWithoutSummary.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    color = BackgroundWarm,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "इतर सापडलेले मंडळ सदस्य (${matchingMembersWithoutSummary.size}):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                }
                            }

                            items(matchingMembersWithoutSummary, key = { "extra_${it.id}" }) { member ->
                                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                    ChatMemberPickCard(
                                        member = member,
                                        onClick = { viewModel.openChatWith(member) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: All Members List (सर्व सदस्य) with Live Count and Search
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = SurfaceWarm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "एकूण सदस्य: ${otherMembers.size}" else "सापडलेले सदस्य: ${filteredAllMembers.size} / ${otherMembers.size}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "नाव / नंबरने शोधता येते",
                                style = MaterialTheme.typography.bodySmall,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (filteredAllMembers.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.PersonSearch,
                            title = "'$searchQuery' नाव किंवा नंबरचा सदस्य आढळला नाही",
                            subtitle = "कृपया दुसरा नंबर किंवा नाव टाकून शोधा."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAllMembers, key = { it.id }) { member ->
                                ChatMemberPickCard(
                                    member = member,
                                    onClick = { viewModel.openChatWith(member) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }

        // New Chat FAB
        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = SaffronPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(imageVector = Icons.Default.Chat, contentDescription = "नवीन चॅट")
        }

        // Member Selector Dialog with Search Box
        if (showNewChatDialog) {
            var dialogSearchQuery by remember { mutableStateOf("") }
            val dialogFilteredMembers = remember(otherMembers, dialogSearchQuery) {
                if (dialogSearchQuery.isBlank()) {
                    otherMembers
                } else {
                    val q = dialogSearchQuery.trim()
                    otherMembers.filter {
                        it.fullName.contains(q, ignoreCase = true) ||
                                it.mobileNumber.contains(q)
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("नवीन चॅट सुरू करा", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SaffronLight.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "${otherMembers.size} सदस्य",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SaffronDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dialog Search input
                        OutlinedTextField(
                            value = dialogSearchQuery,
                            onValueChange = { dialogSearchQuery = it },
                            placeholder = { Text("नाव किंवा मोबाईल नंबर शोधा...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (dialogSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { dialogSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        if (dialogFilteredMembers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "कोणताही सदस्य आढळला नाही",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(dialogFilteredMembers, key = { it.id }) { member ->
                                    ChatMemberPickCard(
                                        member = member,
                                        onClick = {
                                            showNewChatDialog = false
                                            viewModel.openChatWith(member)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("बंद करा")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatConversationRow(
    summary: com.example.data.model.ChatConversationSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatar(
            photoUrl = summary.otherUser.profilePhotoUrl,
            name = summary.otherUser.fullName,
            size = 50
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.otherUser.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestampToMarathi(summary.lastTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.unreadCount > 0) SaffronPrimary else TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "📱 ${summary.otherUser.mobileNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (summary.unreadCount > 0) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (summary.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(SaffronPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${summary.unreadCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMemberPickCard(member: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(photoUrl = member.profilePhotoUrl, name = member.fullName, size = 48)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (member.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SaffronPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "👑 ॲडमिन",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = member.mobileNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (member.bloodGroup.isNotBlank()) {
                        Text(
                            text = " • ${member.bloodGroup}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SaffronPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SuccessGreen.copy(alpha = 0.12f),
                modifier = Modifier.clickable { onClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "चॅट करा",
                        tint = SuccessGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "चॅट",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        }
    }
}

// 1-on-1 Fullscreen WhatsApp-style Chat Room
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: MandalViewModel,
    partner: User,
    onBack: () -> Unit
) {
    val messages by viewModel.activeConversationMessages.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val approvedMembers by viewModel.approvedMembers.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var showDocDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Direct Gallery Launchers
    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendChatMessage(
                text = "",
                attachmentType = "IMAGE",
                attachmentUrl = uri.toString()
            )
        }
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendChatMessage(
                text = "",
                attachmentType = "VIDEO",
                attachmentUrl = uri.toString(),
                attachmentName = "गॅलरी व्हिडिओ",
                attachmentExtra = ""
            )
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = partner.fullName,
                subtitle = "मोबाईल: ${partner.mobileNumber}",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Action Icon
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier.testTag("chat_attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("संदेश लिहा (Type message)...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_detail_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPhotoDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Photo",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(text = inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("chat_detail_send_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SaffronPrimary,
                            contentColor = Color.White
                        ) {
                            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "🚩 ${partner.fullName} यांच्याशी चॅट सुरू करा!\nयेथे एकमेकांशी संवाद, फोटो, व्हिडिओ, डॉक्युमेंट्स आणि कॉन्टॅक्ट शेअर करा.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUser?.id
                        ChatBubble(
                            message = msg,
                            isSentByMe = isMe,
                            onImageClick = { previewImageUrl = it }
                        )
                    }
                }
            }
        }
    }

    // Attachment Options Bottom Sheet
    if (showAttachmentMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "अटॅचमेंट पाठवा (Share & Attach)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Photo
                    AttachmentItemOption(
                        icon = Icons.Default.Image,
                        label = "गॅलरी फोटो",
                        color = Color(0xFFE11D48),
                        onClick = {
                            showAttachmentMenu = false
                            photoGalleryLauncher.launch("image/*")
                        }
                    )

                    // Video
                    AttachmentItemOption(
                        icon = Icons.Default.Videocam,
                        label = "गॅलरी व्हिडिओ",
                        color = Color(0xFF7C3AED),
                        onClick = {
                            showAttachmentMenu = false
                            videoGalleryLauncher.launch("video/*")
                        }
                    )

                    // Document
                    AttachmentItemOption(
                        icon = Icons.Default.Description,
                        label = "दस्तावेज (Docs)",
                        color = Color(0xFF2563EB),
                        onClick = {
                            showAttachmentMenu = false
                            showDocDialog = true
                        }
                    )

                    // Contact
                    AttachmentItemOption(
                        icon = Icons.Default.ContactPhone,
                        label = "संपर्क (Contact)",
                        color = Color(0xFF059669),
                        onClick = {
                            showAttachmentMenu = false
                            showContactDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = {
                            showAttachmentMenu = false
                            showPhotoDialog = true
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("फोटो पर्याय (Photo Options)", fontSize = 12.sp, color = SaffronPrimary)
                    }

                    TextButton(
                        onClick = {
                            showAttachmentMenu = false
                            showVideoDialog = true
                        }
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("व्हिडिओ लिंक / माहिती", fontSize = 12.sp, color = SaffronPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 1. Photo Attachment Dialog
    if (showPhotoDialog) {
        var photoUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80") }
        var photoCaption by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("फोटो पाठवा (Send Photo)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            photoGalleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("मोबाईल गॅलरीतून निवडा (Choose from Gallery)")
                    }

                    HorizontalDivider(color = DividerColor)
                    Text("किंवा फोटो URL / टेम्पलेट निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    GalleryImagePicker(
                        onImageSelected = { photoUrl = it },
                        selectedImageUrl = photoUrl
                    )

                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text("फोटो URL (Image URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = photoCaption,
                        onValueChange = { photoCaption = it },
                        label = { Text("कॅप्शन किंवा मेसेज (Caption)") },
                        placeholder = { Text("फोटोबद्दल माहिती...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (photoUrl.isNotBlank()) {
                            viewModel.sendChatMessage(
                                text = photoCaption,
                                attachmentType = "IMAGE",
                                attachmentUrl = photoUrl
                            )
                            showPhotoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("पाठवा (Send)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // 2. Video Attachment Dialog
    if (showVideoDialog) {
        var videoTitle by remember { mutableStateOf("मंडळ गणेशोत्सव आरती व उत्सव व्हिडिओ") }
        var videoUrl by remember { mutableStateOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ") }
        var thumbUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80") }
        var videoMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            title = { Text("व्हिडिओ पाठवा (Send Video)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showVideoDialog = false
                            videoGalleryLauncher.launch("video/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("मोबाईल गॅलरीतून व्हिडिओ निवडा")
                    }

                    HorizontalDivider(color = DividerColor)
                    Text("किंवा व्हिडिओ लिंक / माहिती टाका:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = { videoTitle = it },
                        label = { Text("व्हिडिओचे नाव (Title)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("व्हिडिओ लिंक (YouTube / MP4 URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = thumbUrl,
                        onValueChange = { thumbUrl = it },
                        label = { Text("थंबनेल इमेज URL (Thumbnail)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = videoMsg,
                        onValueChange = { videoMsg = it },
                        label = { Text("संदेश (Message text)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoUrl.isNotBlank()) {
                            viewModel.sendChatMessage(
                                text = videoMsg,
                                attachmentType = "VIDEO",
                                attachmentUrl = videoUrl,
                                attachmentName = videoTitle,
                                attachmentExtra = thumbUrl
                            )
                            showVideoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("पाठवा (Send)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // 3. Document Attachment Dialog
    if (showDocDialog) {
        val sampleDocs = listOf(
            Triple("मंडळ नियमावली व घटना २०२६.pdf", "PDF • 1.8 MB", "https://jayhindmandal.org/docs/bylaws.pdf"),
            Triple("गणेशोत्सव नियोजन व कार्यक्रम अहवाल.pdf", "PDF • 3.2 MB", "https://jayhindmandal.org/docs/ganeshotsav.pdf"),
            Triple("वार्षिक जमा-खर्च हिशोब अहवाल.pdf", "PDF • 2.1 MB", "https://jayhindmandal.org/docs/accounts.pdf"),
            Triple("क्रीडा महोत्सव वेळापत्रक व नियम.pdf", "PDF • 950 KB", "https://jayhindmandal.org/docs/sports.pdf")
        )
        var selectedDoc by remember { mutableStateOf(sampleDocs.first()) }
        var customDocName by remember { mutableStateOf("") }
        var customDocUrl by remember { mutableStateOf("https://jayhindmandal.org/docs/document.pdf") }

        AlertDialog(
            onDismissRequest = { showDocDialog = false },
            title = { Text("दस्तावेज पाठवा (Send Document)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("उपलब्ध मंडळ दस्तऐवज निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    sampleDocs.forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDoc == doc && customDocName.isBlank()) SaffronLight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedDoc == doc && customDocName.isBlank()) SaffronPrimary else DividerColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDoc = doc
                                    customDocName = ""
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BloodRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(doc.first, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(doc.second, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("किंवा कस्टम दस्तऐवज नाव टाका:", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = customDocName,
                        onValueChange = { customDocName = it },
                        label = { Text("दस्तऐवजाचे नाव") },
                        placeholder = { Text("उदा. अहवाल_२०२६.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val docName = if (customDocName.isNotBlank()) customDocName else selectedDoc.first
                        val docExtra = if (customDocName.isNotBlank()) "PDF Document • 1.5 MB" else selectedDoc.second
                        val docUrl = if (customDocName.isNotBlank()) customDocUrl else selectedDoc.third

                        viewModel.sendChatMessage(
                            text = "",
                            attachmentType = "DOCUMENT",
                            attachmentUrl = docUrl,
                            attachmentName = docName,
                            attachmentExtra = docExtra
                        )
                        showDocDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("पाठवा (Send Doc)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // 4. Contact Attachment Dialog
    if (showContactDialog) {
        var contactSearch by remember { mutableStateOf("") }
        var customContactName by remember { mutableStateOf("") }
        var customContactPhone by remember { mutableStateOf("") }

        val filteredContactMembers = remember(approvedMembers, contactSearch) {
            if (contactSearch.isBlank()) {
                approvedMembers
            } else {
                val q = contactSearch.trim()
                approvedMembers.filter {
                    it.fullName.contains(q, ignoreCase = true) || it.mobileNumber.contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("संपर्क क्रमांक पाठवा (Share Contact)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("मंडळातील सदस्याचा संपर्क निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = contactSearch,
                        onValueChange = { contactSearch = it },
                        placeholder = { Text("सदस्य नाव किंवा नंबर शोधा...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredContactMembers, key = { it.id }) { member ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendChatMessage(
                                            text = "",
                                            attachmentType = "CONTACT",
                                            attachmentUrl = member.mobileNumber,
                                            attachmentName = member.fullName,
                                            attachmentExtra = member.mobileNumber
                                        )
                                        showContactDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MemberAvatar(photoUrl = member.profilePhotoUrl, name = member.fullName, size = 36)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(member.mobileNumber, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SaffronPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = SaffronDark, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("पाठवा", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SaffronDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = DividerColor)
                    Text("किंवा इतर कोणताही संपर्क क्रमांक लिहा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = customContactName,
                        onValueChange = { customContactName = it },
                        label = { Text("नाव (Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customContactPhone,
                        onValueChange = { customContactPhone = it },
                        label = { Text("मोबाईल नंबर (Phone Number)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customContactName.isNotBlank() && customContactPhone.isNotBlank()) {
                            viewModel.sendChatMessage(
                                text = "",
                                attachmentType = "CONTACT",
                                attachmentUrl = customContactPhone,
                                attachmentName = customContactName,
                                attachmentExtra = customContactPhone
                            )
                            showContactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("पाठवा (Send)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // Fullscreen Photo Preview
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = "Fullscreen Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun AttachmentItemOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextPrimary)
    }
}
