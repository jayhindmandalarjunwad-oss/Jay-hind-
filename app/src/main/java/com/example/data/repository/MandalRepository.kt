package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.seed.SeedData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MandalRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val commentDao = db.commentDao()
    private val chatDao = db.chatDao()
    private val galleryDao = db.galleryDao()
    private val eventDao = db.eventDao()
    private val announcementDao = db.announcementDao()
    private val notificationDao = db.notificationDao()
    private val bannerDao = db.bannerDao()
    private val mandalInfoDao = db.mandalInfoDao()

    private val prefs = context.getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
    private val _mandalLogoUrl = MutableStateFlow<String?>(prefs.getString("mandal_logo_url", null))
    val mandalLogoUrl: StateFlow<String?> = _mandalLogoUrl.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Current logged-in user state (Starts null on fresh download to show Login Panel)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Firebase Firestore with offline cache enabled
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }

    init {
        try {
            com.google.firebase.FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "FirebaseApp init error: ${e.message}")
        }
        repositoryScope.launch {
            seedDatabaseIfEmpty()
            startFirestoreSync()
            forceSyncFromFirebase()
            // Check if user previously logged in on this device
            val savedUserId = prefs.getString("logged_user_id", null)
            if (!savedUserId.isNullOrBlank()) {
                val savedUser = userDao.getUserById(savedUserId)
                if (savedUser != null && savedUser.status == "APPROVED") {
                    _currentUser.value = savedUser.toDomain()
                }
            }
        }
    }

    suspend fun forceSyncFromFirebase() = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseSync", "Starting manual Firestore force sync...")
            // 1. Sync Users
            val userSnap = Tasks.await(firestore.collection("users").get())
            val users = userSnap.documents.mapNotNull { it.toUserEntity() }
            if (users.isNotEmpty()) {
                userDao.insertUsers(users)
                Log.d("FirebaseSync", "Fetched ${users.size} users from Firestore")
            }

            // 2. Sync Posts
            val postSnap = Tasks.await(firestore.collection("posts").get())
            val posts = postSnap.documents.mapNotNull { it.toPostEntity() }
            if (posts.isNotEmpty()) {
                postDao.insertPosts(posts)
                Log.d("FirebaseSync", "Fetched ${posts.size} posts from Firestore")
            }

            // 3. Sync Comments
            val commSnap = Tasks.await(firestore.collection("comments").get())
            val comments = commSnap.documents.mapNotNull { it.toCommentEntity() }
            if (comments.isNotEmpty()) {
                commentDao.insertComments(comments)
            }

            // 4. Sync Chat Messages
            val chatSnap = Tasks.await(firestore.collection("chat_messages").get())
            val chats = chatSnap.documents.mapNotNull { it.toChatMessageEntity() }
            if (chats.isNotEmpty()) {
                chatDao.insertMessages(chats)
            }

            // 5. Sync Announcements
            val annSnap = Tasks.await(firestore.collection("announcements").get())
            val anns = annSnap.documents.mapNotNull { it.toAnnouncementEntity() }
            if (anns.isNotEmpty()) {
                announcementDao.insertAnnouncements(anns)
            }

            // 6. Sync Events
            val eventSnap = Tasks.await(firestore.collection("events").get())
            val events = eventSnap.documents.mapNotNull { it.toEventEntity() }
            if (events.isNotEmpty()) {
                eventDao.insertEvents(events)
            }

            // 7. Sync Gallery
            val albumSnap = Tasks.await(firestore.collection("albums").get())
            val albums = albumSnap.documents.mapNotNull { it.toAlbumEntity() }
            if (albums.isNotEmpty()) galleryDao.insertAlbums(albums)

            val photoSnap = Tasks.await(firestore.collection("photos").get())
            val photos = photoSnap.documents.mapNotNull { it.toPhotoEntity() }
            if (photos.isNotEmpty()) galleryDao.insertPhotos(photos)

            val videoSnap = Tasks.await(firestore.collection("videos").get())
            val videos = videoSnap.documents.mapNotNull { it.toVideoEntity() }
            if (videos.isNotEmpty()) galleryDao.insertVideos(videos)

            // 8. Sync Banners
            val bannerSnap = Tasks.await(firestore.collection("banners").get())
            val banners = bannerSnap.documents.mapNotNull { it.toBannerEntity() }
            if (banners.isNotEmpty()) bannerDao.insertBanners(banners)

            // 9. Sync Notifications
            val notifSnap = Tasks.await(firestore.collection("notifications").get())
            val notifs = notifSnap.documents.mapNotNull { it.toNotificationEntity() }
            if (notifs.isNotEmpty()) notificationDao.insertNotifications(notifs)

            // 10. Sync Mandal Info
            val infoDoc = Tasks.await(firestore.collection("mandal_info").document("mandal_default").get())
            if (infoDoc.exists()) {
                val info = infoDoc.toMandalInfoEntity()
                if (info != null) {
                    mandalInfoDao.saveMandalInfo(info)
                    if (!info.logoUrl.isNullOrBlank()) {
                        _mandalLogoUrl.value = info.logoUrl
                    }
                }
            }
            Log.d("FirebaseSync", "Firestore force sync completed successfully.")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Firestore force sync failed: ${e.message}", e)
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingAdmin = userDao.getUserById(SeedData.defaultAdmin.id) ?: userDao.getUserByMobile("9545791089")
            if (existingAdmin == null) {
                userDao.insertUsers(SeedData.seedUsers)
                postDao.insertPosts(SeedData.seedPosts)
                commentDao.insertComments(SeedData.seedComments)
                galleryDao.insertAlbums(SeedData.seedAlbums)
                galleryDao.insertPhotos(SeedData.seedPhotos)
                galleryDao.insertVideos(SeedData.seedVideos)
                eventDao.insertEvents(SeedData.seedEvents)
                announcementDao.insertAnnouncements(SeedData.seedAnnouncements)
                notificationDao.insertNotifications(SeedData.seedNotifications)
                chatDao.insertMessages(SeedData.seedChatMessages)
                bannerDao.insertBanners(SeedData.seedBanners)
                mandalInfoDao.saveMandalInfo(SeedData.defaultMandalInfo)
            } else {
                // Ensure admin mobile number, name, and password match credentials
                userDao.updateUser(
                    existingAdmin.copy(
                        fullName = "वैभव चौगुले",
                        mobileNumber = "9545791089",
                        password = "ADMIN",
                        role = "ADMIN",
                        status = "APPROVED",
                        designation = ""
                    )
                )
                if (mandalInfoDao.getMandalInfoDirect() == null) {
                    mandalInfoDao.saveMandalInfo(SeedData.defaultMandalInfo)
                }
            }

            // Sync default Admin and initial content to Firebase Firestore if not present
            try {
                val adminDoc = Tasks.await(firestore.collection("users").document("admin_1").get())
                if (!adminDoc.exists()) {
                    Tasks.await(firestore.collection("users").document("admin_1").set(SeedData.defaultAdmin.toMap(), SetOptions.merge()))
                    for (post in SeedData.seedPosts) {
                        firestore.collection("posts").document(post.id).set(post.toMap(), SetOptions.merge())
                    }
                    for (banner in SeedData.seedBanners) {
                        firestore.collection("banners").document(banner.id).set(banner.toMap(), SetOptions.merge())
                    }
                    for (album in SeedData.seedAlbums) {
                        firestore.collection("albums").document(album.id).set(album.toMap(), SetOptions.merge())
                    }
                    for (photo in SeedData.seedPhotos) {
                        firestore.collection("photos").document(photo.id).set(photo.toMap(), SetOptions.merge())
                    }
                    for (video in SeedData.seedVideos) {
                        firestore.collection("videos").document(video.id).set(video.toMap(), SetOptions.merge())
                    }
                    for (event in SeedData.seedEvents) {
                        firestore.collection("events").document(event.id).set(event.toMap(), SetOptions.merge())
                    }
                    for (ann in SeedData.seedAnnouncements) {
                        firestore.collection("announcements").document(ann.id).set(ann.toMap(), SetOptions.merge())
                    }
                    firestore.collection("mandal_info").document("mandal_default").set(SeedData.defaultMandalInfo.toMap(), SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Seed database Firestore error: ${e.message}", e)
            }
        }
    }

    // REAL-TIME FIRESTORE SYNCHRONIZATION
    private fun startFirestoreSync() {
        try {
            // Real-time Users Sync (Members, Registrations, Approvals)
            firestore.collection("users").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Users snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val users = snapshots.documents.mapNotNull { it.toUserEntity() }
                    if (users.isNotEmpty()) {
                        userDao.insertUsers(users)
                        Log.d("FirebaseSync", "Real-time sync: updated ${users.size} users")
                    }
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            userDao.deleteUser(change.document.id)
                        }
                    }
                    val currentId = _currentUser.value?.id
                    if (currentId != null) {
                        val updated = userDao.getUserById(currentId)
                        if (updated != null) {
                            _currentUser.value = updated.toDomain()
                        }
                    }
                }
            }

            // Real-time Posts Sync
            firestore.collection("posts").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Posts snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val posts = snapshots.documents.mapNotNull { it.toPostEntity() }
                    if (posts.isNotEmpty()) {
                        postDao.insertPosts(posts)
                    }
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            postDao.deletePost(change.document.id)
                        }
                    }
                }
            }

            // Real-time Comments Sync
            firestore.collection("comments").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Comments snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val comments = snapshots.documents.mapNotNull { it.toCommentEntity() }
                    if (comments.isNotEmpty()) {
                        commentDao.insertComments(comments)
                    }
                }
            }

            // Real-time Chat Messages Sync
            firestore.collection("chat_messages").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Chat snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val messages = snapshots.documents.mapNotNull { it.toChatMessageEntity() }
                    if (messages.isNotEmpty()) {
                        chatDao.insertMessages(messages)
                    }
                }
            }

            // Real-time Announcements Sync
            firestore.collection("announcements").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Announcements snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toAnnouncementEntity() }
                    if (list.isNotEmpty()) {
                        announcementDao.insertAnnouncements(list)
                    }
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            announcementDao.deleteAnnouncement(change.document.id)
                        }
                    }
                }
            }

            // Real-time Events Sync
            firestore.collection("events").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Events snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toEventEntity() }
                    if (list.isNotEmpty()) {
                        eventDao.insertEvents(list)
                    }
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            eventDao.deleteEvent(change.document.id)
                        }
                    }
                }
            }

            // Real-time Gallery Albums, Photos, Videos
            firestore.collection("albums").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Albums snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toAlbumEntity() }
                    if (list.isNotEmpty()) galleryDao.insertAlbums(list)
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) galleryDao.deleteAlbum(change.document.id)
                    }
                }
            }

            firestore.collection("photos").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Photos snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toPhotoEntity() }
                    if (list.isNotEmpty()) galleryDao.insertPhotos(list)
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) galleryDao.deletePhoto(change.document.id)
                    }
                }
            }

            firestore.collection("videos").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Videos snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toVideoEntity() }
                    if (list.isNotEmpty()) galleryDao.insertVideos(list)
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) galleryDao.deleteVideo(change.document.id)
                    }
                }
            }

            // Real-time Banners Sync
            firestore.collection("banners").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Banners snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toBannerEntity() }
                    if (list.isNotEmpty()) bannerDao.insertBanners(list)
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.REMOVED) bannerDao.deleteBanner(change.document.id)
                    }
                }
            }

            // Real-time Mandal Info Sync
            firestore.collection("mandal_info").document("mandal_default").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Mandal info snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                repositoryScope.launch {
                    val info = snapshot.toMandalInfoEntity()
                    if (info != null) {
                        mandalInfoDao.saveMandalInfo(info)
                        if (!info.logoUrl.isNullOrBlank()) {
                            _mandalLogoUrl.value = info.logoUrl
                        }
                    }
                }
            }

            // Real-time Notifications Sync
            firestore.collection("notifications").addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirebaseSync", "Notifications snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                repositoryScope.launch {
                    val list = snapshots.documents.mapNotNull { it.toNotificationEntity() }
                    if (list.isNotEmpty()) notificationDao.insertNotifications(list)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "startFirestoreSync error: ${e.message}", e)
        }
    }

    // AUTH & USERS
    suspend fun login(mobile: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val cleanPass = pass.trim()

        var user = userDao.getUserByMobile(cleanMobile)

        // If not found in local Room or status is still PENDING_APPROVAL locally, fetch latest from Firestore
        try {
            val queryTask = firestore.collection("users").whereEqualTo("mobileNumber", cleanMobile).get()
            val snapshot = Tasks.await(queryTask)
            val doc = snapshot.documents.firstOrNull()
            if (doc != null) {
                val remoteUser = doc.toUserEntity()
                if (remoteUser != null) {
                    userDao.insertUser(remoteUser)
                    user = remoteUser
                }
            }
        } catch (e: Exception) {
            // Fall back to local DB
        }

        if (user == null) {
            return@withContext Result.failure(Exception("हा मोबाईल नंबर नोंदणीकृत नाही. कृपया नोंदणी करा."))
        }
        if (user.password != cleanPass) {
            return@withContext Result.failure(Exception("पासवर्ड चुकीचा आहे. कृपया पुन्हा तपासा."))
        }
        if (user.status == "PENDING_APPROVAL") {
            return@withContext Result.failure(Exception("आपले खाते मंजुरीच्या प्रतीक्षेत आहे (Pending Approval). मंडळाच्या ॲडमिनने मंजुरी दिल्यावर आपण लॉगिन करू शकाल."))
        }
        if (user.status == "REJECTED" || user.status == "BLOCKED") {
            return@withContext Result.failure(Exception("आपले खाते निलंबित किंवा नामंजूर करण्यात आले आहे. कृपया मंडळाशी संपर्क साधा."))
        }

        val domainUser = user.toDomain()
        prefs.edit().putString("logged_user_id", domainUser.id).apply()
        _currentUser.value = domainUser
        Result.success(domainUser)
    }

    fun logout() {
        prefs.edit().remove("logged_user_id").apply()
        _currentUser.value = null
    }

    suspend fun registerMember(
        fullName: String,
        mobileNumber: String,
        password: String,
        profilePhotoUrl: String,
        gender: String,
        bloodGroup: String,
        dateOfBirth: String,
        address: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanMobile = mobileNumber.trim()

        // Check local Room DB
        val existing = userDao.getUserByMobile(cleanMobile)
        if (existing != null) {
            return@withContext Result.failure(Exception("हा मोबाईल नंबर आधीच नोंदणीकृत आहे."))
        }

        // Check Firestore
        try {
            val queryTask = firestore.collection("users").whereEqualTo("mobileNumber", cleanMobile).get()
            val snapshot = Tasks.await(queryTask)
            if (!snapshot.isEmpty) {
                return@withContext Result.failure(Exception("हा मोबाईल नंबर आधीच नोंदणीकृत आहे."))
            }
        } catch (e: Exception) {
            // Offline fallback
        }

        val newId = "user_" + UUID.randomUUID().toString().take(8)
        val entity = UserEntity(
            id = newId,
            fullName = fullName.trim(),
            mobileNumber = cleanMobile,
            password = password.trim(),
            profilePhotoUrl = profilePhotoUrl.ifEmpty {
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80"
            },
            gender = gender,
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth,
            address = address.trim(),
            role = "MEMBER",
            designation = "",
            status = "PENDING_APPROVAL", // Goes to admin approval!
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(entity)

        // Add admin notification
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notifEntity = NotificationEntity(
            id = notifId,
            title = "नवीन सभासद नोंदणी (मंजुरी प्रतीक्षा)",
            message = "${fullName.trim()} यांनी नवीन सभासदत्व नोंदणी केली आहे. कृपया Admin Panel मधून मंजुरी द्या.",
            type = "ADMIN",
            targetUserId = "ADMIN",
            targetRoute = "ADMIN_PENDING",
            targetId = newId,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notifEntity)

        // Push to Firebase Firestore so Admin on any phone sees it in real time
        try {
            firestore.collection("users").document(newId).set(entity.toMap(), SetOptions.merge())
                .addOnSuccessListener { Log.d("FirebaseSync", "Registered user $newId synced to Firestore") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to sync user $newId to Firestore", e) }
            firestore.collection("notifications").document(notifId).set(notifEntity.toMap(), SetOptions.merge())
                .addOnSuccessListener { Log.d("FirebaseSync", "Notification $notifId synced to Firestore") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to sync notification $notifId to Firestore", e) }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error pushing registration to Firestore", e)
        }

        Result.success("नोंदणी यशस्वी झाली! आपले खाते 'Pending Approval' मध्ये आहे. मंडळाच्या ॲडमिन मंजुरीनंतर आपण लॉगिन करू शकाल.")
    }

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        gender: String,
        bloodGroup: String,
        dateOfBirth: String,
        address: String,
        profilePhotoUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("सभासद सापडला नाही"))
        val updated = user.copy(
            fullName = fullName,
            gender = gender,
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth,
            address = address,
            profilePhotoUrl = profilePhotoUrl.ifEmpty { user.profilePhotoUrl }
        )
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated.toDomain()
        }
        try {
            firestore.collection("users").document(userId).set(updated.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating profile on Firestore", e)
        }
        Result.success(Unit)
    }

    suspend fun changePassword(userId: String, oldPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("सभासद सापडला नाही"))
        if (user.password != oldPass) {
            return@withContext Result.failure(Exception("सध्याचा पासवर्ड चुकीचा आहे."))
        }
        val updated = user.copy(password = newPass)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated.toDomain()
        }
        try {
            firestore.collection("users").document(userId).set(mapOf("password" to newPass), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error changing password on Firestore", e)
        }
        Result.success(Unit)
    }

    // USERS LISTS & BIRTHDAYS
    val approvedMembers: Flow<List<User>> = userDao.getApprovedUsers().map { list -> list.map { it.toDomain() } }
    val pendingMembers: Flow<List<User>> = userDao.getPendingUsers().map { list -> list.map { it.toDomain() } }
    val allMembers: Flow<List<User>> = userDao.getAllUsers().map { list -> list.map { it.toDomain() } }

    // Strict birthday filtering: Only display if TODAY matches user's exact birthday (day and month)
    val todayBirthdayMembers: Flow<List<User>> = approvedMembers.map { members ->
        val sdfMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault())
        val currentMonthDay = sdfMonthDay.format(Date())
        members.filter { member ->
            val dob = member.dateOfBirth.trim()
            if (dob.isBlank()) return@filter false
            try {
                val parts = if (dob.contains("-")) dob.split("-") else if (dob.contains("/")) dob.split("/") else emptyList()
                if (parts.size == 3) {
                    val (m, d) = if (parts[0].length == 4) {
                        parts[1].padStart(2, '0') to parts[2].padStart(2, '0')
                    } else {
                        parts[1].padStart(2, '0') to parts[0].padStart(2, '0')
                    }
                    "$m-$d" == currentMonthDay
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    // ADMIN ACTIONS ON USERS & ROLE MANAGEMENT
    suspend fun changeUserRole(userId: String, newRole: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext
        val updated = user.copy(role = newRole)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated.toDomain()
        }
        try {
            firestore.collection("users").document(userId).set(mapOf("role" to newRole), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error changing user role on Firestore", e)
        }
    }

    suspend fun setMemberStatus(userId: String, status: String) = withContext(Dispatchers.IO) {
        userDao.updateUserStatus(userId, status)
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                firestore.collection("users").document(userId).set(user.toMap(), SetOptions.merge())
                    .addOnSuccessListener { Log.d("FirebaseSync", "Member $userId status successfully set to $status on Firestore") }
                    .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to update member $userId status on Firestore", e) }
            } else {
                firestore.collection("users").document(userId).set(mapOf("status" to status), SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error setting member status on Firestore", e)
        }
    }

    suspend fun deleteMember(userId: String) = withContext(Dispatchers.IO) {
        userDao.deleteUser(userId)
        try {
            firestore.collection("users").document(userId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting member on Firestore", e)
        }
    }

    suspend fun deleteMemberAndTransferRights(userId: String) = withContext(Dispatchers.IO) {
        val admin = userDao.getUserById("admin_1") ?: userDao.getUserByMobile("9545791089")
        if (admin != null) {
            postDao.reassignPostsAuthor(
                oldAuthorId = userId,
                newAuthorId = admin.id,
                newAuthorName = admin.fullName,
                newAuthorPhotoUrl = admin.profilePhotoUrl
            )
        }
        userDao.deleteUser(userId)
        try {
            firestore.collection("users").document(userId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting member and transferring rights on Firestore", e)
        }
    }

    // POSTS & FEED
    val posts: Flow<List<Post>> = postDao.getAllPosts().map { list -> list.map { it.toDomain() } }

    suspend fun createPost(
        content: String,
        imageUrl: String?,
        videoUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("कृपया प्रथम लॉगिन करा"))
        val newPost = PostEntity(
            id = "post_" + UUID.randomUUID().toString().take(8),
            authorId = user.id,
            authorName = user.fullName,
            authorPhotoUrl = user.profilePhotoUrl,
            authorRole = if (user.isAdmin) "ADMIN" else "",
            content = content.trim(),
            imageUrlsJson = imageUrl ?: "",
            videoUrl = videoUrl,
            likedUserIdsJson = "",
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        postDao.insertPost(newPost)

        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = "${user.fullName} यांनी नवीन पोस्ट केली 🚩",
            message = if (content.isNotBlank()) content.take(60) else "नवीन फोटो किंवा माहिती पोस्ट केली आहे.",
            type = "POST",
            targetRoute = "POST_COMMENTS",
            targetId = newPost.id,
            targetExtra = content.take(40),
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("posts").document(newPost.id).set(newPost.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
            Log.d("FirebaseSync", "New post ${newPost.id} and notification published to Firestore")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to upload post to Firestore: ${e.message}", e)
        }
        Result.success(Unit)
    }

    suspend fun toggleLikePost(postId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val post = postDao.getAllPosts().first().find { it.id == postId } ?: return@withContext
        val currentLikes = post.likedUserIdsJson.split(",").filter { it.isNotBlank() }.toMutableList()
        if (currentLikes.contains(user.id)) {
            currentLikes.remove(user.id)
        } else {
            currentLikes.add(user.id)
        }
        val updatedLikesJson = currentLikes.joinToString(",")
        postDao.updatePostLikes(postId, updatedLikesJson)
        try {
            firestore.collection("posts").document(postId).set(mapOf("likedUserIdsJson" to updatedLikesJson), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating post likes on Firestore", e)
        }
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        postDao.deletePost(postId)
        try {
            firestore.collection("posts").document(postId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting post on Firestore", e)
        }
    }

    // COMMENTS
    fun getComments(postId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPost(postId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addComment(postId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        val comment = CommentEntity(
            id = "comm_" + UUID.randomUUID().toString().take(8),
            postId = postId,
            authorId = user.id,
            authorName = user.fullName,
            authorPhotoUrl = user.profilePhotoUrl,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)
        postDao.incrementCommentsCount(postId)

        val post = postDao.getAllPosts().first().find { it.id == postId }
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        var notifEntity: NotificationEntity? = null
        if (post != null && post.authorId != user.id) {
            notifEntity = NotificationEntity(
                id = notifId,
                title = "आपल्या पोस्टवर नवीन कमेंट 💬",
                message = "${user.fullName} यांनी आपल्या पोस्टवर कमेंट केली: \"${text.take(45)}\"",
                type = "COMMENT",
                targetUserId = post.authorId,
                targetRoute = "POST_COMMENTS",
                targetId = postId,
                targetExtra = post.content.take(30),
                timestamp = System.currentTimeMillis()
            )
            notificationDao.insertNotification(notifEntity)
        }

        try {
            firestore.collection("comments").document(comment.id).set(comment.toMap(), SetOptions.merge())
            firestore.collection("posts").document(postId).set(mapOf("commentsCount" to ((post?.commentsCount ?: 0) + 1)), SetOptions.merge())
            if (notifEntity != null) {
                firestore.collection("notifications").document(notifId).set(notifEntity.toMap(), SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding comment on Firestore", e)
        }
        Result.success(Unit)
    }

    // REAL-TIME CHAT
    fun getConversationMessages(conversationId: String, userA: String, userB: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesBetweenUsers(conversationId, userA, userB).map { list -> list.map { it.toDomain() } }
    }

    fun getConversationMessages(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForConversation(conversationId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun sendMessage(
        receiverId: String,
        receiverName: String,
        messageText: String,
        imageUrl: String? = null,
        attachmentType: String? = null,
        attachmentUrl: String? = null,
        attachmentName: String? = null,
        attachmentExtra: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        val convId = getConversationId(user.id, receiverId)
        val finalImageUrl = imageUrl ?: if (attachmentType == "IMAGE") attachmentUrl else null
        val msg = ChatMessageEntity(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            conversationId = convId,
            senderId = user.id,
            receiverId = receiverId,
            senderName = user.fullName,
            senderPhotoUrl = user.profilePhotoUrl,
            messageText = messageText.trim(),
            imageUrl = finalImageUrl,
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            attachmentExtra = attachmentExtra,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        chatDao.insertMessage(msg)

        val preview = if (messageText.isNotBlank()) messageText.take(50) else when (attachmentType) {
            "IMAGE" -> "📷 फोटो पाठवला आहे"
            "VIDEO" -> "🎥 व्हिडिओ पाठवला आहे"
            "DOCUMENT" -> "📄 डॉक्युमेंट: ${attachmentName ?: ""}"
            "CONTACT" -> "👤 संपर्क क्रमांक: ${attachmentName ?: ""}"
            else -> "नवीन संदेश प्राप्त झाला"
        }
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = "${user.fullName} कडून मेसेज",
            message = preview,
            type = "CHAT",
            targetUserId = receiverId,
            targetRoute = "CHAT",
            targetId = user.id,
            targetExtra = user.fullName,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("chat_messages").document(msg.id).set(msg.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
            Log.d("FirebaseSync", "Chat message sent to Firestore: ${msg.id}")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error sending chat message on Firestore", e)
        }
        Result.success(Unit)
    }

    fun getConversationSummaries(currentUserId: String): Flow<List<ChatConversationSummary>> {
        return combine(
            chatDao.getAllMessagesForUser(currentUserId),
            allMembers
        ) { messages, members ->
            val memberMap = members.associateBy { it.id }
            val grouped = messages.groupBy { msg ->
                if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
            }
            grouped.mapNotNull { (otherId, msgs) ->
                val lastMsg = msgs.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                val otherUser = memberMap[otherId] ?: return@mapNotNull null
                val unread = msgs.count { it.receiverId == currentUserId && !it.isRead }
                val displayMsg = if (lastMsg.messageText.isNotBlank()) {
                    lastMsg.messageText
                } else when (lastMsg.attachmentType) {
                    "IMAGE" -> "📷 फोटो"
                    "VIDEO" -> "🎥 व्हिडिओ"
                    "DOCUMENT" -> "📄 ${lastMsg.attachmentName ?: "दस्तावेज"}"
                    "CONTACT" -> "👤 ${lastMsg.attachmentName ?: "संपर्क"}"
                    else -> "संदेश"
                }
                ChatConversationSummary(
                    otherUser = otherUser,
                    lastMessage = displayMsg,
                    lastTimestamp = lastMsg.timestamp,
                    unreadCount = unread
                )
            }.sortedByDescending { it.lastTimestamp }
        }
    }

    suspend fun markChatAsRead(conversationId: String, currentUserId: String, partnerId: String = "") = withContext(Dispatchers.IO) {
        if (partnerId.isNotBlank()) {
            chatDao.markMessagesAsReadBetween(conversationId, currentUserId, partnerId)
        } else {
            chatDao.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun getConversationId(userA: String, userB: String): String {
        return if (userA < userB) "conv_${userA}_${userB}" else "conv_${userB}_${userA}"
    }

    // GALLERY (ALBUMS, PHOTOS, VIDEOS)
    val albums: Flow<List<Album>> = galleryDao.getAllAlbums().map { list -> list.map { it.toDomain() } }
    val videos: Flow<List<VideoItem>> = galleryDao.getAllVideos().map { list -> list.map { it.toDomain() } }

    fun getPhotosForAlbum(albumId: String): Flow<List<GalleryPhoto>> {
        return galleryDao.getPhotosForAlbum(albumId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun createAlbum(title: String, category: String, coverImageUrl: String, description: String) = withContext(Dispatchers.IO) {
        val album = AlbumEntity(
            id = "album_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            category = category.trim(),
            coverImageUrl = coverImageUrl.ifEmpty { "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80" },
            description = description.trim(),
            photoCount = 0
        )
        galleryDao.insertAlbum(album)
        try {
            firestore.collection("albums").document(album.id).set(album.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error creating album on Firestore", e)
        }
    }

    suspend fun addPhotoToAlbum(albumId: String, imageUrl: String, caption: String) = withContext(Dispatchers.IO) {
        val photo = PhotoEntity(
            id = "ph_" + UUID.randomUUID().toString().take(8),
            albumId = albumId,
            imageUrl = imageUrl,
            caption = caption.trim()
        )
        galleryDao.insertPhoto(photo)
        try {
            firestore.collection("photos").document(photo.id).set(photo.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding photo on Firestore", e)
        }
    }

    suspend fun deleteAlbum(albumId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteAlbum(albumId)
        try {
            firestore.collection("albums").document(albumId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting album on Firestore", e)
        }
    }

    suspend fun deletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        galleryDao.deletePhoto(photoId)
        try {
            firestore.collection("photos").document(photoId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting photo on Firestore", e)
        }
    }

    suspend fun addVideo(title: String, description: String, category: String, videoUrl: String, thumbnailUrl: String) = withContext(Dispatchers.IO) {
        val video = VideoEntity(
            id = "vid_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            description = description.trim(),
            category = category.trim(),
            videoUrl = videoUrl.trim(),
            thumbnailUrl = thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80" }
        )
        galleryDao.insertVideo(video)
        try {
            firestore.collection("videos").document(video.id).set(video.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding video on Firestore", e)
        }
    }

    suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteVideo(videoId)
        try {
            firestore.collection("videos").document(videoId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting video on Firestore", e)
        }
    }

    // EVENTS
    val events: Flow<List<MandalEvent>> = eventDao.getAllEvents().map { list -> list.map { it.toDomain() } }

    suspend fun createEvent(
        title: String,
        date: String,
        time: String,
        location: String,
        description: String,
        imageUrl: String,
        category: String = ""
    ) = withContext(Dispatchers.IO) {
        val event = EventEntity(
            id = "event_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            date = date.trim(),
            time = time.trim(),
            location = location.trim(),
            description = description.trim(),
            imageUrl = imageUrl.ifEmpty { "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80" },
            category = category.trim()
        )
        eventDao.insertEvent(event)

        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = "नवीन कार्यक्रम: $title 🚩",
            message = "$date रोजी $location येथे '$title' आयोजित करण्यात आला आहे.",
            type = "EVENT",
            targetRoute = "EVENTS",
            targetId = event.id,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("events").document(event.id).set(event.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error creating event on Firestore", e)
        }
    }

    suspend fun updateEvent(
        eventId: String,
        title: String,
        date: String,
        time: String,
        location: String,
        description: String,
        imageUrl: String
    ) = withContext(Dispatchers.IO) {
        val existing = eventDao.getAllEvents().first().find { it.id == eventId }
        val updated = EventEntity(
            id = eventId,
            title = title.trim(),
            date = date.trim(),
            time = time.trim(),
            location = location.trim(),
            description = description.trim(),
            imageUrl = imageUrl.ifEmpty { existing?.imageUrl ?: "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80" },
            category = "",
            isRegistered = existing?.isRegistered ?: false,
            attendeesCount = existing?.attendeesCount ?: 0
        )
        eventDao.updateEvent(updated)
        try {
            firestore.collection("events").document(eventId).set(updated.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating event on Firestore", e)
        }
    }

    suspend fun toggleEventRegistration(eventId: String, isRegistered: Boolean) = withContext(Dispatchers.IO) {
        eventDao.toggleEventRegistration(eventId, !isRegistered)
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        eventDao.deleteEvent(eventId)
        try {
            firestore.collection("events").document(eventId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting event on Firestore", e)
        }
    }

    // ANNOUNCEMENTS
    val announcements: Flow<List<Announcement>> = announcementDao.getAllAnnouncements().map { list -> list.map { it.toDomain() } }

    suspend fun createAnnouncement(title: String, content: String, priority: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        val ann = AnnouncementEntity(
            id = "ann_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            content = content.trim(),
            priority = priority,
            date = SimpleDateFormat("dd MMMM yyyy", Locale("mr", "IN")).format(Date()),
            author = user?.fullName ?: "मंडळ कार्यकारणी"
        )
        announcementDao.insertAnnouncement(ann)

        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = "महत्वाची सूचना: $title 📢",
            message = content.take(70),
            type = "ANNOUNCEMENT",
            targetRoute = "ANNOUNCEMENTS",
            targetId = ann.id,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("announcements").document(ann.id).set(ann.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error creating announcement on Firestore", e)
        }
    }

    suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
        announcementDao.deleteAnnouncement(id)
        try {
            firestore.collection("announcements").document(id).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting announcement on Firestore", e)
        }
    }

    // NOTIFICATIONS
    val notifications: Flow<List<MandalNotification>> = combine(
        notificationDao.getAllNotifications(),
        _currentUser,
        todayBirthdayMembers
    ) { allNotifs, user, birthdayMembers ->
        val filtered = allNotifs.filter { notif ->
            when (notif.type) {
                "COMMENT" -> notif.targetUserId == user?.id
                "CHAT" -> notif.targetUserId == user?.id
                "ADMIN" -> user?.isAdmin == true
                "BIRTHDAY" -> birthdayMembers.isNotEmpty()
                else -> {
                    notif.targetUserId == null || notif.targetUserId == user?.id || (notif.targetUserId == "ADMIN" && user?.isAdmin == true)
                }
            }
        }.map { it.toDomain() }

        if (birthdayMembers.isNotEmpty() && filtered.none { it.type == "BIRTHDAY" }) {
            val names = birthdayMembers.take(2).joinToString(" व ") { it.fullName } + (if (birthdayMembers.size > 2) " आणि इतर" else "")
            val dynBirthday = MandalNotification(
                id = "dyn_bday_today",
                title = "आज वाढदिवस आहे! 🎂🎉",
                message = "आज आपले सहकारी सभासद $names यांचा वाढदिवस आहे. त्यांना हार्दिक शुभेच्छा द्या!",
                type = "BIRTHDAY",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                targetRoute = "BIRTHDAYS",
                targetId = birthdayMembers.firstOrNull()?.id
            )
            listOf(dynBirthday) + filtered
        } else {
            filtered
        }
    }

    val unreadNotificationsCount: Flow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        if (!id.startsWith("dyn_")) {
            notificationDao.markAsRead(id)
        }
    }

    suspend fun broadcastNotification(title: String, message: String) = withContext(Dispatchers.IO) {
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = title.trim(),
            message = message.trim(),
            type = "ADMIN",
            targetRoute = "ANNOUNCEMENTS",
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)
        try {
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error broadcasting notification on Firestore", e)
        }
    }

    // BANNERS MANAGEMENT
    val banners: Flow<List<MandalBanner>> = bannerDao.getAllBanners().map { list -> list.map { it.toDomain() } }

    suspend fun addBanner(imageUrl: String, title: String = "", subtitle: String = "", actionUrl: String = "") = withContext(Dispatchers.IO) {
        val banner = BannerEntity(
            id = "banner_" + UUID.randomUUID().toString().take(8),
            imageUrl = imageUrl.trim(),
            title = title.trim(),
            subtitle = subtitle.trim(),
            actionUrl = actionUrl.trim(),
            orderIndex = System.currentTimeMillis().toInt()
        )
        bannerDao.insertBanner(banner)
        try {
            firestore.collection("banners").document(banner.id).set(banner.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding banner on Firestore", e)
        }
    }

    suspend fun updateBanner(id: String, imageUrl: String, title: String = "", subtitle: String = "", actionUrl: String = "") = withContext(Dispatchers.IO) {
        val banner = BannerEntity(
            id = id,
            imageUrl = imageUrl.trim(),
            title = title.trim(),
            subtitle = subtitle.trim(),
            actionUrl = actionUrl.trim()
        )
        bannerDao.updateBanner(banner)
        try {
            firestore.collection("banners").document(id).set(banner.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating banner on Firestore", e)
        }
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        bannerDao.deleteBanner(id)
        try {
            firestore.collection("banners").document(id).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting banner on Firestore", e)
        }
    }

    // MANDAL INFO
    val mandalInfo: Flow<MandalInfo> = mandalInfoDao.getMandalInfo().map {
        it?.toDomain() ?: SeedData.defaultMandalInfo.toDomain()
    }

    suspend fun updateMandalInfo(
        mandalName: String,
        tagline: String,
        locationTitle: String,
        aboutDescription: String,
        email: String,
        address: String,
        phone: String,
        youtubeHandle: String,
        facebookHandle: String,
        instagramHandle: String,
        adminWebLink: String
    ) = withContext(Dispatchers.IO) {
        val entity = MandalInfoEntity(
            id = "mandal_default",
            mandalName = mandalName.trim(),
            tagline = tagline.trim(),
            locationTitle = locationTitle.trim(),
            aboutDescription = aboutDescription.trim(),
            email = email.trim(),
            address = address.trim(),
            phone = phone.trim(),
            youtubeHandle = youtubeHandle.trim(),
            facebookHandle = facebookHandle.trim(),
            instagramHandle = instagramHandle.trim(),
            adminWebLink = adminWebLink.trim(),
            updatedAt = System.currentTimeMillis()
        )
        mandalInfoDao.saveMandalInfo(entity)
        try {
            firestore.collection("mandal_info").document("mandal_default").set(entity.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating mandal info on Firestore", e)
        }
    }

    // MANDAL LOGO MANAGEMENT
    fun updateMandalLogo(url: String?) {
        val cleanUrl = url?.trim()?.ifEmpty { null }
        prefs.edit().putString("mandal_logo_url", cleanUrl).apply()
        _mandalLogoUrl.value = cleanUrl
        repositoryScope.launch {
            try {
                firestore.collection("mandal_info").document("mandal_default").set(mapOf("logoUrl" to (cleanUrl ?: "")), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating mandal logo on Firestore", e)
            }
        }
    }

    fun deleteMandalLogo() {
        prefs.edit().remove("mandal_logo_url").apply()
        _mandalLogoUrl.value = null
        repositoryScope.launch {
            try {
                firestore.collection("mandal_info").document("mandal_default").set(mapOf("logoUrl" to ""), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error deleting mandal logo on Firestore", e)
            }
        }
    }
}

// Domain Mapping Extensions
fun UserEntity.toDomain() = User(
    id = id,
    fullName = fullName,
    mobileNumber = mobileNumber,
    password = password,
    profilePhotoUrl = profilePhotoUrl,
    gender = gender,
    bloodGroup = bloodGroup,
    dateOfBirth = dateOfBirth,
    address = address,
    role = role,
    designation = designation,
    status = status,
    createdAt = createdAt,
    isOnline = isOnline,
    lastSeen = lastSeen,
    fcmToken = fcmToken
)

fun PostEntity.toDomain() = Post(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    authorRole = authorRole,
    content = content,
    imageUrls = if (imageUrlsJson.isNotBlank()) imageUrlsJson.split(",") else emptyList(),
    videoUrl = videoUrl,
    likedUserIds = if (likedUserIdsJson.isNotBlank()) likedUserIdsJson.split(",") else emptyList(),
    commentsCount = commentsCount,
    timestamp = timestamp
)

fun CommentEntity.toDomain() = Comment(
    id = id,
    postId = postId,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    text = text,
    timestamp = timestamp
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    receiverId = receiverId,
    senderName = senderName,
    senderPhotoUrl = senderPhotoUrl,
    messageText = messageText,
    imageUrl = imageUrl,
    attachmentType = attachmentType,
    attachmentUrl = attachmentUrl,
    attachmentName = attachmentName,
    attachmentExtra = attachmentExtra,
    timestamp = timestamp,
    isRead = isRead
)

fun AlbumEntity.toDomain() = Album(
    id = id,
    title = title,
    category = category,
    coverImageUrl = coverImageUrl,
    description = description,
    photoCount = photoCount,
    createdAt = createdAt
)

fun PhotoEntity.toDomain() = GalleryPhoto(
    id = id,
    albumId = albumId,
    imageUrl = imageUrl,
    caption = caption,
    uploadedAt = uploadedAt
)

fun VideoEntity.toDomain() = VideoItem(
    id = id,
    title = title,
    description = description,
    category = category,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    uploadedAt = uploadedAt
)

fun EventEntity.toDomain() = MandalEvent(
    id = id,
    title = title,
    date = date,
    time = time,
    location = location,
    description = description,
    imageUrl = imageUrl,
    category = category,
    attendeesCount = attendeesCount,
    isRegistered = isRegistered,
    createdAt = createdAt
)

fun AnnouncementEntity.toDomain() = Announcement(
    id = id,
    title = title,
    content = content,
    priority = priority,
    date = date,
    author = author,
    createdAt = createdAt
)

fun NotificationEntity.toDomain() = MandalNotification(
    id = id,
    title = title,
    message = message,
    type = type,
    timestamp = timestamp,
    isRead = isRead,
    targetUserId = targetUserId,
    targetRoute = targetRoute,
    targetId = targetId,
    targetExtra = targetExtra
)

fun BannerEntity.toDomain() = MandalBanner(
    id = id,
    imageUrl = imageUrl,
    title = title,
    subtitle = subtitle,
    actionUrl = actionUrl,
    orderIndex = orderIndex,
    createdAt = createdAt
)

fun MandalInfoEntity.toDomain() = MandalInfo(
    id = id,
    mandalName = mandalName,
    tagline = tagline,
    locationTitle = locationTitle,
    aboutDescription = aboutDescription,
    email = email,
    address = address,
    phone = phone,
    youtubeHandle = youtubeHandle,
    facebookHandle = facebookHandle,
    instagramHandle = instagramHandle,
    adminWebLink = adminWebLink,
    logoUrl = logoUrl,
    updatedAt = updatedAt
)

// Firestore Map Converters
fun UserEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "fullName" to fullName,
    "mobileNumber" to mobileNumber,
    "password" to password,
    "profilePhotoUrl" to profilePhotoUrl,
    "gender" to gender,
    "bloodGroup" to bloodGroup,
    "dateOfBirth" to dateOfBirth,
    "address" to address,
    "role" to role,
    "designation" to designation,
    "status" to status,
    "createdAt" to createdAt,
    "isOnline" to isOnline,
    "lastSeen" to lastSeen,
    "fcmToken" to fcmToken
)

fun DocumentSnapshot.toUserEntity(): UserEntity? {
    val id = getString("id") ?: id
    val mobile = getString("mobileNumber") ?: return null
    return UserEntity(
        id = id,
        fullName = getString("fullName") ?: "",
        mobileNumber = mobile,
        password = getString("password") ?: "",
        profilePhotoUrl = getString("profilePhotoUrl") ?: "",
        gender = getString("gender") ?: "",
        bloodGroup = getString("bloodGroup") ?: "",
        dateOfBirth = getString("dateOfBirth") ?: "",
        address = getString("address") ?: "",
        role = getString("role") ?: "MEMBER",
        designation = getString("designation") ?: "",
        status = getString("status") ?: "APPROVED",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        isOnline = getBoolean("isOnline") ?: false,
        lastSeen = getLong("lastSeen") ?: 0L,
        fcmToken = getString("fcmToken") ?: ""
    )
}

fun PostEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "authorId" to authorId,
    "authorName" to authorName,
    "authorPhotoUrl" to authorPhotoUrl,
    "authorRole" to authorRole,
    "content" to content,
    "imageUrlsJson" to imageUrlsJson,
    "videoUrl" to videoUrl,
    "likedUserIdsJson" to likedUserIdsJson,
    "commentsCount" to commentsCount,
    "timestamp" to timestamp
)

fun DocumentSnapshot.toPostEntity(): PostEntity? {
    val id = getString("id") ?: id
    val authorId = getString("authorId") ?: return null
    return PostEntity(
        id = id,
        authorId = authorId,
        authorName = getString("authorName") ?: "",
        authorPhotoUrl = getString("authorPhotoUrl") ?: "",
        authorRole = getString("authorRole") ?: "",
        content = getString("content") ?: "",
        imageUrlsJson = getString("imageUrlsJson") ?: "",
        videoUrl = getString("videoUrl"),
        likedUserIdsJson = getString("likedUserIdsJson") ?: "",
        commentsCount = (getLong("commentsCount") ?: 0L).toInt(),
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )
}

fun CommentEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "postId" to postId,
    "authorId" to authorId,
    "authorName" to authorName,
    "authorPhotoUrl" to authorPhotoUrl,
    "text" to text,
    "timestamp" to timestamp
)

fun DocumentSnapshot.toCommentEntity(): CommentEntity? {
    val id = getString("id") ?: id
    val postId = getString("postId") ?: return null
    return CommentEntity(
        id = id,
        postId = postId,
        authorId = getString("authorId") ?: "",
        authorName = getString("authorName") ?: "",
        authorPhotoUrl = getString("authorPhotoUrl") ?: "",
        text = getString("text") ?: "",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )
}

fun ChatMessageEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "conversationId" to conversationId,
    "senderId" to senderId,
    "receiverId" to receiverId,
    "senderName" to senderName,
    "senderPhotoUrl" to senderPhotoUrl,
    "messageText" to messageText,
    "imageUrl" to imageUrl,
    "attachmentType" to attachmentType,
    "attachmentUrl" to attachmentUrl,
    "attachmentName" to attachmentName,
    "attachmentExtra" to attachmentExtra,
    "timestamp" to timestamp,
    "isRead" to isRead
)

fun DocumentSnapshot.toChatMessageEntity(): ChatMessageEntity? {
    val id = getString("id") ?: id
    val conversationId = getString("conversationId") ?: return null
    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        senderId = getString("senderId") ?: "",
        receiverId = getString("receiverId") ?: "",
        senderName = getString("senderName") ?: "",
        senderPhotoUrl = getString("senderPhotoUrl") ?: "",
        messageText = getString("messageText") ?: "",
        imageUrl = getString("imageUrl"),
        attachmentType = getString("attachmentType"),
        attachmentUrl = getString("attachmentUrl"),
        attachmentName = getString("attachmentName"),
        attachmentExtra = getString("attachmentExtra"),
        timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
        isRead = getBoolean("isRead") ?: false
    )
}

fun AlbumEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "category" to category,
    "coverImageUrl" to coverImageUrl,
    "description" to description,
    "photoCount" to photoCount,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toAlbumEntity(): AlbumEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return AlbumEntity(
        id = id,
        title = title,
        category = getString("category") ?: "",
        coverImageUrl = getString("coverImageUrl") ?: "",
        description = getString("description") ?: "",
        photoCount = (getLong("photoCount") ?: 0L).toInt(),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
}

fun PhotoEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "albumId" to albumId,
    "imageUrl" to imageUrl,
    "caption" to caption,
    "uploadedAt" to uploadedAt
)

fun DocumentSnapshot.toPhotoEntity(): PhotoEntity? {
    val id = getString("id") ?: id
    val albumId = getString("albumId") ?: return null
    return PhotoEntity(
        id = id,
        albumId = albumId,
        imageUrl = getString("imageUrl") ?: "",
        caption = getString("caption") ?: "",
        uploadedAt = getLong("uploadedAt") ?: System.currentTimeMillis()
    )
}

fun VideoEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "category" to category,
    "videoUrl" to videoUrl,
    "thumbnailUrl" to thumbnailUrl,
    "duration" to duration,
    "uploadedAt" to uploadedAt
)

fun DocumentSnapshot.toVideoEntity(): VideoEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return VideoEntity(
        id = id,
        title = title,
        description = getString("description") ?: "",
        category = getString("category") ?: "",
        videoUrl = getString("videoUrl") ?: "",
        thumbnailUrl = getString("thumbnailUrl") ?: "",
        duration = getString("duration") ?: "",
        uploadedAt = getLong("uploadedAt") ?: System.currentTimeMillis()
    )
}

fun EventEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "date" to date,
    "time" to time,
    "location" to location,
    "description" to description,
    "imageUrl" to imageUrl,
    "category" to category,
    "attendeesCount" to attendeesCount,
    "isRegistered" to isRegistered,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toEventEntity(): EventEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return EventEntity(
        id = id,
        title = title,
        date = getString("date") ?: "",
        time = getString("time") ?: "",
        location = getString("location") ?: "",
        description = getString("description") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        category = getString("category") ?: "",
        attendeesCount = (getLong("attendeesCount") ?: 0L).toInt(),
        isRegistered = getBoolean("isRegistered") ?: false,
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
}

fun AnnouncementEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "content" to content,
    "priority" to priority,
    "date" to date,
    "author" to author,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toAnnouncementEntity(): AnnouncementEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return AnnouncementEntity(
        id = id,
        title = title,
        content = getString("content") ?: "",
        priority = getString("priority") ?: "NORMAL",
        date = getString("date") ?: "",
        author = getString("author") ?: "",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
}

fun NotificationEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "message" to message,
    "type" to type,
    "timestamp" to timestamp,
    "isRead" to isRead,
    "targetUserId" to targetUserId,
    "targetRoute" to targetRoute,
    "targetId" to targetId,
    "targetExtra" to targetExtra
)

fun DocumentSnapshot.toNotificationEntity(): NotificationEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return NotificationEntity(
        id = id,
        title = title,
        message = getString("message") ?: "",
        type = getString("type") ?: "GENERAL",
        timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
        isRead = getBoolean("isRead") ?: false,
        targetUserId = getString("targetUserId"),
        targetRoute = getString("targetRoute"),
        targetId = getString("targetId"),
        targetExtra = getString("targetExtra")
    )
}

fun BannerEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "imageUrl" to imageUrl,
    "title" to title,
    "subtitle" to subtitle,
    "actionUrl" to actionUrl,
    "orderIndex" to orderIndex,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toBannerEntity(): BannerEntity? {
    val id = getString("id") ?: id
    val imageUrl = getString("imageUrl") ?: return null
    return BannerEntity(
        id = id,
        imageUrl = imageUrl,
        title = getString("title") ?: "",
        subtitle = getString("subtitle") ?: "",
        actionUrl = getString("actionUrl") ?: "",
        orderIndex = (getLong("orderIndex") ?: 0L).toInt(),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
}

fun MandalInfoEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "mandalName" to mandalName,
    "tagline" to tagline,
    "locationTitle" to locationTitle,
    "aboutDescription" to aboutDescription,
    "email" to email,
    "address" to address,
    "phone" to phone,
    "youtubeHandle" to youtubeHandle,
    "facebookHandle" to facebookHandle,
    "instagramHandle" to instagramHandle,
    "adminWebLink" to adminWebLink,
    "logoUrl" to logoUrl,
    "updatedAt" to updatedAt
)

fun DocumentSnapshot.toMandalInfoEntity(): MandalInfoEntity? {
    val id = getString("id") ?: "mandal_default"
    val mandalName = getString("mandalName") ?: return null
    return MandalInfoEntity(
        id = id,
        mandalName = mandalName,
        tagline = getString("tagline") ?: "",
        locationTitle = getString("locationTitle") ?: "",
        aboutDescription = getString("aboutDescription") ?: "",
        email = getString("email") ?: "",
        address = getString("address") ?: "",
        phone = getString("phone") ?: "",
        youtubeHandle = getString("youtubeHandle") ?: "",
        facebookHandle = getString("facebookHandle") ?: "",
        instagramHandle = getString("instagramHandle") ?: "",
        adminWebLink = getString("adminWebLink") ?: "",
        logoUrl = getString("logoUrl") ?: "",
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
    )
}
