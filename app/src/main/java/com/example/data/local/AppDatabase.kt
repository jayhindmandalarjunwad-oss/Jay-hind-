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
        MandalInfoEntity::class,
        BusinessListingEntity::class,
        BusinessLeadClickEntity::class
    ],
    version = 15,
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
    abstract fun businessDirectoryDao(): BusinessDirectoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE business_directory ADD COLUMN globalOrder INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note globalOrder: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE business_directory ADD COLUMN categoryOrder INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note categoryOrder: ${e.message}")
                }
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE business_directory ADD COLUMN photosJson TEXT NOT NULL DEFAULT '[]'")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note photosJson: ${e.message}")
                }
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE business_directory ADD COLUMN callClicks INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note callClicks: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE business_directory ADD COLUMN whatsappClicks INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note whatsappClicks: ${e.message}")
                }
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS business_lead_clicks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            businessId TEXT NOT NULL,
                            businessName TEXT NOT NULL,
                            ownerName TEXT NOT NULL DEFAULT '',
                            category TEXT NOT NULL DEFAULT '',
                            contactNumber TEXT NOT NULL DEFAULT '',
                            clickType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            monthYear TEXT NOT NULL DEFAULT ''
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration note business_lead_clicks: ${e.message}")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jayhind_mandal_db"
                )
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
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
