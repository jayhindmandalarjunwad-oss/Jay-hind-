package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.BusinessListing
import com.example.ui.components.ImageCropperDialog
import com.example.ui.components.MandalTopHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.FirebaseStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val CATEGORIES = listOf(
    "सर्व",
    "किराणा व जनरल स्टोअर्स",
    "हॉस्पिटल व मेडिकल",
    "इलेक्ट्रिकल व इलेक्ट्रॉनिक्स",
    "शेती व अवजारे / ट्रॅक्टर",
    "हॉटेल व खाद्यपदार्थ",
    "मंडप, डेकोरेशन व साऊंड",
    "कपडे व टेलरिंग",
    "ऑटोमोबाईल व गॅरेज",
    "बांधकाम व हार्डवेअर",
    "इतर व्यावसायिक सेवा"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDirectoryScreen(
    viewModel: MandalViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val mandalLogoUrl by viewModel.mandalLogoUrl.collectAsStateWithLifecycle()
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सर्व") }
    var showAddDialog by remember { mutableStateOf(false) }
    var businessToEdit by remember { mutableStateOf<BusinessListing?>(null) }
    var businessToDelete by remember { mutableStateOf<BusinessListing?>(null) }

    val filteredBusinesses = remember(businesses, searchQuery, selectedCategory) {
        businesses.filter { item ->
            val matchCategory = selectedCategory == "सर्व" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    item.businessName.contains(searchQuery, ignoreCase = true) ||
                    item.ownerName.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.address.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    Scaffold(
        topBar = {
            MandalTopHeader(
                title = "स्थानिक व्यावसायिक डिरेक्टरी",
                subtitle = "गावातील दुकाने, कारागीर व सेवांची संपर्क डायरी",
                showBackButton = true,
                onBackClick = onBack,
                logoUrl = mandalLogoUrl,
                actions = {
                    if (currentUser?.isAnyAdmin == true) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = "नवीन व्यवसाय जोडा",
                                tint = SaffronPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser?.isAnyAdmin == true) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF0D9488),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    text = { Text("दुकान / सेवा जोडा", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_add_business")
                )
            }
        },
        containerColor = BackgroundWarm
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp, start = 14.dp, end = 14.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Intro Banner
            item(key = "directory_header_banner") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "अर्जुनवाड लोकल यलो पेजेस (Yellow Pages)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "गावातील सर्व व्यावसायिक व कारागिरांची संपर्क डायरी. थेट कॉल किंवा व्हॉट्सॲपवर संपर्क करा.",
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Search Bar
            item(key = "directory_search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("दुकान, कारागीर किंवा सेवा शोधा...", fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "शोधा",
                            tint = Color(0xFF0D9488)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0D9488),
                        unfocusedBorderColor = CardBorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("directory_search_input"),
                    singleLine = true
                )
            }

            // Category Chips Row
            item(key = "directory_category_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CATEGORIES) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceWarm,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color(0xFF0D9488) else CardBorderColor
                            )
                        )
                    }
                }
            }

            // Count summary
            item(key = "directory_count_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "एकूण उपलब्ध व्यवसाय: ${filteredBusinesses.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            // Businesses List
            if (filteredBusinesses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWarm)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "कोणताही व्यवसाय आढळला नाही",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "कृपया शोध शब्द किंवा वर्गवारी बदलून पहा.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredBusinesses, key = { it.id }) { business ->
                    BusinessListingCard(
                        business = business,
                        isAdmin = currentUser?.isAnyAdmin == true,
                        onCall = {
                            try {
                                val cleanNum = business.contactNumber.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showSnackbar("कॉल लावता आला नाही.")
                            }
                        },
                        onWhatsApp = {
                            try {
                                val targetNum = business.whatsappNumber.ifBlank { business.contactNumber }
                                val cleanNum = targetNum.replace(Regex("[^0-9]"), "")
                                val formatted = if (cleanNum.length == 10) "91$cleanNum" else cleanNum
                                val url = "https://wa.me/$formatted?text=${Uri.encode("नमस्कार, मला आपल्या ${business.businessName} सेवेबद्दल चौकशी करायची आहे.")}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showSnackbar("व्हॉट्सॲप उघडता आले नाही.")
                            }
                        },
                        onEdit = { businessToEdit = business },
                        onDelete = { businessToDelete = business }
                    )
                }
            }
        }
    }

    // Add Business Dialog
    if (showAddDialog) {
        AddEditBusinessDialog(
            initialBusiness = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, owner, cat, desc, contact, wa, addr, photo ->
                viewModel.addBusiness(name, owner, cat, desc, contact, wa, addr, photo) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Business Dialog
    if (businessToEdit != null) {
        AddEditBusinessDialog(
            initialBusiness = businessToEdit,
            onDismiss = { businessToEdit = null },
            onSave = { name, owner, cat, desc, contact, wa, addr, photo ->
                viewModel.updateBusiness(
                    id = businessToEdit!!.id,
                    businessName = name,
                    ownerName = owner,
                    category = cat,
                    description = desc,
                    contactNumber = contact,
                    whatsappNumber = wa,
                    address = addr,
                    photoUrl = photo
                ) {
                    businessToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (businessToDelete != null) {
        AlertDialog(
            onDismissRequest = { businessToDelete = null },
            title = { Text("व्यवसाय हटवायचा आहे का?", fontWeight = FontWeight.Bold) },
            text = { Text("आपण '${businessToDelete?.businessName}' डिरेक्टरीमधून कायमचा काढून टाकत आहात.") },
            confirmButton = {
                Button(
                    onClick = {
                        businessToDelete?.let { viewModel.deleteBusiness(it.id) }
                        businessToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("हटवा")
                }
            },
            dismissButton = {
                TextButton(onClick = { businessToDelete = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}

@Composable
private fun BusinessListingCard(
    business: BusinessListing,
    isAdmin: Boolean,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Business Image or Icon
                if (business.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = business.photoUrl,
                        contentDescription = business.businessName,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(70.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDFA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCFBF1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDFA),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF14B8A6))
                    ) {
                        Text(
                            text = business.category,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = business.businessName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (business.ownerName.isNotBlank()) {
                        Text(
                            text = "संचालक: ${business.ownerName}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    if (business.address.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = business.address,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "संपादित करा",
                                tint = NavySecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "हटवा",
                                tint = BloodRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (business.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = business.description,
                    fontSize = 12.5.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Call & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "थेट कॉल",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onWhatsApp,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF16A34A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditBusinessDialog(
    initialBusiness: BusinessListing?,
    onDismiss: () -> Unit,
    onSave: (
        businessName: String,
        ownerName: String,
        category: String,
        description: String,
        contactNumber: String,
        whatsappNumber: String,
        address: String,
        photoUrl: String
    ) -> Unit
) {
    val isEdit = initialBusiness != null
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var businessName by remember { mutableStateOf(initialBusiness?.businessName ?: "") }
    var ownerName by remember { mutableStateOf(initialBusiness?.ownerName ?: "") }
    var category by remember { mutableStateOf(initialBusiness?.category ?: CATEGORIES[1]) }
    var description by remember { mutableStateOf(initialBusiness?.description ?: "") }
    var contactNumber by remember { mutableStateOf(initialBusiness?.contactNumber ?: "") }
    var whatsappNumber by remember { mutableStateOf(initialBusiness?.whatsappNumber ?: "") }
    var address by remember { mutableStateOf(initialBusiness?.address ?: "") }
    var photoUrl by remember { mutableStateOf(initialBusiness?.photoUrl ?: "") }

    var isUploading by remember { mutableStateOf(false) }
    var uriToCrop by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun uploadPhoto(uri: Uri) {
        coroutineScope.launch {
            try {
                isUploading = true
                val cleanName = businessName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "business" }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val customName = "JayHind_Business_${cleanName}_${timeStamp}.webp"
                val uploaded = withContext(Dispatchers.IO) {
                    FirebaseStorageHelper.uploadImage(context, uri, folder = "businesses", customFileName = customName)
                }
                if (uploaded.isNotBlank()) {
                    photoUrl = uploaded
                }
            } catch (t: Throwable) {
                android.util.Log.e("BusinessDialog", "Photo upload error: ${t.message}")
            } finally {
                isUploading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            uriToCrop = tempCameraUri
        }
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uriToCrop = uri
        }
    }

    fun launchCamera() {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File.createTempFile("biz_camera_", ".jpg", cacheDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("BusinessDialog", "Camera launch error: ${e.message}")
        }
    }

    // Cropper Dialog
    if (uriToCrop != null) {
        ImageCropperDialog(
            sourceUri = uriToCrop!!,
            onDismiss = { uriToCrop = null },
            onImageCropped = { croppedUri ->
                uriToCrop = null
                uploadPhoto(croppedUri)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "व्यवसायाची माहिती संपादित करा" else "नवीन व्यवसाय / दुकान जोडा",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Upload Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (photoUrl.isNotBlank()) {
                        Box(modifier = Modifier.size(70.dp)) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoUrl = "" },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWarm,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF0D9488))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "दुकानाचा / बोर्डाचा फोटो",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { launchCamera() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कॅमेरा", fontSize = 11.5.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { singlePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("गॅलरी / क्रॉप", fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Business Name
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("दुकानाचे / व्यवसायाचे नाव *") },
                    placeholder = { Text("उदा. अर्जुन इलेक्ट्रॉनिक्स") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("संचालकाचे / मालकाचे नाव") },
                    placeholder = { Text("उदा. रमेश पाटील") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selector
                Text(
                    text = "व्यवसायाचा प्रकार (वर्गवारी):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CATEGORIES.filter { it != "सर्व" }) { cat ->
                        val isSel = category == cat
                        FilterChip(
                            selected = isSel,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contact Number
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("संपर्क नंबर (कॉलसाठी) *") },
                    placeholder = { Text("१० अंकी मोबाईल नंबर") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Number
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    label = { Text("WhatsApp नंबर (रिकामे ठेवल्यास संपर्क नंबर वापरला जाईल)") },
                    placeholder = { Text("१० अंकी WhatsApp नंबर") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पत्ता / ठिकाण") },
                    placeholder = { Text("उदा. मेन रोड, ग्रामपंचायत जवळ, अर्जुनवाड") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("उपलब्ध सेवा किंवा उत्पादनांची माहिती") },
                    placeholder = { Text("उदा. सर्व प्रकारचे वायर, फॅन रिपेअरिंग, होम सर्व्हिस उपलब्ध...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 120.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(
                            businessName,
                            ownerName,
                            category,
                            description,
                            contactNumber,
                            whatsappNumber.ifBlank { contactNumber },
                            address,
                            photoUrl
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "माहिती अपडेट करा" else "व्यवसाय जोडा",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
