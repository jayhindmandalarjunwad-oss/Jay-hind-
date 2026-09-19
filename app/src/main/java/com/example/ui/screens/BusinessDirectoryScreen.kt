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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BusinessLeadClick
import com.example.data.model.BusinessListing
import com.example.ui.components.ImageCropperDialog
import com.example.ui.components.MandalTopHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MandalViewModel
import com.example.util.BusinessAnalyticsExcelExporter
import com.example.util.FirebaseStorageHelper
import com.example.util.MediaUtils
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
private val DEFAULT_CATEGORIES = CATEGORIES

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

    val prefs = remember { context.getSharedPreferences("mandal_business_prefs", android.content.Context.MODE_PRIVATE) }
    var categoriesList by remember {
        val saved = prefs.getString("custom_categories_order", null)
        val initialList = if (!saved.isNullOrBlank()) {
            val list = saved.split("|||").filter { it.isNotBlank() }
            if (list.contains("सर्व")) list else listOf("सर्व") + list
        } else {
            DEFAULT_CATEGORIES
        }
        mutableStateOf(initialList)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सर्व") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryOrderDialog by remember { mutableStateOf(false) }
    var businessToEdit by remember { mutableStateOf<BusinessListing?>(null) }
    var businessToDelete by remember { mutableStateOf<BusinessListing?>(null) }
    var photoGalleryBusiness by remember { mutableStateOf<BusinessListing?>(null) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }

    val isCategoryMode = selectedCategory != "सर्व"

    val filteredBusinesses = remember(businesses, searchQuery, selectedCategory) {
        val list = businesses.filter { item ->
            val matchCategory = selectedCategory == "सर्व" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    item.businessName.contains(searchQuery, ignoreCase = true) ||
                    item.ownerName.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.address.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
        if (selectedCategory == "सर्व") {
            list.sortedWith(compareBy<BusinessListing> { it.globalOrder }.thenByDescending { it.timestamp })
        } else {
            list.sortedWith(compareBy<BusinessListing> { it.categoryOrder }.thenBy<BusinessListing> { it.globalOrder }.thenByDescending { it.timestamp })
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
                        IconButton(
                            onClick = { showAnalyticsDialog = true },
                            modifier = Modifier.testTag("btn_business_analytics_report")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "मासिक विश्लेषण व अहवाल",
                                tint = Color(0xFF2563EB)
                            )
                        }
                        IconButton(
                            onClick = { showCategoryOrderDialog = true },
                            modifier = Modifier.testTag("btn_reorder_categories")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "कॅटेगरी क्रम व्यवस्थापन",
                                tint = Color(0xFF0D9488)
                            )
                        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categoriesList) { cat ->
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

                    if (currentUser?.isAnyAdmin == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalIconButton(
                            onClick = { showCategoryOrderDialog = true },
                            modifier = Modifier.size(34.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFFCCFBF1),
                                contentColor = Color(0xFF0F766E)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "कॅटेगरी क्रम व्यवस्थापन",
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

                    if (currentUser?.isAnyAdmin == true && filteredBusinesses.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = if (isCategoryMode) "कॅटेगरीनुसार क्रमवारी चालू" else "सर्व व्यवसाय क्रमवारी चालू",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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
                itemsIndexed(items = filteredBusinesses, key = { _, item -> item.id }) { index, business ->
                    BusinessListingCard(
                        business = business,
                        isAdmin = currentUser?.isAnyAdmin == true,
                        positionRank = index + 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < filteredBusinesses.size - 1,
                        onMoveUp = {
                            viewModel.moveBusinessUp(business, filteredBusinesses, isCategoryMode)
                        },
                        onMoveDown = {
                            viewModel.moveBusinessDown(business, filteredBusinesses, isCategoryMode)
                        },
                        onCall = {
                            viewModel.recordBusinessLeadClick(business, "CALL")
                            try {
                                val cleanNum = business.contactNumber.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNum"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showSnackbar("कॉल लावता आला नाही.")
                            }
                        },
                        onWhatsApp = {
                            viewModel.recordBusinessLeadClick(business, "WHATSAPP")
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
                        onDelete = { businessToDelete = business },
                        onPhotoClick = { photoGalleryBusiness = business }
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
            onSave = { name, owner, cat, desc, contact, wa, addr, photo, photosJson ->
                viewModel.addBusiness(name, owner, cat, desc, contact, wa, addr, photo, photosJson) {
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
            onSave = { name, owner, cat, desc, contact, wa, addr, photo, photosJson ->
                viewModel.updateBusiness(
                    id = businessToEdit!!.id,
                    businessName = name,
                    ownerName = owner,
                    category = cat,
                    description = desc,
                    contactNumber = contact,
                    whatsappNumber = wa,
                    address = addr,
                    photoUrl = photo,
                    photosJson = photosJson
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

    // Category Reorder Dialog for Admin
    if (showCategoryOrderDialog) {
        CategoryOrderDialog(
            currentCategories = categoriesList,
            onDismiss = { showCategoryOrderDialog = false },
            onSave = { updatedList ->
                categoriesList = updatedList
                prefs.edit().putString("custom_categories_order", updatedList.joinToString("|||")).apply()
                showCategoryOrderDialog = false
                viewModel.showSnackbar("✅ कॅटेगरीजचा क्रम यशस्वीरित्या सेव्ह झाला!")
            }
        )
    }

    // Business Photo Gallery Slider Dialog
    if (photoGalleryBusiness != null) {
        val isUserAdmin = currentUser?.isAnyAdmin == true
        BusinessPhotoGalleryDialog(
            business = photoGalleryBusiness!!,
            isAdmin = isUserAdmin,
            onDismiss = { photoGalleryBusiness = null },
            onDeletePhoto = { photoToDelete ->
                val currentBiz = photoGalleryBusiness ?: return@BusinessPhotoGalleryDialog
                val currentPhotos = currentBiz.photosList.toMutableList()
                currentPhotos.remove(photoToDelete)
                val newCover = currentPhotos.firstOrNull() ?: ""
                val newJson = org.json.JSONArray(currentPhotos).toString()
                viewModel.updateBusiness(
                    id = currentBiz.id,
                    businessName = currentBiz.businessName,
                    ownerName = currentBiz.ownerName,
                    category = currentBiz.category,
                    description = currentBiz.description,
                    contactNumber = currentBiz.contactNumber,
                    whatsappNumber = currentBiz.whatsappNumber,
                    address = currentBiz.address,
                    photoUrl = newCover,
                    photosJson = newJson
                ) {
                    if (currentPhotos.isEmpty()) {
                        photoGalleryBusiness = null
                    } else {
                        photoGalleryBusiness = currentBiz.copy(
                            photoUrl = newCover,
                            photosJson = newJson
                        )
                    }
                    viewModel.showSnackbar("फोटो यशस्वीरित्या डिलीट केला!")
                }
            }
        )
    }

    // Business Analytics & Monthly Report Dialog (टप्पा २)
    if (showAnalyticsDialog) {
        BusinessAnalyticsReportDialog(
            viewModel = viewModel,
            businesses = businesses,
            onDismiss = { showAnalyticsDialog = false }
        )
    }
}

@Composable
private fun CategoryOrderDialog(
    currentCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var editableList by remember { mutableStateOf(currentCategories) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFCCFBF1),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "कॅटेगरी क्रम व्यवस्थापन",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "पट्टीमध्ये कॅटेगरी पुढे-मागे सरकवण्यासाठी वर-खाली बटणे वापरा",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editableList.forEachIndexed { index, category ->
                        val isFirst = index == 0
                        val isLast = index == editableList.size - 1

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (category == "सर्व") Color(0xFFF0FDFA) else SurfaceWarm,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (category == "सर्व") Color(0xFF0D9488) else CardBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, CardBorderColor),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = category,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (category == "सर्व") FontWeight.Bold else FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                // Move Up Button
                                IconButton(
                                    onClick = {
                                        if (!isFirst) {
                                            val mutable = editableList.toMutableList()
                                            val temp = mutable[index - 1]
                                            mutable[index - 1] = category
                                            mutable[index] = temp
                                            editableList = mutable
                                        }
                                    },
                                    enabled = !isFirst,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropUp,
                                        contentDescription = "वर हलवा",
                                        tint = if (!isFirst) Color(0xFF0D9488) else Color.LightGray,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Move Down Button
                                IconButton(
                                    onClick = {
                                        if (!isLast) {
                                            val mutable = editableList.toMutableList()
                                            val temp = mutable[index + 1]
                                            mutable[index + 1] = category
                                            mutable[index] = temp
                                            editableList = mutable
                                        }
                                    },
                                    enabled = !isLast,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "खाली हलवा",
                                        tint = if (!isLast) Color(0xFF0D9488) else Color.LightGray,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(editableList) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("क्रम सेव्ह करा", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessListingCard(
    business: BusinessListing,
    isAdmin: Boolean,
    positionRank: Int = 1,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: () -> Unit = {}
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
                // Business Image or Icon with Multiple Photos Badge & Click to Gallery
                BusinessThumbnailImage(
                    business = business,
                    onClick = onPhotoClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge and Rank
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE0F2FE),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF0284C7))
                            ) {
                                Text(
                                    text = "#$positionRank",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Move Up
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropUp,
                                contentDescription = "वर हलवा",
                                tint = if (canMoveUp) Color(0xFF0D9488) else Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Move Down
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "खाली हलवा",
                                tint = if (canMoveDown) Color(0xFF0D9488) else Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "संपादित करा",
                                tint = NavySecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "हटवा",
                                tint = BloodRed,
                                modifier = Modifier.size(17.dp)
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

            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${business.callClicks} कॉल",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F766E)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${business.whatsappClicks} WA",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFE0E7FF),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "एकूण ${business.totalClicks} संपर्क",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3730A3),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
private fun BusinessThumbnailImage(
    business: BusinessListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos = business.photosList
    val coverPhoto = photos.firstOrNull() ?: business.photoUrl

    val bitmap = remember(coverPhoto) {
        if (coverPhoto.isNotBlank() && (coverPhoto.startsWith("data:") || coverPhoto.startsWith("/") || !coverPhoto.startsWith("http"))) {
            MediaUtils.loadBitmap(context, coverPhoto)
        } else null
    }

    Box(
        modifier = modifier
            .size(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = business.businessName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (coverPhoto.isNotBlank()) {
            AsyncImage(
                model = coverPhoto,
                contentDescription = business.businessName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF0FDFA)
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

        // Photo Count Badge (उदा. 📷 ३ किंवा 📷 १)
        if (photos.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 6.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${photos.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Green dot indicator for Drive backup (खालच्या कोपऱ्यात ग्रीन डॉट, वापरकर्त्याला ड्राईव्ह चे नाव न दाखवता)
        val hasDriveSync = remember(photos) {
            photos.any { it.contains("drive.google.com") || it.contains("docs.google.com") || it.contains("googleusercontent.com") }
        }
        if (hasDriveSync) {
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .size(8.dp)
                    .background(Color(0xFF22C55E), CircleShape)
                    .border(1.2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun BusinessPhotoGalleryDialog(
    business: BusinessListing,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onDeletePhoto: ((photoUrl: String) -> Unit)? = null
) {
    val photos = remember(business.photosJson, business.photoUrl) { business.photosList }
    if (photos.isEmpty()) {
        onDismiss()
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = business.businessName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (business.ownerName.isNotBlank()) {
                            Text(
                                text = "संचालक: ${business.ownerName}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Indicator (e.g. 2 / 4)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Admin Delete Specific Photo Button
                    if ((isAdmin || onDeletePhoto != null) && photos.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(36.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "हा फोटो डिलीट करा",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करा",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Main Swipeable Horizontal Pager
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val photoSource = photos[page]
                        val bitmap = remember(photoSource) {
                            if (photoSource.isNotBlank() && (photoSource.startsWith("data:") || photoSource.startsWith("/") || !photoSource.startsWith("http"))) {
                                MediaUtils.loadBitmap(context, photoSource)
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "${business.businessName} - फोटो ${page + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                AsyncImage(
                                    model = photoSource,
                                    contentDescription = "${business.businessName} - फोटो ${page + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // Left Chevron Button
                    if (pagerState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "मागील फोटो",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Right Chevron Button
                    if (pagerState.currentPage < photos.size - 1) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "पुढील फोटो",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Bottom Indicator Dots & Thumbnails
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        photos.indices.forEach { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }

                    // Small Thumbnails Row
                    if (photos.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(photos) { index, thumbSource ->
                                val isSelected = pagerState.currentPage == index
                                val thumbBitmap = remember(thumbSource) {
                                    if (thumbSource.isNotBlank() && (thumbSource.startsWith("data:") || thumbSource.startsWith("/") || !thumbSource.startsWith("http"))) {
                                        MediaUtils.loadBitmap(context, thumbSource)
                                    } else null
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                ) {
                                    if (thumbBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = thumbBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = thumbSource,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Confirm Delete Dialog for Current Photo
            if (showDeleteConfirm && photos.isNotEmpty()) {
                val currentPhotoIndex = pagerState.currentPage.coerceIn(0, photos.lastIndex)
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "हा फोटो डिलीट करायचा का?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "फोटो क्र. ${currentPhotoIndex + 1} या व्यवसायाच्या गॅलरीतून कायमस्वरूपी काढून टाकला जाईल. आपण खात्री केली आहे का?",
                            fontSize = 13.5.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                val photoToDelete = photos[currentPhotoIndex]
                                onDeletePhoto?.invoke(photoToDelete)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("होय, डिलीट करा", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("रद्द करा")
                        }
                    }
                )
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
        photoUrl: String,
        photosJson: String
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

    val initialPhotos = remember(initialBusiness) {
        val list = initialBusiness?.photosList?.toMutableList() ?: mutableListOf()
        if (list.isEmpty() && !initialBusiness?.photoUrl.isNullOrBlank()) {
            list.add(initialBusiness!!.photoUrl)
        }
        list
    }
    var uploadedPhotos by remember { mutableStateOf(initialPhotos) }

    var isUploading by remember { mutableStateOf(false) }
    var uploadingMessage by remember { mutableStateOf("") }
    var uriToCrop by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val maxPhotos = 5

    fun uploadSinglePhoto(uri: Uri) {
        if (uploadedPhotos.size >= maxPhotos) return
        coroutineScope.launch {
            try {
                isUploading = true
                uploadingMessage = "फोटो अपलोड होत आहे..."
                val cleanName = businessName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "business" }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val customName = "JayHind_Business_${cleanName}_${timeStamp}_${uploadedPhotos.size + 1}.webp"
                val uploaded = withContext(Dispatchers.IO) {
                    FirebaseStorageHelper.uploadImage(context, uri, folder = "businesses", customFileName = customName)
                }
                if (uploaded.isNotBlank()) {
                    uploadedPhotos = (uploadedPhotos + uploaded).toMutableList()
                }
            } catch (t: Throwable) {
                android.util.Log.e("BusinessDialog", "Photo upload error: ${t.message}")
            } finally {
                isUploading = false
                uploadingMessage = ""
            }
        }
    }

    fun uploadMultiplePhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val availableSlots = maxPhotos - uploadedPhotos.size
        val toUpload = uris.take(availableSlots)
        coroutineScope.launch {
            try {
                isUploading = true
                val newPhotos = mutableListOf<String>()
                for ((idx, uri) in toUpload.withIndex()) {
                    uploadingMessage = "फोटो (${idx + 1}/${toUpload.size}) अपलोड होत आहे..."
                    val cleanName = businessName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "business" }
                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    val customName = "JayHind_Business_${cleanName}_${timeStamp}_${uploadedPhotos.size + idx + 1}.webp"
                    val uploaded = withContext(Dispatchers.IO) {
                        FirebaseStorageHelper.uploadImage(context, uri, folder = "businesses", customFileName = customName)
                    }
                    if (uploaded.isNotBlank()) {
                        newPhotos.add(uploaded)
                    }
                }
                uploadedPhotos = (uploadedPhotos + newPhotos).toMutableList()
            } catch (t: Throwable) {
                android.util.Log.e("BusinessDialog", "Multiple photo upload error: ${t.message}")
            } finally {
                isUploading = false
                uploadingMessage = ""
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

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uploadMultiplePhotos(uris)
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
                uploadSinglePhoto(croppedUri)
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

                // Photos Management Section (3 to 5 photos)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "व्यवसायाचे फोटो (${uploadedPhotos.size}/$maxPhotos)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = TextPrimary
                                )
                            }
                            if (uploadedPhotos.size in 1..maxPhotos) {
                                Text(
                                    text = "पहिला = कव्हर फोटो",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0D9488),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal list of uploaded photos
                        if (uploadedPhotos.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(uploadedPhotos) { index, photoSource ->
                                    val bitmap = remember(photoSource) {
                                        if (photoSource.isNotBlank() && (photoSource.startsWith("data:") || photoSource.startsWith("/") || !photoSource.startsWith("http"))) {
                                            MediaUtils.loadBitmap(context, photoSource)
                                        } else null
                                    }

                                    Box(modifier = Modifier.size(76.dp)) {
                                        if (bitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (index == 0) 2.dp else 1.dp,
                                                        color = if (index == 0) Color(0xFF0D9488) else CardBorderColor,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            AsyncImage(
                                                model = photoSource,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (index == 0) 2.dp else 1.dp,
                                                        color = if (index == 0) Color(0xFF0D9488) else CardBorderColor,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Cover Photo Tag on First Image
                                        if (index == 0) {
                                            Surface(
                                                color = Color(0xFF0D9488),
                                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "कव्हर",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Remove Photo Button
                                        IconButton(
                                            onClick = {
                                                val mutable = uploadedPhotos.toMutableList()
                                                mutable.removeAt(index)
                                                uploadedPhotos = mutable
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color(0xFFDC2626), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "काढून टाका",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Uploading Progress Indicator
                        if (isUploading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF0D9488)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uploadingMessage.ifBlank { "फोटो अपलोड होत आहे..." },
                                    fontSize = 12.sp,
                                    color = Color(0xFF0D9488),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Action buttons for adding photos (only if slots available)
                        if (uploadedPhotos.size < maxPhotos) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchCamera() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    enabled = !isUploading
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कॅमेरा", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { singlePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.1f).height(34.dp),
                                    enabled = !isUploading
                                ) {
                                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("क्रॉप फोटो", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { multiplePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.3f).height(34.dp),
                                    enabled = !isUploading
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("गॅलरी (+फोटो)", fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = "💡 ३ ते ५ फोटो जोडू शकता. पहिला फोटो कव्हर म्हणून दिसेल.",
                                fontSize = 10.5.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        } else {
                            Text(
                                text = "✅ कमाल ५ फोटो मर्यादा पूर्ण झाली आहे.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
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
                        val coverPhoto = uploadedPhotos.firstOrNull() ?: ""
                        val photosJson = org.json.JSONArray(uploadedPhotos).toString()
                        onSave(
                            businessName,
                            ownerName,
                            category,
                            description,
                            contactNumber,
                            whatsappNumber.ifBlank { contactNumber },
                            address,
                            coverPhoto,
                            photosJson
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isUploading
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAnalyticsReportDialog(
    viewModel: MandalViewModel,
    businesses: List<BusinessListing>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var availableMonths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf("सर्व") }
    var monthClicks by remember { mutableStateOf<List<BusinessLeadClick>>(emptyList()) }
    var isLoadingClicks by remember { mutableStateOf(false) }

    // Load available months
    LaunchedEffect(Unit) {
        val months = withContext(Dispatchers.IO) {
            viewModel.getAvailableReportMonths()
        }
        availableMonths = months
        if (months.isNotEmpty()) {
            selectedMonth = months.first()
        }
    }

    // Load clicks when month changes
    LaunchedEffect(selectedMonth) {
        isLoadingClicks = true
        monthClicks = withContext(Dispatchers.IO) {
            if (selectedMonth == "सर्व") {
                viewModel.getLeadClicksForMonth("")
            } else {
                viewModel.getLeadClicksForMonth(selectedMonth)
            }
        }
        isLoadingClicks = false
    }

    // Compute aggregated statistics per business for the selected period
    val businessStats = remember(businesses, monthClicks, selectedMonth) {
        businesses.map { b ->
            val clicksForBusiness = monthClicks.filter { it.businessId == b.id }
            val callCount = if (selectedMonth == "सर्व" && clicksForBusiness.isEmpty()) b.callClicks else clicksForBusiness.count { it.clickType == "CALL" }
            val waCount = if (selectedMonth == "सर्व" && clicksForBusiness.isEmpty()) b.whatsappClicks else clicksForBusiness.count { it.clickType == "WHATSAPP" }
            Triple(b, callCount, waCount)
        }.sortedByDescending { it.second + it.third }
    }

    val totalCalls = remember(businessStats) { businessStats.sumOf { it.second } }
    val totalWhatsApp = remember(businessStats) { businessStats.sumOf { it.third } }
    val totalLeads = totalCalls + totalWhatsApp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "व्यावसायिक मासिक अहवाल",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "कॉल व WhatsApp क्लिक्स विश्लेषण",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Month Selector Chips
                Text(
                    text = "महिना निवडा:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedMonth == "सर्व",
                            onClick = { selectedMonth = "सर्व" },
                            label = { Text("सर्व काळ (All Time)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    items(availableMonths) { month ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { selectedMonth = month },
                            label = { Text(month) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Calls
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDFA),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF99F6E4))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("थेट कॉल", fontSize = 11.sp, color = Color(0xFF0F766E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalCalls",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }

                    // Total WhatsApp
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("WhatsApp", fontSize = 11.sp, color = Color(0xFF166534))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalWhatsApp",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    // Total Leads
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("एकूण संपर्क", fontSize = 11.sp, color = Color(0xFF1E40AF))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalLeads",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown list
                Text(
                    text = "व्यवसायनिहाय कामगिरी (रँकिंग)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (businessStats.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("कोणताही व्यवसाय आढळला नाही.", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(businessStats) { index, (b, calls, wa) ->
                                val sum = calls + wa
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.7.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rank badge
                                        Surface(
                                            color = when (index) {
                                                0 -> Color(0xFFFEF3C7)
                                                1 -> Color(0xFFF1F5F9)
                                                2 -> Color(0xFFFED7AA)
                                                else -> Color(0xFFF3F4F6)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (index) {
                                                        0 -> Color(0xFFB45309)
                                                        1 -> Color(0xFF475569)
                                                        2 -> Color(0xFFC2410C)
                                                        else -> Color(0xFF6B7280)
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.businessName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${b.category} • ${b.ownerName.ifBlank { "संचालक" }}",
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                color = Color(0xFFEFF6FF),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "$sum लीड्स",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "📞 $calls  💬 $wa",
                                                fontSize = 10.5.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Download Excel (CSV) & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    BusinessAnalyticsExcelExporter.saveAndGetUri(
                                        context = context,
                                        monthYear = selectedMonth,
                                        businesses = businesses,
                                        clicks = monthClicks
                                    )
                                }
                                if (result != null) {
                                    viewModel.showSnackbar("✅ अहवाल सेव्ह झाला: ${result.first.name}")
                                    BusinessAnalyticsExcelExporter.openCsvFile(context, result.second)
                                } else {
                                    viewModel.showSnackbar("अहवाल सेव्ह करण्यात त्रुटी आली.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel डाऊनलोड", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    BusinessAnalyticsExcelExporter.saveAndGetUri(
                                        context = context,
                                        monthYear = selectedMonth,
                                        businesses = businesses,
                                        clicks = monthClicks
                                    )
                                }
                                if (result != null) {
                                    BusinessAnalyticsExcelExporter.shareCsvFile(
                                        context = context,
                                        fileUri = result.second,
                                        monthYear = selectedMonth
                                    )
                                } else {
                                    viewModel.showSnackbar("अहवाल तयार करताना त्रुटी आली.")
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("अहवाल शेअर", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
