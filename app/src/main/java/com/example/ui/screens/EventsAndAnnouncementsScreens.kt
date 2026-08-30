package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.MandalEvent
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MandalViewModel

@Composable
fun EventsScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin = currentUser?.isAdmin == true

    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<MandalEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<MandalEvent?>(null) }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = "मंडळ कार्यक्रम (Events Calendar)",
                subtitle = "सर्व आगामी व नियोजित उपक्रम",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SaffronPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Event",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SaffronPrimary,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("नवीन कार्यक्रम", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
                .testTag("events_screen_root")
        ) {
            if (events.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Event,
                    title = "कोणतेही आगामी कार्यक्रम नाहीत",
                    subtitle = "नवीन कार्यक्रम लवकरच जाहीर केले जातील."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        FullEventCard(
                            event = event,
                            isAdmin = isAdmin,
                            onRegisterClick = { viewModel.toggleEventRegistration(event) },
                            onImageClick = { viewModel.openFullscreenPhoto(it) },
                            onEditClick = { eventToEdit = event },
                            onDeleteClick = { eventToDelete = event }
                        )
                    }
                }
            }
        }
    }

    // Admin Event Create Dialog
    if (showCreateDialog) {
        AddEditEventDialog(
            event = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, date, time, location, desc, imgUrl ->
                viewModel.createEvent(title, date, time, location, desc, imgUrl)
                showCreateDialog = false
            }
        )
    }

    // Admin Event Edit Dialog
    if (eventToEdit != null) {
        AddEditEventDialog(
            event = eventToEdit,
            onDismiss = { eventToEdit = null },
            onSave = { title, date, time, location, desc, imgUrl ->
                eventToEdit?.let { ev ->
                    viewModel.updateEvent(ev.id, title, date, time, location, desc, imgUrl)
                }
                eventToEdit = null
            }
        )
    }

    // Admin Delete Event Confirmation
    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("कार्यक्रम हटवा (Delete Event)", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("'${eventToDelete?.title}' हा कार्यक्रम मंडळ कॅलेंडरमधून कायमचा हटवायचा आहे का?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        eventToDelete?.let { viewModel.deleteEvent(it.id) }
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("हटवा (Delete)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { eventToDelete = null }) {
                    Text("रद्द करा", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun FullEventCard(
    event: MandalEvent,
    isAdmin: Boolean,
    onRegisterClick: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (event.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { onImageClick(event.imageUrl) }
                ) {
                    UniversalAsyncImage(
                        model = event.imageUrl,
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Fullscreen indicator
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
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

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (isAdmin) {
                        Row {
                            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Event",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Event",
                                    tint = BloodRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${event.date} • ${event.time}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BloodRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = event.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.isRegistered) SuccessGreen else SaffronPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (event.isRegistered) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (event.isRegistered) "मी उपस्थित राहणार आहे (${event.attendeesCount})" else "उपस्थिती नोंदवा (${event.attendeesCount})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditEventDialog(
    event: MandalEvent?,
    onDismiss: () -> Unit,
    onSave: (title: String, date: String, time: String, location: String, desc: String, imgUrl: String) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var date by remember { mutableStateOf(event?.date ?: "15 सप्टेंबर 2026") }
    var time by remember { mutableStateOf(event?.time ?: "सकाळी ९:०० वा.") }
    var location by remember { mutableStateOf(event?.location ?: "मंडळ प्रांगण, अर्जुनवाड") }
    var desc by remember { mutableStateOf(event?.description ?: "") }
    var imgUrl by remember { mutableStateOf(event?.imageUrl ?: "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (event == null) "नवीन कार्यक्रम जोडा" else "कार्यक्रम संपादित करा (Edit Event)",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("कार्यक्रमाचे नाव") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("तारीख (उदा. 15 सप्टेंबर 2026)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("वेळ (उदा. सकाळी ९:०० वा.)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("स्थळ / ठिकाण") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("कार्यक्रमाची माहिती") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text(
                    text = "बॅनर फोटो (गॅलरीतून निवडा):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                GalleryImagePicker(
                    selectedImageUrl = imgUrl,
                    onImageSelected = { imgUrl = it },
                    label = "गॅलरीतून बॅनर निवडा",
                    helperText = "मोबाईल गॅलरीतून फोटो अपलोड करा",
                    height = 120.dp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, date, time, location, desc, imgUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text(if (event == null) "कार्यक्रम जोडा" else "बदल जतन करा")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करा", color = TextPrimary)
            }
        }
    )
}

// ANNOUNCEMENTS SCREEN (NOTICE BOARD)
@Composable
fun AnnouncementsScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin = currentUser?.isAdmin == true

    var showCreateDialog by remember { mutableStateOf(false) }
    var announcementToEdit by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = "मंडळ सूचना फलक (Notice Board)",
                subtitle = "अधिकृत पत्रके, परिपत्रके व निर्णय",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SaffronPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Announcement",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SaffronPrimary,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Notice")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("नवीन सूचना", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
                .testTag("announcements_screen_root")
        ) {
            if (announcements.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Campaign,
                    title = "कोणतीही नवीन सूचना नाही",
                    subtitle = "नवीन सूचना येथे प्रसिद्ध केल्या जातील."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(announcements, key = { it.id }) { ann ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ann.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    PriorityBadge(priority = ann.priority)

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { announcementToEdit = ann },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Announcement",
                                                tint = SaffronPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { announcementToDelete = ann },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Announcement",
                                                tint = BloodRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = ann.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = DividerColor)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "आदेशान्वये: ${ann.author}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = SaffronPrimary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = ann.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Announcement Create Dialog
    if (showCreateDialog) {
        AddEditAnnouncementDialog(
            announcement = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, content, priority ->
                viewModel.createAnnouncement(title, content, priority)
                showCreateDialog = false
            }
        )
    }

    // Admin Announcement Edit Dialog
    if (announcementToEdit != null) {
        AddEditAnnouncementDialog(
            announcement = announcementToEdit,
            onDismiss = { announcementToEdit = null },
            onSave = { title, content, priority ->
                announcementToEdit?.let { ann ->
                    viewModel.updateAnnouncement(ann.id, title, content, priority)
                }
                announcementToEdit = null
            }
        )
    }

    // Admin Delete Announcement Confirmation Dialog
    if (announcementToDelete != null) {
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            title = { Text("सूचना हटवा (Delete Notice)", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("'${announcementToDelete?.title}' ही सूचना सूचना फलकावरून कायमची हटवायची आहे का?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        announcementToDelete?.let { viewModel.deleteAnnouncement(it.id) }
                        announcementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("हटवा (Delete)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { announcementToDelete = null }) {
                    Text("रद्द करा", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun AddEditAnnouncementDialog(
    announcement: Announcement?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf(announcement?.title ?: "") }
    var content by remember { mutableStateOf(announcement?.content ?: "") }
    var priority by remember { mutableStateOf(announcement?.priority ?: "HIGH") }

    val priorityOptions = listOf("HIGH" to "अति महत्त्वाचे (High)", "MEDIUM" to "महत्त्वाचे (Medium)", "NORMAL" to "सामान्य (Normal)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (announcement == null) "नवीन सूचना प्रसिद्ध करा" else "सूचना संपादित करा (Edit Notice)",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("सूचनेचे शीर्षक") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("सूचनेचा तपशील / मजकूर") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Text(
                    text = "प्राधान्यक्रम (Priority):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = priority == key,
                            onClick = { priority = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title, content, priority)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text(if (announcement == null) "सूचना प्रसिद्ध करा" else "बदल जतन करा")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करा", color = TextPrimary)
            }
        }
    )
}

// NOTIFICATIONS SCREEN
@Composable
fun NotificationsScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationPermissionGranted = granted
        if (granted) {
            viewModel.showSnackbar("सूचना (Notifications) परवानगी सक्षम केली आहे! 🔔")
        }
    }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = "सूचना व नोटिफिकेशन्स (Alerts)",
                subtitle = "मंडळाकडून प्राप्त झालेले संदेश",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (notifications.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                                Text("सर्व वाचले", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "सर्व हटवा",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWarm)
                .testTag("notifications_screen_root")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // If notification permission is missing on Android 13+, show a prompt card
                if (!isNotificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SaffronPrimary.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "मोबाईलवर त्वरित नोटिफिकेशन्स मिळवा",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "नवीन पोस्ट, वाढदिवस व मेसेजचे थेट अलर्ट मिळवण्यासाठी परवानगी द्या.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("परवानगी द्या", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.NotificationsNone,
                            title = "कोणतीही नवीन नोटिफिकेशन नाही",
                            subtitle = "नवीन सदस्य, कार्यक्रम, वाढदिवस आणि आपल्या पोस्टवरील कमेंट्स येथे दिसतील."
                        )
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SaffronLight.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 संबंधित पानावर जाण्यासाठी नोटिफिकेशनवर टॅप करा.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SaffronDark,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { showClearConfirmDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BloodRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("सर्व साफ करा", color = BloodRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(notifications, key = { it.id }) { notif ->
                        val iconVector = when (notif.type) {
                            "ADMIN" -> Icons.Default.AdminPanelSettings
                            "EVENT" -> Icons.Default.Event
                            "ANNOUNCEMENT" -> Icons.Default.Campaign
                            "BIRTHDAY" -> Icons.Default.Cake
                            "COMMENT", "POST" -> Icons.Default.Comment
                            "CHAT" -> Icons.Default.Chat
                            else -> Icons.Default.Notifications
                        }

                        val iconBgColor = when (notif.type) {
                            "ADMIN" -> BloodRed
                            "EVENT" -> SaffronPrimary
                            "ANNOUNCEMENT" -> NavySecondary
                            "BIRTHDAY" -> Color(0xFFD97706)
                            "COMMENT", "POST" -> Color(0xFF7C3AED)
                            "CHAT" -> SuccessGreen
                            else -> SaffronPrimary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.handleNotificationClick(notif)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else SurfaceWarm
                            ),
                            border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (!notif.isRead) 2.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(iconBgColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = iconBgColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!notif.isRead) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(SaffronPrimary)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTimestampToMarathi(notif.timestamp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SaffronPrimary,
                                            fontSize = 10.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "पहा",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SaffronPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = SaffronPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
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

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("सर्व नोटिफिकेशन्स हटवा", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("आपण सर्व सूचना व नोटिफिकेशन्स कायमचे हटवू इच्छिता का?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotifications()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("होय, सर्व हटवा")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("रद्द करा", color = TextPrimary)
                }
            }
        )
    }
}
