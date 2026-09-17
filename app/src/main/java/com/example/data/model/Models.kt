package com.example.data.model

data class User(
    val id: String = "",
    val fullName: String = "सभासद",
    val mobileNumber: String = "",
    val password: String = "",
    val profilePhotoUrl: String = "",
    val gender: String = "पुरुष",
    val bloodGroup: String = "O+",
    val dateOfBirth: String = "",
    val address: String = "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर",
    val role: String = "MEMBER", // ADMIN, MEMBER, PRESIDENT, SECRETARY, TREASURER
    val designation: String = "सभासद",
    val status: String = "APPROVED", // APPROVED, PENDING_APPROVAL, REJECTED, BLOCKED
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val activeSessionId: String = ""
) {
    val isAdmin: Boolean get() {
        val r = (role as String?).orEmpty().uppercase()
        return r == "ADMIN" || r == "PRESIDENT" || r == "SECRETARY"
    }
    val isContentAdmin: Boolean get() {
        val r = (role as String?).orEmpty().uppercase()
        return r == "CONTENT_ADMIN"
    }
    val isAnyAdmin: Boolean get() = isAdmin || isContentAdmin
    val isApproved: Boolean get() {
        val s = (status as String?).orEmpty().uppercase()
        return s == "APPROVED"
    }
}

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String,
    val authorRole: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val likedUserIds: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSponsored: Boolean = false,
    val sponsorBusinessName: String? = null,
    val sponsorContactNumber: String? = null,
    val sponsorCtaText: String? = null
) {
    fun isLikedBy(userId: String): Boolean = likedUserIds.contains(userId)
    val likesCount: Int get() = likedUserIds.size
}

data class BusinessListing(
    val id: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val category: String = "इतर",
    val description: String = "",
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val address: String = "अर्जुनवाड",
    val photoUrl: String = "",
    val isVerified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likedUserIds: List<String> = emptyList(),
    val parentId: String? = null,
    val replyToAuthorName: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
) {
    fun isLikedBy(userId: String): Boolean = likedUserIds.contains(userId)
    val likesCount: Int get() = likedUserIds.size
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val senderPhotoUrl: String,
    val messageText: String,
    val imageUrl: String? = null,
    val attachmentType: String? = null, // IMAGE, VIDEO, DOCUMENT, CONTACT
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentExtra: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class ChatConversationSummary(
    val otherUser: User,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int
)

data class Album(
    val id: String,
    val title: String,
    val category: String,
    val coverImageUrl: String,
    val description: String,
    val photoCount: Int = 0,
    val albumType: String = "PHOTO", // "PHOTO" or "VIDEO"
    val createdAt: Long = System.currentTimeMillis()
)

data class GalleryPhoto(
    val id: String,
    val albumId: String,
    val imageUrl: String,
    val caption: String,
    val viewCount: Int = 0,
    val uploadedAt: Long = System.currentTimeMillis()
)

data class VideoItem(
    val id: String,
    val albumId: String = "",
    val title: String,
    val description: String,
    val category: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val duration: String = "03:45",
    val viewCount: Int = 0,
    val uploadedAt: Long = System.currentTimeMillis()
)

data class MandalEvent(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val attendeesCount: Int = 0,
    val isRegistered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val priority: String = "NORMAL", // URGENT, HIGH, NORMAL
    val date: String,
    val author: String = "मंडळ कार्यकारणी",
    val createdAt: Long = System.currentTimeMillis()
)

data class MandalNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // POST, COMMENT, CHAT, EVENT, ANNOUNCEMENT, BIRTHDAY, ADMIN
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetUserId: String? = null,
    val targetRoute: String? = null,
    val targetId: String? = null,
    val targetExtra: String? = null
)

data class MandalBanner(
    val id: String,
    val imageUrl: String,
    val title: String = "",
    val subtitle: String = "",
    val actionUrl: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class MandalInfo(
    val id: String = "mandal_default",
    val mandalName: String = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
    val tagline: String = "अर्जुनवाड • सभासद कुटुंब व डिजिटल सेवा मंच",
    val locationTitle: String = "🚩 अर्जुनवाड, ता. शिरोळ",
    val aboutDescription: String = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड हे गावातील सामाजिक, सांस्कृतिक, क्रीडा, आरोग्य व शैक्षणिक प्रगतीसाठी अविरत कार्यरत असणारे अग्रगण्य मंडळ आहे. सर्व सभासदांना एकत्र आणून समाजोपयोगी उपक्रम राबवणे हे आमचे मुख्य ध्येय आहे.",
    val email: String = "jayhindmandalarjunwad@gmail.com",
    val address: String = "मु. पो. अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर - ४१६१०१",
    val phone: String = "+91 98765 43210",
    val youtubeHandle: String = "@JayHindMandalArjunwad",
    val facebookHandle: String = "JayHindMandalArjunwad",
    val instagramHandle: String = "jayhind_mandal_arjunwad",
    val adminWebLink: String = "",
    val logoUrl: String = "",
    val officialStampUrl: String = "",
    val presidentSignatureUrl: String = "",
    val presidentName: String = "अध्यक्ष",
    val idCardObjectives: String = "",
    val idCardRules: String = "",
    val emergencyContacts: String = "",
    val isLiveStreamActive: Boolean = false,
    val liveStreamTitle: String = "श्री गणेश महाआरती थेट प्रक्षेपण",
    val liveStreamUrl: String = "https://www.youtube.com/@JayHindMandalArjunwad/live",
    val liveStreamStartedAt: Long = 0L,
    val liveViewerCount: Int = 0,
    val showFestiveBanner: Boolean = true,
    val manualFestivalId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class LiveViewer(
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val joinedAt: Long = System.currentTimeMillis(),
    val lastHeartbeat: Long = System.currentTimeMillis()
)

data class LiveReaction(
    val id: String,
    val emoji: String,
    val label: String
)

data class LiveComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val edited: Boolean = false,
    val streamStartedAt: Long = 0L
)

data class MemberFeedback(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userMobile: String = "",
    val userDesignation: String = "सभासद",
    val userPhotoUrl: String = "",
    val category: String = "सर्वसाधारण सूचना",
    val rating: Int = 5,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "NEW" // NEW, READ, RESOLVED
)

data class EmergencyBloodAlert(
    val id: String = "",
    val bloodGroup: String = "O+",
    val patientName: String = "",
    val hospital: String = "",
    val unitsNeeded: String = "1",
    val contactPerson: String = "",
    val contactNumber: String = "",
    val additionalNote: String = "",
    val createdBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
