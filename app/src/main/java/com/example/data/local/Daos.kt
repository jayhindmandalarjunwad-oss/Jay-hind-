package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE status = 'APPROVED' ORDER BY fullName ASC")
    fun getApprovedUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE status = 'PENDING_APPROVAL' ORDER BY createdAt DESC")
    fun getPendingUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("UPDATE posts SET authorId = :newAuthorId, authorName = :newAuthorName, authorPhotoUrl = :newAuthorPhotoUrl WHERE authorId = :oldAuthorId")
    suspend fun reassignPostsAuthor(oldAuthorId: String, newAuthorId: String, newAuthorName: String, newAuthorPhotoUrl: String)

    @Query("UPDATE posts SET likedUserIdsJson = :likesJson WHERE id = :postId")
    suspend fun updatePostLikes(postId: String, likesJson: String)

    @Query("UPDATE posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentsCount(postId: String)

    @Query("UPDATE posts SET content = :content, imageUrlsJson = :imageUrls, videoUrl = :videoUrl WHERE id = :postId")
    suspend fun updatePostContent(postId: String, content: String, imageUrls: String, videoUrl: String?)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId OR (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(convId: String, userA: String, userB: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE senderId = :userId OR receiverId = :userId OR conversationId = 'conv_mandal_group' OR receiverId = 'GROUP_MANDAL' ORDER BY timestamp DESC")
    fun getAllMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE (conversationId = :convId OR (senderId = :partnerId AND receiverId = :userId)) AND receiverId = :userId")
    suspend fun markMessagesAsReadBetween(convId: String, userId: String, partnerId: String)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE conversationId = :convId AND receiverId = :userId")
    suspend fun markMessagesAsRead(convId: String, userId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadChatCount(userId: String): Flow<Int>
}

@Dao
interface GalleryDao {
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)

    @Query("SELECT * FROM photos WHERE albumId = :albumId ORDER BY uploadedAt DESC")
    fun getPhotosForAlbum(albumId: String): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: String)

    @Query("SELECT * FROM videos ORDER BY uploadedAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("UPDATE events SET isRegistered = :registered, attendeesCount = CASE WHEN :registered THEN attendeesCount + 1 ELSE attendeesCount - 1 END WHERE id = :eventId")
    suspend fun toggleEventRegistration(eventId: String, registered: Boolean)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications WHERE targetId = :targetId")
    suspend fun deleteNotificationsByTargetId(targetId: String)
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners ORDER BY orderIndex ASC, createdAt DESC")
    fun getAllBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<BannerEntity>)

    @Update
    suspend fun updateBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: String)
}

@Dao
interface MandalInfoDao {
    @Query("SELECT * FROM mandal_info WHERE id = 'mandal_default' LIMIT 1")
    fun getMandalInfo(): Flow<MandalInfoEntity?>

    @Query("SELECT * FROM mandal_info WHERE id = 'mandal_default' LIMIT 1")
    suspend fun getMandalInfoDirect(): MandalInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMandalInfo(info: MandalInfoEntity)
}
