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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    private const val DOCS_FOLDER_NAME = "3. DOCUMENTS"

    // Zero-Cost Google Apps Script Web App for automated serverless Google Drive uploads
    private const val DEFAULT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbw_Dji_koCbpSxfTUp1itMtk2_MgcCMcfzCj9NKHc2SokGNfnI5wW9x44YBW_vJ0wAx/exec"
    private const val PREF_KEY_CUSTOM_SCRIPT_URL = "google_apps_script_web_app_url"

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

    private fun isFolderOrDirectory(doc: DocumentFile): Boolean {
        val type = doc.type ?: ""
        return doc.isDirectory || 
               type.equals("vnd.android.document/directory", ignoreCase = true) || 
               type.equals("application/vnd.google-apps.folder", ignoreCase = true) ||
               (!doc.isFile && doc.canWrite())
    }

    /**
     * Finds an existing subfolder inside a DocumentFile or creates it if it doesn't exist.
     * Prevents duplicate folders on Google Drive by:
     * 1. Checking local SharedPreferences cache for the exact folder tree URI.
     * 2. Inspecting parent's existing children using case-insensitive name matching and handling
     *    both standard Android and Google Drive custom MIME types.
     * 3. Reusing existing folder if found, only creating if truly absent.
     */
    private fun findOrCreateSubFolder(
        context: Context,
        parent: DocumentFile,
        subFolderName: String,
        pathKey: String = ""
    ): DocumentFile? {
        val cleanName = subFolderName.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Try resolving from cached URI if available
        if (pathKey.isNotBlank()) {
            val cachedUriStr = prefs.getString("cached_folder_uri_$pathKey", null)
            if (!cachedUriStr.isNullOrBlank()) {
                try {
                    val cachedUri = Uri.parse(cachedUriStr)
                    val cachedDoc = DocumentFile.fromTreeUri(context, cachedUri)
                    if (cachedDoc != null && cachedDoc.exists() && isFolderOrDirectory(cachedDoc) && cachedDoc.canWrite()) {
                        return cachedDoc
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cached folder URI lookup skipped: ${e.message}")
                }
            }
        }

        // 2. Query parent's children to find any existing folder with same name (case-insensitive)
        val children = try {
            parent.listFiles()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list files in parent: ${e.message}")
            emptyArray()
        }

        val matchingFolders = children.filter { child ->
            val name = child.name?.trim() ?: ""
            name.equals(cleanName, ignoreCase = true) && isFolderOrDirectory(child)
        }

        val existingFolder = if (matchingFolders.isNotEmpty()) {
            // If multiple folders exist from earlier syncs, pick the newest/most populated one
            matchingFolders.maxByOrNull { it.lastModified() } ?: matchingFolders.first()
        } else {
            children.firstOrNull { child ->
                val name = child.name?.trim() ?: ""
                name.equals(cleanName, ignoreCase = true)
            }
        }

        if (existingFolder != null) {
            if (pathKey.isNotBlank()) {
                prefs.edit().putString("cached_folder_uri_$pathKey", existingFolder.uri.toString()).apply()
            }
            return existingFolder
        }

        // 3. Truly doesn't exist, create it
        val created = parent.createDirectory(cleanName)
        if (created != null && pathKey.isNotBlank()) {
            prefs.edit().putString("cached_folder_uri_$pathKey", created.uri.toString()).apply()
        }
        return created
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
            // Check if file already exists (prevent duplicate file uploads)
            val existing = try {
                folder.listFiles().firstOrNull { it.name?.trim().equals(fileName.trim(), ignoreCase = true) }
                    ?: folder.findFile(fileName)
            } catch (e: Exception) {
                folder.findFile(fileName)
            }

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

            // 1. Build Folders on Google Drive with persistent caching to ensure a single folder per month/year
            val yearKey = yearFolderName
            val monthKey = "${yearKey}_${monthFolderName}"
            val photosKey = "${monthKey}_PHOTOS"
            val voiceKey = "${monthKey}_VOICE"

            val yearFolder = findOrCreateSubFolder(context, rootFolder, yearFolderName, yearKey) ?: rootFolder
            val monthFolder = findOrCreateSubFolder(context, yearFolder, monthFolderName, monthKey) ?: yearFolder

            val photosRootFolder = findOrCreateSubFolder(context, monthFolder, PHOTOS_FOLDER_NAME, photosKey) ?: monthFolder
            val voiceRootFolder = findOrCreateSubFolder(context, monthFolder, VOICE_FOLDER_NAME, voiceKey) ?: monthFolder

            // Subfolders in PHOTOS
            val galleryFolder = findOrCreateSubFolder(context, photosRootFolder, "Gallary", "${photosKey}_Gallary") ?: photosRootFolder
            val bannerFolder = findOrCreateSubFolder(context, photosRootFolder, "Banner", "${photosKey}_Banner") ?: photosRootFolder
            val eventFolder = findOrCreateSubFolder(context, photosRootFolder, "Event", "${photosKey}_Event") ?: photosRootFolder
            val postFolder = findOrCreateSubFolder(context, photosRootFolder, "Post", "${photosKey}_Post") ?: photosRootFolder
            val groupChatPhotoFolder = findOrCreateSubFolder(context, photosRootFolder, "Group Chat", "${photosKey}_GroupChat") ?: photosRootFolder
            val oneToOneChatPhotoFolder = findOrCreateSubFolder(context, photosRootFolder, "One to One Chat", "${photosKey}_OneToOneChat") ?: photosRootFolder

            // Subfolders in VOICE MESSAGE
            val groupChatVoiceFolder = findOrCreateSubFolder(context, voiceRootFolder, "Group Chat", "${voiceKey}_GroupChat") ?: voiceRootFolder
            val oneToOneChatVoiceFolder = findOrCreateSubFolder(context, voiceRootFolder, "One to One Chat", "${voiceKey}_OneToOneChat") ?: voiceRootFolder

            // Fetch Items from Database
            val allPhotos = db.galleryDao().getAllPhotosDirect()
            val allPosts = db.postDao().getAllPostsDirect().filter { 
                val json = it.imageUrlsJson?.trim() ?: ""
                json.isNotBlank() && json != "[]" && json != "null"
            }
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

            // 4. Upload Posts (Only actual post images, ignore authorPhotoUrl, skip text-only posts)
            for (post in allPosts) {
                val key = "post_${post.id}"
                if (uploadedSet.contains(key)) {
                    skippedCount++
                    continue
                }

                val postImages = mutableListOf<String>()
                post.imageUrlsJson?.let { jsonStr ->
                    try {
                        val arr = JSONArray(jsonStr)
                        for (i in 0 until arr.length()) {
                            val url = arr.optString(i)
                            if (url.isNotBlank()) {
                                postImages.add(url)
                            }
                        }
                    } catch (e: Exception) {
                        if (jsonStr.isNotBlank() && !jsonStr.startsWith("[")) {
                            postImages.add(jsonStr)
                        }
                    }
                }

                if (postImages.isEmpty()) {
                    skippedCount++
                    continue
                }

                _syncProgress.value = _syncProgress.value.copy(
                    currentStep = "पोस्ट फोटो: ${post.authorName}",
                    completedItems = uploadedCount + skippedCount
                )

                val user = userMap[post.authorId]
                val authorName = sanitizeName(post.authorName)
                val mobile = user?.mobileNumber ?: "9800000000"
                val dateStr = formatTimestamp(post.timestamp)

                var postAnySuccess = false
                for ((idx, imgSource) in postImages.withIndex()) {
                    val bytes = resolveMediaBytes(context, imgSource)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val fileName = if (postImages.size > 1) {
                            "Post_${authorName}_${mobile}_${dateStr}_${idx + 1}.jpg"
                        } else {
                            "Post_${authorName}_${mobile}_${dateStr}.jpg"
                        }
                        val success = writeMediaToDocumentFile(context, postFolder, fileName, "image/jpeg", bytes)
                        if (success) {
                            postAnySuccess = true
                            uploadedCount++
                        } else {
                            failCount++
                        }
                    } else {
                        failCount++
                    }
                }

                if (postAnySuccess) {
                    uploadedSet.add(key)
                }
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

    /**
     * Retrieves configured Google Apps Script Web App URL.
     */
    fun getScriptWebAppUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_CUSTOM_SCRIPT_URL, DEFAULT_WEB_APP_URL) ?: DEFAULT_WEB_APP_URL
    }

    fun setScriptWebAppUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_CUSTOM_SCRIPT_URL, url.trim()).apply()
    }

    /**
     * Uploads media bytes to Google Drive via Google Apps Script Web App (Zero Cost, Serverless).
     * Returns Google Drive shareable / streamable URL.
     */
    suspend fun uploadToDriveViaWebApp(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        subfolder: String = "Archive_15Days"
    ): String? = withContext(Dispatchers.IO) {
        val scriptUrl = getScriptWebAppUrl(context)
        if (scriptUrl.isBlank()) return@withContext null

        try {
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val jsonPayload = JSONObject().apply {
                put("fileData", base64Data)
                put("filename", fileName)
                put("mimeType", mimeType)
                put("subfolder", subfolder)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(scriptUrl)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Script upload HTTP error: ${response.code} - ${response.message}")
                    return@withContext null
                }
                val respBody = response.body?.string() ?: ""
                Log.d(TAG, "Script upload response: $respBody")
                try {
                    val jsonObj = JSONObject(respBody)
                    val status = jsonObj.optString("status")
                    if (status.equals("success", ignoreCase = true) || jsonObj.has("fileUrl")) {
                        val fileUrl = jsonObj.optString("fileUrl")
                        val downloadUrl = jsonObj.optString("downloadUrl")
                        val fileId = jsonObj.optString("fileId")

                        // Return a direct stream-friendly URL if fileId is present
                        if (fileId.isNotBlank()) {
                            return@withContext "https://drive.google.com/uc?export=view&id=$fileId"
                        } else if (fileUrl.isNotBlank()) {
                            return@withContext fileUrl
                        } else if (downloadUrl.isNotBlank()) {
                            return@withContext downloadUrl
                        }
                    }
                } catch (e: Exception) {
                    // Check if response is raw text containing drive link
                    if (respBody.contains("drive.google.com")) {
                        return@withContext respBody.trim()
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during WebApp Drive upload: ${e.message}", e)
            null
        }
    }

    /**
     * Safely removes a file from Firebase Storage once archived to Google Drive.
     * Prevents Firebase 5GB storage exhaustion.
     */
    suspend fun deleteFromFirebaseStorage(firebaseUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (!firebaseUrl.contains("firebasestorage.googleapis.com") && !firebaseUrl.contains("appspot.com")) {
            return@withContext false
        }
        try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(firebaseUrl)
            storageRef.delete().await()
            Log.d(TAG, "Successfully deleted file from Firebase Storage: $firebaseUrl")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete file from Firebase Storage (${e.message}): $firebaseUrl")
            false
        }
    }

    /**
     * 15-DAY AUTOMATED MEDIA ARCHIVAL & PURGE SYSTEM
     *
     * Rule:
     * - Posts older than 15 days -> Moves photos to Google Drive, deletes from Firebase Storage, updates Room DB.
     * - Banners older than 15 days -> Moves to Google Drive, deletes from Firebase Storage, updates Room DB.
     * - Events older than 15 days -> Moves to Google Drive, deletes from Firebase Storage, updates Room DB.
     * - Chat Photos, Voice Notes, & PDFs older than 15 days -> Moves to Google Drive, deletes from Firebase Storage, updates Room DB.
     * - EXCLUSIONS: Gallery photos and Member Profile photos remain strictly on Firebase Storage!
     */
    suspend fun pruneAndArchiveMediaOlderThan15Days(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fifteenDaysAgo = now - (15L * 24L * 60L * 60L * 1000L)
        val db = AppDatabase.getDatabase(context)

        var totalArchived = 0
        var totalDeletedFromFirebase = 0

        Log.d(TAG, "Starting 15-Day Media Archival & Purge. Cutoff timestamp: $fifteenDaysAgo")

        try {
            // 1. POSTS OLDER THAN 15 DAYS
            val allPosts: List<PostEntity> = db.postDao().getAllPostsDirect()
            val oldPosts: List<PostEntity> = allPosts.filter { postEntity ->
                postEntity.timestamp < fifteenDaysAgo && !postEntity.imageUrlsJson.isNullOrBlank()
            }
            for (post in oldPosts) {
                try {
                    val jsonArray = JSONArray(post.imageUrlsJson)
                    var updated = false
                    val newUrls = JSONArray()

                    for (i in 0 until jsonArray.length()) {
                        val originalUrl = jsonArray.optString(i)
                        if (originalUrl.contains("firebasestorage.googleapis.com")) {
                            val bytes = resolveMediaBytes(context, originalUrl)
                            if (bytes != null && bytes.isNotEmpty()) {
                                val fileName = "JayHind_Post_${post.id}_img$i.webp"
                                val driveUrl = uploadToDriveViaWebApp(context, bytes, fileName, "image/webp", "Posts")
                                if (driveUrl != null) {
                                    newUrls.put(driveUrl)
                                    deleteFromFirebaseStorage(originalUrl)
                                    totalDeletedFromFirebase++
                                    totalArchived++
                                    updated = true
                                    continue
                                }
                            }
                        }
                        newUrls.put(originalUrl)
                    }

                    if (updated) {
                        val updatedPost = post.copy(imageUrlsJson = newUrls.toString())
                        db.postDao().updatePost(updatedPost)
                        // Also sync updated drive link to Firestore
                        try {
                            val urlsList = mutableListOf<String>()
                            for (j in 0 until newUrls.length()) urlsList.add(newUrls.getString(j))
                            FirebaseFirestore.getInstance().collection("posts").document(post.id)
                                .update("imageUrls", urlsList)
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error archiving post ${post.id}: ${e.message}")
                }
            }

            // 2. BANNERS OLDER THAN 15 DAYS
            val allBanners: List<BannerEntity> = db.bannerDao().getAllBannersDirect()
            val oldBanners: List<BannerEntity> = allBanners.filter { bannerEntity ->
                bannerEntity.createdAt < fifteenDaysAgo && bannerEntity.imageUrl.contains("firebasestorage.googleapis.com")
            }
            for (banner in oldBanners) {
                try {
                    val bytes = resolveMediaBytes(context, banner.imageUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val fileName = "JayHind_Banner_${banner.id}.webp"
                        val driveUrl = uploadToDriveViaWebApp(context, bytes, fileName, "image/webp", "Banners")
                        if (driveUrl != null) {
                            deleteFromFirebaseStorage(banner.imageUrl)
                            val updatedBanner = banner.copy(imageUrl = driveUrl)
                            db.bannerDao().updateBanner(updatedBanner)
                            try {
                                FirebaseFirestore.getInstance().collection("banners").document(banner.id)
                                    .update("imageUrl", driveUrl)
                            } catch (_: Exception) {}
                            totalArchived++
                            totalDeletedFromFirebase++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error archiving banner ${banner.id}: ${e.message}")
                }
            }

            // 3. EVENTS OLDER THAN 15 DAYS
            val allEvents: List<EventEntity> = db.eventDao().getAllEventsDirect()
            val oldEvents: List<EventEntity> = allEvents.filter { eventEntity ->
                val img = eventEntity.imageUrl ?: ""
                eventEntity.createdAt < fifteenDaysAgo && img.contains("firebasestorage.googleapis.com")
            }
            for (event in oldEvents) {
                try {
                    val eventImg = event.imageUrl ?: continue
                    val bytes = resolveMediaBytes(context, eventImg)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val fileName = "JayHind_Event_${event.id}.webp"
                        val driveUrl = uploadToDriveViaWebApp(context, bytes, fileName, "image/webp", "Events")
                        if (driveUrl != null) {
                            deleteFromFirebaseStorage(eventImg)
                            val updatedEvent = event.copy(imageUrl = driveUrl)
                            db.eventDao().updateEvent(updatedEvent)
                            try {
                                FirebaseFirestore.getInstance().collection("events").document(event.id)
                                    .update("imageUrl", driveUrl)
                            } catch (_: Exception) {}
                            totalArchived++
                            totalDeletedFromFirebase++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error archiving event ${event.id}: ${e.message}")
                }
            }

            // 4. CHAT MESSAGES OLDER THAN 15 DAYS (Photos, Voice Notes, Documents/PDFs)
            val allChats: List<ChatMessageEntity> = db.chatDao().getAllChatMessagesDirect()
            val oldChats: List<ChatMessageEntity> = allChats.filter { msg ->
                val attUrl = msg.attachmentUrl ?: ""
                val attType = msg.attachmentType ?: ""
                msg.timestamp < fifteenDaysAgo &&
                        attUrl.contains("firebasestorage.googleapis.com") &&
                        (attType == "IMAGE" || attType == "VOICE" || attType == "DOCUMENT" || attType == "VIDEO")
            }

            for (chat in oldChats) {
                val originalUrl = chat.attachmentUrl ?: continue
                try {
                    val bytes = resolveMediaBytes(context, originalUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val mime = when (chat.attachmentType) {
                            "VOICE" -> "audio/mp4"
                            "DOCUMENT" -> "application/pdf"
                            "VIDEO" -> "video/mp4"
                            else -> "image/webp"
                        }
                        val ext = when (chat.attachmentType) {
                            "VOICE" -> ".m4a"
                            "DOCUMENT" -> ".pdf"
                            "VIDEO" -> ".mp4"
                            else -> ".webp"
                        }
                        val folderName = when (chat.attachmentType) {
                            "VOICE" -> "VoiceNotes"
                            "DOCUMENT" -> "Documents"
                            "VIDEO" -> "Videos"
                            else -> "ChatPhotos"
                        }
                        val fileName = "JayHind_Chat_${chat.id}$ext"

                        val driveUrl = uploadToDriveViaWebApp(context, bytes, fileName, mime, folderName)
                        if (driveUrl != null) {
                            deleteFromFirebaseStorage(originalUrl)
                            val updatedChat = chat.copy(attachmentUrl = driveUrl)
                            db.chatDao().updateChatMessage(updatedChat)
                            try {
                                FirebaseFirestore.getInstance().collection("chat_messages").document(chat.id)
                                    .update("attachmentUrl", driveUrl)
                            } catch (_: Exception) {}
                            totalArchived++
                            totalDeletedFromFirebase++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error archiving chat ${chat.id}: ${e.message}")
                }
            }

            val summary = "१५ दिवसांपेक्षा जुने $totalArchived मीडिया Google Drive वर हलवले व Firebase वरून $totalDeletedFromFirebase फायली हटवल्या."
            Log.d(TAG, "Media Archival Complete: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error in pruneAndArchiveMediaOlderThan15Days: ${e.message}", e)
            Result.failure(e)
        }
    }
}
