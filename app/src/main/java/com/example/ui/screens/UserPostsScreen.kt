package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel

@Composable
fun UserPostsScreen(
    user: User,
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val activePostComments by viewModel.activePostComments.collectAsStateWithLifecycle()
    var postToEdit by remember { mutableStateOf<Post?>(null) }

    // Filter posts for this specific user & sort by timestamp descending (newest date first)
    val userPosts = remember(posts, user.id, user.fullName) {
        posts.filter { post ->
            post.authorId == user.id ||
                    (user.fullName.isNotBlank() && post.authorName.equals(user.fullName, ignoreCase = true))
        }.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("user_posts_screen_root")
    ) {
        // =========================================================================
        // 1. PINNED STICKY HEADER (~ 1/7th screen size, approx 76dp)
        // =========================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⬅️ बॅक बटण
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("user_posts_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "मागे जा",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 👤 गोल प्रोफाईल फोटो
                MemberAvatar(
                    photoUrl = user.profilePhotoUrl,
                    name = user.fullName,
                    size = 46,
                    dateOfBirth = user.dateOfBirth
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 📛 नाव आणि संक्षिप्त ओळख
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val roleText = if (user.designation.isNotBlank()) user.designation else "सभासद"
                    Text(
                        text = "$roleText • अर्जुनवाड",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // =========================================================================
        // 2. TIMELINE FEED: सर्व पोस्ट्स एकाखाली एक (Newest to Oldest)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (userPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "यांनी अद्याप कोणतीही पोस्ट केलेली नाही.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(userPosts, key = { it.id }) { post ->
                        PostItemCard(
                            post = post,
                            currentUser = currentUser,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { viewModel.openComments(post) },
                            onDeleteClick = { viewModel.deletePost(post.id) },
                            onEditClick = { postToEdit = post },
                            onImageClick = { viewModel.openFullscreenPhoto(it) },
                            onMultiImageClick = { images, idx -> viewModel.openFullscreenPhotos(images, idx) },
                            onAuthorClick = null, // Already viewing this user's timeline
                            isAuthorBirthdayToday = com.example.ui.components.isBirthdayToday(user.dateOfBirth)
                        )
                    }
                }
            }
        }
    }

    // Edit Post Dialog
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

    // Comments Bottom Sheet
    if (activeCommentPost != null) {
        CommentsBottomSheet(
            post = activeCommentPost,
            comments = activePostComments,
            currentUser = currentUser,
            onDismiss = { viewModel.closeComments() },
            onAddComment = { viewModel.addComment(it) }
        )
    }
}
