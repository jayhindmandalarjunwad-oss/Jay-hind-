package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun ForwardMessageDialog(
    message: ChatMessage,
    currentUser: User?,
    approvedMembers: List<User>,
    groupUser: User,
    onDismiss: () -> Unit,
    onForward: (List<User>) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val selectedUsers = remember { mutableStateListOf<User>() }
    val maxLimit = 5

    val otherMembers = remember(approvedMembers, currentUser) {
        approvedMembers.filter { it.id != currentUser?.id }
    }

    // Filter Group & Members
    val isGroupMatch = remember(searchQuery, groupUser) {
        if (searchQuery.isBlank()) true
        else {
            val q = searchQuery.trim()
            groupUser.fullName.contains(q, ignoreCase = true) ||
                    "ग्रुप".contains(q, ignoreCase = true) ||
                    "मंडळ".contains(q, ignoreCase = true) ||
                    "जय हिंद".contains(q, ignoreCase = true)
        }
    }

    val filteredMembers = remember(otherMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            otherMembers
        } else {
            val q = searchQuery.trim()
            otherMembers.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                        it.mobileNumber.contains(q) ||
                        it.designation.contains(q, ignoreCase = true) ||
                        it.bloodGroup.contains(q, ignoreCase = true)
            }
        }
    }

    fun toggleUserSelection(target: User) {
        val alreadySelected = selectedUsers.any { it.id == target.id }
        if (alreadySelected) {
            selectedUsers.removeAll { it.id == target.id }
        } else {
            if (selectedUsers.size >= maxLimit) {
                Toast.makeText(
                    context,
                    "एका वेळी जास्तीत जास्त ५ जणांनाच फॉरवर्ड करू शकता!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                selectedUsers.add(target)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWarm,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("forward_message_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SaffronPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SaffronPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "मेसेज फॉरवर्ड करा",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedUsers.size == maxLimit) "कमाल ५ जण निवडले (मर्यादा पूर्ण)" else "कमाल ५ जणांना फॉरवर्ड करा",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedUsers.size == maxLimit) BloodRed else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Selection Count Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedUsers.size == maxLimit) BloodRed.copy(alpha = 0.15f) else SaffronPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (selectedUsers.size == maxLimit) BloodRed else SaffronPrimary
                        )
                    ) {
                        Text(
                            text = "${selectedUsers.size}/$maxLimit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedUsers.size == maxLimit) BloodRed else SaffronPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CardBorderColor, thickness = 1.dp)

                // 2. Compact Message Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageUrl = message.attachmentUrl.takeIf { message.attachmentType == "IMAGE" } ?: message.imageUrl
                        when {
                            !imageUrl.isNullOrBlank() -> {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                ) {
                                    UniversalAsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Image preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📷 फोटो",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SaffronPrimary
                                    )
                                    if (message.messageText.isNotBlank()) {
                                        Text(
                                            text = message.messageText,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            message.attachmentType == "VIDEO" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BloodRed.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = BloodRed, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎥 व्हिडिओ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BloodRed
                                    )
                                    Text(
                                        text = message.attachmentName ?: "व्हिडिओ संदेश",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            message.attachmentType == "DOCUMENT" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NavySecondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📄 डॉक्युमेंट / PDF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NavySecondary
                                    )
                                    Text(
                                        text = message.attachmentName ?: "कागदपत्र",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            message.attachmentType == "CONTACT" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "👤 संपर्क: ${message.attachmentName ?: "मंडळ संपर्क"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = message.attachmentExtra ?: message.attachmentUrl ?: "",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            message.attachmentType == "VOICE" || message.attachmentType == "AUDIO" -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎙️ व्हॉईस संदेश",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SaffronPrimary
                                    )
                                    Text(
                                        text = message.attachmentExtra ?: "ऑडिओ नोट",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = message.messageText,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("नाव किंवा मोबाईल नंबर शोधा...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = CardBorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // 4. Selected Contacts Chips (WhatsApp Style)
                if (selectedUsers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (target in selectedUsers) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SaffronPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = if (target.id == "GROUP_MANDAL") "मंडळ ग्रुप" else target.fullName.split(" ").firstOrNull() ?: target.fullName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SaffronDark
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { selectedUsers.removeAll { it.id == target.id } },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = BloodRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Contacts List (Group Pinned + Approved Members)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    // Pinned Mandal Group
                    if (isGroupMatch) {
                        item(key = "group_mandal_target") {
                            val isSelected = selectedUsers.any { it.id == groupUser.id }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SaffronPrimary.copy(alpha = 0.10f) else Color.White,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) SaffronPrimary else CardBorderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { toggleUserSelection(groupUser) }
                                    .testTag("forward_target_group")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SaffronPrimary,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🚩 जय हिंद मंडळ (सर्व सदस्य ग्रुप)",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "सर्व सभासदांना एकत्र संदेश पाठवा",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleUserSelection(groupUser) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SaffronPrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Section Divider label if members follow
                    if (filteredMembers.isNotEmpty()) {
                        item {
                            Text(
                                text = "मंडळ सभासद (${filteredMembers.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                    }

                    items(filteredMembers, key = { it.id }) { member ->
                        val isSelected = selectedUsers.any { it.id == member.id }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SaffronPrimary.copy(alpha = 0.10f) else Color.White,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) SaffronPrimary else CardBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleUserSelection(member) }
                                .testTag("forward_target_${member.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (member.profilePhotoUrl.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    ) {
                                        UniversalAsyncImage(
                                            model = member.profilePhotoUrl,
                                            contentDescription = member.fullName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = NavySecondary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = member.fullName.take(1),
                                                fontWeight = FontWeight.Bold,
                                                color = NavySecondary,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.fullName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (member.designation.isNotBlank()) member.designation else member.mobileNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { toggleUserSelection(member) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SaffronPrimary,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (!isGroupMatch && filteredMembers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "कोणताही सभासद सापडला नाही.",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = CardBorderColor, thickness = 1.dp)

                // 6. Action Bottom Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("रद्द करा", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (selectedUsers.isNotEmpty()) {
                                onForward(selectedUsers.toList())
                            }
                        },
                        enabled = selectedUsers.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronPrimary,
                            disabledContainerColor = SaffronPrimary.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedUsers.isEmpty()) "फॉरवर्ड करा" else "फॉरवर्ड (${selectedUsers.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
