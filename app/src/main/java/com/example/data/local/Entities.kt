package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val mobileNumber: String,
    val password: String,
    val profilePhotoUrl: String = "",
    val gender: String = "पुरुष",
    val bloodGroup: String = "O+",
    val dateOfBirth: String = "1998-08-22",
    val address: String = "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर",
    val role: String = "MEMBER", // ADMIN, MEMBER, PRESIDENT, SECRETARY, TREASURER
    val designation: String = "सभासद",
    val status: String = "APPROVED", // APPROVED, PENDING_APPROVAL, REJECTED, BLOCKED
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val activeSessionId: String = ""
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String,
    val authorRole: String,
    val content: String,
    val imageUrlsJson: String = "", // Comma separated or single url
    val videoUrl: String? = null,
    val likedUserIdsJson: String = "", // Comma separated user IDs
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val coverImageUrl: String,
    val description: String,
    val photoCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val albumId: String,
    val imageUrl: String,
    val caption: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val duration: String = "03:45",
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val priority: String = "NORMAL", // URGENT, HIGH, NORMAL
    val date: String,
    val author: String = "मंडळ कार्यकारणी",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val title: String = "",
    val subtitle: String = "",
    val actionUrl: String = "",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mandal_info")
data class MandalInfoEntity(
    @PrimaryKey val id: String = "mandal_default",
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
    val isLiveStreamActive: Boolean = false,
    val liveStreamTitle: String = "श्री गणेश महाआरती थेट प्रक्षेपण",
    val liveStreamUrl: String = "https://www.youtube.com/@JayHindMandalArjunwad/live",
    val liveStreamStartedAt: Long = 0L,
    val liveViewerCount: Int = 148,
    val updatedAt: Long = System.currentTimeMillis()
)
