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
import com.example.util.MediaUtils
import kotlinx.coroutines.launch
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
    var showVideoDialog by remember { mutableStateOf(false) }
    var showDocDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }
    var playingVideoMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Direct Gallery Launchers
    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val base64 = MediaUtils.uriToBase64(context, uri) ?: uri.toString()
                viewModel.sendChatMessage(
                    text = "",
                    attachmentType = "IMAGE",
                    attachmentUrl = base64
                )
            }
        }
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                Toast.makeText(context, "व्हिडिओ तयार होत आहे...", Toast.LENGTH_SHORT).show()
                val thumbBase64 = MediaUtils.getVideoThumbnailBase64(context, uri) ?: ""
                val videoData = MediaUtils.uriToVideoData(context, uri)
                viewModel.sendChatMessage(
                    text = "",
                    attachmentType = "VIDEO",
                    attachmentUrl = videoData,
                    attachmentName = "व्हिडिओ",
                    attachmentExtra = thumbBase64
                )
            }
        }
    }

    // Direct Document / PDF Launcher
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "दस्तावेज.pdf"
            var fileSize = "PDF Document"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            val mb = sizeBytes / (1024.0 * 1024.0)
                            fileSize = if (mb >= 1.0) String.format(Locale.getDefault(), "PDF • %.1f MB", mb) else String.format(Locale.getDefault(), "PDF • %d KB", sizeBytes / 1024)
                        }
                    }
                }
            } catch (_: Exception) {}

            viewModel.sendChatMessage(
                text = "",
                attachmentType = "DOCUMENT",
                attachmentUrl = uri.toString(),
                attachmentName = fileName,
                attachmentExtra = fileSize
            )
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
            val base64Audio = AudioRecorderHelper.fileToBase64(file)
            if (base64Audio.isNotBlank()) {
                viewModel.sendChatMessage(
                    text = "",
                    attachmentType = "VOICE",
                    attachmentUrl = base64Audio,
                    attachmentName = "व्हॉईस संदेश",
                    attachmentExtra = durationLabel
                )
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
                modifier = Modifier.navigationBarsPadding()
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
                            isGroupChat = isGroupChat,
                            onImageClick = { previewImageUrl = it },
                            onVideoClick = { playingVideoMessage = it }
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
                    // Voice Note
                    AttachmentItemOption(
                        icon = Icons.Default.Mic,
                        label = "व्हॉईस मेसेज",
                        color = Color(0xFFD97706),
                        onClick = {
                            showAttachmentMenu = false
                            startVoiceRecording()
                        }
                    )

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

                    // Document / PDF
                    AttachmentItemOption(
                        icon = Icons.Default.PictureAsPdf,
                        label = "दस्तावेज / PDF",
                        color = Color(0xFF2563EB),
                        onClick = {
                            showAttachmentMenu = false
                            showDocDialog = true
                        }
                    )

                    // Video
                    AttachmentItemOption(
                        icon = Icons.Default.Videocam,
                        label = "व्हिडिओ",
                        color = Color(0xFF7C3AED),
                        onClick = {
                            showAttachmentMenu = false
                            videoGalleryLauncher.launch("video/*")
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

    // 3. Document / PDF Attachment Dialog
    if (showDocDialog) {
        val sampleDocs = listOf(
            Triple("बैठक इतिवृत्त व ठराव अहवाल.pdf", "PDF • 1.2 MB", "https://jayhindmandal.org/docs/minutes.pdf"),
            Triple("गणेशोत्सव नियोजन व कार्यक्रम अहवाल.pdf", "PDF • 3.2 MB", "https://jayhindmandal.org/docs/ganeshotsav.pdf"),
            Triple("मंडळ नियमावली व घटना २०२६.pdf", "PDF • 1.8 MB", "https://jayhindmandal.org/docs/bylaws.pdf"),
            Triple("वार्षिक जमा-खर्च हिशोब अहवाल.pdf", "PDF • 2.1 MB", "https://jayhindmandal.org/docs/accounts.pdf"),
            Triple("क्रीडा महोत्सव वेळापत्रक व नियम.pdf", "PDF • 950 KB", "https://jayhindmandal.org/docs/sports.pdf")
        )
        var selectedDoc by remember { mutableStateOf(sampleDocs.first()) }
        var customDocName by remember { mutableStateOf("") }
        var customDocUrl by remember { mutableStateOf("https://jayhindmandal.org/docs/document.pdf") }

        AlertDialog(
            onDismissRequest = { showDocDialog = false },
            title = { Text("दस्तावेज / PDF पाठवा (Send PDF)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showDocDialog = false
                            docPickerLauncher.launch("application/pdf")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("मोबाईलमधून PDF निवडा (Pick from Device)")
                    }

                    HorizontalDivider(color = DividerColor)
                    Text("किंवा अधिकृत मंडळ दस्तऐवज निवडा:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

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

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("किंवा स्वतःचे नाव टाका:", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = customDocName,
                        onValueChange = { customDocName = it },
                        label = { Text("दस्तऐवजाचे नाव") },
                        placeholder = { Text("उदा. बैठक_इतिवृत्त_२०२६.pdf") },
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
                    .background(Color.Black.copy(alpha = 0.92f)),
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
                                        val success = MediaUtils.saveImageToGallery(context, url, "JayHind_ChatPhoto")
                                        isSavingPhoto = false
                                        if (success) {
                                            Toast.makeText(context, "फोटो गॅलरीमध्ये सेव्ह केला! (Saved to Gallery)", Toast.LENGTH_SHORT).show()
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
