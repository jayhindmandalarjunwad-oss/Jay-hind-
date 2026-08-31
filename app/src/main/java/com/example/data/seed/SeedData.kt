package com.example.data.seed

import com.example.data.local.AlbumEntity
import com.example.data.local.AnnouncementEntity
import com.example.data.local.BannerEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommentEntity
import com.example.data.local.EventEntity
import com.example.data.local.MandalInfoEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PhotoEntity
import com.example.data.local.PostEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeedData {
    val todayDobStr: String by lazy {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // Set today's month-day with birth year 1996
        val monthDay = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date())
        "1996-$monthDay"
    }

    val defaultAdmin = UserEntity(
        id = "admin_1",
        fullName = "वैभव चौगुले",
        mobileNumber = "9545791089",
        password = "ADMIN",
        profilePhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
        gender = "पुरुष",
        bloodGroup = "O+",
        dateOfBirth = "1990-01-01",
        address = "गावभाग, अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर",
        role = "ADMIN",
        designation = "",
        status = "APPROVED",
        isOnline = true
    )

    val seedUsers = listOf(
        defaultAdmin
    )

    val seedPosts = emptyList<PostEntity>()
    val seedComments = emptyList<CommentEntity>()
    val seedAlbums = emptyList<AlbumEntity>()
    val seedPhotos = emptyList<PhotoEntity>()
    val seedVideos = emptyList<VideoEntity>()
    val seedEvents = emptyList<EventEntity>()
    val seedAnnouncements = emptyList<AnnouncementEntity>()
    val seedNotifications = emptyList<NotificationEntity>()
    val seedChatMessages = emptyList<ChatMessageEntity>()
    val seedBanners = emptyList<BannerEntity>()

    val defaultMandalInfo = MandalInfoEntity(
        id = "mandal_default",
        mandalName = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
        tagline = "अर्जुनवाड • सभासद कुटुंब व डिजिटल सेवा मंच",
        locationTitle = "🚩 अर्जुनवाड, ता. शिरोळ",
        aboutDescription = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड हे गावातील सामाजिक, सांस्कृतिक, क्रीडा, आरोग्य व शैक्षणिक प्रगतीसाठी अविरत कार्यरत असणारे अग्रगण्य मंडळ आहे. सर्व सभासदांना एकत्र आणून समाजोपयोगी उपक्रम राबवणे हे आमचे मुख्य ध्येय आहे.",
        email = "jayhindmandalarjunwad@gmail.com",
        address = "मु. पो. अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर - ४१६१०१",
        phone = "+91 98765 43210",
        youtubeHandle = "@JayHindMandalArjunwad",
        facebookHandle = "JayHindMandalArjunwad",
        instagramHandle = "jayhind_mandal_arjunwad",
        adminWebLink = "",
        logoUrl = "",
        isLiveStreamActive = false,
        liveStreamTitle = "श्री गणेश महाआरती थेट प्रक्षेपण",
        liveStreamUrl = "https://www.youtube.com/@JayHindMandalArjunwad/live",
        liveStreamStartedAt = 0L,
        liveViewerCount = 148
    )
}
