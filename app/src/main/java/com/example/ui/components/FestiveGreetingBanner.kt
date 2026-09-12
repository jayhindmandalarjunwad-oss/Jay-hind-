package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.*
import com.example.util.FestivalItem
import com.example.util.FestivalRegistry

/**
 * Data structure representing an active festival or celebration
 */
data class FestiveOccasion(
    val title: String,
    val subtitle: String,
    val greetingWish: String,
    val emojiBadge: String,
    val gradientColors: List<Color>,
    val accentColor: Color = GoldenTertiary,
    val festivalDrawableRes: Int? = null
)

/**
 * Detects the active festival based on current system date or manual admin selection.
 * Returns null if no festival or special day is active today.
 */
fun detectCurrentFestival(manualFestivalId: String = ""): FestiveOccasion? {
    // 1. जर ॲडमिनने मॅन्युअल सण निवडला असेल
    if (manualFestivalId.isNotBlank()) {
        val manualFest = FestivalRegistry.findById(manualFestivalId)
        if (manualFest != null) {
            return FestiveOccasion(
                title = manualFest.title,
                subtitle = manualFest.subtitle,
                greetingWish = manualFest.greetingWish,
                emojiBadge = manualFest.emojiBadge,
                gradientColors = manualFest.gradientColors,
                accentColor = GoldenTertiary,
                festivalDrawableRes = manualFest.drawableRes
            )
        }
    }

    // 2. कॅलेंडरनुसार आजचा सण किंवा विशेष दिन शोधणे (७०+ सणांचे ऑटो-मॅपिंग)
    val autoFest = FestivalRegistry.findFestivalForCurrentDate()
    if (autoFest != null) {
        return FestiveOccasion(
            title = autoFest.title,
            subtitle = autoFest.subtitle,
            greetingWish = autoFest.greetingWish,
            emojiBadge = autoFest.emojiBadge,
            gradientColors = autoFest.gradientColors,
            accentColor = GoldenTertiary,
            festivalDrawableRes = autoFest.drawableRes
        )
    }

    // 3. ज्या दिवशी कोणताही विशेष दिन किंवा सण नसेल, त्या दिवशी null (बॅनर लपवला जाईल)
    return null
}

/**
 * सण व उत्सवानुसार डायनॅमिक ग्रीटिंग (Festive Seasonal Banner)
 * Always prominently showcases:
 * 1. Top Center: "॥ सत्यमेव जयते ॥"
 * 2. Mandal's Official App Logo
 * 3. Full Name: "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड"
 * 4. Festive Wishes & Cultural Theme
 */
@Composable
fun FestiveGreetingBanner(
    festiveOccasion: FestiveOccasion,
    mandalLogoUrl: String? = null,
    onBannerClick: (() -> Unit)? = null,
    isCarouselItem: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Subtle breathing glow animation for the banner
    val infiniteTransition = rememberInfiniteTransition(label = "festiveGlow")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val cardModifier = if (isCarouselItem) {
        Modifier
            .width(340.dp)
            .height(170.dp)
            .then(modifier)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .clickable(enabled = onBannerClick != null) { onBannerClick?.invoke() }
            .testTag("festive_seasonal_banner")
    } else {
        Modifier
            .fillMaxWidth()
            .then(modifier)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .clickable(enabled = onBannerClick != null) { onBannerClick?.invoke() }
            .testTag("festive_seasonal_banner")
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldenTertiary.copy(alpha = 0.85f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = festiveOccasion.gradientColors
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = if (isCarouselItem) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================
                // १. नेहमी बॅनरच्या टॉपला मध्यभागी "॥ सत्यमेव जयते ॥"
                // ==========================================
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldenTertiary.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "॥ सत्यमेव जयते ॥",
                            color = Color(0xFFFFF0B3),
                            fontSize = if (isCarouselItem) 10.sp else 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                    }
                }

                // ==========================================
                // २. मंडळाचे नाव, लोगो आणि सणाचा भव्य अवतार
                // ==========================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // मंडळाचा अधिकृत लोगो (गोल वर्तुळात)
                    Box(
                        modifier = Modifier
                            .size(if (isCarouselItem) 46.dp else 52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, GoldenTertiary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!mandalLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = mandalLogoUrl,
                                contentDescription = "जय हिंद मंडळ लोगो",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_jayhind_logo),
                                contentDescription = "जय हिंद मंडळ लोगो",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // मंडळाचे नाव व सणाचा बॅज
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = festiveOccasion.emojiBadge,
                                fontSize = if (isCarouselItem) 14.sp else 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                                color = Color.White,
                                fontSize = if (isCarouselItem) 11.5.sp else 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "अर्जुनवाड • स्थापना १९९६",
                            color = Color(0xFFFFD54F),
                            fontSize = if (isCarouselItem) 9.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = festiveOccasion.subtitle,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = if (isCarouselItem) 9.sp else 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // सणाचे खास आयकॉन / चिन्ह (उजव्या बाजूला)
                    if (festiveOccasion.festivalDrawableRes != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(if (isCarouselItem) 42.dp else 48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.5.dp, GoldenTertiary.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = festiveOccasion.festivalDrawableRes),
                                contentDescription = festiveOccasion.title,
                                modifier = Modifier.size(if (isCarouselItem) 34.dp else 38.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // डिव्हायडर
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.35f),
                    thickness = 0.7.dp,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )

                // ==========================================
                // ३. सण / उत्सवाची अधिकृत शुभेच्छा
                // ==========================================
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldenTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = festiveOccasion.title,
                            color = Color(0xFFFFE082),
                            fontSize = if (isCarouselItem) 11.5.sp else 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldenTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = festiveOccasion.greetingWish,
                        color = Color.White.copy(alpha = shimmerAlpha),
                        fontSize = if (isCarouselItem) 10.5.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = if (isCarouselItem) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
