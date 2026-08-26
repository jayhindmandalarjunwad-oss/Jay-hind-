package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberCard
import com.example.ui.components.MemberDetailSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(viewModel: MandalViewModel) {
    val members by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val selectedBloodGroup by viewModel.selectedBloodGroupFilter.collectAsStateWithLifecycle()
    val selectedMemberForDetail by viewModel.selectedMemberForDetail.collectAsStateWithLifecycle()

    val bloodGroups = listOf("सर्व", "A+", "B+", "AB+", "O+", "A-", "B-", "AB-", "O-")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("members_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Filter Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setMemberSearchQuery(it) },
                        placeholder = { Text("नाव किंवा मोबाईल नंबर शोधा...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = SaffronPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setMemberSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = DividerColor
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "रक्तगट:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bloodGroups) { bg ->
                                val isSelected = selectedBloodGroup == bg
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedBloodGroupFilter(bg) },
                                    label = {
                                        Text(
                                            text = bg,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (bg == "सर्व") SaffronPrimary else BloodRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Members Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "एकूण सभासद: ${members.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                if (selectedBloodGroup != "सर्व") {
                    Text(
                        text = "फिल्टर: $selectedBloodGroup",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = BloodRed
                    )
                }
            }

            // Members List
            if (members.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PersonSearch,
                    title = "सभासद सापडले नाहीत",
                    subtitle = "कृपया नाव, मोबाईल किंवा रक्तगट तपासून पहा.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("members_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        MemberCard(
                            member = member,
                            onCardClick = { viewModel.selectMemberForDetail(member) },
                            onChatClick = { viewModel.openChatWith(member) }
                        )
                    }
                }
            }
        }

        // Member Profile Detail Bottom Sheet
        if (selectedMemberForDetail != null) {
            MemberDetailSheet(
                member = selectedMemberForDetail,
                onDismiss = { viewModel.selectMemberForDetail(null) },
                onChatClick = { partner ->
                    viewModel.selectMemberForDetail(null)
                    viewModel.openChatWith(partner)
                }
            )
        }
    }
}
