package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        ChatMessageEntity::class,
        AlbumEntity::class,
        PhotoEntity::class,
        VideoEntity::class,
        EventEntity::class,
        AnnouncementEntity::class,
        NotificationEntity::class,
        BannerEntity::class,
        MandalInfoEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun chatDao(): ChatDao
    abstract fun galleryDao(): GalleryDao
    abstract fun eventDao(): EventDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun notificationDao(): NotificationDao
    abstract fun bannerDao(): BannerDao
    abstract fun mandalInfoDao(): MandalInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jayhind_mandal_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }

        fun checkpoint(context: Context) {
            try {
                getDatabase(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Checkpoint error: ${e.message}")
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Close DB error: ${e.message}")
                }
                INSTANCE = null
            }
        }
    }
}
