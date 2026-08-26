package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.seed.SeedData
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

    init {
        repositoryScope.launch {
            seedDatabaseIfEmpty()
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

    private suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingAdmin = userDao.getUserById(SeedData.defaultAdmin.id)
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
                // Ensure admin mobile number and password match the specified credentials
                userDao.updateUser(
                    existingAdmin.copy(
                        mobileNumber = "9545791089",
                        password = "ADMIN",
                        role = "ADMIN",
                        status = "APPROVED"
                    )
                )
                if (mandalInfoDao.getMandalInfoDirect() == null) {
                    mandalInfoDao.saveMandalInfo(SeedData.defaultMandalInfo)
                }
            }
        }
    }

    // AUTH & USERS
    suspend fun login(mobile: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByMobile(mobile.trim())
        if (user == null) {
            return@withContext Result.failure(Exception("हा मोबाईल नंबर नोंदणीकृत नाही. कृपया नोंदणी करा."))
        }
        if (user.password != pass.trim()) {
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
        val existing = userDao.getUserByMobile(mobileNumber.trim())
        if (existing != null) {
            return@withContext Result.failure(Exception("हा मोबाईल नंबर आधीच नोंदणीकृत आहे."))
        }

        val newId = "user_" + UUID.randomUUID().toString().take(8)
        val entity = UserEntity(
            id = newId,
            fullName = fullName.trim(),
            mobileNumber = mobileNumber.trim(),
            password = password.trim(),
            profilePhotoUrl = profilePhotoUrl.ifEmpty {
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80"
            },
            gender = gender,
            bloodGroup = bloodGroup,
            dateOfBirth = dateOfBirth,
            address = address.trim(),
            role = "MEMBER",
            designation = "सभासद",
            status = "PENDING_APPROVAL", // Goes to admin approval!
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(entity)

        // Add admin notification
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "नवीन सभासद नोंदणी",
                message = "${fullName} यांनी नवीन सभासदत्व नोंदणी केली आहे. कृपया Admin Panel मधून मंजुरी द्या.",
                type = "ADMIN"
            )
        )

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
        Result.success(Unit)
    }

    suspend fun changePassword(userId: String, oldPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("सभासद सापडला नाही"))
        if (user.password != oldPass) {
            return@withContext Result.failure(Exception("सध्याचा पासवर्ड चुकीचा आहे."))
        }
        val updated = user.copy(password = newPass)
        userDao.updateUser(updated)
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
                        // yyyy-MM-dd or yyyy/MM/dd
                        parts[1].padStart(2, '0') to parts[2].padStart(2, '0')
                    } else {
                        // dd-MM-yyyy or dd/MM/yyyy
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
    }

    suspend fun setMemberStatus(userId: String, status: String) = withContext(Dispatchers.IO) {
        userDao.updateUserStatus(userId, status)
    }

    suspend fun deleteMember(userId: String) = withContext(Dispatchers.IO) {
        userDao.deleteUser(userId)
    }

    suspend fun deleteMemberAndTransferRights(userId: String) = withContext(Dispatchers.IO) {
        val admin = userDao.getUserById("admin_1") ?: userDao.getUserByMobile("9545791089")
        if (admin != null) {
            // Reassign all posts and contributions of this member to the Administrator
            postDao.reassignPostsAuthor(
                oldAuthorId = userId,
                newAuthorId = admin.id,
                newAuthorName = admin.fullName,
                newAuthorPhotoUrl = admin.profilePhotoUrl
            )
        }
        userDao.deleteUser(userId)
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
            authorRole = "",
            content = content.trim(),
            imageUrlsJson = imageUrl ?: "",
            videoUrl = videoUrl,
            likedUserIdsJson = "",
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        postDao.insertPost(newPost)

        // Broadcast post notification with deep link to post comments / detail
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "${user.fullName} यांनी नवीन पोस्ट केली 🚩",
                message = if (content.isNotBlank()) content.take(60) else "नवीन फोटो किंवा माहिती पोस्ट केली आहे.",
                type = "POST",
                targetRoute = "POST_COMMENTS",
                targetId = newPost.id,
                targetExtra = content.take(40)
            )
        )
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
        postDao.updatePostLikes(postId, currentLikes.joinToString(","))
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        postDao.deletePost(postId)
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

        // Targeted Notification: Only notify the author of the post (if someone else commented)
        val post = postDao.getAllPosts().first().find { it.id == postId }
        if (post != null && post.authorId != user.id) {
            notificationDao.insertNotification(
                NotificationEntity(
                    id = "notif_" + UUID.randomUUID().toString().take(8),
                    title = "आपल्या पोस्टवर नवीन कमेंट 💬",
                    message = "${user.fullName} यांनी आपल्या पोस्टवर कमेंट केली: \"${text.take(45)}\"",
                    type = "COMMENT",
                    targetUserId = post.authorId,
                    targetRoute = "POST_COMMENTS",
                    targetId = postId,
                    targetExtra = post.content.take(30)
                )
            )
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

        // Notification to receiver
        val preview = if (messageText.isNotBlank()) messageText.take(50) else when (attachmentType) {
            "IMAGE" -> "📷 फोटो पाठवला आहे"
            "VIDEO" -> "🎥 व्हिडिओ पाठवला आहे"
            "DOCUMENT" -> "📄 डॉक्युमेंट: ${attachmentName ?: ""}"
            "CONTACT" -> "👤 संपर्क क्रमांक: ${attachmentName ?: ""}"
            else -> "नवीन संदेश प्राप्त झाला"
        }
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "${user.fullName} कडून मेसेज",
                message = preview,
                type = "CHAT",
                targetUserId = receiverId,
                targetRoute = "CHAT",
                targetId = user.id,
                targetExtra = user.fullName
            )
        )
        Result.success(Unit)
    }

    fun getConversationSummaries(currentUserId: String): Flow<List<ChatConversationSummary>> {
        return combine(
            chatDao.getAllMessagesForUser(currentUserId),
            allMembers
        ) { messages, members ->
            val memberMap = members.associateBy { it.id }
            // Group strictly by other member's ID so there are never duplicate items or keys
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
    }

    suspend fun addPhotoToAlbum(albumId: String, imageUrl: String, caption: String) = withContext(Dispatchers.IO) {
        val photo = PhotoEntity(
            id = "ph_" + UUID.randomUUID().toString().take(8),
            albumId = albumId,
            imageUrl = imageUrl,
            caption = caption.trim()
        )
        galleryDao.insertPhoto(photo)
    }

    suspend fun deleteAlbum(albumId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteAlbum(albumId)
    }

    suspend fun deletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        galleryDao.deletePhoto(photoId)
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
    }

    suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteVideo(videoId)
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

        // Notification for new event with deep link to EVENTS
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "नवीन कार्यक्रम: $title 🚩",
                message = "$date रोजी $location येथे '$title' आयोजित करण्यात आला आहे.",
                type = "EVENT",
                targetRoute = "EVENTS",
                targetId = event.id
            )
        )
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
    }

    suspend fun toggleEventRegistration(eventId: String, isRegistered: Boolean) = withContext(Dispatchers.IO) {
        eventDao.toggleEventRegistration(eventId, !isRegistered)
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        eventDao.deleteEvent(eventId)
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

        // Push notification with deep link to ANNOUNCEMENTS
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = "महत्वाची सूचना: $title 📢",
                message = content.take(70),
                type = "ANNOUNCEMENT",
                targetRoute = "ANNOUNCEMENTS",
                targetId = ann.id
            )
        )
    }

    suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
        announcementDao.deleteAnnouncement(id)
    }

    // NOTIFICATIONS WITH STRICT RELEVANCE FILTERING & TODAY'S BIRTHDAY SUPPORT
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
                "BIRTHDAY" -> birthdayMembers.isNotEmpty() // Only show birthday alerts when someone has a birthday today
                else -> {
                    // EVENT, ANNOUNCEMENT, POST: Visible to all or targeted
                    notif.targetUserId == null || notif.targetUserId == user?.id || (notif.targetUserId == "ADMIN" && user?.isAdmin == true)
                }
            }
        }.map { it.toDomain() }

        // Prepend dynamic birthday notification if today's birthdays are active and not in DB
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
        notificationDao.insertNotification(
            NotificationEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = title.trim(),
                message = message.trim(),
                type = "ADMIN",
                targetRoute = "ANNOUNCEMENTS"
            )
        )
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
    }

    suspend fun deleteBanner(id: String) = withContext(Dispatchers.IO) {
        bannerDao.deleteBanner(id)
    }

    // MANDAL INFO (ABOUT US, CONTACTS, SOCIAL HANDLES, ADMIN LINK)
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
    }

    // MANDAL LOGO MANAGEMENT
    fun updateMandalLogo(url: String?) {
        val cleanUrl = url?.trim()?.ifEmpty { null }
        prefs.edit().putString("mandal_logo_url", cleanUrl).apply()
        _mandalLogoUrl.value = cleanUrl
    }

    fun deleteMandalLogo() {
        prefs.edit().remove("mandal_logo_url").apply()
        _mandalLogoUrl.value = null
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

    // CHAT DOMAIN MAPPING
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

fun PhotoEntity.toDomain() = PhotoEntity(
    id = id,
    albumId = albumId,
    imageUrl = imageUrl,
    caption = caption,
    uploadedAt = uploadedAt
).let { GalleryPhoto(it.id, it.albumId, it.imageUrl, it.caption, it.uploadedAt) }

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
