package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MandalLogoBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MandalViewModel
import com.example.ui.viewmodel.NavigationTab

@Composable
fun QuickAccessScreen(
    viewModel: MandalViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .testTag("quick_access_screen_root"),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SaffronContainer, SurfaceWarm)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MandalLogoBadge(logoUrl = mandalLogoUrl, size = 64)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "अर्जुनवाड • ता. शिरोळ, जि. कोल्हापूर",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = SaffronDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SaffronPrimary,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "जलद सुविधा व माहिती केंद्र (Quick Access)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // QUICK SERVICES GRID
        item {
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = "जलद सुविधा (Quick Services)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "मंडळाच्या विविध सेवा व विभागांमध्ये थेट प्रवेश करा",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Service Cards Grid (2 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickServiceCard(
                        title = "मंडळ गॅलरी",
                        subtitle = "फोटो व व्हिडिओ ॲल्बम",
                        icon = Icons.Default.PhotoLibrary,
                        accentColor = NavySecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.GALLERY) }
                    )
                    QuickServiceCard(
                        title = "मंडळ कार्यक्रम",
                        subtitle = "आगामी व नियोजित उपक्रम",
                        icon = Icons.Default.Event,
                        accentColor = GoldenTertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.EVENTS) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickServiceCard(
                        title = "सूचना फलक",
                        subtitle = "अधिकृत परिपत्रके व सूचना",
                        icon = Icons.Default.Campaign,
                        accentColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.ANNOUNCEMENTS) }
                    )
                    QuickServiceCard(
                        title = "रक्तगट शोध",
                        subtitle = "तातडीने रक्तदाते शोधा",
                        icon = Icons.Default.Bloodtype,
                        accentColor = BloodRed,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setSelectedBloodGroupFilter("सर्व")
                            viewModel.setNavigationTab(NavigationTab.MEMBERS)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickServiceCard(
                        title = "सभासद संवाद",
                        subtitle = "थेट मेसेंजर चॅट",
                        icon = Icons.Default.Chat,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setNavigationTab(NavigationTab.CHAT) }
                    )
                    QuickServiceCard(
                        title = "नोटिफिकेशन्स",
                        subtitle = "प्राप्त झालेले संदेश",
                        icon = Icons.Default.Notifications,
                        accentColor = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) }
                    )
                }

                if (currentUser?.isAdmin == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldenTertiary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldenTertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "मंडळ ॲडमिन पॅनेल (Admin Panel)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "सभासद मंजुरी, कार्यक्रम संपादन व सर्व नियंत्रण",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5A3E00)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // ABOUT US SECTION (आमच्याबद्दल माहिती)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("quick_access_about_us_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "आमच्याबद्दल माहिती (About Us)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = SaffronPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड हे गावातील सामाजिक, सांस्कृतिक, क्रीडा, आरोग्य व शैक्षणिक प्रगतीसाठी अविरत कार्यरत असणारे अग्रगण्य मंडळ आहे.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "मंडळाचे मुख्य उद्दिष्टे व उपक्रम:\n" +
                                "• गणेशोत्सव व शिवजयंती उत्साहात व शिस्तबद्ध पद्धतीने साजरी करणे.\n" +
                                "• रक्तदान शिबिरे व मोफत आरोग्य तपासणी शिबिरे.\n" +
                                "• गुणवंत विद्यार्थ्यांचा सत्कार व शैक्षणिक साहित्य वाटप.\n" +
                                "• क्रीडा स्पर्धा व युवकांसाठी व्यायाम व मैदानी खेळांना प्रोत्साहन.\n" +
                                "• गावातील सामाजिक सलोखा व सर्वसमावेशक विकास.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "अधिकृत संपर्क व पत्ता:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Contact Clickable
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantWarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:jayhindmandalarjunwad@gmail.com")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SaffronPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "अधिकृत ई-मेल (Click to Email):",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "jayhindmandalarjunwad@gmail.com",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavySecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Address Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceVariantWarm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NavySecondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Address",
                                    tint = NavySecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "मंडळ कार्यालय पत्ता:",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "मु. पो. अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर - ४१६१०१",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickServiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
