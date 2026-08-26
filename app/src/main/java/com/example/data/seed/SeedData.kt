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
        fullName = "सचिन आनंदराव पाटील",
        mobileNumber = "9545791089",
        password = "ADMIN",
        profilePhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
        gender = "पुरुष",
        bloodGroup = "O+",
        dateOfBirth = "1988-04-15",
        address = "गावभाग, अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर",
        role = "ADMIN",
        designation = "",
        status = "APPROVED",
        isOnline = true
    )

    val seedUsers = listOf(
        defaultAdmin,
        UserEntity(
            id = "user_2",
            fullName = "अमोल दिनकर शिंदे",
            mobileNumber = "9876543211",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "B+",
            dateOfBirth = "1994-08-23", // Actual birthday only
            address = "शिवाजी चौक, अर्जुनवाड, ता. शिरोळ",
            role = "ADMIN",
            designation = "",
            status = "APPROVED",
            isOnline = true
        ),
        UserEntity(
            id = "user_3",
            fullName = "रोहित शिवाजी अर्जुनवाडकर",
            mobileNumber = "9876543212",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "AB+",
            dateOfBirth = "1994-11-20",
            address = "महालक्ष्मी मंदिर परिसर, अर्जुनवाड",
            role = "ADMIN",
            designation = "",
            status = "APPROVED",
            isOnline = false
        ),
        UserEntity(
            id = "user_4",
            fullName = "प्रसाद मारुती कांबळे",
            mobileNumber = "9876543213",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "A+",
            dateOfBirth = "1996-05-18",
            address = "आंबेडकर नगर, अर्जुनवाड, ता. शिरोळ",
            role = "MEMBER",
            designation = "",
            status = "APPROVED",
            isOnline = true
        ),
        UserEntity(
            id = "user_5",
            fullName = "ओंकार विलास देसाई",
            mobileNumber = "9876543214",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "O-",
            dateOfBirth = "1997-03-10",
            address = "कुस्ती मैदान जवळ, अर्जुनवाड",
            role = "MEMBER",
            designation = "",
            status = "APPROVED",
            isOnline = true
        ),
        UserEntity(
            id = "user_6",
            fullName = "सुनील संभाजी गायकवाड",
            mobileNumber = "9876543215",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "A-",
            dateOfBirth = "1999-07-28",
            address = "बस स्टँड जवळ, अर्जुनवाड",
            role = "MEMBER",
            designation = "",
            status = "APPROVED",
            isOnline = false
        ),
        UserEntity(
            id = "user_7",
            fullName = "गणेश बापूराव मोरे",
            mobileNumber = "9876543216",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "B-",
            dateOfBirth = "1995-12-05",
            address = "हनुमान मंदिर रोड, अर्जुनवाड",
            role = "MEMBER",
            designation = "",
            status = "PENDING_APPROVAL", // For admin approval demo
            isOnline = false
        ),
        UserEntity(
            id = "user_8",
            fullName = "मयूर प्रकाश चौगुले",
            mobileNumber = "9876543217",
            password = "password",
            profilePhotoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
            gender = "पुरुष",
            bloodGroup = "AB-",
            dateOfBirth = "2000-01-18",
            address = "कृष्णा नदी काठ परिसर, अर्जुनवाड",
            role = "MEMBER",
            designation = "",
            status = "PENDING_APPROVAL", // For admin approval demo
            isOnline = false
        )
    )

    val seedPosts = listOf(
        PostEntity(
            id = "post_1",
            authorId = "admin_1",
            authorName = "सचिन आनंदराव पाटील",
            authorPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            authorRole = "",
            content = "🚩 जय हिंद मंडळ अर्जुनवाड तर्फे यावर्षीचा गणेशोत्सव भव्य स्वरूपात साजरा करण्याचे ठरले आहे. सर्व सभासदांनी देखावा तयारी व मूर्ती स्वागतासाठी एकत्र यावे! जय हिंद! गणपती बाप्पा मोरया! 🚩",
            imageUrlsJson = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=800&auto=format&fit=crop&q=80",
            likedUserIdsJson = "admin_1,user_2,user_3,user_4,user_5",
            commentsCount = 3,
            timestamp = System.currentTimeMillis() - (1000 * 60 * 30) // 30 mins ago
        ),
        PostEntity(
            id = "post_2",
            authorId = "user_4",
            authorName = "प्रसाद कांबळे",
            authorPhotoUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&auto=format&fit=crop&q=80",
            authorRole = "",
            content = "🏆 'अर्जुनवाड प्रीमियर लीग २०२६' क्रिकेट स्पर्धेत जय हिंद संघाने प्रथम क्रमांक पटकावून अजिंक्यपद पटकावले! सर्व खेळाडूंचे व पाठीराख्यांचे मनःपूर्वक अभिनंदन! 🏏🎉",
            imageUrlsJson = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&auto=format&fit=crop&q=80",
            likedUserIdsJson = "admin_1,user_2,user_5,user_6",
            commentsCount = 2,
            timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 4) // 4 hours ago
        ),
        PostEntity(
            id = "post_3",
            authorId = "user_2",
            authorName = "अमोल शिंदे",
            authorPhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            authorRole = "",
            content = "🩸 'रक्तदान हेच जीवनदान!' मंडळातर्फे आयोजित भव्य रक्तदान शिबिरामध्ये अर्जुनवाड गावातील तब्बल १०८ रक्तदात्यांनी सहभाग घेतला. सर्व रक्तदात्यांचे मनःपूर्वक आभार!",
            imageUrlsJson = "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=800&auto=format&fit=crop&q=80",
            likedUserIdsJson = "admin_1,user_3,user_4",
            commentsCount = 1,
            timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24) // 1 day ago
        )
    )

    val seedComments = listOf(
        CommentEntity(
            id = "comm_1",
            postId = "post_1",
            authorId = "user_2",
            authorName = "अमोल शिंदे",
            authorPhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            text = "गणपती बाप्पा मोरया! संपूर्ण मंडळ तयारीसाठी सज्ज आहे! 🚩",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 20)
        ),
        CommentEntity(
            id = "comm_2",
            postId = "post_1",
            authorId = "user_4",
            authorName = "प्रसाद कांबळे",
            authorPhotoUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&auto=format&fit=crop&q=80",
            text = "सर्व तरुण मित्र एकत्र येऊन देखावा साकारणार आहोत!",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 15)
        ),
        CommentEntity(
            id = "comm_3",
            postId = "post_2",
            authorId = "admin_1",
            authorName = "सचिन पाटील",
            authorPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            text = "अभिनंदन जय हिंद संघ! अर्जुनवाड गावाचे नाव उज्ज्वल केले!",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 2)
        )
    )

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
            title = "सर्व सभासदांची तातडीची बैठक!",
            content = "गणेशोत्सव २०२६ च्या नियोजनासाठी उद्या रात्री ८:०० वाजता मंडळ कार्यालयात महत्वाची बैठक आयोजित केली आहे. सर्व सभासदांनी वेळेवर उपस्थित राहावे.",
            priority = "URGENT",
            date = "२२ ऑगस्ट २०२६",
            author = "सचिन पाटील"
        ),
        AnnouncementEntity(
            id = "ann_2",
            title = "गणेशोत्सव वर्गणी व देणगी संकलन सुरू",
            content = "यावर्षीच्या उत्सवासाठी अधिकृत पावती पुस्तकांद्वारे वर्गणी गोळा करण्याचे काम सुरू झाले आहे. सर्व सहकार्य करावे.",
            priority = "HIGH",
            date = "२० ऑगस्ट २०२६",
            author = "रोहित अर्जुनवाडकर"
        ),
        AnnouncementEntity(
            id = "ann_3",
            title = "अर्जुनवाड क्रीडा संकुल सराव वेळ बदल",
            content = "क्रिकेट व कबड्डी सरावाची वेळ दररोज सायंकाळी ५:३० ते ७:३० राहील याची खेळाडूंनी नोंद घ्यावी.",
            priority = "NORMAL",
            date = "१८ ऑगस्ट २०२६",
            author = "प्रसाद कांबळे"
        )
    )

    val seedNotifications = listOf(
        NotificationEntity(
            id = "notif_1",
            title = "आज वाढदिवस आहे! 🎉",
            message = "आज आपले सहकारी सभासद अमोल शिंदे व प्रसाद कांबळे यांचा वाढदिवस आहे. त्यांना शुभेच्छा द्या!",
            type = "BIRTHDAY",
            targetRoute = "BIRTHDAYS",
            targetId = "user_2"
        ),
        NotificationEntity(
            id = "notif_2",
            title = "नवीन सूचना: तातडीची बैठक",
            message = "अध्यक्षांनी उद्या रात्री ८:०० वाजता गणेशोत्सव नियोजनाची बैठक बोलावली आहे.",
            type = "ANNOUNCEMENT",
            targetRoute = "ANNOUNCEMENTS",
            targetId = "ann_1"
        ),
        NotificationEntity(
            id = "notif_3",
            title = "नवीन पोस्ट प्रसिद्ध झाली",
            message = "प्रसाद कांबळे यांनी 'अर्जुनवाड प्रीमियर लीग ट्रॉफी' फोटो पोस्ट केले आहेत.",
            type = "POST",
            targetRoute = "POST_COMMENTS",
            targetId = "post_3"
        ),
        NotificationEntity(
            id = "notif_4",
            title = "नवीन सभासद नोंदणी (Admin Approval)",
            message = "गणेश मोरे व मयूर चौगुले यांनी नोंदणी केली आहे. Admin Panel मधून मंजुरी द्या.",
            type = "ADMIN",
            targetUserId = "ADMIN",
            targetRoute = "ADMIN_PENDING",
            targetId = "user_7"
        )
    )

    val seedChatMessages = listOf(
        ChatMessageEntity(
            id = "msg_1",
            conversationId = "conv_admin_1_user_2",
            senderId = "user_2",
            receiverId = "admin_1",
            senderName = "अमोल शिंदे",
            senderPhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            messageText = "जय हिंद अध्यक्ष महोदय! रक्तदान शिबिराची सर्व तयारी पूर्ण झाली आहे.",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 45),
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_2",
            conversationId = "conv_admin_1_user_2",
            senderId = "admin_1",
            receiverId = "user_2",
            senderName = "सचिन पाटील",
            senderPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            messageText = "खूप छान अमोल! बॅनर आणि मंडप व्यवस्था पूर्ण झाली का?",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 30),
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_3",
            conversationId = "conv_admin_1_user_2",
            senderId = "user_2",
            receiverId = "admin_1",
            senderName = "अमोल शिंदे",
            senderPhotoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            messageText = "होय, सर्व बॅनर लावले आहेत आणि डॉक्टरांची टीम सकाळी ९ वाजता पोहोचेल.",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 10),
            isRead = true
        )
    )

    val seedBanners = listOf(
        BannerEntity(
            id = "banner_1",
            imageUrl = "https://images.unsplash.com/photo-1567157577867-05ccb1388e66?w=1000&auto=format&fit=crop&q=80",
            title = "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ",
            subtitle = "🚩 भव्य गणेशोत्सव २०२६ व सांस्कृतिक महोत्सव",
            orderIndex = 0
        ),
        BannerEntity(
            id = "banner_2",
            imageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=1000&auto=format&fit=crop&q=80",
            title = "अर्जुनवाड प्रीमियर लीग २०२६",
            subtitle = "🏆 भव्य ग्रामीण क्रिकेट स्पर्धा - सर्व खेळाडूंचे स्वागत",
            orderIndex = 1
        ),
        BannerEntity(
            id = "banner_3",
            imageUrl = "https://images.unsplash.com/photo-1615461066841-6116e61058f4?w=1000&auto=format&fit=crop&q=80",
            title = "महा रक्तदान व आरोग्य तपासणी शिबीर",
            subtitle = "🩸 'रक्तदान हेच श्रेष्ठ जीवनदान' - सहकार्य करा",
            orderIndex = 2
        )
    )

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
