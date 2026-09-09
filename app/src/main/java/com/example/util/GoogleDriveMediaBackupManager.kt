package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PhotoEntity
import com.example.data.local.PostEntity
import com.example.data.local.BannerEntity
import com.example.data.local.EventEntity
import com.example.data.local.UserEntity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
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
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

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

    private val _connectedAccount = MutableStateFlow<String?>(null)
    val connectedAccount: StateFlow<String?> = _connectedAccount.asStateFlow()

    // Folder ID cache to prevent repeated Drive folder lookup API calls during batch sync
    private val folderIdCache = mutableMapOf<String, String>()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("connected_email", null)
        val lastTime = prefs.getLong("last_sync_timestamp", 0L)
        val lastFormatted = prefs.getString("last_sync_formatted", "") ?: ""
        val lastSummary = prefs.getString("last_sync_summary", "") ?: ""

        _connectedAccount.value = savedEmail ?: "jayhindmandalarjunwad@gmail.com"
        _syncProgress.value = DriveSyncProgress(
            lastSyncTimestamp = lastTime,
            lastSyncFormatted = lastFormatted,
            lastSyncSummary = lastSummary
        )
    }

    /**
     * Builds Google Sign-In Client configured with the Drive scope
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(context: Context, account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        val email = account.email ?: "jayhindmandalarjunwad@gmail.com"
        _connectedAccount.value = email

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("connected_email", email)
            .apply()
        Log.d(TAG, "Google Drive linked to account: $email")
        return true
    }

    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            if (account != null) {
                return@withContext GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_SCOPE")
            }
            // Fallback to checking accounts via AccountManager
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            val targetAccount = accounts.firstOrNull { 
                it.name.equals("jayhindmandalarjunwad@gmail.com", ignoreCase = true) 
            } ?: accounts.firstOrNull()

            if (targetAccount != null) {
                return@withContext GoogleAuthUtil.getToken(context, targetAccount, "oauth2:$DRIVE_SCOPE")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get OAuth token: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Finds an existing folder or creates a new one in Google Drive.
     */
    private suspend fun findOrCreateDriveFolder(
        token: String,
        folderName: String,
        parentFolderId: String = "root"
    ): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$parentFolderId/$folderName"
        folderIdCache[cacheKey]?.let { return@withContext it }

        try {
            // 1. Search if folder already exists
            val query = "name = '${folderName.replace("'", "\\'")}' and '$parentFolderId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(searchReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val files = json.optJSONArray("files")
                        if (files != null && files.length() > 0) {
                            val id = files.getJSONObject(0).getString("id")
                            folderIdCache[cacheKey] = id
                            return@withContext id
                        }
                    }
                }
            }

            // 2. Not found, create new folder
            val createJson = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                put("parents", JSONArray().put(parentFolderId))
            }

            val createReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .addHeader("Authorization", "Bearer $token")
                .post(createJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(createReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val id = json.getString("id")
                        folderIdCache[cacheKey] = id
                        Log.d(TAG, "Created Drive Folder: $folderName (ID: $id)")
                        return@withContext id
                    }
                } else {
                    Log.e(TAG, "Failed creating folder $folderName: ${response.code} - ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findOrCreateDriveFolder for $folderName: ${e.message}", e)
        }
        return@withContext null
    }

    /**
     * Uploads media bytes to Google Drive inside the specified parent folder with a unique filename.
     * Sets reader permission and returns the direct CDN image/audio URL.
     */
    private suspend fun uploadFileToDrive(
        token: String,
        folderId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Check if file already exists in folder
            val query = "name = '${fileName.replace("'", "\\'")}' and '$folderId' in parents and trashed = false"
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(searchReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val files = JSONObject(body).optJSONArray("files")
                        if (files != null && files.length() > 0) {
                            val existingId = files.getJSONObject(0).getString("id")
                            return@withContext if (mimeType.startsWith("image/")) {
                                "https://lh3.googleusercontent.com/d/$existingId"
                            } else {
                                "https://drive.google.com/uc?export=download&id=$existingId"
                            }
                        }
                    }
                }
            }

            // Multipart upload to Google Drive v3
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().put(folderId))
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val uploadReq = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            var fileId: String? = null
            httpClient.newCall(uploadReq).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string()
                    if (!respBody.isNullOrBlank()) {
                        fileId = JSONObject(respBody).getString("id")
                    }
                } else {
                    Log.e(TAG, "Failed uploading $fileName: ${response.code} ${response.message}")
                }
            }

            if (fileId != null) {
                // Grant public read permission so the file can be viewed directly in app
                try {
                    val permJson = JSONObject().apply {
                        put("role", "reader")
                        put("type", "anyone")
                    }
                    val permReq = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$fileId/permissions")
                        .addHeader("Authorization", "Bearer $token")
                        .post(permJson.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    httpClient.newCall(permReq).execute().close()
                } catch (pe: Exception) {
                    Log.w(TAG, "Drive permission grant note: ${pe.message}")
                }

                val directUrl = if (mimeType.startsWith("image/")) {
                    "https://lh3.googleusercontent.com/d/$fileId"
                } else {
                    "https://drive.google.com/uc?export=download&id=$fileId"
                }
                Log.d(TAG, "Uploaded to Google Drive: $fileName -> $directUrl")
                return@withContext directUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading $fileName to Drive: ${e.message}", e)
        }
        return@withContext null
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
                    // Raw Base64
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
     * Main Sync Function:
     * Builds the exact requested folder hierarchy:
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
     *
     * Uploads with unique filenames: [SenderName]_[Mobile]_[DateTime].[jpg/m4a]
     */
    suspend fun performCompleteMediaBackup(
        context: Context,
        isAutoNightly: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (_syncProgress.value.isSyncing) {
            return@withContext Result.failure(Exception("सिंक आधीपासूनच प्रगतीपथावर आहे."))
        }

        _syncProgress.value = _syncProgress.value.copy(
            isSyncing = true,
            currentStep = "गुगल ड्राइव्ह पडताळणी...",
            error = null
        )

        try {
            val token = getAccessToken(context)
            val db = AppDatabase.getDatabase(context)
            val userMap = db.userDao().getAllUsersDirect().associateBy { it.id }

            val now = System.currentTimeMillis()
            val yearFolderName = "YEAR" + SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date(now))
            val monthFolderName = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(now)).uppercase(Locale.ENGLISH)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val uploadedSet = prefs.getStringSet("uploaded_item_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            // Collect all pending media items from Room
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
                currentStep = "फोल्डर स्ट्रक्चर तयार करत आहे..."
            )

            var uploadedCount = 0
            var skippedCount = 0
            var failCount = 0

            // If we have an OAuth token, we create real Google Drive folders via REST API
            if (token != null) {
                val rootId = findOrCreateDriveFolder(token, ROOT_FOLDER_NAME, "root")
                    ?: return@withContext Result.failure(Exception("गुगल ड्राइव्ह मुख्य फोल्डर तयार करता आले नाही."))

                val yearId = findOrCreateDriveFolder(token, yearFolderName, rootId) ?: rootId
                val monthId = findOrCreateDriveFolder(token, monthFolderName, yearId) ?: yearId

                val photosRootId = findOrCreateDriveFolder(token, PHOTOS_FOLDER_NAME, monthId) ?: monthId
                val voiceRootId = findOrCreateDriveFolder(token, VOICE_FOLDER_NAME, monthId) ?: monthId

                // Photos Subfolders
                val galleryFolderId = findOrCreateDriveFolder(token, "Gallary", photosRootId) ?: photosRootId
                val bannerFolderId = findOrCreateDriveFolder(token, "Banner", photosRootId) ?: photosRootId
                val eventFolderId = findOrCreateDriveFolder(token, "Event", photosRootId) ?: photosRootId
                val postFolderId = findOrCreateDriveFolder(token, "Post", photosRootId) ?: photosRootId
                val groupChatPhotoFolderId = findOrCreateDriveFolder(token, "Group Chat", photosRootId) ?: photosRootId
                val oneToOneChatPhotoFolderId = findOrCreateDriveFolder(token, "One to One Chat", photosRootId) ?: photosRootId

                // Voice Subfolders
                val groupChatVoiceFolderId = findOrCreateDriveFolder(token, "Group Chat", voiceRootId) ?: voiceRootId
                val oneToOneChatVoiceFolderId = findOrCreateDriveFolder(token, "One to One Chat", voiceRootId) ?: voiceRootId

                // 1. Upload Gallery Photos
                for (photo in allPhotos) {
                    val key = "gallery_${photo.id}"
                    if (uploadedSet.contains(key)) {
                        skippedCount++
                        continue
                    }
                    _syncProgress.value = _syncProgress.value.copy(
                        currentStep = "गॅलरी फोटो अपलोड: ${photo.caption.take(15)}",
                        completedItems = uploadedCount + skippedCount
                    )
                    val bytes = resolveMediaBytes(context, photo.imageUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val dateStr = formatTimestamp(photo.uploadedAt)
                        val captionClean = sanitizeName(photo.caption)
                        val fileName = "Gallary_${captionClean}_${dateStr}.jpg"
                        val driveUrl = uploadFileToDrive(token, galleryFolderId, fileName, "image/jpeg", bytes)
                        if (driveUrl != null) {
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
                        currentStep = "बॅनर फोटो अपलोड...",
                        completedItems = uploadedCount + skippedCount
                    )
                    val bytes = resolveMediaBytes(context, banner.imageUrl)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val dateStr = formatTimestamp(banner.createdAt)
                        val titleClean = sanitizeName(banner.title)
                        val fileName = "Banner_${titleClean}_${dateStr}.jpg"
                        val driveUrl = uploadFileToDrive(token, bannerFolderId, fileName, "image/jpeg", bytes)
                        if (driveUrl != null) {
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
                        currentStep = "कार्यक्रम फोटो अपलोड: ${event.title.take(15)}",
                        completedItems = uploadedCount + skippedCount
                    )
                    val img = event.imageUrl ?: ""
                    val bytes = resolveMediaBytes(context, img)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val dateStr = formatTimestamp(event.createdAt)
                        val titleClean = sanitizeName(event.title)
                        val fileName = "Event_${titleClean}_${dateStr}.jpg"
                        val driveUrl = uploadFileToDrive(token, eventFolderId, fileName, "image/jpeg", bytes)
                        if (driveUrl != null) {
                            uploadedSet.add(key)
                            uploadedCount++
                        } else failCount++
                    } else failCount++
                }

                // 4. Upload Posts Photos
                for (post in allPosts) {
                    val key = "post_${post.id}"
                    if (uploadedSet.contains(key)) {
                        skippedCount++
                        continue
                    }
                    _syncProgress.value = _syncProgress.value.copy(
                        currentStep = "पोस्ट फोटो अपलोड: ${post.authorName}",
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
                        val driveUrl = uploadFileToDrive(token, postFolderId, fileName, "image/jpeg", bytes)
                        if (driveUrl != null) {
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

                    // Photo in Chat (imageUrl or attachmentType == IMAGE)
                    val chatPhotoSrc = if (!chat.imageUrl.isNullOrBlank()) chat.imageUrl else if (chat.attachmentType.equals("IMAGE", ignoreCase = true)) chat.attachmentUrl else null
                    if (!chatPhotoSrc.isNullOrBlank()) {
                        val pKey = "chat_photo_${chat.id}"
                        if (!uploadedSet.contains(pKey)) {
                            _syncProgress.value = _syncProgress.value.copy(
                                currentStep = "चॅट फोटो अपलोड: $senderName",
                                completedItems = uploadedCount + skippedCount
                            )
                            val bytes = resolveMediaBytes(context, chatPhotoSrc)
                            if (bytes != null && bytes.isNotEmpty()) {
                                val targetFolderId = if (isGroup) groupChatPhotoFolderId else oneToOneChatPhotoFolderId
                                val prefix = if (isGroup) "GroupChat" else "OneToOneChat"
                                val fileName = "${prefix}_${senderName}_${mobile}_${dateStr}.jpg"
                                val driveUrl = uploadFileToDrive(token, targetFolderId, fileName, "image/jpeg", bytes)
                                if (driveUrl != null) {
                                    uploadedSet.add(pKey)
                                    uploadedCount++
                                } else failCount++
                            }
                        } else skippedCount++
                    }

                    // Voice Note in Chat (attachmentType == VOICE / AUDIO or audio URL)
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
                                currentStep = "व्हॉइस मेसेज अपलोड: $senderName",
                                completedItems = uploadedCount + skippedCount
                            )
                            val bytes = resolveMediaBytes(context, chatVoiceSrc)
                            if (bytes != null && bytes.isNotEmpty()) {
                                val targetFolderId = if (isGroup) groupChatVoiceFolderId else oneToOneChatVoiceFolderId
                                val prefix = if (isGroup) "GroupChat" else "OneToOneChat"
                                val fileName = "${prefix}_${senderName}_${mobile}_${dateStr}.m4a"
                                val driveUrl = uploadFileToDrive(token, targetFolderId, fileName, "audio/mp4", bytes)
                                if (driveUrl != null) {
                                    uploadedSet.add(aKey)
                                    uploadedCount++
                                } else failCount++
                            }
                        } else skippedCount++
                    }
                }
            } else {
                Log.w(TAG, "No Google OAuth token available. Ready for user Drive connection.")
            }

            // Save set of uploaded keys
            prefs.edit()
                .putStringSet("uploaded_item_ids", uploadedSet)
                .putLong("last_sync_timestamp", now)
                .putString("last_sync_formatted", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now)))
                .putString("last_sync_summary", "$uploadedCount फाइल्स Google Drive वर यशस्वीरीत्या सेव्ह!")
                .apply()

            val summaryMsg = if (token != null) {
                "$uploadedCount नवीन मीडिया फाइल्स ($yearFolderName / $monthFolderName) Google Drive वर यशस्वीरीत्या सिंक झाल्या! ($skippedCount आधीच सुरक्षित)"
            } else {
                "Google Drive खाते कनेक्ट करणे आवश्यक आहे. कृपया 'Connect Drive' बटण दाबा."
            }

            _syncProgress.value = DriveSyncProgress(
                isSyncing = false,
                currentStep = "पूर्ण झाले!",
                totalItems = totalCount,
                completedItems = totalCount,
                lastSyncTimestamp = now,
                lastSyncFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("mr", "IN")).format(Date(now)),
                lastSyncSummary = summaryMsg
            )

            // Also mirror metadata in Firestore system settings
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
            Log.e(TAG, "Fatal error during media sync: ${e.message}", e)
            _syncProgress.value = _syncProgress.value.copy(
                isSyncing = false,
                error = e.message
            )
            return@withContext Result.failure(e)
        }
    }
}
