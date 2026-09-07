package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.ChatConversationSummary
import com.example.ui.components.VideoPlayerDialog
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderHelper
import com.example.util.FirebaseStorageHelper
import com.example.util.MediaUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(viewModel: MandalViewModel) {
    val summaries by viewModel.chatSummaries.collectAsStateWithLifecycle()
    val approvedMembers by viewModel.approvedMembers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val groupMessages by viewModel.groupChatMessages.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: All Members
    var showNewChatDialog by remember { mutableStateOf(false) }

    val otherMembers = remember(approvedMembers, currentUser) {
        approvedMembers.filter { it.id != currentUser?.id }
    }

    val lastGroupMessage = remember(groupMessages) {
        groupMessages.lastOrNull()
    }

    // Filter active chat summaries by Name, Mobile Number, or Message text
    val filteredSummaries = remember(summaries, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            summaries
        } else {
            val q = searchQuery.trim()
            summaries.filter {
                it.otherUser.fullName.contains(q, ignoreCase = true) ||
                        it.otherUser.mobileNumber.contains(q) ||
                        it.lastMessage.contains(q, ignoreCase = true)
            }
        }
        list.distinctBy { it.otherUser.id }
    }

    // Filter ALL mandal members by Name or Mobile Number
    val filteredAllMembers = remember(otherMembers, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            otherMembers
        } else {
            val q = searchQuery.trim()
            otherMembers.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                        it.mobileNumber.contains(q) ||
                        it.bloodGroup.contains(q, ignoreCase = true)
            }
        }
        list.distinctBy { it.id }
    }

    // Members who don't have an active conversation yet matching search
    val matchingMembersWithoutSummary = remember(filteredAllMembers, summaries) {
        val existingChatUserIds = summaries.map { it.otherUser.id }.toSet()
        filteredAllMembers.filter { it.id !in existingChatUserIds }.distinctBy { it.id }
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

                    // Secondary Tab Row: [चॅट्स] [सर्व सदस्य]
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
                                    Text("चॅट्स (${summaries.size + 1})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
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

            // Tab 0: Active Chats & Pinned Mandal Community Group
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                ) {
                    // 1. PINNED OFFICIAL MANDAL GROUP CHAT
                    if (searchQuery.isBlank() || "जय हिंद मंडळ सर्व सदस्य ग्रुप".contains(searchQuery, ignoreCase = true)) {
                        item {
                            MandalGroupChatPinnedCard(
                                totalMembers = approvedMembers.size,
                                lastMessage = lastGroupMessage,
                                onClick = { viewModel.openGroupChat() }
                            )
                            HorizontalDivider(
                                color = DividerColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (filteredSummaries.isEmpty() && searchQuery.isNotBlank()) {
                        // When searching and no 1-on-1 chat matched
                        if (filteredAllMembers.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronLight.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🔍 सापडलेले सदस्य (${filteredAllMembers.size}):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SaffronDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            items(filteredAllMembers, key = { it.id }) { member ->
                                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                    ChatMemberPickCard(
                                        member = member,
                                        onClick = { viewModel.openChatWith(member) }
                                    )
                                }
                            }
                        } else {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.PersonSearch,
                                    title = "'$searchQuery' साठी कोणीही सदस्य आढळले नाही",
                                    subtitle = "कृपया अचूक नाव किंवा मोबाईल नंबर टाकून पुन्हा प्रयत्न करा."
                                )
                            }
                        }
                    } else {
                        // Active 1-on-1 Conversations
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SaffronPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("नवीन चॅट सुरू करा", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        OutlinedTextField(
                            value = dialogSearchQuery,
                            onValueChange = { dialogSearchQuery = it },
                            placeholder = { Text("नाव किंवा नंबर शोधा...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SaffronPrimary) },
                            trailingIcon = {
                                if (dialogSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { dialogSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        // Option to open Mandal Group Chat directly
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SaffronLight.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showNewChatDialog = false
                                    viewModel.openGroupChat()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SaffronPrimary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🚩 जय हिंद मंडळ - सर्व सदस्य ग्रुप", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SaffronDark)
                                    Text("सर्व सदस्यांसोबत ग्रुप चर्चा करा", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("किंवा वैयक्तिक सदस्य निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(dialogFilteredMembers, key = { it.id }) { member ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNewChatDialog = false
                                            viewModel.openChatWith(member)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MemberAvatar(photoUrl = member.profilePhotoUrl, name = member.fullName, size = 36)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(member.mobileNumber, fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("बंद करा")
                    }
                }
            )
        }
    }
}

@Composable
fun MandalGroupChatPinnedCard(
    totalMembers: Int,
    lastMessage: ChatMessage?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, SaffronPrimary.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("group_chat_card")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Icon with gradient badge
            Box {
                Surface(
                    shape = CircleShape,
                    color = SaffronPrimary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Group",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                ) {}
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚩 जय हिंद मंडळ सर्व सदस्य",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SaffronPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ग्रुप चॅट",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "सर्व $totalMembers सभासद • अधिकृत मंडळ मंच",
                    style = MaterialTheme.typography.bodySmall,
                    color = SaffronDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(3.dp))

                val previewText = when {
                    lastMessage == null -> "येथे सर्व सदस्य एकत्र चर्चा व माहिती शेअर करू शकतात..."
                    lastMessage.attachmentType == "VOICE" -> "🎙️ ${lastMessage.senderName}: व्हॉईस संदेश"
                    lastMessage.attachmentType == "DOCUMENT" -> "📄 ${lastMessage.senderName}: ${lastMessage.attachmentName ?: "दस्तावेज"}"
                    lastMessage.attachmentType == "IMAGE" -> "📷 ${lastMessage.senderName}: फोटो"
                    lastMessage.attachmentType == "VIDEO" -> "🎥 ${lastMessage.senderName}: व्हिडिओ"
                    else -> "${lastMessage.senderName}: ${lastMessage.messageText}"
                }

                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatConversationRow(
    summary: ChatConversationSummary,
    onClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(summary.lastTimestamp) { timeFormatter.format(Date(summary.lastTimestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("chat_conversation_${summary.otherUser.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatar(
            photoUrl = summary.otherUser.profilePhotoUrl,
            name = summary.otherUser.fullName,
            size = 50
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.otherUser.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formattedTime,
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
                                text = "👑 मुख्य ॲडमिन",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (member.isContentAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✍️ कन्टेन्ट ॲडमिन",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
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

// Fullscreen WhatsApp-style Chat Room (Supports 1-on-1 & Mandal Group Chat)
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isGroupChat = partner.id == "GROUP_MANDAL"

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }
    var playingVideoMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Camera Capture States
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPhotoUriForPreview by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoCaption by remember { mutableStateOf("") }
    var isSendingCameraPhoto by remember { mutableStateOf(false) }

    // Media Upload Progress States (WhatsApp Style)
    var isUploadingMedia by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadStatusText by remember { mutableStateOf("") }

    // 1. Camera Launcher & Permissions
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            capturedPhotoUriForPreview = cameraPhotoUri
            cameraPhotoCaption = ""
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
                val photoFile = File(photoDir, "cam_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraPhotoUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "कॅमेरा सुरू करता आला नाही: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "कॅमेऱ्याचा वापर करण्यासाठी परवानगी आवश्यक आहे", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val photoDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
                val photoFile = File(photoDir, "cam_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraPhotoUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "कॅमेरा सुरू करता आला नाही: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 2. Direct Contacts Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            val details = MediaUtils.getContactDetailsFromUri(context, contactUri)
            if (details != null) {
                val (cName, cPhone) = details
                viewModel.sendChatMessage(
                    text = "",
                    attachmentType = "CONTACT",
                    attachmentName = cName,
                    attachmentUrl = cPhone,
                    attachmentExtra = cPhone
                )
                Toast.makeText(context, "✅ संपर्क पाठवला: $cName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "संपर्काची माहिती मिळाली नाही", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "मोबाईलमधील संपर्क वाचण्यासाठी परवानगी आवश्यक आहे", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchContactPicker() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            contactPickerLauncher.launch(null)
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // 3. Direct Gallery Launchers
    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isUploadingMedia = true
                    uploadStatusText = "फोटो कॉम्प्रेस व पाठवत आहे..."
                    uploadProgress = 0
                    val currentSenderName = currentUser?.fullName?.ifBlank { "Member" } ?: "Member"
                    val downloadUrl = FirebaseStorageHelper.uploadImage(
                        context = context,
                        uri = uri,
                        folder = "chat_media/images",
                        senderName = currentSenderName
                    ) { prog ->
                        uploadProgress = prog
                    }
                    viewModel.sendChatMessage(
                        text = "",
                        attachmentType = "IMAGE",
                        attachmentUrl = downloadUrl
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "फोटो पाठवण्यात त्रुटी आली", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingMedia = false
                }
            }
        }
    }

    // 4. Direct Document / PDF Launcher
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isUploadingMedia = true
                    uploadStatusText = "कागदपत्र अपलोड होत आहे..."
                    uploadProgress = 0
                    var fileName = "दस्तावेज.pdf"
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex != -1) {
                                fileName = cursor.getString(nameIndex) ?: fileName
                            }
                        }
                    } catch (_: Exception) {}

                    val currentSenderName = currentUser?.fullName?.ifBlank { "Member" } ?: "Member"
                    val (docUrl, sizeLabel) = FirebaseStorageHelper.uploadDocument(
                        context = context,
                        uri = uri,
                        fileName = fileName,
                        senderName = currentSenderName
                    ) { prog ->
                        uploadProgress = prog
                    }
                    viewModel.sendChatMessage(
                        text = "",
                        attachmentType = "DOCUMENT",
                        attachmentUrl = docUrl,
                        attachmentName = fileName,
                        attachmentExtra = sizeLabel
                    )
                    Toast.makeText(context, "✅ कागदपत्र पाठवले!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "कागदपत्र पाठवता आले नाही", Toast.LENGTH_LONG).show()
                } finally {
                    isUploadingMedia = false
                }
            }
        }
    }

    // Voice Recording Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = AudioRecorderHelper.startRecording(context)
            if (!started) {
                Toast.makeText(context, "रेकॉर्डिंग सुरू करण्यात अयशस्वी", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "व्हॉईस मेसेजसाठी मायक्रोफोन परवानगी आवश्यक आहे", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val started = AudioRecorderHelper.startRecording(context)
            if (!started) {
                Toast.makeText(context, "रेकॉर्डिंग सुरू करण्यात अयशस्वी", Toast.LENGTH_SHORT).show()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun finishVoiceRecordingAndSend() {
        val result = AudioRecorderHelper.stopRecording()
        if (result != null) {
            val (file, durationMillis) = result
            val seconds = (durationMillis / 1000).toInt().coerceAtLeast(1)
            val durationLabel = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
            scope.launch {
                try {
                    isUploadingMedia = true
                    uploadStatusText = "व्हॉईस संदेश पाठवत आहे..."
                    uploadProgress = 0
                    val currentSenderName = currentUser?.fullName?.ifBlank { "Member" } ?: "Member"
                    val audioUrl = FirebaseStorageHelper.uploadAudio(
                        context = context,
                        audioFile = file,
                        senderName = currentSenderName
                    ) { prog ->
                        uploadProgress = prog
                    }
                    if (audioUrl.isNotBlank()) {
                        viewModel.sendChatMessage(
                            text = "",
                            attachmentType = "VOICE",
                            attachmentUrl = audioUrl,
                            attachmentName = "व्हॉईस संदेश",
                            attachmentExtra = durationLabel
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "व्हॉईस मेसेज पाठवता आला नाही", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingMedia = false
                }
            }
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerManager.stop()
            AudioRecorderHelper.cancelRecording()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = SaffronPrimary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    if (isGroupChat) {
                        Surface(
                            shape = CircleShape,
                            color = SaffronDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        MemberAvatar(photoUrl = partner.profilePhotoUrl, name = partner.fullName, size = 40)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGroupChat) "🚩 जय हिंद मंडळ सर्व सदस्य" else partner.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isGroupChat) "${approvedMembers.size} सभासद • अधिकृत कम्युनिटी मंच" else "📱 ${partner.mobileNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (AudioRecorderHelper.isRecording) {
                    // LIVE VOICE RECORDING BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Cancel Button
                        IconButton(
                            onClick = { AudioRecorderHelper.cancelRecording() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Cancel Recording",
                                tint = BloodRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Glowing Recording Indicator & Duration
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "recDot")
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
                                label = "dotPulse"
                            )

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(BloodRed.copy(alpha = dotAlpha))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val sec = AudioRecorderHelper.recordingDurationSeconds
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d रेकॉर्डिंग...", sec / 60, sec % 60),
                                fontWeight = FontWeight.Bold,
                                color = BloodRed,
                                fontSize = 14.sp
                            )
                        }

                        // Send Voice Note Button
                        Button(
                            onClick = { finishVoiceRecordingAndSend() },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Audio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // STANDARD CHAT INPUT BAR
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
                            placeholder = {
                                Text(
                                    if (isGroupChat) "ग्रुपमध्ये संदेश लिहा..." else "संदेश लिहा (Type message)...",
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_detail_input"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = DividerColor
                            ),
                            trailingIcon = {
                                IconButton(onClick = { launchCamera() }) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (inputText.isNotBlank()) {
                            // Text Send Button
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
                        } else {
                            // Voice Note Recording Mic Button
                            IconButton(
                                onClick = { startVoiceRecording() },
                                modifier = Modifier.testTag("chat_voice_mic_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SaffronPrimary,
                                    contentColor = Color.White
                                ) {
                                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Record Voice Note",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SaffronLight.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isGroupChat) Icons.Default.Groups else Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isGroupChat) "🚩 जय हिंद मंडळ - सर्व सदस्य ग्रुप\nयेथे सर्व सदस्य एकत्र चर्चा करू शकतात, व्हॉईस नोट्स, फोटो व डॉक्युमेंट्स पाठवू शकतात." else "🚩 ${partner.fullName} यांच्याशी चॅट सुरू करा!\nयेथे एकमेकांशी संवाद, व्हॉईस नोट्स, फोटो, आणि डॉक्युमेंट्स शेअर करा.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val distinctMessages = remember(messages) { messages.distinctBy { it.id } }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    items(distinctMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUser?.id
                        ChatBubble(
                            message = msg,
                            isSentByMe = isMe,
                            isGroupChat = isGroupChat,
                            isAdmin = currentUser?.isAnyAdmin == true,
                            onImageClick = { previewImageUrl = it },
                            onVideoClick = { playingVideoMessage = it },
                            onDeleteClick = { viewModel.deleteChatMessage(it) }
                        )
                    }
                }
            }

            // WhatsApp-style Media Uploading Indicator
            if (isUploadingMedia) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { (uploadProgress / 100f).coerceIn(0f, 1f) },
                            color = SaffronPrimary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uploadStatusText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uploadProgress > 0) {
                                Text(
                                    text = "$uploadProgress%",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
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
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "अटॅचमेंट पाठवा (Share & Attach)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera (Photo)
                    AttachmentItemOption(
                        icon = Icons.Default.CameraAlt,
                        label = "कॅमेरा",
                        color = Color(0xFFD97706),
                        onClick = {
                            showAttachmentMenu = false
                            launchCamera()
                        }
                    )

                    // Photo Gallery
                    AttachmentItemOption(
                        icon = Icons.Default.Image,
                        label = "गॅलरी",
                        color = Color(0xFFE11D48),
                        onClick = {
                            showAttachmentMenu = false
                            photoGalleryLauncher.launch("image/*")
                        }
                    )

                    // Document / PDF
                    AttachmentItemOption(
                        icon = Icons.Default.PictureAsPdf,
                        label = "दस्तावेज/PDF",
                        color = Color(0xFF2563EB),
                        onClick = {
                            showAttachmentMenu = false
                            try {
                                docPickerLauncher.launch("application/pdf")
                            } catch (_: Exception) {
                                docPickerLauncher.launch("*/*")
                            }
                        }
                    )

                    // Contact
                    AttachmentItemOption(
                        icon = Icons.Default.ContactPhone,
                        label = "संपर्क",
                        color = Color(0xFF059669),
                        onClick = {
                            showAttachmentMenu = false
                            showContactDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Real-Time Camera Photo Preview Dialog (WhatsApp Style)
    if (capturedPhotoUriForPreview != null) {
        Dialog(
            onDismissRequest = {
                if (!isSendingCameraPhoto) {
                    capturedPhotoUriForPreview = null
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
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
                            text = "📸 फोटो प्रिव्ह्यू (Photo Preview)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { capturedPhotoUriForPreview = null },
                            enabled = !isSendingCameraPhoto
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Captured Photo Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = capturedPhotoUriForPreview,
                            contentDescription = "Camera Captured Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caption TextField
                    OutlinedTextField(
                        value = cameraPhotoCaption,
                        onValueChange = { cameraPhotoCaption = it },
                        placeholder = { Text("फोटोबद्दल काहीतरी लिहा (कॅप्शन)...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Retake / Send)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedPhotoUriForPreview = null
                                launchCamera()
                            },
                            enabled = !isSendingCameraPhoto,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("पुन्हा काढा")
                        }

                        Button(
                            onClick = {
                                val uri = capturedPhotoUriForPreview
                                if (uri != null && !isSendingCameraPhoto) {
                                    scope.launch {
                                        isSendingCameraPhoto = true
                                        val currentSenderName = currentUser?.fullName?.ifBlank { "Member" } ?: "Member"
                                        val photoUrl = FirebaseStorageHelper.uploadImage(
                                            context = context,
                                            uri = uri,
                                            folder = "chat_media/images",
                                            senderName = currentSenderName
                                        )
                                        viewModel.sendChatMessage(
                                            text = cameraPhotoCaption.trim(),
                                            attachmentType = "IMAGE",
                                            attachmentUrl = photoUrl
                                        )
                                        isSendingCameraPhoto = false
                                        capturedPhotoUriForPreview = null
                                        Toast.makeText(context, "✅ फोटो पाठवला!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSendingCameraPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSendingCameraPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("पाठवा (Send)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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

    // 2. Contact Attachment Dialog
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
                    Button(
                        onClick = {
                            showContactDialog = false
                            launchContactPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PermContactCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📱 मोबाईल संपर्कातून निवडा (Phone Contacts)", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = DividerColor)
                    Text("किंवा मंडळातील सदस्याचा संपर्क निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

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
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                UniversalAsyncImage(
                    model = previewImageUrl ?: "",
                    contentDescription = "Fullscreen Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Top Controls: Close and Save to Gallery
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = {
                                previewImageUrl?.let { url ->
                                    scope.launch {
                                        isSavingPhoto = true
                                        val success = MediaUtils.saveImageToGallery(
                                            context = context,
                                            imageUrlOrBase64 = url,
                                            fileNamePrefix = "JayHind_ChatPhoto",
                                            subFolder = "JayHind_Mandal_Chat"
                                        )
                                        isSavingPhoto = false
                                        if (success) {
                                            Toast.makeText(context, "फोटो गॅलरीमध्ये सेव्ह केला! (Pictures/JayHind_Mandal_Chat)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "फोटो सेव्ह करण्यात अयशस्वी झाले.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isSavingPhoto
                        ) {
                            if (isSavingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Photo",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Close Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        IconButton(onClick = { previewImageUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // In-App Video Player Dialog (WhatsApp-style)
    if (playingVideoMessage != null) {
        val videoMsg = playingVideoMessage!!
        VideoPlayerDialog(
            videoUrl = videoMsg.attachmentUrl ?: "",
            title = videoMsg.attachmentName ?: if (videoMsg.messageText.isNotBlank()) videoMsg.messageText else "व्हिडिओ",
            senderName = videoMsg.senderName,
            thumbnailUrl = videoMsg.attachmentExtra,
            onDismiss = { playingVideoMessage = null }
        )
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
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextPrimary)
    }
}
