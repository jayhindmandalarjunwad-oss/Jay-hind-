package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.seed.SeedData
import com.example.util.MediaUtils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MandalRepository(context: Context) {
    private val appContext: Context = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
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
    private val businessDirectoryDao = db.businessDirectoryDao()

    private val prefs = context.getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
    private val _mandalLogoUrl = MutableStateFlow<String?>(prefs.getString("mandal_logo_url", null))
    val mandalLogoUrl: StateFlow<String?> = _mandalLogoUrl.asStateFlow()

    private val _sessionSecurityNotice = MutableStateFlow<String?>(null)
    val sessionSecurityNotice: StateFlow<String?> = _sessionSecurityNotice.asStateFlow()

    fun clearSessionSecurityNotice() {
        _sessionSecurityNotice.value = null
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private fun loadUserFromPrefs(): User? {
        val id = prefs.getString("logged_user_id", null) ?: return null
        if (id.isBlank()) return null
        val name = prefs.getString("logged_user_name", "") ?: ""
        val mobile = prefs.getString("logged_user_mobile", "") ?: ""
        val pass = prefs.getString("logged_user_pass", "") ?: ""
        val role = prefs.getString("logged_user_role", "MEMBER") ?: "MEMBER"
        val photo = prefs.getString("logged_user_photo", "") ?: ""
        val status = prefs.getString("logged_user_status", "APPROVED") ?: "APPROVED"
        val gender = prefs.getString("logged_user_gender", "पुरुष") ?: "पुरुष"
        val blood = prefs.getString("logged_user_blood", "O+") ?: "O+"
        val dob = prefs.getString("logged_user_dob", "1998-08-22") ?: "1998-08-22"
        val address = prefs.getString("logged_user_address", "") ?: ""
        val designation = prefs.getString("logged_user_designation", "सभासद") ?: "सभासद"
        val session = prefs.getString("logged_user_session_id", "") ?: ""

        if (status != "APPROVED") return null

        return User(
            id = id,
            fullName = name.ifEmpty { "सभासद" },
            mobileNumber = mobile,
            password = pass,
            profilePhotoUrl = photo,
            gender = gender,
            bloodGroup = blood,
            dateOfBirth = dob,
            address = address,
            role = role,
            designation = designation,
            status = status,
            activeSessionId = session
        )
    }

    private fun saveUserToPrefs(user: User?) {
        if (user != null) {
            prefs.edit()
                .putString("logged_user_id", user.id)
                .putString("logged_user_name", user.fullName)
                .putString("logged_user_mobile", user.mobileNumber)
                .putString("logged_user_pass", user.password)
                .putString("logged_user_role", user.role)
                .putString("logged_user_photo", user.profilePhotoUrl)
                .putString("logged_user_status", user.status)
                .putString("logged_user_gender", user.gender)
                .putString("logged_user_blood", user.bloodGroup)
                .putString("logged_user_dob", user.dateOfBirth)
                .putString("logged_user_address", user.address)
                .putString("logged_user_designation", user.designation)
                .putString("logged_user_session_id", user.activeSessionId)
                .apply()
        } else {
            prefs.edit()
                .remove("logged_user_id")
                .remove("logged_user_name")
                .remove("logged_user_mobile")
                .remove("logged_user_pass")
                .remove("logged_user_role")
                .remove("logged_user_photo")
                .remove("logged_user_status")
                .remove("logged_user_gender")
                .remove("logged_user_blood")
                .remove("logged_user_dob")
                .remove("logged_user_address")
                .remove("logged_user_designation")
                .remove("logged_user_session_id")
                .apply()
        }
    }

    // Current logged-in user state (Persisted across restarts)
    private val _currentUser = MutableStateFlow<User?>(loadUserFromPrefs())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Firebase live connection / status tracking
    private val _cloudSyncStatus = MutableStateFlow("Firebase चालू आहे")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    // Real-time Live Stream Comments & Reactions StateFlow (Synced instantly across all watchers)
    private val _liveComments = MutableStateFlow<List<LiveComment>>(emptyList())
    val liveComments: StateFlow<List<LiveComment>> = _liveComments.asStateFlow()

    // Real-time Live Stream Active Viewers StateFlow (Synced live across all watching members)
    private val _realtimeLiveViewerCount = MutableStateFlow(0)
    val realtimeLiveViewerCount: StateFlow<Int> = _realtimeLiveViewerCount.asStateFlow()

    // Member Feedbacks StateFlow (Real-time synced, private to Super Admin)
    private val _feedbacks = MutableStateFlow<List<MemberFeedback>>(emptyList())
    val feedbacks: StateFlow<List<MemberFeedback>> = _feedbacks.asStateFlow()

    // Real-time Emergency Blood Alert StateFlow (Synced across all devices)
    private val _activeBloodAlert = MutableStateFlow<EmergencyBloodAlert?>(null)
    val activeBloodAlert: StateFlow<EmergencyBloodAlert?> = _activeBloodAlert.asStateFlow()

    private val processedChatNotificationIds = java.util.Collections.synchronizedSet(java.util.LinkedHashSet<String>())
    private val processedNotificationIds = java.util.Collections.synchronizedSet(java.util.LinkedHashSet<String>())

    private var livePresenceHeartbeatJob: Job? = null
    private var currentLivePresenceSessionId: String? = null

    // Firebase Firestore instance
    private val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.d("FirebaseSync", "Firestore settings note: ${e.message}")
        }
        db
    }

    init {
        try {
            com.google.firebase.FirebaseApp.initializeApp(context)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "FirebaseApp init error: ${e.message}")
        }
        // Start real-time Firestore synchronization immediately
        startFirestoreSync()

        repositoryScope.launch {
            seedDatabaseIfEmpty()
            forceSyncFromFirebase()
            // Refresh current user from database
            val savedUserId = prefs.getString("logged_user_id", null)
            if (!savedUserId.isNullOrBlank()) {
                val savedUser = userDao.getUserById(savedUserId)
                if (savedUser != null && savedUser.status == "APPROVED") {
                    val domain = savedUser.toDomain()
                    _currentUser.value = domain
                    saveUserToPrefs(domain)
                }
            }
        }
    }

    suspend fun forceSyncFromFirebase() = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseSync", "Starting Firestore force sync in parallel coroutines...")

            // Run independent sync tasks concurrently so that failure in one collection does not stop others
            kotlinx.coroutines.coroutineScope {
                // 1. Sync Users
                launch {
                    try {
                        val userSnap = Tasks.await(firestore.collection("users").get())
                        val users = userSnap.documents.mapNotNull { it.toUserEntity() }
                        if (users.isNotEmpty()) {
                            userDao.insertUsers(users)
                            Log.d("FirebaseSync", "Fetched ${users.size} users from Firestore")
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Users sync error: ${e.message}")
                    }
                }

                // 2. Sync Posts
                launch {
                    try {
                        val postSnap = Tasks.await(firestore.collection("posts").get())
                        val posts = postSnap.documents.mapNotNull { it.toPostEntity() }
                        if (posts.isNotEmpty()) {
                            postDao.insertPosts(posts)
                            Log.d("FirebaseSync", "Fetched ${posts.size} posts from Firestore")
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Posts sync error: ${e.message}")
                    }
                }

                // 3. Sync Comments
                launch {
                    try {
                        val commSnap = Tasks.await(firestore.collection("comments").get())
                        val comments = commSnap.documents.mapNotNull { it.toCommentEntity() }
                        if (comments.isNotEmpty()) {
                            commentDao.insertComments(comments)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Comments sync error: ${e.message}")
                    }
                }

                // 4. Sync Chat Messages
                launch {
                    try {
                        val chatSnap = Tasks.await(firestore.collection("chat_messages").get())
                        val chats = chatSnap.documents.mapNotNull { it.toChatMessageEntity() }
                        if (chats.isNotEmpty()) {
                            chatDao.insertMessages(chats)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Chats sync error: ${e.message}")
                    }
                }

                // 5. Sync Announcements
                launch {
                    try {
                        val annSnap = Tasks.await(firestore.collection("announcements").get())
                        val anns = annSnap.documents.mapNotNull { it.toAnnouncementEntity() }
                        if (anns.isNotEmpty()) {
                            announcementDao.insertAnnouncements(anns)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Announcements sync error: ${e.message}")
                    }
                }

                // 6. Sync Events
                launch {
                    try {
                        val eventSnap = Tasks.await(firestore.collection("events").get())
                        val events = eventSnap.documents.mapNotNull { it.toEventEntity() }
                        if (events.isNotEmpty()) {
                            eventDao.insertEvents(events)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Events sync error: ${e.message}")
                    }
                }

                // 7. Sync Gallery
                launch {
                    try {
                        val albumSnap = Tasks.await(firestore.collection("albums").get())
                        val albums = albumSnap.documents.mapNotNull { it.toAlbumEntity() }
                        if (albums.isNotEmpty()) galleryDao.insertAlbums(albums)

                        val photoSnap = Tasks.await(firestore.collection("photos").get())
                        val photos = photoSnap.documents.mapNotNull { it.toPhotoEntity() }
                        if (photos.isNotEmpty()) galleryDao.insertPhotos(photos)

                        val videoSnap = Tasks.await(firestore.collection("videos").get())
                        val videos = videoSnap.documents.mapNotNull { it.toVideoEntity() }
                        if (videos.isNotEmpty()) galleryDao.insertVideos(videos)
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Gallery sync error: ${e.message}")
                    }
                }

                // 8. Sync Banners
                launch {
                    try {
                        val bannerSnap = Tasks.await(firestore.collection("banners").get())
                        val banners = bannerSnap.documents.mapNotNull { it.toBannerEntity() }
                        if (banners.isNotEmpty()) bannerDao.insertBanners(banners)
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Banners sync error: ${e.message}")
                    }
                }

                // 9. Sync Notifications
                launch {
                    try {
                        val notifSnap = Tasks.await(firestore.collection("notifications").get())
                        val notifs = notifSnap.documents.mapNotNull { it.toNotificationEntity() }
                        val deletedIds = getDeletedNotificationIds()
                        val readIds = getReadNotificationIds()
                        val filteredNotifs = notifs.filter { it.id !in deletedIds }.map { notif ->
                            if (notif.id in readIds) notif.copy(isRead = true) else notif
                        }
                        if (filteredNotifs.isNotEmpty()) notificationDao.insertNotifications(filteredNotifs)
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Notifications sync error: ${e.message}")
                    }
                }

                // 10. Sync Mandal Info
                launch {
                    try {
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
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Mandal info sync error: ${e.message}")
                    }
                }

                // 11. Sync Business Directory
                launch {
                    try {
                        val bizSnap = Tasks.await(firestore.collection("business_directory").get())
                        val businesses = bizSnap.documents.mapNotNull { it.toBusinessListingEntity() }
                        if (businesses.isNotEmpty()) {
                            businessDirectoryDao.insertBusinesses(businesses)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Business directory sync error: ${e.message}")
                    }
                }
            }
            Log.d("FirebaseSync", "Firestore parallel force sync completed.")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Firestore force sync error: ${e.message}", e)
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

            // Cleanup obsolete dummy banners, posts, albums, photos, videos, events, announcements, and mock chat messages from local DB
            bannerDao.deleteBanner("banner_1")
            bannerDao.deleteBanner("banner_2")
            bannerDao.deleteBanner("banner_3")
            postDao.deletePost("post_1")
            galleryDao.deleteAlbum("album_1")
            galleryDao.deleteAlbum("album_2")
            galleryDao.deleteAlbum("album_3")
            galleryDao.deleteAlbum("album_4")
            galleryDao.deletePhoto("ph_1")
            galleryDao.deletePhoto("ph_2")
            galleryDao.deletePhoto("ph_3")
            galleryDao.deletePhoto("ph_4")
            galleryDao.deletePhoto("ph_5")
            galleryDao.deleteVideo("vid_1")
            galleryDao.deleteVideo("vid_2")
            galleryDao.deleteVideo("vid_3")
            eventDao.deleteEvent("event_1")
            eventDao.deleteEvent("event_2")
            eventDao.deleteEvent("event_3")
            announcementDao.deleteAnnouncement("ann_1")
            chatDao.deleteMessage("msg_group_1")
            chatDao.deleteMessage("msg_group_2")
            chatDao.deleteMessage("msg_group_3")

            // Sync default Admin and initial content to Firebase Firestore if not present
            try {
                // Also purge obsolete built-ins on Firestore
                firestore.collection("banners").document("banner_1").delete()
                firestore.collection("banners").document("banner_2").delete()
                firestore.collection("banners").document("banner_3").delete()
                firestore.collection("posts").document("post_1").delete()
                firestore.collection("albums").document("album_1").delete()
                firestore.collection("albums").document("album_2").delete()
                firestore.collection("albums").document("album_3").delete()
                firestore.collection("albums").document("album_4").delete()
                firestore.collection("photos").document("ph_1").delete()
                firestore.collection("photos").document("ph_2").delete()
                firestore.collection("photos").document("ph_3").delete()
                firestore.collection("photos").document("ph_4").delete()
                firestore.collection("photos").document("ph_5").delete()
                firestore.collection("videos").document("vid_1").delete()
                firestore.collection("videos").document("vid_2").delete()
                firestore.collection("videos").document("vid_3").delete()
                firestore.collection("events").document("event_1").delete()
                firestore.collection("events").document("event_2").delete()
                firestore.collection("events").document("event_3").delete()
                firestore.collection("announcements").document("ann_1").delete()
                firestore.collection("chat_messages").document("msg_group_1").delete()
                firestore.collection("chat_messages").document("msg_group_2").delete()
                firestore.collection("chat_messages").document("msg_group_3").delete()

                val adminDoc = Tasks.await(firestore.collection("users").document("admin_1").get())
                if (!adminDoc.exists()) {
                    Tasks.await(firestore.collection("users").document("admin_1").set(SeedData.defaultAdmin.toMap(), SetOptions.merge()))
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
                    val localSessionId = prefs.getString("logged_user_session_id", "") ?: ""
                    if (currentId != null) {
                        val updated = userDao.getUserById(currentId)
                        if (updated != null) {
                            if (updated.status != "APPROVED") {
                                handleSessionTerminated("आपले खाते मंजुरीच्या प्रतीक्षेत किंवा निलंबित आहे.")
                            } else if (localSessionId.isNotBlank() && updated.activeSessionId.isNotBlank() && updated.activeSessionId != localSessionId) {
                                Log.w("AuthSession", "Single-device login security: Duplicate login detected! Local: $localSessionId, Remote: ${updated.activeSessionId}")
                                handleSessionTerminated("⚠️ आपले खाते दुसऱ्या मोबाईलवर लॉगिन झाले आहे. सुरक्षिततेसाठी या मोबाईलमधून आपोआप लॉगआऊट करण्यात आले आहे.")
                            } else {
                                _currentUser.value = updated.toDomain().copy(activeSessionId = localSessionId)
                            }
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
                    val currentUserId = _currentUser.value?.id
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val msg = change.document.toChatMessageEntity()
                            if (msg != null && msg.senderId != currentUserId && (System.currentTimeMillis() - msg.timestamp) < 60000) {
                                if (processedChatNotificationIds.add(msg.id)) {
                                    val notifId = Math.abs(msg.id.hashCode())
                                    val isGroup = msg.receiverId == "GROUP_MANDAL" || msg.conversationId == "conv_mandal_group"
                                    if (isGroup) {
                                        val previewText = if (msg.messageText.isNotBlank()) msg.messageText else "नवीन संदेश आला आहे"
                                        com.example.util.SystemNotificationHelper.showSystemNotification(
                                            context = appContext,
                                            title = "🚩 जय हिंद ग्रुप: ${msg.senderName}",
                                            message = previewText,
                                            notificationId = notifId,
                                            channelId = com.example.util.SystemNotificationHelper.CHANNEL_GROUP_CHAT,
                                            targetRoute = "CHAT",
                                            targetId = "GROUP_MANDAL"
                                        )
                                    } else if (msg.receiverId == currentUserId) {
                                        val previewText = if (msg.messageText.isNotBlank()) msg.messageText else "नवीन मेसेज आला आहे"
                                        com.example.util.SystemNotificationHelper.showSystemNotification(
                                            context = appContext,
                                            title = "${msg.senderName} कडून मेसेज 💬",
                                            message = previewText,
                                            notificationId = notifId,
                                            channelId = com.example.util.SystemNotificationHelper.CHANNEL_CHAT,
                                            targetRoute = "CHAT",
                                            targetId = msg.senderId
                                        )
                                    }
                                }
                            }
                        } else if (change.type == DocumentChange.Type.REMOVED) {
                            chatDao.deleteMessage(change.document.id)
                        }
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
                    }
                    val cleanLogo = snapshot.getString("logoUrl")?.trim()?.ifEmpty { null }
                    _mandalLogoUrl.value = cleanLogo
                    if (cleanLogo != null) {
                        prefs.edit().putString("mandal_logo_url", cleanLogo).apply()
                    } else {
                        prefs.edit().remove("mandal_logo_url").apply()
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
                    val deletedIds = getDeletedNotificationIds()
                    val readIds = getReadNotificationIds()
                    val filteredList = list.filter { it.id !in deletedIds }.map { notif ->
                        if (notif.id in readIds) notif.copy(isRead = true) else notif
                    }
                    if (filteredList.isNotEmpty()) notificationDao.insertNotifications(filteredList)
                    val currentUser = _currentUser.value
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val notif = change.document.toNotificationEntity()
                            if (notif != null && (System.currentTimeMillis() - notif.timestamp) < 90000) {
                                val isRelevant = when (notif.type) {
                                    "COMMENT" -> notif.targetUserId == currentUser?.id
                                    "CHAT" -> notif.targetUserId == currentUser?.id
                                    "ADMIN" -> currentUser?.isAdmin == true
                                    "POST" -> currentUser != null && notif.targetExtra != currentUser.id
                                    "BLOOD_ALERT" -> currentUser != null
                                    else -> notif.targetUserId == null || notif.targetUserId == currentUser?.id || (notif.targetUserId == "ADMIN" && currentUser?.isAdmin == true)
                                }
                                if (isRelevant && notif.type != "CHAT") {
                                    if (processedNotificationIds.add(notif.id)) {
                                        val notifId = Math.abs(notif.id.hashCode())
                                        val notifChannel = if (notif.type == "BLOOD_ALERT") {
                                            com.example.util.SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD
                                        } else {
                                            com.example.util.SystemNotificationHelper.CHANNEL_GENERAL
                                        }
                                        com.example.util.SystemNotificationHelper.showSystemNotification(
                                            context = appContext,
                                            title = notif.title,
                                            message = notif.message,
                                            notificationId = notifId,
                                            channelId = notifChannel,
                                            targetRoute = notif.targetRoute ?: "ANNOUNCEMENTS",
                                            targetId = notif.targetId
                                        )
                                    }
                                }
                            }
                        } else if (change.type == DocumentChange.Type.REMOVED) {
                            notificationDao.deleteNotification(change.document.id)
                        }
                    }
                }
            }

            // Real-time Live Stream Comments Sync (Instant broadcast to all active watchers)
            firestore.collection("live_comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limitToLast(150)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("FirebaseSync", "Live comments snapshot listener error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener
                    repositoryScope.launch {
                        val currentMandal = mandalInfoDao.getMandalInfoDirect()
                        val streamStartedAt = currentMandal?.liveStreamStartedAt ?: 0L
                        val isLiveActive = currentMandal?.isLiveStreamActive ?: false

                        val list = snapshots.documents.mapNotNull { doc ->
                            try {
                                val commentTimestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val commentStreamStartedAt = doc.getLong("streamStartedAt") ?: 0L

                                // If live stream is active and streamStartedAt is set, only show comments from this current stream session
                                if (isLiveActive && streamStartedAt > 0L) {
                                    if (commentTimestamp < (streamStartedAt - 60_000L) && commentStreamStartedAt < streamStartedAt) {
                                        return@mapNotNull null
                                    }
                                }

                                LiveComment(
                                    id = doc.getString("id") ?: doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    userName = doc.getString("userName") ?: "सभासद",
                                    userPhoto = doc.getString("userPhoto") ?: "",
                                    message = doc.getString("message") ?: "",
                                    timestamp = commentTimestamp,
                                    edited = doc.getBoolean("edited") ?: false,
                                    streamStartedAt = commentStreamStartedAt
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        _liveComments.value = list
                    }
                }

            // Real-time Live Stream Active Viewers Sync (Counts actual concurrent viewers)
            firestore.collection("live_viewers")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("FirebaseSync", "Live viewers snapshot listener error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener
                    val now = System.currentTimeMillis()
                    // Active viewers with heartbeat within 60 seconds
                    val activeViewers = snapshots.documents.filter { doc ->
                        val lastHb = doc.getLong("lastHeartbeat") ?: doc.getLong("joinedAt") ?: 0L
                        (now - lastHb) < 60_000L
                    }
                    val isSelfWatching = (currentLivePresenceSessionId != null)
                    val count = if (isSelfWatching) maxOf(1, activeViewers.size) else activeViewers.size
                    _realtimeLiveViewerCount.value = count
                }

            // Real-time Member Feedbacks Sync (Admin only)
            firestore.collection("feedbacks")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("FirebaseSync", "Feedbacks snapshot error: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener
                    val list = snapshots.documents.mapNotNull { doc ->
                        try {
                            MemberFeedback(
                                id = doc.getString("id") ?: doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "सभासद",
                                userMobile = doc.getString("userMobile") ?: "",
                                userDesignation = doc.getString("userDesignation") ?: "सभासद",
                                userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                                category = doc.getString("category") ?: "सर्वसाधारण सूचना",
                                rating = (doc.getLong("rating") ?: 5L).toInt(),
                                message = doc.getString("message") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                status = doc.getString("status") ?: "NEW"
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    _feedbacks.value = list
                }

            // Real-time Emergency Blood Alerts Listener
            firestore.collection("emergency_blood_alerts")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    repositoryScope.launch {
                        val alerts = snapshots.documents.mapNotNull { doc ->
                            try {
                                com.example.data.model.EmergencyBloodAlert(
                                    id = doc.getString("id") ?: doc.id,
                                    bloodGroup = doc.getString("bloodGroup") ?: "O+",
                                    patientName = doc.getString("patientName") ?: "",
                                    hospital = doc.getString("hospital") ?: "",
                                    unitsNeeded = doc.getString("unitsNeeded") ?: "1",
                                    contactPerson = doc.getString("contactPerson") ?: "",
                                    contactNumber = doc.getString("contactNumber") ?: "",
                                    additionalNote = doc.getString("additionalNote") ?: "",
                                    createdBy = doc.getString("createdBy") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    isActive = doc.getBoolean("isActive") ?: true
                                )
                            } catch (_: Exception) { null }
                        }
                        _activeBloodAlert.value = alerts.maxByOrNull { it.timestamp }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "startFirestoreSync error: ${e.message}", e)
        }
    }

    suspend fun refreshAllFromFirestore() = withContext(Dispatchers.IO) {
        forceSyncFromFirebase()
    }

    // AUTH & USERS
    suspend fun login(mobile: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val digitsOnlyMobile = cleanMobile.filter { it.isDigit() }
        val raw10Digit = if (digitsOnlyMobile.length >= 10) digitsOnlyMobile.takeLast(10) else digitsOnlyMobile
        val cleanPass = pass.trim()

        var user = userDao.getUserByMobile(cleanMobile)
            ?: if (raw10Digit.isNotBlank()) userDao.getUserByMobile(raw10Digit) else null

        // If not found in local Room or status is still PENDING_APPROVAL locally, fetch latest from Firestore
        try {
            val queryTask = firestore.collection("users").whereEqualTo("mobileNumber", cleanMobile).get(com.google.firebase.firestore.Source.DEFAULT)
            val snapshot = Tasks.await(queryTask)
            var doc = snapshot.documents.firstOrNull()

            // If not found by cleanMobile, try looking up by 10-digit raw number
            if (doc == null && raw10Digit.isNotBlank() && raw10Digit != cleanMobile) {
                val fallbackTask = firestore.collection("users").whereEqualTo("mobileNumber", raw10Digit).get(com.google.firebase.firestore.Source.DEFAULT)
                val fallbackSnapshot = Tasks.await(fallbackTask)
                doc = fallbackSnapshot.documents.firstOrNull()
            }

            if (doc != null) {
                val remoteUser = doc.toUserEntity()
                if (remoteUser != null) {
                    userDao.insertUser(remoteUser)
                    user = remoteUser
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Login firestore fetch error: ${e.message}")
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
        if (user.status == "REJECTED") {
            return@withContext Result.failure(Exception("आपले खाते नामंजूर करण्यात आले आहे. कृपया मंडळाशी संपर्क साधा."))
        }

        // Generate unique Single-Device Session ID
        val newSessionId = "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(8)
        val updatedUser = user.copy(
            activeSessionId = newSessionId,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )
        userDao.updateUser(updatedUser)

        // Sync new session ID to Firestore immediately
        try {
            firestore.collection("users").document(updatedUser.id).set(
                mapOf(
                    "activeSessionId" to newSessionId,
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            Log.d("AuthSession", "New active session $newSessionId registered for user ${updatedUser.id}")
        } catch (e: Exception) {
            Log.e("AuthSession", "Failed to update activeSessionId on Firestore: ${e.message}")
        }

        val domainUser = updatedUser.toDomain()
        saveUserToPrefs(domainUser)
        _currentUser.value = domainUser
        _sessionSecurityNotice.value = null
        Result.success(domainUser)
    }

    fun handleSessionTerminated(reason: String) {
        val currentId = _currentUser.value?.id
        saveUserToPrefs(null)
        _currentUser.value = null
        _sessionSecurityNotice.value = reason
        Log.w("AuthSession", "Session terminated for user $currentId: $reason")
    }

    fun logout() {
        val current = _currentUser.value
        if (current != null) {
            try {
                firestore.collection("users").document(current.id).set(
                    mapOf("isOnline" to false, "lastSeen" to System.currentTimeMillis()),
                    SetOptions.merge()
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
        saveUserToPrefs(null)
        _currentUser.value = null
        _sessionSecurityNotice.value = null
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
            title = "👤 नवीन सभासद नोंदणी (मंजुरी प्रतीक्षा)",
            message = "${fullName.trim()} (📞 $cleanMobile) यांनी नोंदणी केली आहे. मंजुरी देण्यासाठी येथे क्लिक करा.",
            type = "ADMIN",
            targetUserId = "ADMIN",
            targetRoute = "ADMIN_PENDING",
            targetId = newId,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notifEntity)

        // Push to Firebase Firestore so Admin on any phone sees it in real time
        try {
            val userWriteTask = firestore.collection("users").document(newId).set(entity.toMap(), SetOptions.merge())
            Tasks.await(userWriteTask)
            Log.d("FirebaseSync", "Registered user $newId synced to Firestore successfully")

            val notifWriteTask = firestore.collection("notifications").document(notifId).set(notifEntity.toMap(), SetOptions.merge())
            Tasks.await(notifWriteTask)
            Log.d("FirebaseSync", "Notification $notifId synced to Firestore successfully")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error pushing registration to Firestore: ${e.message}", e)
            val errLower = (e.message ?: "").lowercase()
            if (errLower.contains("permission_denied") || errLower.contains("permission-denied") || errLower.contains("missing or insufficient permissions")) {
                return@withContext Result.failure(Exception("Firebase Firestore सुरक्षा नियम (Rules) ब्लॉक आहेत! कृपया Firebase Console मध्ये Rules Publish करा."))
            } else if (errLower.contains("unavailable") || errLower.contains("network")) {
                return@withContext Result.failure(Exception("इंटरनेट कनेक्शन तपासा किंवा Firebase सर्व्हरशी संपर्क होऊ शकला नाही: ${e.localizedMessage}"))
            } else {
                return@withContext Result.failure(Exception("Firebase क्लाउडवर नोंदणी पाठवता आली नाही: ${e.localizedMessage}"))
            }
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

    // ADMIN ACTIONS ON USERS & ROLE / DESIGNATION MANAGEMENT
    suspend fun changeUserRole(userId: String, newRole: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext
        val updated = user.copy(role = newRole)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated.toDomain()
            prefs.edit().putString("logged_user_role", newRole).apply()
        }
        try {
            firestore.collection("users").document(userId).set(mapOf("role" to newRole), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error changing user role on Firestore", e)
        }
    }

    suspend fun updateMemberDesignationAndRole(userId: String, newDesignation: String, newRole: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext
        val updated = user.copy(designation = newDesignation.trim(), role = newRole)
        userDao.updateUser(updated)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated.toDomain()
            prefs.edit()
                .putString("logged_user_role", newRole)
                .putString("logged_user_designation", newDesignation.trim())
                .apply()
        }
        try {
            firestore.collection("users").document(userId).set(
                mapOf(
                    "designation" to newDesignation.trim(),
                    "role" to newRole
                ),
                SetOptions.merge()
            )
            Log.d("FirebaseSync", "Successfully updated designation for user $userId to $newDesignation")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating member designation on Firestore", e)
        }
    }

    suspend fun setMemberStatus(userId: String, status: String) = withContext(Dispatchers.IO) {
        userDao.updateUserStatus(userId, status)
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                Tasks.await(firestore.collection("users").document(userId).set(user.toMap(), SetOptions.merge()))
                Log.d("FirebaseSync", "Member $userId status successfully set to $status on Firestore")
            } else {
                Tasks.await(firestore.collection("users").document(userId).set(mapOf("status" to status), SetOptions.merge()))
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error setting member status on Firestore: ${e.message}", e)
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
    val posts: Flow<List<Post>> = combine(
        postDao.getAllPosts(),
        commentDao.getAllComments()
    ) { postEntities, allComments ->
        val commentCounts = allComments.groupBy { it.postId }.mapValues { it.value.size }
        postEntities.map { entity ->
            val actualCount = commentCounts[entity.id] ?: entity.commentsCount
            entity.toDomain().copy(commentsCount = actualCount)
        }
    }

    suspend fun createPost(
        content: String,
        imageUrl: String?,
        videoUrl: String?,
        isSponsored: Boolean = false,
        sponsorBusinessName: String? = null,
        sponsorContactNumber: String? = null,
        sponsorCtaText: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("कृपया प्रथम लॉगिन करा"))
        if (user.status == "BLOCKED") {
            return@withContext Result.failure(Exception("आपले खाते ब्लॉक असल्याने आपण नवीन पोस्ट करू शकत नाही."))
        }
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
            timestamp = System.currentTimeMillis(),
            isSponsored = isSponsored,
            sponsorBusinessName = sponsorBusinessName?.trim()?.ifBlank { null },
            sponsorContactNumber = sponsorContactNumber?.trim()?.ifBlank { null },
            sponsorCtaText = sponsorCtaText?.trim()?.ifBlank { null }
        )
        postDao.insertPost(newPost)

        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = "🚩 नवीन पोस्ट: ${user.fullName}",
            message = if (content.isNotBlank()) content.take(75).trim() + if (content.length > 75) "..." else "" else "मंडळाच्या फीडमध्ये नवीन छायाचित्र/माहिती पोस्ट केली आहे.",
            type = "POST",
            targetRoute = "POST",
            targetId = newPost.id,
            targetExtra = user.id, // Author ID so author doesn't get self-notified
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("posts").document(newPost.id).set(newPost.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
            Log.d("FirebaseSync", "New post ${newPost.id} and notification published to Firestore")

            // Send high-priority FCM push so phones wake up on lock screen
            com.example.util.FcmPushSenderHelper.sendPushToTopic(
                topic = "mandal_posts",
                title = notif.title,
                message = notif.message,
                type = "POST",
                targetRoute = "POST",
                targetId = newPost.id,
                senderId = user.id
            )
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to upload post to Firestore: ${e.message}", e)
        }
        Result.success(Unit)
    }

    suspend fun toggleLikePost(postId: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        if (user.status == "BLOCKED") return@withContext
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
        notificationDao.deleteNotificationsByTargetId(postId)
        try {
            firestore.collection("posts").document(postId).delete()
            val notifQuery = firestore.collection("notifications").whereEqualTo("targetId", postId).get()
            val notifSnap = Tasks.await(notifQuery)
            for (doc in notifSnap.documents) {
                firestore.collection("notifications").document(doc.id).delete()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting post on Firestore", e)
        }
    }

    suspend fun updatePost(
        postId: String,
        content: String,
        imageUrl: String?,
        videoUrl: String?,
        isSponsored: Boolean = false,
        sponsorBusinessName: String? = null,
        sponsorContactNumber: String? = null,
        sponsorCtaText: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanContent = content.trim()
        val cleanImage = imageUrl ?: ""
        val cleanBizName = sponsorBusinessName?.trim()?.ifBlank { null }
        val cleanContact = sponsorContactNumber?.trim()?.ifBlank { null }
        val cleanCta = sponsorCtaText?.trim()?.ifBlank { null }
        postDao.updatePostContent(
            postId = postId,
            content = cleanContent,
            imageUrls = cleanImage,
            videoUrl = videoUrl,
            isSponsored = isSponsored,
            sponsorBusinessName = cleanBizName,
            sponsorContactNumber = cleanContact,
            sponsorCtaText = cleanCta
        )
        try {
            val updateMap = mutableMapOf<String, Any?>(
                "content" to cleanContent,
                "imageUrlsJson" to cleanImage,
                "videoUrl" to videoUrl,
                "isSponsored" to isSponsored,
                "sponsorBusinessName" to cleanBizName,
                "sponsorContactNumber" to cleanContact,
                "sponsorCtaText" to cleanCta
            )
            firestore.collection("posts").document(postId).set(updateMap, SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating post on Firestore", e)
        }
        Result.success(Unit)
    }

    // COMMENTS
    fun getComments(postId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPost(postId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addComment(
        postId: String,
        text: String,
        parentId: String? = null,
        replyToAuthorName: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        if (user.status == "BLOCKED") {
            return@withContext Result.failure(Exception("आपले खाते ब्लॉक असल्याने आपण कमेंट करू शकत नाही."))
        }
        val comment = CommentEntity(
            id = "comm_" + UUID.randomUUID().toString().take(8),
            postId = postId,
            authorId = user.id,
            authorName = user.fullName,
            authorPhotoUrl = user.profilePhotoUrl,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            likedUserIdsJson = "",
            parentId = parentId,
            replyToAuthorName = replyToAuthorName,
            isEdited = false,
            editedAt = null
        )
        commentDao.insertComment(comment)
        postDao.incrementCommentsCount(postId)

        val post = postDao.getAllPosts().first().find { it.id == postId }
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        var notifEntity: NotificationEntity? = null
        if (post != null && post.authorId != user.id) {
            val title = if (replyToAuthorName != null) "कमेंटला रिप्लाय आला 💬" else "आपल्या पोस्टवर नवीन कमेंट 💬"
            val msg = if (replyToAuthorName != null) {
                "${user.fullName} यांनी ${replyToAuthorName} यांच्या कमेंटला उत्तर दिले: \"${text.take(45)}\""
            } else {
                "${user.fullName} यांनी आपल्या पोस्टवर कमेंट केली: \"${text.take(45)}\""
            }
            notifEntity = NotificationEntity(
                id = notifId,
                title = title,
                message = msg,
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

    suspend fun toggleLikeComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        val existing = commentDao.getCommentById(commentId) ?: return@withContext Result.failure(Exception("कमेंट सापडली नाही"))
        val currentLikes = if (existing.likedUserIdsJson.isNotBlank()) {
            existing.likedUserIdsJson.split(",").filter { it.isNotBlank() }.toMutableList()
        } else {
            mutableListOf()
        }

        if (currentLikes.contains(user.id)) {
            currentLikes.remove(user.id)
        } else {
            currentLikes.add(user.id)
        }

        val updatedLikesJson = currentLikes.joinToString(",")
        commentDao.updateCommentLikes(commentId, updatedLikesJson)
        try {
            firestore.collection("comments").document(commentId).update("likedUserIdsJson", updatedLikesJson)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating comment like on Firestore: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun editComment(commentId: String, newText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        val existing = commentDao.getCommentById(commentId) ?: return@withContext Result.failure(Exception("कमेंट सापडली नाही"))
        if (existing.authorId != user.id && !user.isAnyAdmin) {
            return@withContext Result.failure(Exception("आपण केवळ स्वतःची कमेंट संपादित करू शकता"))
        }
        val clean = newText.trim()
        if (clean.isBlank()) {
            return@withContext Result.failure(Exception("कमेंट रिक्त असू शकत नाही"))
        }
        val now = System.currentTimeMillis()
        commentDao.updateCommentText(commentId, clean, now)
        try {
            firestore.collection("comments").document(commentId).update(
                mapOf(
                    "text" to clean,
                    "isEdited" to true,
                    "editedAt" to now
                )
            )
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating comment on Firestore: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("लॉगिन आवश्यक आहे"))
        val existing = commentDao.getCommentById(commentId)
        if (existing != null && existing.authorId != user.id && !user.isAnyAdmin) {
            return@withContext Result.failure(Exception("आपल्याकडे ही कमेंट डिलीट करण्याचे अधिकार नाहीत"))
        }
        commentDao.deleteComment(commentId)
        postDao.decrementCommentsCount(postId)
        try {
            firestore.collection("comments").document(commentId).delete()
            val post = postDao.getAllPosts().first().find { it.id == postId }
            val newCount = post?.commentsCount ?: 0
            firestore.collection("posts").document(postId).set(mapOf("commentsCount" to newCount), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting comment on Firestore: ${e.message}")
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
        if (user.status == "BLOCKED") {
            return@withContext Result.failure(Exception("आपले खाते ब्लॉक असल्याने आपण मेसेज पाठवू शकत नाही."))
        }
        val isGroup = receiverId == "GROUP_MANDAL"
        val convId = if (isGroup) "conv_mandal_group" else getConversationId(user.id, receiverId)
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
            "VOICE" -> "🎙️ व्हॉईस संदेश (${attachmentExtra ?: "ऑडिओ"})"
            "DOCUMENT" -> "📄 डॉक्युमेंट: ${attachmentName ?: ""}"
            "CONTACT" -> "👤 संपर्क क्रमांक: ${attachmentName ?: ""}"
            else -> "नवीन संदेश प्राप्त झाला"
        }
        val notifId = "notif_" + UUID.randomUUID().toString().take(8)
        val notif = NotificationEntity(
            id = notifId,
            title = if (isGroup) "🚩 जय हिंद ग्रुप: ${user.fullName}" else "${user.fullName} कडून मेसेज",
            message = preview,
            type = "CHAT",
            targetUserId = if (isGroup) null else receiverId,
            targetRoute = "CHAT",
            targetId = if (isGroup) "GROUP_MANDAL" else user.id,
            targetExtra = if (isGroup) "🚩 जय हिंद मंडळ - सर्व सदस्य ग्रुप" else user.fullName,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notif)

        try {
            firestore.collection("chat_messages").document(msg.id).set(msg.toMap(), SetOptions.merge())
            firestore.collection("notifications").document(notifId).set(notif.toMap(), SetOptions.merge())
            Log.d("FirebaseSync", "Chat message sent to Firestore: ${msg.id}")

            if (isGroup) {
                com.example.util.FcmPushSenderHelper.sendPushToTopic(
                    topic = "mandal_group_chat",
                    title = notif.title,
                    message = notif.message,
                    type = "GROUP_CHAT",
                    targetRoute = "CHAT",
                    targetId = "GROUP_MANDAL",
                    senderId = user.id
                )
            } else if (!receiverId.isNullOrBlank()) {
                val receiverDoc = Tasks.await(firestore.collection("users").document(receiverId).get())
                val receiverToken = receiverDoc.getString("fcmToken")
                if (!receiverToken.isNullOrBlank()) {
                    com.example.util.FcmPushSenderHelper.sendPushToToken(
                        token = receiverToken,
                        title = notif.title,
                        message = notif.message,
                        type = "CHAT",
                        targetRoute = "CHAT",
                        targetId = user.id,
                        senderId = user.id
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error sending chat message on Firestore", e)
        }
        Result.success(Unit)
    }

    suspend fun deleteChatMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatDao.deleteMessage(messageId)
            firestore.collection("chat_messages").document(messageId).delete()
            Log.d("FirebaseSync", "Chat message deleted: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting chat message on Firestore", e)
            Result.failure(e)
        }
    }

    fun getConversationSummaries(currentUserId: String): Flow<List<ChatConversationSummary>> {
        return combine(
            chatDao.getSummaryMessagesForUser(currentUserId),
            allMembers
        ) { messages, members ->
            val memberMap = members.associateBy { it.id }
            val nonGroupMessages = messages.filter { it.receiverId != "GROUP_MANDAL" && it.conversationId != "conv_mandal_group" }
            val grouped = nonGroupMessages.groupBy { msg ->
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
                    "VOICE" -> "🎙️ व्हॉईस संदेश"
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

    val groupChatMessages: Flow<List<ChatMessage>> = chatDao.getMessagesForConversation("conv_mandal_group").map { list -> list.map { it.toDomain() } }

    suspend fun markChatAsRead(conversationId: String, currentUserId: String, partnerId: String = "") = withContext(Dispatchers.IO) {
        if (partnerId.isNotBlank()) {
            chatDao.markMessagesAsReadBetween(conversationId, currentUserId, partnerId)
        } else {
            chatDao.markMessagesAsRead(conversationId, currentUserId)
        }
        try {
            val query = firestore.collection("chat_messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
            val snap = Tasks.await(query)
            for (doc in snap.documents) {
                val docConvId = doc.getString("conversationId")
                val docSenderId = doc.getString("senderId")
                if (docConvId == conversationId || docSenderId == partnerId) {
                    firestore.collection("chat_messages").document(doc.id).update("isRead", true)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating chat read status on Firestore", e)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val unreadChatCount: Flow<Int> = _currentUser.flatMapLatest { user ->
        if (user != null) chatDao.getUnreadChatCount(user.id) else kotlinx.coroutines.flow.flowOf(0)
    }

    fun getConversationId(userA: String, userB: String): String {
        return if (userA < userB) "conv_${userA}_${userB}" else "conv_${userB}_${userA}"
    }

    // GALLERY (ALBUMS, PHOTOS, VIDEOS)
    val photoAlbums: Flow<List<Album>> = galleryDao.getPhotoAlbums().map { list -> list.map { it.toDomain() } }
    val videoAlbums: Flow<List<Album>> = galleryDao.getVideoAlbums().map { list -> list.map { it.toDomain() } }
    val albums: Flow<List<Album>> = photoAlbums
    val videos: Flow<List<VideoItem>> = galleryDao.getAllVideos().map { list -> list.map { it.toDomain() } }

    fun getPhotosForAlbum(albumId: String): Flow<List<GalleryPhoto>> {
        return galleryDao.getPhotosForAlbum(albumId).map { list -> list.map { it.toDomain() } }
    }

    fun getVideosForAlbum(albumId: String): Flow<List<VideoItem>> {
        return galleryDao.getVideosForAlbum(albumId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun createAlbum(
        title: String,
        category: String,
        coverImageUrl: String,
        description: String,
        albumType: String = "PHOTO"
    ) = withContext(Dispatchers.IO) {
        val album = AlbumEntity(
            id = (if (albumType == "VIDEO") "valbum_" else "album_") + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            category = category.trim(),
            coverImageUrl = coverImageUrl.ifEmpty {
                if (albumType == "VIDEO") "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
                else "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80"
            },
            description = description.trim(),
            photoCount = 0,
            albumType = albumType
        )
        galleryDao.insertAlbum(album)
        try {
            firestore.collection("albums").document(album.id).set(album.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error creating album on Firestore", e)
        }
    }

    suspend fun updateAlbum(
        albumId: String,
        title: String,
        category: String,
        coverImageUrl: String,
        description: String
    ) = withContext(Dispatchers.IO) {
        val existing = galleryDao.getAlbumById(albumId)
        if (existing == null) {
            // Album doesn't exist in DB yet (e.g. was virtual default_video_album)
            val newAlbum = AlbumEntity(
                id = albumId,
                title = title.trim(),
                category = category.trim(),
                coverImageUrl = coverImageUrl.trim(),
                description = description.trim(),
                photoCount = 0,
                albumType = "VIDEO"
            )
            galleryDao.insertAlbum(newAlbum)
            galleryDao.assignUnassignedVideosToAlbum(albumId)
            try {
                firestore.collection("albums").document(albumId).set(newAlbum.toMap(), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error creating album on Firestore", e)
            }
        } else {
            galleryDao.updateAlbum(albumId, title.trim(), category.trim(), coverImageUrl.trim(), description.trim())
            if (albumId == "default_video_album") {
                galleryDao.assignUnassignedVideosToAlbum(albumId)
            }
            try {
                val updates = mapOf(
                    "title" to title.trim(),
                    "category" to category.trim(),
                    "coverImageUrl" to coverImageUrl.trim(),
                    "description" to description.trim()
                )
                firestore.collection("albums").document(albumId).update(updates)
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating album on Firestore", e)
            }
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
            val allAlbums = galleryDao.getAllAlbums().first()
            val targetAlbum = allAlbums.find { it.id == albumId }
            if (targetAlbum != null) {
                val updatedAlbum = targetAlbum.copy(
                    photoCount = targetAlbum.photoCount + 1,
                    coverImageUrl = if (targetAlbum.coverImageUrl.isBlank()) imageUrl else targetAlbum.coverImageUrl
                )
                galleryDao.insertAlbum(updatedAlbum)
                firestore.collection("albums").document(albumId).set(updatedAlbum.toMap(), SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating album photo count", e)
        }
        try {
            firestore.collection("photos").document(photo.id).set(photo.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding photo on Firestore", e)
        }
    }

    suspend fun deleteAlbum(albumId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteAlbum(albumId)
        galleryDao.deletePhotosForAlbum(albumId)
        galleryDao.deleteVideosForAlbum(albumId)
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

    suspend fun updatePhotoCaption(photoId: String, caption: String) = withContext(Dispatchers.IO) {
        galleryDao.updatePhotoCaption(photoId, caption.trim())
        try {
            firestore.collection("photos").document(photoId).update("caption", caption.trim())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating photo caption on Firestore", e)
        }
    }

    suspend fun incrementPhotoViewCount(photoId: String) = withContext(Dispatchers.IO) {
        if (photoId.isBlank()) return@withContext
        galleryDao.incrementPhotoViewCount(photoId)
        try {
            firestore.collection("photos").document(photoId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1))
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error incrementing photo view count on Firestore", e)
        }
    }

    suspend fun incrementVideoViewCount(videoId: String) = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext
        galleryDao.incrementVideoViewCount(videoId)
        try {
            firestore.collection("videos").document(videoId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1))
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error incrementing video view count on Firestore", e)
        }
    }

    suspend fun addVideoToAlbum(
        albumId: String,
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String
    ) = withContext(Dispatchers.IO) {
        if (albumId == "default_video_album") {
            val existing = galleryDao.getAlbumById(albumId)
            if (existing == null) {
                val defAlbum = AlbumEntity(
                    id = albumId,
                    title = "मंडळ मुख्य व्हिडिओ संग्रह",
                    category = category.trim().ifEmpty { "सांस्कृतिक व उत्सव" },
                    coverImageUrl = thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80" },
                    description = "मंडळाचे सर्व उत्सव व सांस्कृतिक कार्यक्रमांचे व्हिडिओ",
                    photoCount = 0,
                    albumType = "VIDEO"
                )
                galleryDao.insertAlbum(defAlbum)
            }
        }
        val video = VideoEntity(
            id = "vid_" + UUID.randomUUID().toString().take(8),
            albumId = albumId,
            title = title.trim(),
            description = description.trim(),
            category = category.trim(),
            videoUrl = videoUrl.trim(),
            thumbnailUrl = thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80" }
        )
        galleryDao.insertVideo(video)
        try {
            val allAlbums = galleryDao.getAllAlbums().first()
            val targetAlbum = allAlbums.find { it.id == albumId }
            if (targetAlbum != null) {
                val updatedAlbum = targetAlbum.copy(
                    photoCount = targetAlbum.photoCount + 1,
                    coverImageUrl = if (targetAlbum.coverImageUrl.isBlank()) video.thumbnailUrl else targetAlbum.coverImageUrl
                )
                galleryDao.insertAlbum(updatedAlbum)
                firestore.collection("albums").document(albumId).set(updatedAlbum.toMap(), SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating video album count", e)
        }
        try {
            firestore.collection("videos").document(video.id).set(video.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error adding video on Firestore", e)
        }
    }

    suspend fun updateVideo(
        videoId: String,
        title: String,
        description: String,
        category: String,
        thumbnailUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        if (thumbnailUrl.isNotBlank()) {
            galleryDao.updateVideoDetails(videoId, title.trim(), description.trim(), category.trim(), thumbnailUrl.trim())
        } else {
            galleryDao.updateVideo(videoId, title.trim(), description.trim(), category.trim())
        }
        try {
            val updates = mutableMapOf<String, Any>(
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category.trim()
            )
            if (thumbnailUrl.isNotBlank()) {
                updates["thumbnailUrl"] = thumbnailUrl.trim()
            }
            firestore.collection("videos").document(videoId).update(updates)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating video on Firestore", e)
        }
    }

    suspend fun addVideo(title: String, description: String, category: String, videoUrl: String, thumbnailUrl: String) = withContext(Dispatchers.IO) {
        val video = VideoEntity(
            id = "vid_" + UUID.randomUUID().toString().take(8),
            albumId = "",
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

    suspend fun deleteVideo(videoId: String, albumId: String = "") = withContext(Dispatchers.IO) {
        galleryDao.deleteVideo(videoId)
        if (albumId.isNotBlank()) {
            try {
                val allAlbums = galleryDao.getAllAlbums().first()
                val targetAlbum = allAlbums.find { it.id == albumId }
                if (targetAlbum != null && targetAlbum.photoCount > 0) {
                    val updatedAlbum = targetAlbum.copy(photoCount = targetAlbum.photoCount - 1)
                    galleryDao.insertAlbum(updatedAlbum)
                    firestore.collection("albums").document(albumId).set(updatedAlbum.toMap(), SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating album count on video delete", e)
            }
        }
        try {
            firestore.collection("videos").document(videoId).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting video on Firestore", e)
        }
    }

    // EVENTS (Sorted chronologically by date: earliest date first)
    val events: Flow<List<MandalEvent>> = eventDao.getAllEvents().map { list ->
        list.map { it.toDomain() }.sortedWith(
            compareBy<MandalEvent> { event ->
                com.example.util.DateUtils.parseEventDateToTimestamp(event.date)
            }.thenBy { it.date }
        )
    }

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

            com.example.util.FcmPushSenderHelper.sendPushToTopic(
                topic = "mandal_events",
                title = notif.title,
                message = notif.message,
                type = "EVENT",
                targetRoute = "EVENTS",
                targetId = event.id,
                senderId = ""
            )
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

            com.example.util.FcmPushSenderHelper.sendPushToTopic(
                topic = "mandal_announcements",
                title = notif.title,
                message = notif.message,
                type = "ANNOUNCEMENT",
                targetRoute = "ANNOUNCEMENTS",
                targetId = ann.id,
                senderId = user?.id ?: ""
            )
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error creating announcement on Firestore", e)
        }
    }

    suspend fun updateAnnouncement(
        id: String,
        title: String,
        content: String,
        priority: String
    ) = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        val ann = AnnouncementEntity(
            id = id,
            title = title.trim(),
            content = content.trim(),
            priority = priority,
            date = SimpleDateFormat("dd MMMM yyyy", Locale("mr", "IN")).format(Date()),
            author = user?.fullName ?: "मंडळ कार्यकारणी"
        )
        announcementDao.insertAnnouncement(ann)
        try {
            firestore.collection("announcements").document(id).set(ann.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating announcement on Firestore", e)
        }
    }

    suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
        announcementDao.deleteAnnouncement(id)
        notificationDao.deleteNotificationsByTargetId(id)
        try {
            firestore.collection("announcements").document(id).delete()
            val notifQuery = firestore.collection("notifications").whereEqualTo("targetId", id).get()
            val notifSnap = Tasks.await(notifQuery)
            for (doc in notifSnap.documents) {
                firestore.collection("notifications").document(doc.id).delete()
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting announcement on Firestore", e)
        }
    }

    private fun getReadNotificationIds(): MutableSet<String> {
        return prefs.getStringSet("read_notif_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun getDeletedNotificationIds(): MutableSet<String> {
        return prefs.getStringSet("deleted_notif_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    // NOTIFICATIONS
    val notifications: Flow<List<MandalNotification>> = combine(
        notificationDao.getAllNotifications(),
        _currentUser,
        todayBirthdayMembers
    ) { allNotifs, user, birthdayMembers ->
        val deletedIds = getDeletedNotificationIds()
        val readIds = getReadNotificationIds()

        val filtered = allNotifs.filter { notif ->
            if (notif.id in deletedIds) return@filter false
            when (notif.type) {
                "COMMENT" -> notif.targetUserId == user?.id
                "CHAT" -> notif.targetUserId == user?.id
                "ADMIN" -> user?.isAdmin == true
                "BIRTHDAY" -> birthdayMembers.isNotEmpty()
                else -> {
                    notif.targetUserId == null || notif.targetUserId == user?.id || (notif.targetUserId == "ADMIN" && user?.isAdmin == true)
                }
            }
        }.map { entity ->
            val domain = entity.toDomain()
            if (domain.id in readIds) domain.copy(isRead = true) else domain
        }

        val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        val bdayId = "dyn_bday_$todayDateStr"
        val isBdayDeleted = bdayId in deletedIds
        val isBdayRead = bdayId in readIds

        val resultList = if (birthdayMembers.isNotEmpty() && !isBdayDeleted && filtered.none { it.type == "BIRTHDAY" }) {
            val names = birthdayMembers.take(2).joinToString(" व ") { it.fullName } + (if (birthdayMembers.size > 2) " आणि इतर" else "")
            val dynBirthday = MandalNotification(
                id = bdayId,
                title = "आज वाढदिवस आहे! 🎂🎉",
                message = "आज आपले सहकारी सभासद $names यांचा वाढदिवस आहे. त्यांना हार्दिक शुभेच्छा द्या!",
                type = "BIRTHDAY",
                timestamp = System.currentTimeMillis(),
                isRead = isBdayRead,
                targetRoute = "BIRTHDAYS",
                targetId = birthdayMembers.firstOrNull()?.id
            )
            listOf(dynBirthday) + filtered
        } else {
            filtered
        }

        resultList.sortedByDescending { it.timestamp }
    }

    val unreadNotificationsCount: Flow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val currentRead = getReadNotificationIds()
        try {
            val allList = notificationDao.getAllNotificationsList()
            allList.forEach { currentRead.add(it.id) }
        } catch (_: Exception) {}
        val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        currentRead.add("dyn_bday_$todayDateStr")
        currentRead.add("dyn_bday_today")
        prefs.edit().putStringSet("read_notif_ids", currentRead).apply()
        notificationDao.markAllAsRead()
        com.example.util.SystemNotificationHelper.cancelAllNotifications(appContext)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        val currentDeleted = getDeletedNotificationIds()
        try {
            val allList = notificationDao.getAllNotificationsList()
            allList.forEach { currentDeleted.add(it.id) }
        } catch (_: Exception) {}
        val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        currentDeleted.add("dyn_bday_$todayDateStr")
        currentDeleted.add("dyn_bday_today")
        prefs.edit().putStringSet("deleted_notif_ids", currentDeleted).apply()
        notificationDao.deleteAllNotifications()
        com.example.util.SystemNotificationHelper.cancelAllNotifications(appContext)
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        val currentRead = getReadNotificationIds()
        currentRead.add(id)
        prefs.edit().putStringSet("read_notif_ids", currentRead).apply()
        if (!id.startsWith("dyn_")) {
            notificationDao.markAsRead(id)
        }
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        val currentDeleted = getDeletedNotificationIds()
        currentDeleted.add(id)
        prefs.edit().putStringSet("deleted_notif_ids", currentDeleted).apply()
        if (!id.startsWith("dyn_")) {
            notificationDao.deleteNotification(id)
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

    // ID CARD BACK SETTINGS (नियम, उद्दिष्टे व संपर्क संपादन)
    suspend fun updateIdCardBackSettings(
        idCardObjectives: String,
        idCardRules: String,
        emergencyContacts: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            val updated = current.copy(
                idCardObjectives = idCardObjectives.trim(),
                idCardRules = idCardRules.trim(),
                emergencyContacts = emergencyContacts.trim(),
                updatedAt = System.currentTimeMillis()
            )
            mandalInfoDao.saveMandalInfo(updated)
            try {
                firestore.collection("mandal_info").document("mandal_default")
                    .set(
                        mapOf(
                            "idCardObjectives" to updated.idCardObjectives,
                            "idCardRules" to updated.idCardRules,
                            "emergencyContacts" to updated.emergencyContacts,
                            "updatedAt" to updated.updatedAt
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating id card back settings on Firestore", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MandalRepository", "Error updating ID card back settings", e)
            Result.failure(e)
        }
    }

    // FESTIVE BANNER TOGGLE & MANUAL SELECTION (सण व विशेष दिन बॅनर नियंत्रण)
    suspend fun updateFestiveBannerSettings(
        showFestiveBanner: Boolean,
        manualFestivalId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            val updated = current.copy(
                showFestiveBanner = showFestiveBanner,
                manualFestivalId = manualFestivalId.trim(),
                updatedAt = System.currentTimeMillis()
            )
            mandalInfoDao.saveMandalInfo(updated)
            try {
                firestore.collection("mandal_info").document("mandal_default")
                    .set(
                        mapOf(
                            "showFestiveBanner" to updated.showFestiveBanner,
                            "manualFestivalId" to updated.manualFestivalId,
                            "updatedAt" to updated.updatedAt
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating festive banner settings on Firestore", e)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MandalRepository", "Error updating festive banner settings", e)
            Result.failure(e)
        }
    }

    // MANDAL LOGO MANAGEMENT
    suspend fun updateMandalLogo(url: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUrl = url?.trim()?.ifEmpty { null }
        try {
            // 1. Update Firestore
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("logoUrl" to (cleanUrl ?: "")), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            // 2. Update SharedPreferences for instant restart cache
            if (cleanUrl != null) {
                prefs.edit().putString("mandal_logo_url", cleanUrl).apply()
            } else {
                prefs.edit().remove("mandal_logo_url").apply()
            }

            // 3. Update StateFlow for immediate in-app reactive UI update
            _mandalLogoUrl.value = cleanUrl

            // 4. Update Local Room DB
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(logoUrl = cleanUrl ?: "", updatedAt = System.currentTimeMillis()))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating mandal logo on Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMandalLogo(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Update Firestore
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("logoUrl" to ""), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            // 2. Remove from SharedPreferences
            prefs.edit().remove("mandal_logo_url").apply()

            // 3. Reset StateFlow
            _mandalLogoUrl.value = null

            // 4. Update Local Room DB
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(logoUrl = "", updatedAt = System.currentTimeMillis()))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting mandal logo on Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    // OFFICIAL STAMP MANAGEMENT
    suspend fun updateOfficialStamp(stampUrl: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUrl = stampUrl?.trim()?.ifEmpty { null }
        try {
            // 1. Update Firestore
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("officialStampUrl" to (cleanUrl ?: "")), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            // 2. Update Local Room DB
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(
                officialStampUrl = cleanUrl ?: "",
                updatedAt = System.currentTimeMillis()
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating official stamp on Firestore: ${e.message}", e)
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(
                officialStampUrl = cleanUrl ?: "",
                updatedAt = System.currentTimeMillis()
            ))
            Result.success(Unit)
        }
    }

    suspend fun deleteOfficialStamp(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("officialStampUrl" to ""), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(officialStampUrl = "", updatedAt = System.currentTimeMillis()))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting official stamp on Firestore: ${e.message}", e)
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(officialStampUrl = "", updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        }
    }

    // PRESIDENT SIGNATURE MANAGEMENT
    suspend fun updatePresidentSignature(signatureUrl: String?, presidentName: String = "अध्यक्ष"): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUrl = signatureUrl?.trim()?.ifEmpty { null }
        try {
            // 1. Update Firestore
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("presidentSignatureUrl" to (cleanUrl ?: ""), "presidentName" to presidentName.trim()), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            // 2. Update Local Room DB
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(
                presidentSignatureUrl = cleanUrl ?: "",
                presidentName = presidentName.trim().ifBlank { "अध्यक्ष" },
                updatedAt = System.currentTimeMillis()
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating president signature on Firestore: ${e.message}", e)
            // Still update local DB so offline works
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(
                presidentSignatureUrl = cleanUrl ?: "",
                presidentName = presidentName.trim().ifBlank { "अध्यक्ष" },
                updatedAt = System.currentTimeMillis()
            ))
            Result.success(Unit)
        }
    }

    suspend fun deletePresidentSignature(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("mandal_info").document("mandal_default")
                .set(mapOf("presidentSignatureUrl" to ""), SetOptions.merge())
                .let { com.google.android.gms.tasks.Tasks.await(it) }

            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(presidentSignatureUrl = "", updatedAt = System.currentTimeMillis()))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting president signature on Firestore: ${e.message}", e)
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            mandalInfoDao.saveMandalInfo(existingInfo.copy(presidentSignatureUrl = "", updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        }
    }

    // LIVE STREAM MANAGEMENT
    suspend fun updateLiveStreamStatus(
        isLive: Boolean,
        title: String,
        url: String,
        notifyMembers: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingInfo = mandalInfoDao.getMandalInfoDirect() ?: SeedData.defaultMandalInfo
            val cleanTitle = title.trim().ifEmpty { "श्री गणेश महाआरती थेट प्रक्षेपण" }
            val cleanUrl = url.trim()

            val updatedEntity = existingInfo.copy(
                isLiveStreamActive = isLive,
                liveStreamTitle = cleanTitle,
                liveStreamUrl = cleanUrl,
                liveStreamStartedAt = if (isLive) System.currentTimeMillis() else existingInfo.liveStreamStartedAt,
                updatedAt = System.currentTimeMillis()
            )

            // 1. Save to Room
            mandalInfoDao.saveMandalInfo(updatedEntity)

            // 2. Save to Firestore
            firestore.collection("mandal_info").document("mandal_default")
                .set(updatedEntity.toMap(), SetOptions.merge())

            // 3. If Live Started and notification enabled, broadcast notification to all members
            if (isLive && notifyMembers) {
                val notifId = "notif_live_" + System.currentTimeMillis()
                val liveNotif = NotificationEntity(
                    id = notifId,
                    title = "🔴 थेट प्रक्षेपण सुरू आहे!",
                    message = "$cleanTitle थेट सुरू झाले आहे. दर्शनासाठी व सोहळा पाहण्यासाठी आत्ताच येथे क्लिक करा!",
                    type = "LIVE",
                    targetRoute = "LIVE",
                    timestamp = System.currentTimeMillis()
                )
                notificationDao.insertNotification(liveNotif)
                try {
                    firestore.collection("notifications").document(notifId)
                        .set(liveNotif.toMap(), SetOptions.merge())
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Error sending live notification", e)
                }

                // Show local notification immediately on device
                com.example.util.SystemNotificationHelper.showSystemNotification(
                    context = appContext,
                    title = liveNotif.title,
                    message = liveNotif.message,
                    notificationId = 8888,
                    targetRoute = "LIVE"
                )
            }

            // 4. If Live stopped and URL is valid, auto-archive to dedicated "LIVE VIDEO" Album
            if (!isLive && cleanUrl.isNotBlank()) {
                try {
                    saveLiveStreamRecordingToLiveAlbum(
                        title = cleanTitle,
                        url = cleanUrl
                    )
                } catch (e: Exception) {
                    Log.e("LiveStream", "Auto-archive to LIVE VIDEO album note: ${e.message}", e)
                }
            }

            // 5. Clean up old live comments from past sessions when new stream starts
            if (isLive) {
                try {
                    // Delete comments from previous streams so new stream starts completely fresh
                    firestore.collection("live_comments")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (doc in querySnapshot.documents) {
                                doc.reference.delete()
                            }
                        }
                    _liveComments.value = emptyList()
                } catch (e: Exception) {
                    Log.d("LiveStream", "Past live comments cleanup note: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating live stream status: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun videoDaoListDirect(): List<VideoEntity> {
        return try {
            galleryDao.getAllVideos().first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Automatically archives a finished live stream into the dedicated "LIVE VIDEO" album.
     * 1. If the "LIVE VIDEO" album does not exist (first time live ends), it is created.
     * 2. All subsequent live streams are saved into the same album, with their exact title,
     *    date, and time, sorted by date (newest first).
     */
    suspend fun saveLiveStreamRecordingToLiveAlbum(
        title: String,
        url: String
    ) = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().ifEmpty { "मंडळ थेट प्रक्षेपण" }
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        val marathiDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now))

        // Extract YouTube thumbnail or high quality festival fallback
        val ytThumb = MediaUtils.extractYouTubeThumbnail(cleanUrl)
        val calculatedThumb = if (ytThumb.isNotBlank()) {
            ytThumb
        } else {
            "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80"
        }

        val liveAlbumId = "album_live_videos"
        val liveAlbumTitle = "LIVE VIDEO"

        // 1. Ensure "LIVE VIDEO" album exists in Room & Firestore
        var liveAlbum = galleryDao.getAlbumById(liveAlbumId)
        if (liveAlbum == null) {
            val allAlbums = galleryDao.getAllAlbums().first()
            liveAlbum = allAlbums.find { it.id == liveAlbumId || it.title.equals(liveAlbumTitle, ignoreCase = true) }
        }

        if (liveAlbum == null) {
            val newAlbum = AlbumEntity(
                id = liveAlbumId,
                title = liveAlbumTitle,
                category = "थेट प्रक्षेपण (Live)",
                coverImageUrl = calculatedThumb,
                description = "मंडळाचे सर्व थेट प्रक्षेपणांचे (Live Streams) रेकॉर्डिंग व संग्रह",
                photoCount = 0,
                albumType = "VIDEO",
                createdAt = now
            )
            galleryDao.insertAlbum(newAlbum)
            try {
                firestore.collection("albums").document(liveAlbumId)
                    .set(newAlbum.toMap(), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error creating LIVE VIDEO album on Firestore", e)
            }
            liveAlbum = newAlbum
        }

        val targetAlbumId = liveAlbum.id

        // 2. Check if this stream URL is already saved in this album to prevent duplicate entries
        val allVideos = videoDaoListDirect()
        val alreadyArchived = allVideos.any {
            (it.albumId == targetAlbumId || it.albumId == liveAlbumId) &&
                (it.videoUrl == cleanUrl || (cleanUrl.length > 10 && it.videoUrl.contains(cleanUrl)))
        }

        if (!alreadyArchived) {
            val video = VideoEntity(
                id = "vid_live_" + UUID.randomUUID().toString().take(8),
                albumId = targetAlbumId,
                title = cleanTitle,
                description = "थेट प्रक्षेपणाचे रेकॉर्डिंग • $marathiDate",
                category = "LIVE VIDEO",
                videoUrl = cleanUrl,
                thumbnailUrl = calculatedThumb,
                duration = "थेट रेकॉर्ड",
                uploadedAt = now
            )
            galleryDao.insertVideo(video)
            try {
                firestore.collection("videos").document(video.id)
                    .set(video.toMap(), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error adding live video to Firestore", e)
            }

            // 3. Update album count and cover image
            val currentVideosCount = galleryDao.getVideosForAlbum(targetAlbumId).first().size
            val updatedAlbum = liveAlbum.copy(
                photoCount = currentVideosCount,
                coverImageUrl = if (liveAlbum.coverImageUrl.isBlank() || liveAlbum.coverImageUrl.contains("unsplash")) calculatedThumb else liveAlbum.coverImageUrl
            )
            galleryDao.insertAlbum(updatedAlbum)
            try {
                firestore.collection("albums").document(targetAlbumId)
                    .set(updatedAlbum.toMap(), SetOptions.merge())
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error updating LIVE VIDEO album count on Firestore", e)
            }
            Log.d("LiveStream", "Successfully auto-archived live stream to LIVE VIDEO album: $cleanTitle")
        }

        // 4. Migrate any old unassigned live streams to this dedicated LIVE VIDEO album
        for (v in allVideos) {
            if ((v.albumId.isBlank() || v.albumId == "default_video_album") &&
                (v.category.contains("Live", ignoreCase = true) || v.category.contains("थेट") || v.title.contains("थेट"))
            ) {
                val updatedV = v.copy(albumId = targetAlbumId)
                galleryDao.insertVideo(updatedV)
                try {
                    firestore.collection("videos").document(v.id).update("albumId", targetAlbumId)
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }
        }
    }

    // REAL-TIME LIVE COMMENTS POSTING (INSTANT BROADCAST TO ALL WATCHERS)
    suspend fun postLiveComment(comment: LiveComment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentStreamStartedAt = mandalInfoDao.getMandalInfoDirect()?.liveStreamStartedAt ?: 0L
            val data = hashMapOf<String, Any>(
                "id" to comment.id,
                "userId" to comment.userId,
                "userName" to comment.userName,
                "userPhoto" to comment.userPhoto,
                "message" to comment.message,
                "timestamp" to comment.timestamp,
                "edited" to comment.edited,
                "streamStartedAt" to if (comment.streamStartedAt > 0L) comment.streamStartedAt else currentStreamStartedAt
            )
            firestore.collection("live_comments").document(comment.id).set(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error posting live comment on Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    // EDIT LIVE COMMENT (BY SENDER)
    suspend fun editLiveComment(commentId: String, newMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf<String, Any>(
                "message" to newMessage.trim(),
                "edited" to true,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("live_comments").document(commentId).update(updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error editing live comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    // DELETE LIVE COMMENT (BY SENDER OR ADMIN)
    suspend fun deleteLiveComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("live_comments").document(commentId).delete()
            _liveComments.value = _liveComments.value.filter { it.id != commentId }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting live comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    // CLEAR ALL LIVE COMMENTS (ADMIN SPECIAL ACTION)
    suspend fun clearAllLiveComments(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = Tasks.await(firestore.collection("live_comments").get())
            for (doc in querySnapshot.documents) {
                Tasks.await(doc.reference.delete())
            }
            _liveComments.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error clearing all live comments: ${e.message}", e)
            Result.failure(e)
        }
    }

    // REAL-TIME LIVE STREAM VIEWER PRESENCE (Track actual live audience)
    fun enterLivePresence(user: User?) {
        val userId = user?.id?.ifEmpty { null } 
            ?: ("viewer_" + (prefs.getString("device_uuid", null) ?: UUID.randomUUID().toString().take(8)))
        val userName = user?.fullName?.ifEmpty { "सभासद" } ?: "सभासद"
        val userPhoto = user?.profilePhotoUrl ?: ""
        currentLivePresenceSessionId = userId

        livePresenceHeartbeatJob?.cancel()
        livePresenceHeartbeatJob = repositoryScope.launch {
            try {
                val docRef = firestore.collection("live_viewers").document(userId)
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "userName" to userName,
                    "userPhoto" to userPhoto,
                    "joinedAt" to System.currentTimeMillis(),
                    "lastHeartbeat" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge())
                if (_realtimeLiveViewerCount.value < 1) {
                    _realtimeLiveViewerCount.value = 1
                }

                // Heartbeat every 20 seconds while user is actively watching
                while (isActive) {
                    delay(20_000L)
                    docRef.update("lastHeartbeat", System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "enterLivePresence error: ${e.message}", e)
            }
        }
    }

    fun leaveLivePresence() {
        val sessionId = currentLivePresenceSessionId
        livePresenceHeartbeatJob?.cancel()
        livePresenceHeartbeatJob = null
        currentLivePresenceSessionId = null

        if (sessionId != null) {
            repositoryScope.launch {
                try {
                    firestore.collection("live_viewers").document(sessionId).delete()
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "leaveLivePresence delete error: ${e.message}", e)
                }
            }
        }
    }

    // MEMBER FEEDBACK & SUGGESTIONS OPERATIONS
    suspend fun submitFeedback(
        category: String,
        rating: Int,
        message: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext Result.failure(Exception("अभिप्राय देण्यासाठी कृपया प्रथम लॉगिन करा."))
            val cleanMessage = message.trim()
            if (cleanMessage.isBlank()) {
                return@withContext Result.failure(Exception("कृपया आपला अभिप्राय किंवा सूचना टाईप करा."))
            }

            val feedbackId = "fb_" + UUID.randomUUID().toString().take(8)
            val feedback = MemberFeedback(
                id = feedbackId,
                userId = user.id,
                userName = user.fullName,
                userMobile = user.mobileNumber,
                userDesignation = user.designation,
                userPhotoUrl = user.profilePhotoUrl,
                category = category.trim().ifBlank { "सर्वसाधारण सूचना" },
                rating = rating.coerceIn(1, 5),
                message = cleanMessage,
                timestamp = System.currentTimeMillis(),
                status = "NEW"
            )

            // 1. Save to Firestore
            val feedbackMap = mapOf(
                "id" to feedback.id,
                "userId" to feedback.userId,
                "userName" to feedback.userName,
                "userMobile" to feedback.userMobile,
                "userDesignation" to feedback.userDesignation,
                "userPhotoUrl" to feedback.userPhotoUrl,
                "category" to feedback.category,
                "rating" to feedback.rating,
                "message" to feedback.message,
                "timestamp" to feedback.timestamp,
                "status" to feedback.status
            )
            Tasks.await(firestore.collection("feedbacks").document(feedback.id).set(feedbackMap))

            // 2. Update local state immediately
            val current = _feedbacks.value.toMutableList()
            current.removeAll { it.id == feedback.id }
            current.add(0, feedback)
            _feedbacks.value = current

            // 3. Trigger Admin Notification
            try {
                val notifId = "notif_" + UUID.randomUUID().toString().take(8)
                val notif = NotificationEntity(
                    id = notifId,
                    title = "💬 नवीन सभासद अभिप्राय: ${user.fullName}",
                    message = "${feedback.category} (${feedback.rating}★): '${cleanMessage.take(50)}'",
                    type = "ADMIN",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    targetUserId = "ADMIN",
                    targetRoute = "MEMBER_FEEDBACK",
                    targetId = "MEMBER_FEEDBACK"
                )
                notificationDao.insertNotification(notif)
                firestore.collection("notifications").document(notif.id).set(notif.toMap())
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MandalRepo", "submitFeedback error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(firestore.collection("feedbacks").document(feedbackId).delete())
            _feedbacks.value = _feedbacks.value.filter { it.id != feedbackId }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MandalRepo", "deleteFeedback error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateFeedbackStatus(feedbackId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(firestore.collection("feedbacks").document(feedbackId).update("status", status))
            _feedbacks.value = _feedbacks.value.map {
                if (it.id == feedbackId) it.copy(status = status) else it
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MandalRepo", "updateFeedbackStatus error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // EMERGENCY BLOOD SOS ALERT FUNCTIONS
    suspend fun sendEmergencyBloodAlert(
        bloodGroup: String,
        patientName: String,
        hospital: String,
        unitsNeeded: String,
        contactPerson: String,
        contactNumber: String,
        additionalNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("कृपया प्रथम लॉगिन करा"))
        if (!user.isAdmin) return@withContext Result.failure(Exception("फक्त ॲडमिनच आणीबाणी अलर्ट पाठवू शकतात"))

        val alertId = "blood_alert_" + UUID.randomUUID().toString().take(8)
        val alert = com.example.data.model.EmergencyBloodAlert(
            id = alertId,
            bloodGroup = bloodGroup.trim().uppercase(),
            patientName = patientName.trim(),
            hospital = hospital.trim(),
            unitsNeeded = unitsNeeded.trim().ifBlank { "1" },
            contactPerson = contactPerson.trim().ifBlank { user.fullName },
            contactNumber = contactNumber.trim().ifBlank { user.mobileNumber },
            additionalNote = additionalNote.trim(),
            createdBy = user.fullName,
            timestamp = System.currentTimeMillis(),
            isActive = true
        )

        try {
            val alertMap = hashMapOf<String, Any>(
                "id" to alert.id,
                "bloodGroup" to alert.bloodGroup,
                "patientName" to alert.patientName,
                "hospital" to alert.hospital,
                "unitsNeeded" to alert.unitsNeeded,
                "contactPerson" to alert.contactPerson,
                "contactNumber" to alert.contactNumber,
                "additionalNote" to alert.additionalNote,
                "createdBy" to alert.createdBy,
                "timestamp" to alert.timestamp,
                "isActive" to true
            )
            Tasks.await(firestore.collection("emergency_blood_alerts").document(alertId).set(alertMap))
            _activeBloodAlert.value = alert

            // Also post to notifications collection for all members
            val notif = NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "🚨 तातडीची गरज: ${alert.bloodGroup} रक्त हवे आहे!",
                message = "रुग्ण: ${alert.patientName} | हॉस्पिटल: ${alert.hospital} (${alert.unitsNeeded} बाटल्या). संपर्क: ${alert.contactNumber}",
                type = "BLOOD_ALERT",
                timestamp = System.currentTimeMillis(),
                targetRoute = "BLOOD_ALERT",
                targetId = alertId
            )
            notificationDao.insertNotification(notif)
            firestore.collection("notifications").document(notif.id).set(notif.toMap())

            // Trigger FCM push to all registered members
            com.example.util.FcmPushSenderHelper.sendPushToTopic(
                topic = "mandal_emergency_blood",
                title = notif.title,
                message = notif.message,
                type = "BLOOD_ALERT",
                targetRoute = "BLOOD_ALERT",
                targetId = alertId,
                senderId = alert.createdBy
            )

            // Trigger immediate local notification on device
            com.example.util.SystemNotificationHelper.showSystemNotification(
                context = appContext,
                title = notif.title,
                message = notif.message,
                channelId = com.example.util.SystemNotificationHelper.CHANNEL_EMERGENCY_BLOOD,
                targetRoute = "BLOOD_ALERT",
                targetId = alertId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "sendEmergencyBloodAlert error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resolveEmergencyBloodAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(firestore.collection("emergency_blood_alerts").document(alertId).update("isActive", false))
            if (_activeBloodAlert.value?.id == alertId) {
                _activeBloodAlert.value = null
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "resolveEmergencyBloodAlert error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkAndDispatchBirthdayNotifications() = withContext(Dispatchers.IO) {
        try {
            val prefs = appContext.getSharedPreferences("mandal_prefs", Context.MODE_PRIVATE)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastSentDate = prefs.getString("last_birthday_push_date", "")
            if (lastSentDate == todayStr) {
                return@withContext // Already dispatched today
            }

            val sdfMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault())
            val currentMonthDay = sdfMonthDay.format(Date())

            val allUsers = userDao.getApprovedUsers().first()
            val bdayUsers = allUsers.filter { member ->
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

            if (bdayUsers.isNotEmpty()) {
                val title = "🎂 आजचे वाढदिवस! (जय हिंद मंडळ)"
                val message = if (bdayUsers.size == 1) {
                    "🚩 आज आपले सक्रिय सभासद ${bdayUsers.first().fullName} यांचा वाढदिवस आहे. त्यांना शुभेच्छा देण्यासाठी येथे क्लिक करा!"
                } else {
                    val names = bdayUsers.take(2).joinToString(", ") { it.fullName } + (if (bdayUsers.size > 2) " आणि इतर" else "")
                    "🚩 आज आपले सभासद $names यांचे वाढदिवस आहेत. त्यांना शुभेच्छा देण्यासाठी येथे क्लिक करा!"
                }

                com.example.util.FcmPushSenderHelper.sendPushToTopic(
                    topic = "mandal_birthdays",
                    title = title,
                    message = message,
                    type = "BIRTHDAY",
                    targetRoute = "BIRTHDAYS",
                    targetId = bdayUsers.first().id,
                    senderId = "SYSTEM"
                )

                prefs.edit().putString("last_birthday_push_date", todayStr).apply()
                Log.d("MandalRepository", "Birthday push dispatched successfully for ${bdayUsers.size} members")
            }
        } catch (e: Exception) {
            Log.e("MandalRepository", "Error checking/dispatching birthday push: ${e.message}")
        }
    }

    // BUSINESS DIRECTORY (Local Yellow Pages)
    fun getAllBusinesses(): Flow<List<BusinessListing>> {
        return businessDirectoryDao.getAllBusinesses().map { list -> list.map { it.toDomain() } }
    }

    suspend fun addBusiness(
        businessName: String,
        ownerName: String,
        category: String,
        description: String,
        contactNumber: String,
        whatsappNumber: String,
        address: String,
        photoUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val bizId = "biz_" + UUID.randomUUID().toString().take(8)
        val entity = BusinessListingEntity(
            id = bizId,
            businessName = businessName.trim(),
            ownerName = ownerName.trim(),
            category = category.trim().ifBlank { "इतर" },
            description = description.trim(),
            contactNumber = contactNumber.trim(),
            whatsappNumber = whatsappNumber.trim().ifBlank { contactNumber.trim() },
            address = address.trim().ifBlank { "अर्जुनवाड" },
            photoUrl = photoUrl.trim(),
            isVerified = true,
            timestamp = System.currentTimeMillis()
        )
        businessDirectoryDao.insertBusiness(entity)
        try {
            firestore.collection("business_directory").document(bizId).set(entity.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error saving business to firestore: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun updateBusiness(
        id: String,
        businessName: String,
        ownerName: String,
        category: String,
        description: String,
        contactNumber: String,
        whatsappNumber: String,
        address: String,
        photoUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = BusinessListingEntity(
            id = id,
            businessName = businessName.trim(),
            ownerName = ownerName.trim(),
            category = category.trim().ifBlank { "इतर" },
            description = description.trim(),
            contactNumber = contactNumber.trim(),
            whatsappNumber = whatsappNumber.trim().ifBlank { contactNumber.trim() },
            address = address.trim().ifBlank { "अर्जुनवाड" },
            photoUrl = photoUrl.trim(),
            isVerified = true,
            timestamp = System.currentTimeMillis()
        )
        businessDirectoryDao.insertBusiness(entity)
        try {
            firestore.collection("business_directory").document(id).set(entity.toMap(), SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error updating business on firestore: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun deleteBusiness(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        businessDirectoryDao.deleteBusiness(id)
        try {
            firestore.collection("business_directory").document(id).delete()
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting business on firestore: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun saveFcmToken(token: String) = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value
            val data = hashMapOf<String, Any>(
                "token" to token,
                "platform" to "android",
                "updatedAt" to System.currentTimeMillis()
            )
            if (user != null) {
                data["userId"] = user.id
                data["userName"] = user.fullName
                data["isAdmin"] = user.isAdmin
                data["role"] = user.role
            }
            firestore.collection("fcm_tokens").document(token)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
            if (user != null) {
                firestore.collection("users").document(user.id)
                    .update("fcmToken", token)
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error saving FCM token: ${e.message}")
        }
    }
}

// Domain Mapping Extensions
fun UserEntity.toDomain(): User {
    val cleanRole = (role as String?).orEmpty().trim().ifBlank { "MEMBER" }
    val cleanDesig = (designation as String?).orEmpty().trim().ifBlank {
        if (cleanRole == "ADMIN" || cleanRole == "PRESIDENT" || cleanRole == "SECRETARY") "कार्यकारणी सदस्य" else "सभासद"
    }
    return User(
        id = (id as String?).orEmpty(),
        fullName = (fullName as String?).orEmpty().ifBlank { "सभासद" },
        mobileNumber = (mobileNumber as String?).orEmpty(),
        password = (password as String?).orEmpty(),
        profilePhotoUrl = (profilePhotoUrl as String?).orEmpty(),
        gender = (gender as String?).orEmpty().ifBlank { "पुरुष" },
        bloodGroup = (bloodGroup as String?).orEmpty().ifBlank { "O+" },
        dateOfBirth = (dateOfBirth as String?).orEmpty(),
        address = (address as String?).orEmpty().ifBlank { "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर" },
        role = cleanRole,
        designation = cleanDesig,
        status = (status as String?).orEmpty().ifBlank { "APPROVED" },
        createdAt = try { createdAt } catch (_: Throwable) { System.currentTimeMillis() },
        isOnline = try { isOnline } catch (_: Throwable) { false },
        lastSeen = try { lastSeen } catch (_: Throwable) { 0L },
        fcmToken = (fcmToken as String?).orEmpty(),
        activeSessionId = (activeSessionId as String?).orEmpty()
    )
}

fun parsePostImageUrls(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    if (raw.contains("|||")) {
        return raw.split("|||").map { it.trim() }.filter { it.isNotBlank() }
    }
    val trimmed = raw.trim()
    if (trimmed.startsWith("data:") || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
        return listOf(trimmed)
    }
    return trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

fun PostEntity.toDomain() = Post(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    authorRole = authorRole,
    content = content,
    imageUrls = parsePostImageUrls(imageUrlsJson),
    videoUrl = videoUrl,
    likedUserIds = if (likedUserIdsJson.isNotBlank()) likedUserIdsJson.split(",").filter { it.isNotBlank() } else emptyList(),
    commentsCount = commentsCount,
    timestamp = timestamp,
    isSponsored = isSponsored,
    sponsorBusinessName = sponsorBusinessName,
    sponsorContactNumber = sponsorContactNumber,
    sponsorCtaText = sponsorCtaText
)

fun CommentEntity.toDomain() = Comment(
    id = id,
    postId = postId,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    text = text,
    timestamp = timestamp,
    likedUserIds = if (likedUserIdsJson.isNotBlank()) likedUserIdsJson.split(",").filter { it.isNotBlank() } else emptyList(),
    parentId = parentId,
    replyToAuthorName = replyToAuthorName,
    isEdited = isEdited,
    editedAt = editedAt
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
    albumType = albumType,
    createdAt = createdAt
)

fun PhotoEntity.toDomain() = GalleryPhoto(
    id = id,
    albumId = albumId,
    imageUrl = imageUrl,
    caption = caption,
    viewCount = viewCount,
    uploadedAt = uploadedAt
)

fun VideoEntity.toDomain() = VideoItem(
    id = id,
    albumId = albumId,
    title = title,
    description = description,
    category = category,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    viewCount = viewCount,
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
    officialStampUrl = officialStampUrl,
    presidentSignatureUrl = presidentSignatureUrl,
    presidentName = presidentName,
    idCardObjectives = idCardObjectives,
    idCardRules = idCardRules,
    emergencyContacts = emergencyContacts,
    isLiveStreamActive = isLiveStreamActive,
    liveStreamTitle = liveStreamTitle,
    liveStreamUrl = liveStreamUrl,
    liveStreamStartedAt = liveStreamStartedAt,
    liveViewerCount = liveViewerCount,
    showFestiveBanner = showFestiveBanner,
    manualFestivalId = manualFestivalId,
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
    "fcmToken" to fcmToken,
    "activeSessionId" to activeSessionId
)

fun DocumentSnapshot.toUserEntity(): UserEntity? {
    val id = getString("id") ?: id
    val mobile = getString("mobileNumber") ?: return null
    val roleStr = (getString("role") ?: "MEMBER").trim().ifBlank { "MEMBER" }
    val desigStr = (getString("designation") ?: "").trim()
    return UserEntity(
        id = id,
        fullName = (getString("fullName") ?: "").trim().ifBlank { "सभासद" },
        mobileNumber = mobile.trim(),
        password = getString("password") ?: "",
        profilePhotoUrl = getString("profilePhotoUrl") ?: "",
        gender = (getString("gender") ?: "").trim().ifBlank { "पुरुष" },
        bloodGroup = (getString("bloodGroup") ?: "").trim().ifBlank { "O+" },
        dateOfBirth = getString("dateOfBirth") ?: "",
        address = (getString("address") ?: "").trim().ifBlank { "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर" },
        role = roleStr,
        designation = desigStr,
        status = (getString("status") ?: "APPROVED").trim().ifBlank { "APPROVED" },
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        isOnline = getBoolean("isOnline") ?: false,
        lastSeen = getLong("lastSeen") ?: 0L,
        fcmToken = getString("fcmToken") ?: "",
        activeSessionId = getString("activeSessionId") ?: ""
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
    "timestamp" to timestamp,
    "isSponsored" to isSponsored,
    "sponsorBusinessName" to sponsorBusinessName,
    "sponsorContactNumber" to sponsorContactNumber,
    "sponsorCtaText" to sponsorCtaText
)

fun DocumentSnapshot.toPostEntity(): PostEntity? {
    val id = getString("id") ?: id
    val authorId = getString("authorId") ?: return null
    val rawImage = getString("imageUrlsJson")?.takeIf { it.isNotBlank() }
        ?: getString("imageUrl")?.takeIf { it.isNotBlank() }
        ?: (get("imageUrls") as? List<*>)?.filterNotNull()?.joinToString(",")
        ?: ""
    val rawTs = get("timestamp")
    var postTs = when (rawTs) {
        is Number -> rawTs.toLong()
        is com.google.firebase.Timestamp -> rawTs.toDate().time
        else -> System.currentTimeMillis()
    }
    if (postTs in 1..99999999999L) {
        postTs *= 1000L
    }
    return PostEntity(
        id = id,
        authorId = authorId,
        authorName = getString("authorName") ?: "",
        authorPhotoUrl = getString("authorPhotoUrl") ?: getString("authorProfilePhoto") ?: "",
        authorRole = getString("authorRole") ?: "",
        content = getString("content") ?: "",
        imageUrlsJson = rawImage,
        videoUrl = getString("videoUrl"),
        likedUserIdsJson = getString("likedUserIdsJson") ?: "",
        commentsCount = (getLong("commentsCount") ?: 0L).toInt(),
        timestamp = postTs,
        isSponsored = getBoolean("isSponsored") ?: false,
        sponsorBusinessName = getString("sponsorBusinessName"),
        sponsorContactNumber = getString("sponsorContactNumber"),
        sponsorCtaText = getString("sponsorCtaText")
    )
}

fun CommentEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "postId" to postId,
    "authorId" to authorId,
    "authorName" to authorName,
    "authorPhotoUrl" to authorPhotoUrl,
    "text" to text,
    "timestamp" to timestamp,
    "likedUserIdsJson" to likedUserIdsJson,
    "parentId" to parentId,
    "replyToAuthorName" to replyToAuthorName,
    "isEdited" to isEdited,
    "editedAt" to editedAt
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
        timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
        likedUserIdsJson = getString("likedUserIdsJson") ?: "",
        parentId = getString("parentId"),
        replyToAuthorName = getString("replyToAuthorName"),
        isEdited = getBoolean("isEdited") ?: false,
        editedAt = getLong("editedAt")
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
    "albumType" to albumType,
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
        albumType = getString("albumType") ?: "PHOTO",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
}

fun PhotoEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "albumId" to albumId,
    "imageUrl" to imageUrl,
    "caption" to caption,
    "viewCount" to viewCount,
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
        viewCount = (getLong("viewCount") ?: 0L).toInt(),
        uploadedAt = getLong("uploadedAt") ?: System.currentTimeMillis()
    )
}

fun VideoEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "albumId" to albumId,
    "title" to title,
    "description" to description,
    "category" to category,
    "videoUrl" to videoUrl,
    "thumbnailUrl" to thumbnailUrl,
    "duration" to duration,
    "viewCount" to viewCount,
    "uploadedAt" to uploadedAt
)

fun DocumentSnapshot.toVideoEntity(): VideoEntity? {
    val id = getString("id") ?: id
    val title = getString("title") ?: return null
    return VideoEntity(
        id = id,
        albumId = getString("albumId") ?: "",
        title = title,
        description = getString("description") ?: "",
        category = getString("category") ?: "",
        videoUrl = getString("videoUrl") ?: "",
        thumbnailUrl = getString("thumbnailUrl") ?: "",
        duration = getString("duration") ?: "03:45",
        viewCount = (getLong("viewCount") ?: 0L).toInt(),
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
    "createdAt" to timestamp,
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
    "officialStampUrl" to officialStampUrl,
    "presidentSignatureUrl" to presidentSignatureUrl,
    "presidentName" to presidentName,
    "idCardObjectives" to idCardObjectives,
    "idCardRules" to idCardRules,
    "emergencyContacts" to emergencyContacts,
    "isLiveStreamActive" to isLiveStreamActive,
    "liveStreamTitle" to liveStreamTitle,
    "liveStreamUrl" to liveStreamUrl,
    "liveStreamStartedAt" to liveStreamStartedAt,
    "liveViewerCount" to liveViewerCount,
    "showFestiveBanner" to showFestiveBanner,
    "manualFestivalId" to manualFestivalId,
    "updatedAt" to updatedAt
)

fun DocumentSnapshot.toMandalInfoEntity(): MandalInfoEntity? {
    val id = getString("id") ?: id.ifEmpty { "mandal_default" }
    val mandalName = getString("mandalName") ?: SeedData.defaultMandalInfo.mandalName
    return MandalInfoEntity(
        id = id,
        mandalName = mandalName,
        tagline = getString("tagline") ?: SeedData.defaultMandalInfo.tagline,
        locationTitle = getString("locationTitle") ?: SeedData.defaultMandalInfo.locationTitle,
        aboutDescription = getString("aboutDescription") ?: SeedData.defaultMandalInfo.aboutDescription,
        email = getString("email") ?: SeedData.defaultMandalInfo.email,
        address = getString("address") ?: SeedData.defaultMandalInfo.address,
        phone = getString("phone") ?: SeedData.defaultMandalInfo.phone,
        youtubeHandle = getString("youtubeHandle") ?: SeedData.defaultMandalInfo.youtubeHandle,
        facebookHandle = getString("facebookHandle") ?: SeedData.defaultMandalInfo.facebookHandle,
        instagramHandle = getString("instagramHandle") ?: SeedData.defaultMandalInfo.instagramHandle,
        adminWebLink = getString("adminWebLink") ?: SeedData.defaultMandalInfo.adminWebLink,
        logoUrl = getString("logoUrl") ?: "",
        officialStampUrl = getString("officialStampUrl") ?: "",
        presidentSignatureUrl = getString("presidentSignatureUrl") ?: "",
        presidentName = getString("presidentName") ?: "अध्यक्ष",
        idCardObjectives = getString("idCardObjectives") ?: SeedData.defaultMandalInfo.idCardObjectives,
        idCardRules = getString("idCardRules") ?: SeedData.defaultMandalInfo.idCardRules,
        emergencyContacts = getString("emergencyContacts") ?: SeedData.defaultMandalInfo.emergencyContacts,
        isLiveStreamActive = getBoolean("isLiveStreamActive") ?: false,
        liveStreamTitle = getString("liveStreamTitle") ?: "श्री गणेश महाआरती थेट प्रक्षेपण",
        liveStreamUrl = getString("liveStreamUrl") ?: "",
        liveStreamStartedAt = getLong("liveStreamStartedAt") ?: 0L,
        liveViewerCount = (getLong("liveViewerCount") ?: 0L).toInt(),
        showFestiveBanner = getBoolean("showFestiveBanner") ?: true,
        manualFestivalId = getString("manualFestivalId") ?: "",
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
    )
}

fun BusinessListingEntity.toDomain() = BusinessListing(
    id = id,
    businessName = businessName,
    ownerName = ownerName,
    category = category,
    description = description,
    contactNumber = contactNumber,
    whatsappNumber = whatsappNumber,
    address = address,
    photoUrl = photoUrl,
    isVerified = isVerified,
    timestamp = timestamp
)

fun BusinessListingEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "businessName" to businessName,
    "ownerName" to ownerName,
    "category" to category,
    "description" to description,
    "contactNumber" to contactNumber,
    "whatsappNumber" to whatsappNumber,
    "address" to address,
    "photoUrl" to photoUrl,
    "isVerified" to isVerified,
    "timestamp" to timestamp
)

fun DocumentSnapshot.toBusinessListingEntity(): BusinessListingEntity? {
    val id = getString("id") ?: id
    val bName = getString("businessName") ?: return null
    return BusinessListingEntity(
        id = id,
        businessName = bName,
        ownerName = getString("ownerName") ?: "",
        category = getString("category") ?: "इतर",
        description = getString("description") ?: "",
        contactNumber = getString("contactNumber") ?: "",
        whatsappNumber = getString("whatsappNumber") ?: "",
        address = getString("address") ?: "अर्जुनवाड",
        photoUrl = getString("photoUrl") ?: "",
        isVerified = getBoolean("isVerified") ?: true,
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )
}
