package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post?,
    comments: List<Comment>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onAddComment: (text: String, parentId: String?, replyToAuthorName: String?) -> Unit,
    onToggleLike: (commentId: String) -> Unit,
    onEditComment: (commentId: String, newText: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit
) {
    if (post == null) return
    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "प्रतिक्रिया (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "सर्व सदस्यांच्या प्रतिक्रिया व मते",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा")
                }
            }

            HorizontalDivider(color = DividerColor)

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💬",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "अद्याप कोणतीही प्रतिक्रिया नाही.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "पहिली प्रतिक्रिया देऊन चर्चेत सहभागी व्हा!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        val isLiked = currentUser?.let { comment.isLikedBy(it.id) } == true
                        val isAuthor = currentUser?.id == comment.authorId
                        val isAdmin = currentUser?.isAnyAdmin == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            MemberAvatar(
                                photoUrl = comment.authorPhotoUrl,
                                name = comment.authorName,
                                size = 38
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (comment.parentId != null) SurfaceVariantWarm.copy(alpha = 0.65f) else SurfaceVariantWarm,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        // Top Row: Author Name & Timestamp
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = comment.authorName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (comment.isEdited) {
                                                    Text(
                                                        text = "(संपादित) • ",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    text = formatTimestampToMarathi(comment.timestamp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        // Replying to badge if this is a reply
                                        if (!comment.replyToAuthorName.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SaffronPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                                        contentDescription = null,
                                                        tint = SaffronDark,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "@${comment.replyToAuthorName} यांना उत्तर",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = SaffronDark
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Interactive Action Row: Like, Reply, Edit, Delete
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 6.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // 1. Like Button
                                    Row(
                                        modifier = Modifier
                                            .clickable { onToggleLike(comment.id) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "लाईक करा",
                                            tint = if (isLiked) Color(0xFFE11D48) else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (comment.likesCount > 0) "${comment.likesCount} लाईक" else "लाईक",
                                            fontSize = 12.sp,
                                            fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isLiked) Color(0xFFE11D48) else TextSecondary
                                        )
                                    }

                                    // 2. Reply Button
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                replyingToComment = comment
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = "उत्तर द्या",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "उत्तर द्या",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                    }

                                    // 3. Edit Button (Author only)
                                    if (isAuthor) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { commentToEdit = comment }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "संपादित करा",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "संपादित",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    // 4. Delete Button (Author or Admin)
                                    if (isAuthor || isAdmin) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { commentToDelete = comment }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "हटवा",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isAdmin && !isAuthor) "हटवा (Admin)" else "हटवा",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Replying Banner
            if (replyingToComment != null) {
                Surface(
                    color = SaffronLight.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = SaffronDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "@${replyingToComment?.authorName} यांना उत्तर देत आहात...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SaffronDark
                            )
                        }
                        IconButton(
                            onClick = { replyingToComment = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "रद्द करा",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Input Row - Guaranteed above keyboard
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(
                        photoUrl = currentUser?.profilePhotoUrl ?: "",
                        name = currentUser?.fullName ?: "सभासद",
                        size = 36
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                text = if (replyingToComment != null) "@${replyingToComment?.authorName} यांना उत्तर लिहा..." else "आपली प्रतिक्रिया येथे लिहा...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = DividerColor,
                            focusedContainerColor = SurfaceWarm,
                            unfocusedContainerColor = SurfaceWarm
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(
                                    commentText.trim(),
                                    replyingToComment?.id,
                                    replyingToComment?.authorName
                                )
                                commentText = ""
                                replyingToComment = null
                            }
                        },
                        enabled = commentText.isNotBlank(),
                        modifier = Modifier.testTag("comment_send_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (commentText.isNotBlank()) SaffronPrimary else Color.LightGray,
                            contentColor = Color.White
                        ) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "पाठवा",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Comment Dialog
    if (commentToEdit != null) {
        var editText by remember(commentToEdit) { mutableStateOf(commentToEdit?.text ?: "") }
        AlertDialog(
            onDismissRequest = { commentToEdit = null },
            title = {
                Text(
                    text = "प्रतिक्रिया संपादित करा",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = commentToEdit
                        if (target != null && editText.isNotBlank()) {
                            onEditComment(target.id, editText.trim())
                            commentToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    enabled = editText.isNotBlank()
                ) {
                    Text("बदल सेव्ह करा")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }

    // Delete Confirmation Dialog (Member & Admin)
    if (commentToDelete != null) {
        val target = commentToDelete!!
        val isAdminDelete = currentUser?.isAnyAdmin == true && currentUser.id != target.authorId

        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = {
                Text(
                    text = if (isAdminDelete) "प्रतिक्रिया हटवा (Admin Rights)" else "प्रतिक्रिया हटवायची आहे का?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = if (isAdminDelete)
                        "मंडळ ॲडमिन म्हणून आपण ${target.authorName} यांची ही प्रतिक्रिया हटवत आहात. ही प्रतिक्रिया कायमची हटवली जाईल."
                    else
                        "आपली ही प्रतिक्रिया कायमची हटवली जाईल. पुढे जायचे का?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteComment(target.id)
                        commentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("होय, हटवा")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}
