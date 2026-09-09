package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PhotoEntity
import com.example.data.local.PostEntity
import com.example.data.local.BannerEntity
import com.example.data.local.EventEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveSyncProgress(
    val isSyncing: Boolean = false,
    val currentStep: String = "",
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncFormatted: String = "",
    val lastSyncSummary: String = "",
    val error: String? = null
)

object GoogleDriveMediaBackupManager {

    private const val TAG = "GoogleDriveBackup"
    private const val PREFS_NAME = "google_drive_backup_prefs"
    private const val KEY_FOLDER_URI = "google_drive_folder_tree_uri"
    private const val KEY_FOLDER_NAME = "google_drive_folder_name"

    private const val ROOT_FOLDER_NAME = "JAY HIND MANDAL APP"
    private const val PHOTOS_FOLDER_NAME = "1. PHOTOS"
    private const val VOICE_FOLDER_NAME = "2. VOICE MESSAGE"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _syncProgress = MutableStateFlow(DriveSyncProgress())
    val syncProgress: StateFlow<DriveSyncProgress> = _syncProgress.asStateFlow()

    private val _selectedFolderName = MutableStateFlow<String?>(null)
    val selectedFolderName: StateFlow<String?> = _selectedFolderName.asStateFlow()

    private val _isFolderConfigured = MutableStateFlow(false)
    val isFolderConfigured: StateFlow<Boolean> = _isFolderConfigured.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUriStr = prefs.getString(KEY_FOLDER_URI, null)
        val savedName = prefs.getString(KEY_FOLDER_NAME, null)
        val lastTime = prefs.getLong("last_sync_timestamp", 0L)
        val lastFormatted = prefs.getString("last_sync_formatted", "") ?: ""
        val lastSummary = prefs.getString("last_sync_summary", "") ?: ""

