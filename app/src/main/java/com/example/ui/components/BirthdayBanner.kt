package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun BirthdaySection(
    birthdayMembers: List<User>,
    onMemberClick: (User) -> Unit,
    onChatClick: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    if (birthdayMembers.isEmpty()) return
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("birthday_section_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCE7F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = BirthdayPinkDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "आजचे वाढदिवस 🎂",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${birthdayMembers.size} सभासद",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavySecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(birthdayMembers, key = { it.id }) { member ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceVariantWarm,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier
                            .width(160.dp)
                            .clickable { onMemberClick(member) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MemberAvatar(
                                photoUrl = member.profilePhotoUrl,
                                name = member.fullName,
                                size = 52,
                                showPinkCelebrationRing = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "रक्तगट: ${member.bloodGroup}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BloodRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Call
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${member.mobileNumber}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        contentColor = SuccessGreen
                                    ) {
                                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Call, contentDescription = "कॉल", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Wish on Chat
                                IconButton(
                                    onClick = { onChatClick(member) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SaffronContainer,
                                        contentColor = SaffronDark
                                    ) {
                                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Chat, contentDescription = "शुभेच्छा द्या", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
