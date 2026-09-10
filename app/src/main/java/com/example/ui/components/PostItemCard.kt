package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun PostItemCard(
    post: Post,
    currentUser: User?,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onMultiImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onAuthorClick: ((authorId: String, authorName: String, authorPhoto: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLiked = currentUser?.let { post.isLikedBy(it.id) } == true
    val canEdit = currentUser != null && (currentUser.isAnyAdmin || currentUser.id == post.authorId)
    val canDelete = currentUser != null && (currentUser.isAnyAdmin || currentUser.id == post.authorId)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onAuthorClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAuthorClick(post.authorId, post.authorName, post.authorPhotoUrl)
                                    }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(
                        photoUrl = post.authorPhotoUrl,
                        name = post.authorName,
                        size = 44
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatTimestampToEnglish(post.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B), // Clear muted slate grey for English date/time
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                if (canEdit || canDelete) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "पर्याय", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (canEdit) {
                                DropdownMenuItem(
                                    text = { Text("पोस्ट संपादित करा (Edit Post)", color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onEditClick()
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = SaffronPrimary)
                                    }
                                )
                            }
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("पोस्ट हटवा (Delete Post)", color = BloodRed) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = BloodRed)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Post Content Text
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // Post Media (Multiple Photos support)
            val validImages = post.imageUrls.filter { it.isNotBlank() }
            if (validImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                PostImagesGallery(
                    images = validImages,
                    onImageClick = onImageClick,
                    onMultiImageClick = onMultiImageClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post Stats (Likes & Comments counts - Clean Facebook / Reference Video Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Likes",
                        tint = BloodRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.likesCount} लाईक्स",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLiked) BloodRed else Color(0xFF64748B),
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "${post.commentsCount} प्रतिक्रिया (Comments)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onCommentClick() }
                )
            }

            HorizontalDivider(
                color = DividerColor.copy(alpha = 0.7f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Bottom Action Bar: Like, Comment, Share (Matching Reference Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val likeTint by animateColorAsState(
                    targetValue = if (isLiked) BloodRed else Color(0xFF475569),
                    label = "likeColor"
                )

                TextButton(
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f).testTag("post_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "लाईक",
                        tint = likeTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLiked) "लाईक केले" else "लाईक करा",
                        color = likeTint,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onCommentClick,
                    modifier = Modifier.weight(1f).testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "कमेंट",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "कमेंट",
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "जय हिंद मंडळ अर्जुनवाड")
                            putExtra(Intent.EXTRA_TEXT, "🚩 जय हिंद कला क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड 🚩\n\n${post.content}\n\n- ${post.authorName}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "पोस्ट शेअर करा"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "शेअर",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "शेअर",
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PostImagesGallery(
    images: List<String>,
    onImageClick: (String) -> Unit,
    onMultiImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val handlePhotoClick: (Int) -> Unit = { index ->
        if (images.size > 1) {
            onMultiImageClick(images, index)
        } else {
            onImageClick(images.getOrElse(index) { "" })
        }
    }

    when (images.size) {
        1 -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
                    .clickable { handlePhotoClick(0) }
            ) {
                UniversalAsyncImage(
                    model = images[0],
                    contentDescription = "Post photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )

                // Fullscreen indicator badge
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "मोठा करा",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        2 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                images.take(2).forEachIndexed { index, img ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { handlePhotoClick(index) }
                    ) {
                        UniversalAsyncImage(
                            model = img,
                            contentDescription = "Post photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        3 -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { handlePhotoClick(0) }
                ) {
                    UniversalAsyncImage(
                        model = images[0],
                        contentDescription = "Post photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..2) {
                        val img = images[i]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { handlePhotoClick(i) }
                        ) {
                            UniversalAsyncImage(
                                model = img,
                                contentDescription = "Post photo ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        else -> {
            // 4 or more photos: 2x2 grid with +N indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..1) {
                        val img = images[i]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { handlePhotoClick(i) }
                        ) {
                            UniversalAsyncImage(
                                model = img,
                                contentDescription = "Post photo ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { handlePhotoClick(2) }
                    ) {
                        UniversalAsyncImage(
                            model = images[2],
                            contentDescription = "Post photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { handlePhotoClick(3) }
                    ) {
                        UniversalAsyncImage(
                            model = images[3],
                            contentDescription = "Post photo 4",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (images.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${images.size - 3}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
