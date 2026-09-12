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
import androidx.compose.material.icons.filled.Celebration
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
import com.example.R
import com.example.ui.theme.*
import java.util.Calendar

/**
 * Data structure representing an active festival or seasonal celebration
 */
data class FestiveOccasion(
    val title: String,
    val subtitle: String,
    val greetingWish: String,
    val emojiBadge: String,
    val gradientColors: List<Color>,
    val accentColor: Color = GoldenTertiary
)

/**
 * Detects the active festival based on current system date.
 * Covers major festivals and national events in Maharashtra & India.
 * If no specific festival is active today, returns the Mandal's proud cultural identity greeting.
 */
fun detectCurrentFestival(): FestiveOccasion {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1 // 1-indexed (Jan = 1)
    val day = cal.get(Calendar.DAY_OF_MONTH)

    return when {
        // २६ जानेवारी - प्रजासत्ताक दिन
        month == 1 && day in 24..27 -> FestiveOccasion(
            title = "प्रजासत्ताक दिन",
            subtitle = "भारतीय प्रजासत्ताक दिन चिरायू होवो!",
            greetingWish = "समस्त ग्रामस्थ व देशवासियांना प्रजासत्ताक दिनाच्या हार्दिक शुभेच्छा!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(Color(0xFFFF9933), Color(0xFF1E3A8A), Color(0xFF138808))
        )

        // १९ फेब्रुवारी - शिवजयंती
        month == 2 && day in 17..21 -> FestiveOccasion(
            title = "छत्रपती शिवाजी महाराज जयंती",
            subtitle = "॥ जय भवानी, जय शिवाजी ॥",
            greetingWish = "अखंड महाराष्ट्राचे कुलदैवत छत्रपती शिवरायांच्या जयंतीनिमित्त त्रिवार मानाचा मुजरा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronPrimary, SaffronDark, GoldenTertiary)
        )

        // १४ एप्रिल - डॉ. बाबासाहेब आंबेडकर जयंती
        month == 4 && day in 12..16 -> FestiveOccasion(
            title = "डॉ. बाबासाहेब आंबेडकर जयंती",
            subtitle = "ज्ञानसूर्य भारतरत्न डॉ. बाबासाहेब आंबेडकर",
            greetingWish = "महामानव भारतरत्न डॉ. बाबासाहेब आंबेडकर यांच्या जयंतीनिमित्त विनम्र अभिवादन!",
            emojiBadge = "☸️",
            gradientColors = listOf(Color(0xFF1E3A8A), NavySecondary, Color(0xFF2563EB))
        )

        // १ मे - महाराष्ट्र दिन व कामगार दिन
        month == 5 && day in 1..3 -> FestiveOccasion(
            title = "महाराष्ट्र दिन व कामगार दिन",
            subtitle = "॥ गर्जा महाराष्ट्र माझा ॥",
            greetingWish = "महाराष्ट्र दिन व जागतिक कामगार दिनाच्या सर्व बंधू-भगिनींना हार्दिक शुभेच्छा!",
            emojiBadge = "🚩",
            gradientColors = listOf(SaffronPrimary, SaffronDark, GoldenTertiary)
        )

        // १५ ऑगस्ट - स्वातंत्र्यदिन
        month == 8 && day in 13..16 -> FestiveOccasion(
            title = "स्वातंत्र्यदिन (Independence Day)",
            subtitle = "॥ जय हिंद, जय भारत ॥",
            greetingWish = "भारतीय स्वातंत्र्यदिनाच्या सर्व अर्जुनवाड ग्रामस्थांना मनःपूर्वक हार्दिक शुभेच्छा!",
            emojiBadge = "🇮🇳",
            gradientColors = listOf(Color(0xFFFF9933), NavySecondary, Color(0xFF138808))
        )

        // ऑगस्ट / सप्टेंबर - श्री गणेशोत्सव
        month in 8..9 && (day in 25..31 || day in 1..18) -> FestiveOccasion(
            title = "श्री गणेशोत्सव",
            subtitle = "॥ गणपती बाप्पा मोरया, मंगलमूर्ती मोरया ॥",
            greetingWish = "विघ्नहर्ता बाप्पाच्या आगमनानिमित्त सर्वांना मंगलमय व भक्तीमय शुभेच्छा!",
            emojiBadge = "🌺",
            gradientColors = listOf(SaffronPrimary, Color(0xFFDC2626), GoldenTertiary)
        )

        // ऑक्टोबर / नोव्हेंबर - दसरा व दीपावली (दिवाळी)
        (month == 10 && day in 15..31) || (month == 11 && day in 1..15) -> FestiveOccasion(
            title = "शुभ दीपावली व दसरा महोत्सव",
            subtitle = "॥ सण आनंदाचा, तेजाचा आणि समृद्धीचा ॥",
            greetingWish = "दीपावलीच्या पावन पर्वावर सुख, समाधान व समृद्धी लाभो हीच सदिच्छा!",
            emojiBadge = "🪔",
            gradientColors = listOf(SaffronDark, Color(0xFFB45309), GoldenTertiary)
        )

        // जानेवारी - मकर संक्रांत
        month == 1 && day in 12..16 -> FestiveOccasion(
            title = "मकर संक्रांत",
            subtitle = "॥ तीळगुळ घ्या, गोड गोड बोला ॥",
            greetingWish = "मकर संक्रांतीच्या गोड सणाच्या सर्व बंधू-भगिनींना हार्दिक शुभेच्छा!",
            emojiBadge = "🪁",
            gradientColors = listOf(SaffronPrimary, Color(0xFF0D9488), GoldenTertiary)
        )

        // मार्च / एप्रिल - गुढीपाडवा (मराठी नववर्ष)
        (month == 3 && day >= 20) || (month == 4 && day <= 5) -> FestiveOccasion(
            title = "गुढीपाडवा (मराठी नववर्ष)",
            subtitle = "॥ नववर्ष, नवचैतन्य, नवी उमेद ॥",
            greetingWish = "हिंदू नववर्ष व गुढीपाडव्याच्या सर्वांना मनःपूर्वक हार्दिक शुभेच्छा!",
            emojiBadge = "🌸",
            gradientColors = listOf(SaffronPrimary, GoldenTertiary, SaffronDark)
        )

        // सदैव दिसणारे मंडळाचे अधिकृत गौरव व स्वागत बॅनर (Mandal's Ever-Present Cultural Heritage)
        else -> FestiveOccasion(
            title = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
            subtitle = "॥ कला, क्रीडा व संस्कृतीचा वारसा • एकता आणि प्रगती ॥",
            greetingWish = "समस्त अर्जुनवाड ग्रामस्थ, हितचिंतक व सभासदांचे सहर्ष स्वागत!",
            emojiBadge = "🚩",
            gradientColors = listOf(NavySecondary, SaffronDark, GoldenTertiary)
        )
    }
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
    mandalLogoUrl: String? = null,
    onBannerClick: (() -> Unit)? = null,
    isCarouselItem: Boolean = false,
    modifier: Modifier = Modifier
) {
    val festival = remember { detectCurrentFestival() }

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
                        colors = festival.gradientColors
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
                // २. मंडळाचा लोगो + पूर्ण नाव
                // "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड"
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // मंडळाचा अधिकृत लोगो
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldenTertiary),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(if (isCarouselItem) 38.dp else 44.dp)
                    ) {
                        if (!mandalLogoUrl.isNullOrBlank()) {
                            UniversalAsyncImage(
                                model = mandalLogoUrl,
                                contentDescription = "मंडळ लोगो",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_jayhind_logo),
                                contentDescription = "मंडळ लोगो",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड",
                            color = Color.White,
                            fontSize = if (isCarouselItem) 12.sp else 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = if (isCarouselItem) 15.sp else 16.5.sp
                        )
                        Text(
                            text = "ता. शिरोळ • जि. कोल्हापूर • स्था. १९९६",
                            color = Color(0xFFFEF3C7),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // उत्सव इमोजी / बॅज
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(if (isCarouselItem) 30.dp else 34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = festival.emojiBadge,
                                fontSize = if (isCarouselItem) 15.sp else 17.sp
                            )
                        }
                    }
                }

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
                            text = festival.title,
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
                        text = festival.greetingWish,
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
