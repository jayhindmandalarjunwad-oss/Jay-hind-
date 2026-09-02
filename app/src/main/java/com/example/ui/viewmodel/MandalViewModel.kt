package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.MandalRepository
import com.example.util.IdCardUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    LOGIN,
    REGISTER,
    MAIN,
    ADMIN_PANEL,
    GALLERY,
    PHOTO_GALLERY,
    VIDEO_GALLERY,
    EVENTS,
    ANNOUNCEMENTS,
    NOTIFICATIONS,
    CHAT_DETAIL,
    USER_POSTS
}

enum class NavigationTab {
    HOME,
    QUICK_ACCESS,
    MEMBERS,
    POSTS,
    CHAT,
    PROFILE
}

class MandalViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MandalRepository(application.applicationContext)

    // Current Screen & Tab
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.POSTS)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Mandal Logo State
    val mandalLogoUrl: StateFlow<String?> = repository.mandalLogoUrl

    // Mandal Banners State
    val banners: StateFlow<List<MandalBanner>> = repository.banners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mandal Info (About Us, Socials, Handles, Admin Link)
    val mandalInfo: StateFlow<MandalInfo> = repository.mandalInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MandalInfo())

    // Auth & User State
    val currentUser: StateFlow<User?> = repository.currentUser
    val sessionSecurityNotice: StateFlow<String?> = repository.sessionSecurityNotice

    fun clearSessionSecurityNotice() {
        repository.clearSessionSecurityNotice()
    }

    // UI Feedback Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Members Directory & Filtering
    val approvedMembers: StateFlow<List<User>> = repository.approvedMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMembers: StateFlow<List<User>> = repository.pendingMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<User>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayBirthdays: StateFlow<List<User>> = repository.todayBirthdayMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _memberSearchQuery = MutableStateFlow("")
    val memberSearchQuery: StateFlow<String> = _memberSearchQuery.asStateFlow()

    private val _selectedBloodGroupFilter = MutableStateFlow("सर्व")
    val selectedBloodGroupFilter: StateFlow<String> = _selectedBloodGroupFilter.asStateFlow()

    val filteredMembers: StateFlow<List<User>> = combine(
        approvedMembers,
        _memberSearchQuery,
        _selectedBloodGroupFilter
    ) { members, query, bloodGroup ->
        members.filter { member ->
            val matchesQuery = query.isBlank() ||
                    member.fullName.contains(query, ignoreCase = true) ||
                    member.mobileNumber.contains(query) ||
                    member.address.contains(query, ignoreCase = true)

            val matchesBloodGroup = bloodGroup == "सर्व" || member.bloodGroup.equals(bloodGroup, ignoreCase = true)
            matchesQuery && matchesBloodGroup
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected member for detail view sheet
    private val _selectedMemberForDetail = MutableStateFlow<User?>(null)
    val selectedMemberForDetail: StateFlow<User?> = _selectedMemberForDetail.asStateFlow()

    // Selected user for author timeline/posts screen
    private val _selectedUserForPosts = MutableStateFlow<User?>(null)
    val selectedUserForPosts: StateFlow<User?> = _selectedUserForPosts.asStateFlow()

    // External Verification State (triggered when scanned by device camera deep-link)
    private val _scannedVerificationResult = MutableStateFlow<IdCardUtils.QrVerificationResult?>(null)
    val scannedVerificationResult: StateFlow<IdCardUtils.QrVerificationResult?> = _scannedVerificationResult.asStateFlow()

    // Posts & Feed
    val posts: StateFlow<List<Post>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Comments Sheet State
    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    val activePostComments: StateFlow<List<Comment>> = _activeCommentPost.flatMapLatest { post ->
        if (post != null) repository.getComments(post.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat State
    private val _activeChatPartner = MutableStateFlow<User?>(null)
    val activeChatPartner: StateFlow<User?> = _activeChatPartner.asStateFlow()

    val groupChatMessages: StateFlow<List<ChatMessage>> = repository.groupChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeConversationMessages: StateFlow<List<ChatMessage>> = combine(
        currentUser,
        _activeChatPartner
    ) { user, partner ->
        if (user != null && partner != null) {
            if (partner.id == "GROUP_MANDAL") {
                repository.groupChatMessages
            } else {
                val convId = repository.getConversationId(user.id, partner.id)
                repository.getConversationMessages(convId, user.id, partner.id)
            }
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatSummaries: StateFlow<List<ChatConversationSummary>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getConversationSummaries(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadChatCount: StateFlow<Int> = repository.unreadChatCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Gallery State
    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<VideoItem>> = repository.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    val albumPhotos: StateFlow<List<GalleryPhoto>> = _selectedAlbum.flatMapLatest { album ->
        if (album != null) repository.getPhotosForAlbum(album.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fullscreenPhotoUrl = MutableStateFlow<String?>(null)
    val fullscreenPhotoUrl: StateFlow<String?> = _fullscreenPhotoUrl.asStateFlow()

    private val _fullscreenViewerState = MutableStateFlow<FullscreenViewerState?>(null)
    val fullscreenViewerState: StateFlow<FullscreenViewerState?> = _fullscreenViewerState.asStateFlow()

    private val _playingVideo = MutableStateFlow<VideoItem?>(null)
    val playingVideo: StateFlow<VideoItem?> = _playingVideo.asStateFlow()

    // Events State
    val events: StateFlow<List<MandalEvent>> = repository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Announcements State
    val announcements: StateFlow<List<Announcement>> = repository.announcements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications State
    val notifications: StateFlow<List<MandalNotification>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.ADMIN_PANEL || screen == AppScreen.NOTIFICATIONS || screen == AppScreen.EVENTS) {
            refreshAllData(silent = true)
        }
    }

    fun setNavigationTab(tab: NavigationTab) {
        _currentTab.value = tab
        if (tab == NavigationTab.POSTS || tab == NavigationTab.MEMBERS || tab == NavigationTab.CHAT) {
            refreshAllData(silent = true)
        }
    }

    fun refreshAllData(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) showSnackbar("क्लाऊड डेटा सिंक होत आहे...")
            repository.forceSyncFromFirebase()
            if (!silent) showSnackbar("डेटा यशस्वीरित्या सिंक झाला! 🔄")
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // AUTH ACTIONS
    fun login(mobile: String, pass: String, onSuccess: () -> Unit) {
        if (mobile.isBlank() || pass.isBlank()) {
            showSnackbar("कृपया मोबाईल नंबर आणि पासवर्ड टाका.")
            return
        }
        viewModelScope.launch {
            val res = repository.login(mobile, pass)
            res.onSuccess {
                showSnackbar("स्वागत आहे, ${it.fullName}!")
                _currentScreen.value = AppScreen.MAIN
                _currentTab.value = NavigationTab.POSTS
                refreshAllData(silent = true)
                onSuccess()
            }.onFailure {
                showSnackbar(it.message ?: "लॉगिन अयशस्वी झाले.")
            }
        }
    }

    fun register(
        fullName: String,
        mobileNumber: String,
        password: String,
        profilePhotoUrl: String,
        gender: String,
        bloodGroup: String,
        dateOfBirth: String,
        address: String,
        onSuccess: () -> Unit
    ) {
        if (fullName.isBlank() || mobileNumber.isBlank() || password.isBlank() || address.isBlank()) {
            showSnackbar("कृपया सर्व आवश्यक माहिती भरा.")
            return
        }
        if (mobileNumber.length < 10) {
            showSnackbar("कृपया वैध १० अंकी मोबाईल नंबर टाका.")
            return
        }
        viewModelScope.launch {
            val res = repository.registerMember(
                fullName = fullName,
                mobileNumber = mobileNumber,
                password = password,
                profilePhotoUrl = profilePhotoUrl,
                gender = gender,
                bloodGroup = bloodGroup,
                dateOfBirth = dateOfBirth,
                address = address
            )
            res.onSuccess { message ->
                showSnackbar(message)
                _currentScreen.value = AppScreen.LOGIN
                onSuccess()
            }.onFailure {
                showSnackbar(it.message ?: "नोंदणी अयशस्वी झाली.")
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = AppScreen.LOGIN
        showSnackbar("आपण यशस्वीरित्या लॉगआउट झाला आहात.")
    }

    fun updateProfile(
        fullName: String,
        gender: String,
        bloodGroup: String,
        dateOfBirth: String,
        address: String,
        photoUrl: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateProfile(user.id, fullName, gender, bloodGroup, dateOfBirth, address, photoUrl)
            res.onSuccess {
                showSnackbar("प्रोफाइल माहिती अद्यतनित झाली!")
            }.onFailure {
                showSnackbar(it.message ?: "प्रोफाइल बदलताना त्रुटी आली.")
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        val user = currentUser.value ?: return
        if (newPass.length < 4) {
            showSnackbar("नवीन पासवर्ड किमान ४ अक्षरांचा असावा.")
            return
        }
        viewModelScope.launch {
            val res = repository.changePassword(user.id, oldPass, newPass)
            res.onSuccess {
                showSnackbar("पासवर्ड यशस्वीरित्या बदलला आहे!")
            }.onFailure {
                showSnackbar(it.message ?: "पासवर्ड बदलताना त्रुटी आली.")
            }
        }
    }

    // MEMBERS DIRECTORY ACTIONS
    fun setMemberSearchQuery(query: String) {
        _memberSearchQuery.value = query
    }

    fun setSelectedBloodGroupFilter(bg: String) {
        _selectedBloodGroupFilter.value = bg
    }

    fun selectMemberForDetail(user: User?) {
        _selectedMemberForDetail.value = user
    }

    // QR VERIFICATION ACTIONS (Deep-link & external camera verification)
    fun handleScannedQrPayload(raw: String) {
        val membersList = allMembers.value
        val result = IdCardUtils.parseVerificationQrPayload(raw, membersList)
        if (result != null) {
            _scannedVerificationResult.value = result
            showSnackbar("✅ सभासद पडताळणी यशस्वी: ${result.fullName}")
        } else {
            showSnackbar("QR कोड ओळखता आला नाही. कृपया पुन्हा प्रयत्न करा.")
        }
    }

    fun clearVerificationResult() {
        _scannedVerificationResult.value = null
    }

    fun showVerificationForUser(user: User) {
        val memberId = IdCardUtils.formatMemberId(user)
        val roleStr = if (user.designation.isNotBlank()) user.designation else if (user.isAdmin) "कार्यकारिणी सदस्य" else "सभासद"
        _scannedVerificationResult.value = IdCardUtils.QrVerificationResult(
            memberId = memberId,
            fullName = user.fullName,
            designation = roleStr,
            mobileNumber = user.mobileNumber,
            bloodGroup = user.bloodGroup,
            address = user.address,
            userId = user.id,
            matchedUser = user,
            isOfficialMandal = true,
            rawContent = IdCardUtils.getVerificationPayload(user)
        )
    }

    // POSTS ACTIONS
    fun createPost(content: String, imageUrl: String?, videoUrl: String?, onDone: () -> Unit) {
        val user = currentUser.value
        if (user?.status == "BLOCKED") {
            showSnackbar("आपले खाते ब्लॉक असल्याने आपण नवीन पोस्ट करू शकत नाही. ⚠️")
            return
        }
        if (content.isBlank() && imageUrl == null) {
            showSnackbar("पोस्टसाठी काही मजकूर किंवा फोटो निवडा.")
            return
        }
        viewModelScope.launch {
            val res = repository.createPost(content, imageUrl, videoUrl)
            res.onSuccess {
                showSnackbar("पोस्ट यशस्वीरित्या प्रसिद्ध झाली! 🚩")
                onDone()
            }.onFailure {
                showSnackbar(it.message ?: "पोस्ट करताना त्रुटी आली.")
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = currentUser.value
        if (user?.status == "BLOCKED") {
            showSnackbar("आपले खाते ब्लॉक असल्याने आपण पोस्ट लाईक करू शकत नाही. ⚠️")
            return
        }
        viewModelScope.launch {
            repository.toggleLikePost(postId)
        }
    }

    fun openComments(post: Post) {
        _activeCommentPost.value = post
    }

    fun openUserPosts(authorId: String, authorName: String = "", authorPhoto: String = "") {
        val foundUser = allMembers.value.find { it.id == authorId }
            ?: approvedMembers.value.find { it.id == authorId }
            ?: if (currentUser.value?.id == authorId) currentUser.value else null

        val target = foundUser ?: User(
            id = authorId,
            fullName = authorName.ifEmpty { "सभासद" },
            mobileNumber = "",
            password = "",
            profilePhotoUrl = authorPhoto,
            designation = "सभासद"
        )
        _selectedUserForPosts.value = target
        _currentScreen.value = AppScreen.USER_POSTS
    }

    fun closeUserPosts() {
        _selectedUserForPosts.value = null
        _currentScreen.value = AppScreen.MAIN
    }

    fun closeComments() {
        _activeCommentPost.value = null
    }

    fun addComment(text: String) {
        val user = currentUser.value
        if (user?.status == "BLOCKED") {
            showSnackbar("आपले खाते ब्लॉक असल्याने आपण कमेंट करू शकत नाही. ⚠️")
            return
        }
        val post = _activeCommentPost.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(post.id, text)
            // Update comments count in local state
            _activeCommentPost.value = post.copy(commentsCount = post.commentsCount + 1)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            showSnackbar("पोस्ट हटवण्यात आली आहे.")
        }
    }

    fun updatePost(postId: String, content: String, imageUrl: String?, videoUrl: String?, onDone: () -> Unit) {
        val user = currentUser.value
        if (user?.status == "BLOCKED") {
            showSnackbar("आपले खाते ब्लॉक असल्याने आपण पोस्ट एडिट करू शकत नाही. ⚠️")
            return
        }
        if (content.isBlank() && imageUrl == null) {
            showSnackbar("पोस्टसाठी काही मजकूर किंवा फोटो निवडा.")
            return
        }
        viewModelScope.launch {
            val res = repository.updatePost(postId, content, imageUrl, videoUrl)
            res.onSuccess {
                showSnackbar("पोस्ट यशस्वीरित्या अपडेट झाली! ✏️")
                onDone()
            }.onFailure {
                showSnackbar(it.message ?: "पोस्ट अपडेट करताना त्रुटी आली.")
            }
        }
    }

    // CHAT ACTIONS
    val MANDAL_GROUP_USER = User(
        id = "GROUP_MANDAL",
        fullName = "🚩 जय हिंद मंडळ सर्व सदस्य",
        mobileNumber = "सर्व सभासद",
        password = "",
        role = "ADMIN",
        designation = "अधिकृत मंडळ ग्रुप",
        status = "APPROVED",
        profilePhotoUrl = "https://images.unsplash.com/photo-1544717305-2782549b5136?w=200&auto=format&fit=crop&q=80"
    )

    fun openGroupChat() {
        _activeChatPartner.value = MANDAL_GROUP_USER
        _currentScreen.value = AppScreen.CHAT_DETAIL
    }

    fun openChatWith(partner: User) {
        _activeChatPartner.value = partner
        val user = currentUser.value
        if (user != null) {
            val convId = if (partner.id == "GROUP_MANDAL") "conv_mandal_group" else repository.getConversationId(user.id, partner.id)
            viewModelScope.launch {
                repository.markChatAsRead(convId, user.id, partner.id)
            }
        }
        _currentScreen.value = AppScreen.CHAT_DETAIL
    }

    fun closeChat() {
        _activeChatPartner.value = null
        _currentScreen.value = AppScreen.MAIN
        _currentTab.value = NavigationTab.CHAT
    }

    fun sendChatMessage(
        text: String,
        imageUrl: String? = null,
        attachmentType: String? = null,
        attachmentUrl: String? = null,
        attachmentName: String? = null,
        attachmentExtra: String? = null
    ) {
        val user = currentUser.value
        if (user?.status == "BLOCKED") {
            showSnackbar("आपले खाते ब्लॉक असल्याने आपण मेसेज पाठवू शकत नाही. ⚠️")
            return
        }
        val partner = _activeChatPartner.value ?: return
        if (text.isBlank() && imageUrl == null && attachmentUrl == null && attachmentExtra == null) return
        viewModelScope.launch {
            repository.sendMessage(
                receiverId = partner.id,
                receiverName = partner.fullName,
                messageText = text,
                imageUrl = imageUrl,
                attachmentType = attachmentType,
                attachmentUrl = attachmentUrl,
                attachmentName = attachmentName,
                attachmentExtra = attachmentExtra
            )
        }
    }

    fun deleteChatMessage(message: ChatMessage) {
        val user = currentUser.value ?: return
        val isGroup = message.receiverId == "GROUP_MANDAL" || message.conversationId == "conv_mandal_group"
        val canDelete = if (isGroup) {
            message.senderId == user.id || user.isAdmin
        } else {
            message.senderId == user.id
        }

        if (!canDelete) {
            showSnackbar("आपण हा मेसेज हटवू शकत नाही.")
            return
        }

        viewModelScope.launch {
            repository.deleteChatMessage(message.id)
            showSnackbar("मेसेज हटवण्यात आला. 🗑️")
        }
    }

    // GALLERY ACTIONS
    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
        _currentScreen.value = AppScreen.PHOTO_GALLERY
    }

    fun createAlbum(title: String, category: String, coverImage: String, desc: String) {
        viewModelScope.launch {
            repository.createAlbum(title, category, coverImage, desc)
            showSnackbar("नवीन ॲल्बम तयार झाला!")
        }
    }

    fun addPhotoToActiveAlbum(imageUrl: String, caption: String) {
        val album = _selectedAlbum.value ?: return
        viewModelScope.launch {
            repository.addPhotoToAlbum(album.id, imageUrl, caption)
            showSnackbar("फोटो ॲल्बममध्ये जोडला गेला!")
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            showSnackbar("ॲल्बम हटवला गेला.")
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            repository.deletePhoto(photoId)
            showSnackbar("फोटो हटवला गेला.")
        }
    }

    fun updatePhotoCaption(photoId: String, caption: String) {
        viewModelScope.launch {
            repository.updatePhotoCaption(photoId, caption)
            showSnackbar("फोटोचे शीर्षक अपडेट केले! ✨")
        }
    }

    fun openFullscreenPhoto(url: String?, title: String? = null) {
        if (url.isNullOrBlank()) {
            _fullscreenPhotoUrl.value = null
            _fullscreenViewerState.value = null
        } else {
            _fullscreenPhotoUrl.value = url
            _fullscreenViewerState.value = FullscreenViewerState(
                photos = listOf(url),
                initialIndex = 0,
                titles = if (!title.isNullOrBlank()) listOf(title) else emptyList()
            )
        }
    }

    fun openFullscreenPhotos(photos: List<String>, initialIndex: Int = 0, titles: List<String> = emptyList()) {
        val valid = photos.filter { it.isNotBlank() }
        if (valid.isEmpty()) {
            _fullscreenPhotoUrl.value = null
            _fullscreenViewerState.value = null
        } else {
            val safeIndex = initialIndex.coerceIn(0, valid.size - 1)
            _fullscreenPhotoUrl.value = valid.getOrNull(safeIndex)
            _fullscreenViewerState.value = FullscreenViewerState(
                photos = valid,
                initialIndex = safeIndex,
                titles = titles
            )
        }
    }

    fun closeFullscreenPhoto() {
        _fullscreenPhotoUrl.value = null
        _fullscreenViewerState.value = null
    }

    fun addVideo(title: String, desc: String, category: String, videoUrl: String, thumbUrl: String) {
        viewModelScope.launch {
            repository.addVideo(title, desc, category, videoUrl, thumbUrl)
            showSnackbar("नवीन व्हिडिओ जोडला गेला!")
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
            showSnackbar("व्हिडिओ हटवला गेला.")
        }
    }

    fun playVideo(video: VideoItem?) {
        _playingVideo.value = video
    }

    // EVENTS ACTIONS
    fun createEvent(title: String, date: String, time: String, location: String, desc: String, img: String, cat: String = "") {
        viewModelScope.launch {
            repository.createEvent(title, date, time, location, desc, img, cat)
            showSnackbar("नवीन कार्यक्रम जोडला गेला!")
        }
    }

    fun updateEvent(eventId: String, title: String, date: String, time: String, location: String, desc: String, img: String) {
        viewModelScope.launch {
            repository.updateEvent(eventId, title, date, time, location, desc, img)
            showSnackbar("कार्यक्रम यशस्वीरित्या अद्यतनित केला! ✅")
        }
    }

    fun toggleEventRegistration(event: MandalEvent) {
        viewModelScope.launch {
            repository.toggleEventRegistration(event.id, event.isRegistered)
            showSnackbar(if (!event.isRegistered) "आपली कार्यक्रमासाठी उपस्थिती नोंदवली आहे! 🎉" else "उपस्थिती रद्द केली.")
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            showSnackbar("कार्यक्रम हटवला गेला.")
        }
    }

    // ANNOUNCEMENTS ACTIONS
    fun createAnnouncement(title: String, content: String, priority: String) {
        viewModelScope.launch {
            repository.createAnnouncement(title, content, priority)
            showSnackbar("सूचना फलकावर प्रसिद्ध झाली!")
        }
    }

    fun updateAnnouncement(id: String, title: String, content: String, priority: String) {
        viewModelScope.launch {
            repository.updateAnnouncement(id, title, content, priority)
            showSnackbar("सूचना यशस्वीरित्या बदलण्यात आली! ✏️")
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            showSnackbar("सूचना हटवली गेली.")
        }
    }

    // NOTIFICATIONS
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            showSnackbar("सर्व सूचना वाचल्या म्हणून चिन्हांकित केल्या.")
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            showSnackbar("सर्व नोटिफिकेशन्स हटवले गेले.")
        }
    }

    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notifId)
        }
    }

    fun handleNotificationClick(notif: MandalNotification) {
        markNotificationAsRead(notif.id)
        when (notif.targetRoute) {
            "ADMIN_PENDING" -> {
                if (currentUser.value?.isAdmin == true) {
                    _currentScreen.value = AppScreen.ADMIN_PANEL
                } else {
                    _currentScreen.value = AppScreen.MAIN
                    _currentTab.value = NavigationTab.HOME
                }
            }
            "LIVE" -> {
                _currentScreen.value = AppScreen.MAIN
                _currentTab.value = NavigationTab.HOME
                openLiveStreamPlayer()
            }
            "EVENTS" -> {
                _currentScreen.value = AppScreen.EVENTS
            }
            "ANNOUNCEMENTS" -> {
                _currentScreen.value = AppScreen.ANNOUNCEMENTS
            }
            "BIRTHDAYS" -> {
                _currentScreen.value = AppScreen.MAIN
                _currentTab.value = NavigationTab.MEMBERS
            }
            "POST_COMMENTS", "POST" -> {
                _currentScreen.value = AppScreen.MAIN
                _currentTab.value = NavigationTab.POSTS
                if (!notif.targetId.isNullOrBlank()) {
                    val matchingPost = posts.value.find { it.id == notif.targetId }
                    if (matchingPost != null) {
                        openComments(matchingPost)
                    }
                }
            }
            "CHAT" -> {
                if (!notif.targetId.isNullOrBlank()) {
                    val partner = approvedMembers.value.find { it.id == notif.targetId }
                    if (partner != null) {
                        openChatWith(partner)
                        return
                    }
                }
                _currentScreen.value = AppScreen.MAIN
                _currentTab.value = NavigationTab.CHAT
            }
            else -> {
                _currentScreen.value = AppScreen.MAIN
            }
        }
    }

    fun handleNotificationRoute(targetRoute: String?, targetId: String?) {
        if (targetRoute.isNullOrBlank()) return
        val notif = MandalNotification(
            id = "",
            title = "",
            message = "",
            targetRoute = targetRoute,
            targetId = targetId,
            type = "",
            timestamp = System.currentTimeMillis()
        )
        handleNotificationClick(notif)
    }

    fun broadcastNotification(title: String, message: String) {
        viewModelScope.launch {
            repository.broadcastNotification(title, message)
            showSnackbar("सर्व सभासदांना नोटिफिकेशन पाठवले गेले!")
        }
    }

    // ADMIN ACTIONS ON USERS
    fun approveMember(userId: String) {
        viewModelScope.launch {
            repository.setMemberStatus(userId, "APPROVED")
            showSnackbar("सभासद यशस्वीरित्या मंजूर (Approved) करण्यात आला! ✅")
        }
    }

    fun rejectMember(userId: String) {
        viewModelScope.launch {
            repository.setMemberStatus(userId, "REJECTED")
            showSnackbar("सभासद नोंदणी नामंजूर केली.")
        }
    }

    fun blockMember(userId: String) {
        viewModelScope.launch {
            repository.setMemberStatus(userId, "BLOCKED")
            showSnackbar("सभासद ब्लॉक केला गेला.")
        }
    }

    fun unblockMember(userId: String) {
        viewModelScope.launch {
            repository.setMemberStatus(userId, "APPROVED")
            showSnackbar("सभासद यशस्वीरित्या अनब्लॉक करण्यात आला! ✅")
        }
    }

    fun deleteMember(userId: String) {
        viewModelScope.launch {
            repository.deleteMember(userId)
            showSnackbar("सभासद खाते हटवण्यात आले.")
        }
    }

    fun deleteMemberAndTransferRights(userId: String) {
        viewModelScope.launch {
            repository.deleteMemberAndTransferRights(userId)
            showSnackbar("सभासद हटवला व सर्व हक्क व पोस्ट्स ॲडमिनकडे हस्तांतरित केले गेले! ✅")
        }
    }

    // MANDAL LOGO MANAGEMENT
    fun updateMandalLogo(url: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (url.isBlank()) {
            showSnackbar("कृपया वैध लोगो निवडा.")
            onComplete?.invoke(false)
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.updateMandalLogo(url)
                if (result.isSuccess) {
                    showSnackbar("मंडळ लोगो सर्व ॲपमध्ये यशस्वीरित्या अद्यतनित झाला! 🚩")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ लोगो अपडेट करताना त्रुटी आली. कृपया पुन्हा प्रयत्न करा.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteMandalLogo(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = repository.deleteMandalLogo()
                if (result.isSuccess) {
                    showSnackbar("मंडळ लोगो हटवला गेला आणि डीफॉल्ट लोगो सेट झाला.")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ डीफॉल्ट लोगो सेट करताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    // MANDAL OFFICIAL STAMP MANAGEMENT
    fun updateOfficialStamp(stampUrl: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (stampUrl.isBlank()) {
            showSnackbar("कृपया अधिकृत शिक्क्याचा फोटो किंवा इमेज निवडा.")
            onComplete?.invoke(false)
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.updateOfficialStamp(stampUrl)
                if (result.isSuccess) {
                    showSnackbar("मंडळाचा अधिकृत शिक्का सर्व ओळखपत्रांवर यशस्वीरित्या सेट झाला! 🏛️✅")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ शिक्का अपडेट करताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteOfficialStamp(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = repository.deleteOfficialStamp()
                if (result.isSuccess) {
                    showSnackbar("अधिकृत शिक्का रीसेट झाला आणि डीफॉल्ट डिजिटल शिक्का सेट झाला.")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ शिक्का रीसेट करताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    // MANDAL PRESIDENT SIGNATURE MANAGEMENT
    fun updatePresidentSignature(signatureUrl: String, presidentName: String = "अध्यक्ष", onComplete: ((Boolean) -> Unit)? = null) {
        if (signatureUrl.isBlank()) {
            showSnackbar("कृपया स्वाक्षरीचा फोटो किंवा इमेज निवडा.")
            onComplete?.invoke(false)
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.updatePresidentSignature(signatureUrl, presidentName)
                if (result.isSuccess) {
                    showSnackbar("अध्यक्षांची स्वाक्षरी सर्व ओळखपत्रांवर यशस्वीरित्या अद्यतनित झाली! ✍️✅")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ स्वाक्षरी अपडेट करताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun deletePresidentSignature(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val result = repository.deletePresidentSignature()
                if (result.isSuccess) {
                    showSnackbar("अध्यक्षांची स्वाक्षरी हटवण्यात आली आणि डीफॉल्ट डिजिटल स्वाक्षरी सेट झाली.")
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ स्वाक्षरी हटवताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    // MANDAL BANNERS MANAGEMENT
    fun addBanner(imageUrl: String, title: String = "", subtitle: String = "", actionUrl: String = "") {
        if (imageUrl.isBlank()) {
            showSnackbar("कृपया बॅनरची इमेज URL टाका.")
            return
        }
        viewModelScope.launch {
            repository.addBanner(imageUrl, title, subtitle, actionUrl)
            showSnackbar("नवीन बॅनर यशस्वीरित्या जोडला गेला! 🚩")
        }
    }

    fun updateBanner(id: String, imageUrl: String, title: String = "", subtitle: String = "", actionUrl: String = "") {
        if (imageUrl.isBlank()) {
            showSnackbar("कृपया बॅनरची इमेज URL टाका.")
            return
        }
        viewModelScope.launch {
            repository.updateBanner(id, imageUrl, title, subtitle, actionUrl)
            showSnackbar("बॅनर अद्यतनित करण्यात आला! ✅")
        }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch {
            repository.deleteBanner(id)
            showSnackbar("बॅनर हटवण्यात आला.")
        }
    }

    // MANDAL INFO (ABOUT US & SOCIALS) MANAGEMENT
    fun updateMandalInfo(
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
    ) {
        viewModelScope.launch {
            repository.updateMandalInfo(
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
                adminWebLink = adminWebLink
            )
            showSnackbar("मंडळाची माहिती व सोशल मीडिया लिंक्स यशस्वीरित्या सेव्ह केल्या! ✅")
        }
    }

    // ADMIN ROLE & DESIGNATION MANAGEMENT
    fun changeUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            repository.changeUserRole(userId, newRole)
            showSnackbar("सभासदाचा रोल ($newRole) बदलण्यात आला! ✅")
        }
    }

    fun updateMemberDesignationAndRole(userId: String, newDesignation: String, newRole: String) {
        viewModelScope.launch {
            repository.updateMemberDesignationAndRole(userId, newDesignation, newRole)
            showSnackbar("सभासदाचे पद व अधिकार यशस्वीरित्या अपडेट केले! ✅")
        }
    }

    // LIVE STREAM MANAGEMENT & IN-APP PLAYER
    private val _showLiveStreamPlayer = MutableStateFlow(false)
    val showLiveStreamPlayer: StateFlow<Boolean> = _showLiveStreamPlayer.asStateFlow()

    private val _liveComments = MutableStateFlow<List<LiveComment>>(emptyList())
    val liveComments: StateFlow<List<LiveComment>> = _liveComments.asStateFlow()

    fun openLiveStreamPlayer() {
        _showLiveStreamPlayer.value = true
    }

    fun closeLiveStreamPlayer() {
        _showLiveStreamPlayer.value = false
    }

    fun postLiveComment(message: String) {
        if (message.isBlank()) return
        val user = currentUser.value
        val name = user?.fullName?.ifEmpty { "सभासद" } ?: "जय हिंद सभासद"
        val photo = user?.profilePhotoUrl ?: ""
        val newComment = LiveComment(
            id = "comment_${System.currentTimeMillis()}",
            userName = name,
            userPhoto = photo,
            message = message.trim(),
            timestamp = System.currentTimeMillis()
        )
        _liveComments.value = _liveComments.value + newComment
    }

    fun sendLiveReaction(reactionText: String) {
        val user = currentUser.value
        val name = user?.fullName?.ifEmpty { "सभासद" } ?: "जय हिंद सभासद"
        val newComment = LiveComment(
            id = "reaction_${System.currentTimeMillis()}",
            userName = name,
            userPhoto = user?.profilePhotoUrl ?: "",
            message = reactionText,
            timestamp = System.currentTimeMillis()
        )
        _liveComments.value = _liveComments.value + newComment
        showSnackbar("प्रतिक्रिया नोंदवली: $reactionText 🚩")
    }

    fun setLiveStreamStatus(
        isLive: Boolean,
        title: String,
        url: String,
        notifyMembers: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = repository.updateLiveStreamStatus(
                    isLive = isLive,
                    title = title,
                    url = url,
                    notifyMembers = notifyMembers
                )
                if (result.isSuccess) {
                    if (isLive) {
                        showSnackbar("🔴 थेट प्रक्षेपण सुरू झाले आणि सर्व सभासदांना नोटिफिकेशन पाठवले! 🚩")
                    } else {
                        showSnackbar("⏹️ थेट प्रक्षेपण थांबवले आणि व्हिडिओ गॅलरीमध्ये सेव्ह केले. ✅")
                    }
                    onComplete?.invoke(true)
                } else {
                    showSnackbar("❌ थेट प्रक्षेपण अपडेट करताना त्रुटी आली.")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                showSnackbar("❌ त्रुटी: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    // REFRESH & SYNC
    fun refreshAllData() {
        viewModelScope.launch {
            repository.refreshAllFromFirestore()
            showSnackbar("डेटा रिफ्रेश झाला! 🔄")
        }
    }
}

data class FullscreenViewerState(
    val photos: List<String> = emptyList(),
    val initialIndex: Int = 0,
    val titles: List<String> = emptyList()
)
