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

    val seedPosts = listOf(
        PostEntity(
            id = "post_1",
            authorId = "admin_1",
            authorName = "वैभव चौगुले",
            authorPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            authorRole = "ADMIN",
            content = "🚩 जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड च्या अधिकृत ॲपमध्ये सर्व ग्रामस्थ व सभासदांचे सहर्ष स्वागत! मंडळाच्या सर्व उपक्रम, कार्यक्रम व सूचना या ॲपद्वारे मिळतील. जय हिंद! 🚩",
            imageUrlsJson = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80",
            likedUserIdsJson = "admin_1",
            commentsCount = 0,
            timestamp = System.currentTimeMillis()
        )
    )

    val seedComments = emptyList<CommentEntity>()

    val seedAlbums = listOf(
        AlbumEntity(
            id = "album_1",
            title = "गणपती उत्सव २०२६",
            category = "उत्सव",
            coverImageUrl = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80",
            description = "जय हिंद मंडळाचा १० दिवसांचा भव्य गणेशोत्सव व देखावा",
            photoCount = 8
        ),
        AlbumEntity(
            id = "album_2",
            title = "दहीहंडी सोहळा",
            category = "उत्सव",
            coverImageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?w=600&auto=format&fit=crop&q=80",
            description = "गोविंदा पथकांचा थरारक दहीहंडी उत्सव व बक्षीस वितरण",
            photoCount = 6
        ),
        AlbumEntity(
            id = "album_3",
            title = "क्रीडा महोत्सव व क्रिकेट स्पर्धा",
            category = "क्रीडा",
            coverImageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=600&auto=format&fit=crop&q=80",
            description = "अर्जुनवाड प्रीमियर लीग व कबड्डी सामने",
            photoCount = 10
        ),
        AlbumEntity(
            id = "album_4",
            title = "शिवजयंती व सांस्कृतिक सोहळा",
            category = "सांस्कृतिक",
            coverImageUrl = "https://images.unsplash.com/photo-1608889175123-8ee362201f81?w=600&auto=format&fit=crop&q=80",
            description = "भव्य मिरवणूक, पोवाडे व व्याख्यानमाला",
            photoCount = 5
        )
    )

    val seedPhotos = listOf(
        PhotoEntity(
            id = "ph_1",
            albumId = "album_1",
            imageUrl = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80",
            caption = "गणेश मूर्ती आगमन सोहळा अर्जुनवाड"
        ),
        PhotoEntity(
            id = "ph_2",
            albumId = "album_1",
            imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&auto=format&fit=crop&q=80",
            caption = "मंडपातील रोषणाई व देखावा"
        ),
        PhotoEntity(
            id = "ph_3",
            albumId = "album_2",
            imageUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?w=800&auto=format&fit=crop&q=80",
            caption = "जय हिंद गोविंदा पथक - ५ थर यशस्वी"
        ),
        PhotoEntity(
            id = "ph_4",
            albumId = "album_3",
            imageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&auto=format&fit=crop&q=80",
            caption = "फायनल सामन्यातील विजयी क्षण"
        ),
        PhotoEntity(
            id = "ph_5",
            albumId = "album_4",
            imageUrl = "https://images.unsplash.com/photo-1608889175123-8ee362201f81?w=800&auto=format&fit=crop&q=80",
            caption = "शिवजयंती पालखी मिरवणूक"
        )
    )

    val seedVideos = listOf(
        VideoEntity(
            id = "vid_1",
            title = "जय हिंद मंडळ गणेशोत्सव महाआरती",
            description = "गावातील ज्येष्ठ नागरिक व सभासदांच्या उपस्थितीत संपन्न झालेली महाआरती.",
            category = "उत्सव",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=600&auto=format&fit=crop&q=80",
            duration = "04:15"
        ),
        VideoEntity(
            id = "vid_2",
            title = "अर्जुनवाड प्रीमियर लीग क्रिकेट फायनल हायलाइट्स",
            description = "शेवटच्या चेंडूवर षटकार मारून जय हिंद संघाने जिंकलेला थरारक सामना!",
            category = "क्रीडा",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=600&auto=format&fit=crop&q=80",
            duration = "08:30"
        ),
        VideoEntity(
            id = "vid_3",
            title = "दहीहंडी थरारक सलामी व फोडलेली हंडी",
            description = "अर्जुनवाड चौकातील ५ थरांची नेत्रदीपक सलामी.",
            category = "उत्सव",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1566737236500-c8ac43014a67?w=600&auto=format&fit=crop&q=80",
            duration = "03:40"
        )
    )

    val seedEvents = listOf(
        EventEntity(
            id = "event_1",
            title = "भव्य रक्तदान व आरोग्य तपासणी शिबिर",
            date = "रविवार, ३० ऑगस्ट २०२६",
            time = "सकाळी ९:०० ते दु. २:००",
            location = "जय हिंद मंडळ कार्यालय, अर्जुनवाड",
            description = "मंडळाच्या वर्धापन दिनानिमित्त मोफत आरोग्य तपासणी व भव्य रक्तदान शिबिराचे आयोजन केले आहे. सर्व ग्रामस्थांनी लाभ घ्यावा.",
            imageUrl = "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=800&auto=format&fit=crop&q=80",
            category = "आरोग्य / सामाजिक",
            attendeesCount = 74,
            isRegistered = true
        ),
        EventEntity(
            id = "event_2",
            title = "श्री गणेश मूर्ती प्राणप्रतिष्ठा व महाप्रसाद",
            date = "सोमवार, ७ सप्टेंबर २०२६",
            time = "सकाळी ११:३० वाजता",
            location = "मंडळ गणेश मंडप, शिवाजी चौक, अर्जुनवाड",
            description = "पारंपारिक वाद्यांच्या गजरात बाप्पांचे आगमन व विधिवत पूजा. दुपारी १ वाजता महाप्रसादाचे वाटप.",
            imageUrl = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80",
            category = "धार्मिक / उत्सव",
            attendeesCount = 142,
            isRegistered = false
        ),
        EventEntity(
            id = "event_3",
            title = "गुणवंत विद्यार्थी सत्कार व वार्षिक सभा",
            date = "रविवार, २० सप्टेंबर २०२६",
            time = "सायंकाळी ६:०० वाजता",
            location = "प्राथमिक शाळा प्रांगण, अर्जुनवाड",
            description = "१०वी व १२वी मध्ये विशेष प्रावीण्य मिळवलेल्या अर्जुनवाड गावातील विद्यार्थ्यांचा गौरव समारंभ.",
            imageUrl = "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800&auto=format&fit=crop&q=80",
            category = "शैक्षणिक",
            attendeesCount = 89,
            isRegistered = false
        )
    )

    val seedAnnouncements = listOf(
        AnnouncementEntity(
            id = "ann_1",
            title = "जय हिंद मंडळ अधिकृत ॲप सुरू!",
            content = "मंडळाचे सर्व सभासद व ग्रामस्थांसाठी हे अधिकृत डिजिटल ॲप सुरू करण्यात आले आहे. सर्व सभासदांनी नवीन नोंदणी करावी.",
            priority = "HIGH",
            date = "२५ ऑगस्ट २०२६",
            author = "वैभव चौगुले"
        )
    )

    val seedNotifications = emptyList<NotificationEntity>()

    val seedChatMessages = listOf(
        ChatMessageEntity(
            id = "msg_group_1",
            conversationId = "conv_mandal_group",
            senderId = "user_2",
            receiverId = "GROUP_MANDAL",
            senderName = "संदीप पाटील",
            senderPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
            messageText = "जय हिंद सर्व सदस्यांना! उद्या संध्याकाळी ७ वाजता गणेशोत्सवाची महत्त्वाची बैठक आहे.",
            timestamp = System.currentTimeMillis() - 7200000,
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_group_2",
            conversationId = "conv_mandal_group",
            senderId = "user_3",
            receiverId = "GROUP_MANDAL",
            senderName = "अमोल पाटील",
            senderPhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
            messageText = "बैठकीचे नियोजन व विषय पत्रक सर्वांसाठी पाठवत आहे, कृपया तपासावे.",
            attachmentType = "DOCUMENT",
            attachmentUrl = "https://jayhindmandal.org/docs/ganeshotsav_2026.pdf",
            attachmentName = "गणेशोत्सव_नियोजन_२०२६.pdf",
            attachmentExtra = "PDF Document • 1.4 MB",
            timestamp = System.currentTimeMillis() - 3600000,
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_group_3",
            conversationId = "conv_mandal_group",
            senderId = "user_1",
            receiverId = "GROUP_MANDAL",
            senderName = "वैभव चौगुले",
            senderPhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
            messageText = "सर्व सदस्यांनी वेळेवर उपस्थित राहावे ही विनंती.",
            attachmentType = "VOICE",
            attachmentUrl = "",
            attachmentName = "व्हॉईस संदेश",
            attachmentExtra = "0:28",
            timestamp = System.currentTimeMillis() - 1800000,
            isRead = true
        )
    )

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
        logoUrl = ""
    )
}
