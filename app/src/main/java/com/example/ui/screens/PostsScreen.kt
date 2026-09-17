package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DynamicFeed
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
import kotlinx.coroutines.delay
import com.example.data.model.Post
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel

@Composable
fun PostsScreen(
    viewModel: MandalViewModel,
    onOpenCreatePost: () -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    var isInitialLoading by remember { mutableStateOf(posts.isEmpty()) }
    LaunchedEffect(posts) {
        if (posts.isNotEmpty()) {
            isInitialLoading = false
        } else {
            delay(1200)
            isInitialLoading = false
        }
    }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeCommentPost by viewModel.activeCommentPost.collectAsStateWithLifecycle()
    val activePostComments by viewModel.activePostComments.collectAsStateWithLifecycle()
    val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
    val todayBirthdays by viewModel.todayBirthdays.collectAsStateWithLifecycle()
    val todayBirthdayAuthorIds = remember(todayBirthdays) { todayBirthdays.map { it.id }.toSet() }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    var postForLikers by remember { mutableStateOf<Post?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("posts_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // "काय विचार करत आहात?" Top Input Trigger Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCreatePost() }
                        .testTag("feed_create_post_trigger"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemberAvatar(
                            photoUrl = currentUser?.profilePhotoUrl ?: "",
                            name = currentUser?.fullName ?: "सभासद",
                            size = 42
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceVariantWarm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "काय नवीन आहे? येथे पोस्ट करा...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onOpenCreatePost) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "फोटो जोडा",
                                tint = SaffronPrimary
                            )
                        }
                    }
                }
            }

            // Posts List with Skeleton Shimmer Loading
            if (posts.isEmpty()) {
                if (isInitialLoading) {
                    items(3) {
                        PostCardSkeleton()
                    }
                } else {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.DynamicFeed,
                            title = "कोणतीही पोस्ट उपलब्ध नाही",
                            subtitle = "मंडळातील पहिली पोस्ट तयार करण्यासाठी वरील पर्यायावर क्लिक करा!"
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostItemCard(
                        post = post,
                        currentUser = currentUser,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onCommentClick = { viewModel.openComments(post) },
                        onDeleteClick = { viewModel.deletePost(post.id) },
                        onEditClick = { postToEdit = post },
                        onImageClick = { viewModel.openFullscreenPhoto(it) },
                        onMultiImageClick = { images, idx -> viewModel.openFullscreenPhotos(images, idx) },
                        onAuthorClick = { authorId, authorName, authorPhoto ->
                            viewModel.openUserPosts(authorId, authorName, authorPhoto)
                        },
                        onLikesCountClick = {
                            postForLikers = post
                        },
                        isAuthorBirthdayToday = todayBirthdayAuthorIds.contains(post.authorId)
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenCreatePost,
            containerColor = SaffronPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("feed_fab_create_post")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "नवीन पोस्ट")
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
                onAddComment = { text, parentId, replyToAuthorName ->
                    viewModel.addComment(text, parentId, replyToAuthorName)
                },
                onToggleLike = { viewModel.toggleCommentLike(it) },
                onEditComment = { commentId, newText -> viewModel.editComment(commentId, newText) },
                onDeleteComment = { viewModel.deleteComment(it) }
            )
        }

        // Likers Bottom Sheet (सभासदांची यादी ज्यांनी पोस्ट लाईक केली)
        if (postForLikers != null) {
            LikersBottomSheet(
                post = postForLikers!!,
                allMembers = allMembers,
                onDismiss = { postForLikers = null },
                onMemberClick = { authorId, authorName, authorPhoto ->
                    viewModel.openUserPosts(authorId, authorName, authorPhoto)
                }
            )
        }
    }
}