        val hasValidUri = if (!savedUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(savedUriStr)
                val doc = DocumentFile.fromTreeUri(context, uri)
                doc != null && doc.canWrite()
            } catch (e: Exception) {
                false
            }
        } else false

        _isFolderConfigured.value = hasValidUri
        _selectedFolderName.value = if (hasValidUri) (savedName ?: "JAY HIND MANDAL APP (Google Drive)") else null

        _syncProgress.value = DriveSyncProgress(
            lastSyncTimestamp = lastTime,
            lastSyncFormatted = lastFormatted,
            lastSyncSummary = lastSummary
        )
    }

    /**
     * Persists permanent URI read/write permissions granted by user via Storage Access Framework
     */
    fun saveSelectedFolderUri(context: Context, treeUri: Uri): Boolean {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val name = doc?.name ?: "JAY HIND MANDAL APP"

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FOLDER_URI, treeUri.toString())
                .putString(KEY_FOLDER_NAME, name)
                .apply()

            _isFolderConfigured.value = true
            _selectedFolderName.value = name
            Log.d(TAG, "Storage Access Framework folder saved successfully: $name ($treeUri)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist folder permission: ${e.message}", e)
            return false
        }
    }

    private fun getSavedRootFolder(context: Context): DocumentFile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val doc = DocumentFile.fromTreeUri(context, uri)
            if (doc != null && doc.canWrite()) doc else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds an existing subfolder inside a DocumentFile or creates it if it doesn't exist.
     */
    private fun findOrCreateSubFolder(parent: DocumentFile, subFolderName: String): DocumentFile? {
        val existing = parent.findFile(subFolderName)
        if (existing != null && existing.isDirectory) {
            return existing
        }
        return parent.createDirectory(subFolderName)
    }

    /**
     * Resolves raw bytes from any local URI, HTTP/Firebase URL, or Base64 string.
     */
    private suspend fun resolveMediaBytes(context: Context, source: String): ByteArray? = withContext(Dispatchers.IO) {
        if (source.isBlank()) return@withContext null
        try {
            when {
                source.startsWith("data:") -> {
                    val base64 = source.substringAfter("base64,")
                    Base64.decode(base64.trim(), Base64.DEFAULT)
                }
                source.startsWith("http://") || source.startsWith("https://") -> {
                    val req = Request.Builder().url(source).get().build()
                    httpClient.newCall(req).execute().use { it.body?.bytes() }
                }
                source.startsWith("content://") || source.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
                }
                source.length > 200 && !source.contains("/") -> {
                    Base64.decode(source.trim(), Base64.DEFAULT)
                }
                else -> {
                    val file = File(source)
                    if (file.exists()) file.readBytes() else null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving bytes from source: ${e.message}")
            null
        }
    }

    /**
     * Sanitizes strings for filenames, keeping Marathi letters, English letters, digits, and underscores.
     */
    private fun sanitizeName(raw: String?): String {
        if (raw.isNullOrBlank()) return "Member"
        return raw.replace(Regex("[^\\p{L}\\p{Nd}_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(25)
            .ifEmpty { "Member" }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("dd-MM-yyyy_hh-mma", Locale.ENGLISH).format(Date(timestamp))
    }

    /**
     * Writes bytes directly into DocumentFile safely using Android ContentResolver.
     */
    private suspend fun writeMediaToDocumentFile(
        context: Context,
        folder: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if file already exists
            val existing = folder.findFile(fileName)
            if (existing != null && existing.isFile && existing.length() > 0) {
                return@withContext true // Already uploaded safely
            }

            val targetFile = existing ?: folder.createFile(mimeType, fileName)
                ?: return@withContext false

            context.contentResolver.openOutputStream(targetFile.uri)?.use { os ->
                os.write(bytes)
                os.flush()
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to document file $fileName: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Main Sync Function using 100% Free, Native Android SAF (Storage Access Framework).
     * Creates exact requested hierarchy on Google Drive:
     * JAY HIND MANDAL APP
     *   └── YEAR2026
     *       └── SEP
     *           ├── 1. PHOTOS
     *           │   ├── Gallary
     *           │   ├── Banner
     *           │   ├── Event
     *           │   ├── Post
     *           │   ├── Group Chat
     *           │   └── One to One Chat
     *           └── 2. VOICE MESSAGE
     *               ├── Group Chat
     *               └── One to One Chat
     */
    suspend fun performCompleteMediaBackup(
        context: Context,
        isAutoNightly: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (_syncProgress.value.isSyncing) {
            return@withContext Result.failure(Exception("सिंक आधीपासूनच प्रगतीपथावर आहे."))
        }

        val rootFolder = getSavedRootFolder(context)
        if (rootFolder == null) {
            _isFolderConfigured.value = false
            return@withContext Result.failure(
                Exception("कृपया आधी Google Drive मधील 'JAY HIND MANDAL APP' फोल्डर निवडा ('फोल्डर जोडा' बटण दाबा).")
            )
        }

        _syncProgress.value = _syncProgress.value.copy(
            isSyncing = true,
            currentStep = "गुगल ड्राइव्ह फोल्डर तयार करत आहे...",
            error = null
        )

        try {
            val db = AppDatabase.getDatabase(context)
            val userMap = db.userDao().getAllUsersDirect().associateBy { it.id }

            val now = System.currentTimeMillis()
            val yearFolderName = "YEAR" + SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date(now))
            val monthFolderName = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(now)).uppercase(Locale.ENGLISH)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val uploadedSet = prefs.getStringSet("uploaded_item_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            // 1. Build Folders on Google Drive
            val yearFolder = findOrCreateSubFolder(rootFolder, yearFolderName) ?: rootFolder
            val monthFolder = findOrCreateSubFolder(yearFolder, monthFolderName) ?: yearFolder

            val photosRootFolder = findOrCreateSubFolder(monthFolder, PHOTOS_FOLDER_NAME) ?: monthFolder
            val voiceRootFolder = findOrCreateSubFolder(monthFolder, VOICE_FOLDER_NAME) ?: monthFolder

            // Subfolders in PHOTOS
            val galleryFolder = findOrCreateSubFolder(photosRootFolder, "Gallary") ?: photosRootFolder
            val bannerFolder = findOrCreateSubFolder(photosRootFolder, "Banner") ?: photosRootFolder
            val eventFolder = findOrCreateSubFolder(photosRootFolder, "Event") ?: photosRootFolder
            val postFolder = findOrCreateSubFolder(photosRootFolder, "Post") ?: photosRootFolder
            val groupChatPhotoFolder = findOrCreateSubFolder(photosRootFolder, "Group Chat") ?: photosRootFolder
            val oneToOneChatPhotoFolder = findOrCreateSubFolder(photosRootFolder, "One to One Chat") ?: photosRootFolder

            // Subfolders in VOICE MESSAGE
            val groupChatVoiceFolder = findOrCreateSubFolder(voiceRootFolder, "Group Chat") ?: voiceRootFolder
            val oneToOneChatVoiceFolder = findOrCreateSubFolder(voiceRootFolder, "One to One Chat") ?: voiceRootFolder

            // Fetch Items from Database
            val allPhotos = db.galleryDao().getAllPhotosDirect()
            val allPosts = db.postDao().getAllPostsDirect().filter { !it.imageUrlsJson.isNullOrBlank() || !it.authorPhotoUrl.isNullOrBlank() }
            val allBanners = db.bannerDao().getAllBannersDirect().filter { it.imageUrl.isNotBlank() }
            val allEvents = db.eventDao().getAllEventsDirect().filter { !it.imageUrl.isNullOrBlank() }
            val allChats = db.chatDao().getAllChatMessagesDirect().filter { 
                (!it.imageUrl.isNullOrBlank()) || (!it.attachmentUrl.isNullOrBlank()) 
            }

            var totalCount = allPhotos.size + allPosts.size + allBanners.size + allEvents.size + allChats.size
            if (totalCount == 0) totalCount = 1

            _syncProgress.value = _syncProgress.value.copy(
                totalItems = totalCount,
                completedItems = 0,
                currentStep = "अपलोड सुरू करत आहे..."
            )

            var uploadedCount = 0
            var skippedCount = 0
            var failCount = 0

            // 1. Upload Gallery Photos
            for (photo in allPhotos) {
                val key = "gallery_${photo.id}"
                if (uploadedSet.contains(key)) {
                    skippedCount++
                    continue
                }
                _syncProgress.value = _syncProgress.value.copy(
                    currentStep = "गॅलरी फोटो: ${photo.caption.take(15)}",
                    completedItems = uploadedCount + skippedCount
                )
                val bytes = resolveMediaBytes(context, photo.imageUrl)
                if (bytes != null && bytes.isNotEmpty()) {
                    val dateStr = formatTimestamp(photo.uploadedAt)
                    val captionClean = sanitizeName(photo.caption)
                    val fileName = "Gallary_${captionClean}_${dateStr}.jpg"
                    val success = writeMediaToDocumentFile(context, galleryFolder, fileName, "image/jpeg", bytes)
                    if (success) {
                        uploadedSet.add(key)
                        uploadedCount++
                    } else failCount++
                } else failCount++
            }

            // 2. Upload Banners
            for (banner in allBanners) {
                val key = "banner_${banner.id}"
                if (uploadedSet.contains(key)) {
                    skippedCount++
                    continue
                }
                _syncProgress.value = _syncProgress.value.copy(
                    currentStep = "बॅनर फोटो: ${banner.title.take(15)}",
                    completedItems = uploadedCount + skippedCount
                )
                val bytes = resolveMediaBytes(context, banner.imageUrl)
                if (bytes != null && bytes.isNotEmpty()) {
                    val dateStr = formatTimestamp(banner.createdAt)
                    val titleClean = sanitizeName(banner.title)
                    val fileName = "Banner_${titleClean}_${dateStr}.jpg"
                    val success = writeMediaToDocumentFile(context, bannerFolder, fileName, "image/jpeg", bytes)
                    if (success) {
                        uploadedSet.add(key)
                        uploadedCount++
                    } else failCount++
                } else failCount++
            }

            // 3. Upload Events
            for (event in allEvents) {
                val key = "event_${event.id}"
                if (uploadedSet.contains(key)) {
                    skippedCount++
                    continue
                }
                _syncProgress.value = _syncProgress.value.copy(
                    currentStep = "कार्यक्रम: ${event.title.take(15)}",
                    completedItems = uploadedCount + skippedCount
                )
                val img = event.imageUrl ?: ""
                val bytes = resolveMediaBytes(context, img)
                if (bytes != null && bytes.isNotEmpty()) {
                    val dateStr = formatTimestamp(event.createdAt)
                    val titleClean = sanitizeName(event.title)
                    val fileName = "Event_${titleClean}_${dateStr}.jpg"
                    val success = writeMediaToDocumentFile(context, eventFolder, fileName, "image/jpeg", bytes)
                    if (success) {
                        uploadedSet.add(key)
                        uploadedCount++
                    } else failCount++
                } else failCount++
            }

            // 4. Upload Posts
            for (post in allPosts) {
                val key = "post_${post.id}"
                if (uploadedSet.contains(key)) {
                    skippedCount++
                    continue
                }
                _syncProgress.value = _syncProgress.value.copy(
                    currentStep = "पोस्ट: ${post.authorName}",
                    completedItems = uploadedCount + skippedCount
                )
                val user = userMap[post.authorId]
                val authorName = sanitizeName(post.authorName)
                val mobile = user?.mobileNumber ?: "9800000000"
                val dateStr = formatTimestamp(post.timestamp)

                val imgSource = post.imageUrlsJson?.let {
                    try { JSONArray(it).optString(0) } catch (e: Exception) { null }
                } ?: post.authorPhotoUrl ?: ""

                val bytes = resolveMediaBytes(context, imgSource)
                if (bytes != null && bytes.isNotEmpty()) {
                    val fileName = "Post_${authorName}_${mobile}_${dateStr}.jpg"
                    val success = writeMediaToDocumentFile(context, postFolder, fileName, "image/jpeg", bytes)
                    if (success) {
                        uploadedSet.add(key)
                        uploadedCount++
                    } else failCount++
                } else failCount++
            }

            // 5. Upload Chat Photos & Voice Notes
            for (chat in allChats) {
                val user = userMap[chat.senderId]
                val senderName = sanitizeName(chat.senderName.ifBlank { user?.fullName ?: "Member" })
                val mobile = user?.mobileNumber ?: "9800000000"
                val dateStr = formatTimestamp(chat.timestamp)
                val isGroup = chat.conversationId.contains("group", ignoreCase = true) || chat.receiverId.contains("group", ignoreCase = true)

                // Photo
                val chatPhotoSrc = if (!chat.imageUrl.isNullOrBlank()) chat.imageUrl else if (chat.attachmentType.equals("IMAGE", ignoreCase = true)) chat.attachmentUrl else null
                if (!chatPhotoSrc.isNullOrBlank()) {
                    val pKey = "chat_photo_${chat.id}"
                    if (!uploadedSet.contains(pKey)) {
                        _syncProgress.value = _syncProgress.value.copy(
                            currentStep = "चॅट फोटो: $senderName",
                            completedItems = uploadedCount + skippedCount
                        )
                        val bytes = resolveMediaBytes(context, chatPhotoSrc)
                        if (bytes != null && bytes.isNotEmpty()) {
                            val targetFolder = if (isGroup) groupChatPhotoFolder else oneToOneChatPhotoFolder
                            val prefix = if (isGroup) "GroupChat" else "OneToOneChat"
                            val fileName = "${prefix}_${senderName}_${mobile}_${dateStr}.jpg"
                            val success = writeMediaToDocumentFile(context, targetFolder, fileName, "image/jpeg", bytes)
                            if (success) {
                                uploadedSet.add(pKey)
                                uploadedCount++
                            } else failCount++
                        }
                    } else skippedCount++
                }

                // Voice
                val isVoice = chat.attachmentType.equals("VOICE", ignoreCase = true) ||
                        chat.attachmentType.equals("AUDIO", ignoreCase = true) ||
                        chat.attachmentUrl?.endsWith(".m4a", ignoreCase = true) == true ||
                        chat.attachmentUrl?.endsWith(".mp3", ignoreCase = true) == true ||
                        chat.attachmentName?.contains("voice", ignoreCase = true) == true

                val chatVoiceSrc = if (isVoice) chat.attachmentUrl else null
                if (!chatVoiceSrc.isNullOrBlank()) {
                    val aKey = "chat_voice_${chat.id}"
                    if (!uploadedSet.contains(aKey)) {
                        _syncProgress.value = _syncProgress.value.copy(
                            currentStep = "व्हॉइस मेसेज: $senderName",
                            completedItems = uploadedCount + skippedCount
                        )
                        val bytes = resolveMediaBytes(context, chatVoiceSrc)
                        if (bytes != null && bytes.isNotEmpty()) {
                            val targetFolder = if (isGroup) groupChatVoiceFolder else oneToOneChatVoiceFolder
                            val prefix = if (isGroup) "GroupChat" else "OneToOneChat"
                            val fileName = "${prefix}_${senderName}_${mobile}_${dateStr}.m4a"
                            val success = writeMediaToDocumentFile(context, targetFolder, fileName, "audio/mp4", bytes)
                            if (success) {
                                uploadedSet.add(aKey)
                                uploadedCount++
                            } else failCount++
                        }
                    } else skippedCount++
                }
            }

            // Save state
            prefs.edit()
                .putStringSet("uploaded_item_ids", uploadedSet)
                .putLong("last_sync_timestamp", now)
                .putString("last_sync_formatted", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now)))
                .putString("last_sync_summary", "$uploadedCount फाइल्स Google Drive वर सुरक्षित सेव्ह!")
                .apply()

            val summaryMsg = "$uploadedCount नवीन मीडिया फाइल्स ($yearFolderName / $monthFolderName) Google Drive वर यशस्वीरीत्या सेव्ह झाल्या! ($skippedCount आधीच सुरक्षित)"

            _syncProgress.value = DriveSyncProgress(
                isSyncing = false,
                currentStep = "पूर्ण झाले!",
                totalItems = totalCount,
                completedItems = totalCount,
                lastSyncTimestamp = now,
                lastSyncFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now)),
                lastSyncSummary = summaryMsg
            )

            // Firebase Firestore meta
            try {
                FirebaseFirestore.getInstance().collection("system_settings").document("media_backup_meta")
                    .set(
                        mapOf(
                            "lastDriveSyncTimestamp" to now,
                            "lastDriveSyncDateStr" to SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now)),
                            "uploadedFilesCount" to uploadedCount,
                            "organization" to "जय हिंद तरुण मंडळ, अर्जुनवाड"
                        ),
                        SetOptions.merge()
                    )
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore meta sync note: ${fe.message}")
            }

            return@withContext Result.success(summaryMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during SAF media sync: ${e.message}", e)
            _syncProgress.value = _syncProgress.value.copy(
                isSyncing = false,
                error = e.message
            )
            return@withContext Result.failure(e)
        }
    }
}
