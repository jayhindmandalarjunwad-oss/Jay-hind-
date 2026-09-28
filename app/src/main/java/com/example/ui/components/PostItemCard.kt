package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
    onLikesCountClick: (() -> Unit)? = null,
    onVotePoll: ((optionId: String) -> Unit)? = null,
    onClosePoll: (() -> Unit)? = null,
    isAuthorBirthdayToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isLiked = currentUser?.let { post.isLikedBy(it.id) } == true
    val canEdit = currentUser != null && (currentUser.isAnyAdmin || currentUser.id == post.authorId)
    val canDelete = currentUser != null && (currentUser.isAnyAdmin || currentUser.id == post.authorId)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sponsored Post Banner
            if (post.isSponsored) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0D9488), Color(0xFF14B8A6))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨ प्रायोजित जाहिरात (Sponsored Post)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!post.sponsorBusinessName.isNullOrBlank()) {
                            Text(
                                text = post.sponsorBusinessName,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

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
                            if (!post.isSponsored && post.pollData == null && onAuthorClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAuthorClick(post.authorId, post.authorName, post.authorPhotoUrl)
                                    }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.isSponsored) {
                        // SPONSORED AVATAR: Official Sponsored Ad Badge instead of Admin photo
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF0D9488).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0D9488)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "प्रायोजित जाहिरात",
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (!post.sponsorBusinessName.isNullOrBlank()) post.sponsorBusinessName else "प्रायोजित (Sponsored)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF0D9488).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "प्रायोजित",
                                        color = Color(0xFF0D9488),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "प्रायोजित जाहिरात • ${formatTimestampToEnglish(post.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else if (post.pollData != null) {
                        // MANDAL DECISION POLL: Displays Mandal Name & Emblem
                        val mandalDisplayName = if (post.authorName.contains("मंडळ")) {
                            post.authorName
                        } else {
                            "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड"
                        }
                        if (post.authorPhotoUrl.isNotBlank()) {
                            MemberAvatar(
                                photoUrl = post.authorPhotoUrl,
                                name = mandalDisplayName,
                                size = 44
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = SaffronPrimary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, SaffronPrimary),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.HowToVote,
                                        contentDescription = "मंडळ निर्णय कौल",
                                        tint = SaffronPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mandalDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🚩 मंडळाचा अधिकृत निर्णय कौल • ${formatTimestampToEnglish(post.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4F46E5),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // STANDARD POST: Regular member or admin post (displays author name & avatar without announcement ribbon)
                        MemberAvatar(
                            photoUrl = post.authorPhotoUrl,
                            name = post.authorName,
                            size = 44,
                            showPinkCelebrationRing = isAuthorBirthdayToday
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
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
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

            // Sponsored Post Action Bar (Call / WhatsApp CTA Button)
            if (post.isSponsored && !post.sponsorContactNumber.isNullOrBlank()) {
                val contactNum = post.sponsorContactNumber
                val ctaTitle = post.sponsorCtaText?.ifBlank { null } ?: "संपर्क साधा / ऑर्डर द्या"
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDFA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ctaTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = contactNum,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF115E59),
                                fontSize = 11.5.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Call Button
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = android.net.Uri.parse("tel:$contactNum")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "कॉल",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // WhatsApp Button
                            Button(
                                onClick = {
                                    try {
                                        val cleanPhone = contactNum.filter { it.isDigit() }
                                        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                        val message = "जय हिंद ॲपवरून मी आपल्या जाहिरातीबद्दल विचारत आहे."
                                        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${android.net.Uri.encode(message)}"
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WhatsApp",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Decision Poll Display (Real-time Mandal Polling)
            post.pollData?.let { poll ->
                Spacer(modifier = Modifier.height(10.dp))
                PollDisplaySection(
                    poll = poll,
                    currentUser = currentUser,
                    onVote = { optId -> onVotePoll?.invoke(optId) },
                    onClosePoll = { onClosePoll?.invoke() }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = post.likesCount > 0 && onLikesCountClick != null) {
                            onLikesCountClick?.invoke()
                        }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
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
                    targetValue = if (isLiked) BloodRed else TextSecondary,
                    label = "likeColor"
                )
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.25f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "likeScale"
                )

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick()
                    },
                    modifier = Modifier.weight(1f).testTag("post_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "लाईक",
                        tint = likeTint,
                        modifier = Modifier
                            .size(19.dp)
                            .scale(likeScale)
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCommentClick()
                    },
                    modifier = Modifier.weight(1f).testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "कमेंट",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "कमेंट",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "शेअर",
                        color = TextSecondary,
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
                    targetDimensionPx = 720,
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
                            targetDimensionPx = 450,
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
                        targetDimensionPx = 600,
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
                                targetDimensionPx = 350,
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
                                targetDimensionPx = 350,
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
                            targetDimensionPx = 350,
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
                            targetDimensionPx = 350,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikersBottomSheet(
    post: Post,
    allMembers: List<User>,
    onDismiss: () -> Unit,
    onMemberClick: ((authorId: String, authorName: String, authorPhoto: String) -> Unit)? = null
) {
    val likerUsers: List<User> = remember(post.likedUserIds, allMembers) {
        val memberMap = allMembers.associateBy { it.id }
        post.likedUserIds.mapNotNull { memberMap[it] }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = BloodRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "लाईक केलेले सभासद (${post.likesCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (likerUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "सभासदांची माहिती लोड होत आहे...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(likerUsers.size) { idx ->
                        val user = likerUsers[idx]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onMemberClick?.invoke(user.id, user.fullName, user.profilePhotoUrl)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            MemberAvatar(
                                photoUrl = user.profilePhotoUrl,
                                name = user.fullName,
                                size = 42,
                                dateOfBirth = user.dateOfBirth
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (user.role.isNotBlank()) {
                                    Text(
                                        text = user.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = BloodRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PollDisplaySection(
    poll: com.example.data.model.PollData,
    currentUser: User?,
    onVote: (optionId: String) -> Unit,
    onClosePoll: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val userVotedOptionId = currentUser?.let { poll.userVotedOptionId(it.id) }
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || poll.isClosed || poll.isExpired
    val canVoteNow = poll.canVote && currentUser != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Poll badge and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEEF2FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Poll,
                            contentDescription = "Poll",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚩 जय हिंद मंडळ निर्णय कौल",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4338CA)
                    )
                }

                // Status Chip
                val statusBg = if (poll.isClosed) Color(0xFFFEE2E2) else if (poll.isExpired) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                val statusText = if (poll.isClosed) "🔴 मतदान बंद" else if (poll.isExpired) "⏳ मुदत संपली" else "🟢 मतदान चालू"
                val statusColor = if (poll.isClosed) Color(0xFFDC2626) else if (poll.isExpired) Color(0xFFD97706) else Color(0xFF16A34A)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Poll Question
            Text(
                text = poll.question,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFF0F172A)
            )

            // Expiry note if available
            if (poll.expiresAt != null && !poll.isClosed && !poll.isExpired) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏱️ अंतिम मुदत: ${formatTimestampToEnglish(poll.expiresAt)}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                poll.options.forEach { option ->
                    val isMyVote = userVotedOptionId == option.id
                    val isHighest = poll.totalVotes > 0 && option.voteCount == poll.options.maxOf { it.voteCount } && option.voteCount > 0

                    if (!showResults && canVoteNow) {
                        // Interactive Voting Option Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onVote(option.id)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, Color(0xFF64748B), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option.text,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    } else {
                        // Results View with Animated Progress Bar
                        val animatedPercent by animateFloatAsState(
                            targetValue = option.percentage / 100f,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "poll_fill_${option.id}"
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isMyVote) 1.5.dp else 1.dp,
                                if (isMyVote) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Progress Bar fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedPercent)
                                        .matchParentSize()
                                        .background(
                                            if (isMyVote) Color(0xFFE0E7FF)
                                            else if (isHighest) Color(0xFFFEF3C7)
                                            else Color(0xFFF1F5F9)
                                        )
                                )

                                // Foreground Text & Details
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isMyVote) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Your vote",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = option.text,
                                            fontSize = 13.sp,
                                            fontWeight = if (isMyVote || isHighest) FontWeight.Bold else FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${option.percentage.toInt()}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyVote) Color(0xFF4338CA) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${option.voteCount} मते",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Total votes and Admin close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 एकूण मतदान: ${poll.totalVotes} सभासद",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                if (currentUser?.isAnyAdmin == true && !poll.isClosed) {
                    TextButton(
                        onClick = onClosePoll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BloodRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "मतदान बंद करा",
                            fontSize = 11.sp,
                            color = BloodRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


