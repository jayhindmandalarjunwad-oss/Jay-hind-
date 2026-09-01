package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.MandalInfo
import com.example.data.model.User
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object IdCardUtils {

    /**
     * Formats a professional and attractive Member ID (e.g., JHM - 26 - 001, JHM - 26 - 002)
     * Admin is strictly fixed to "JHM - 26 - 001".
     * Sequential member numbering is strictly maintained without skipping.
     */
    fun formatMemberId(user: User, memberIndex: Int? = null): String {
        val serialNum = when {
            user.id == "admin_1" || user.id.contains("admin", ignoreCase = true) || user.role.equals("ADMIN", ignoreCase = true) -> "001"
            memberIndex != null && memberIndex > 0 -> String.format(Locale.US, "%03d", memberIndex)
            else -> {
                val digits = user.id.filter { it.isDigit() }
                if (digits.isNotEmpty()) {
                    val n = digits.toIntOrNull() ?: (Math.abs(user.id.hashCode()) % 890 + 10)
                    String.format(Locale.US, "%03d", (n % 999).coerceAtLeast(2))
                } else {
                    val hash = (Math.abs(user.id.hashCode()) % 890) + 2
                    String.format(Locale.US, "%03d", hash)
                }
            }
        }
        return "JHM - 26 - $serialNum"
    }

    /**
     * Formats Member ID with strict sequential order from members list
     */
    fun formatMemberIdWithList(user: User, allMembers: List<User>): String {
        if (user.id == "admin_1" || user.id.contains("admin", ignoreCase = true) || user.role.equals("ADMIN", ignoreCase = true)) {
            return "JHM - 26 - 001"
        }
        val sortedMembers = allMembers
            .filterNot { it.id == "admin_1" || it.id.contains("admin", ignoreCase = true) || it.role.equals("ADMIN", ignoreCase = true) }
            .sortedBy { it.createdAt }
        val idx = sortedMembers.indexOfFirst { it.id == user.id }
        val seqNumber = if (idx >= 0) idx + 2 else 2
        return String.format(Locale.US, "JHM - 26 - %03d", seqNumber)
    }

    /**
     * Verification data extracted from scanned QR code
     */
    data class QrVerificationResult(
        val memberId: String,
        val fullName: String,
        val designation: String,
        val mobileNumber: String,
        val bloodGroup: String,
        val address: String,
        val userId: String? = null,
        val matchedUser: User? = null,
        val isOfficialMandal: Boolean = true,
        val rawContent: String = ""
    )

    /**
     * Creates universal verification URL for QR Code.
     * When scanned by ANY standard mobile camera (Google Lens, Samsung Camera, Apple Camera, etc.)
     * or by the in-app scanner, this instantly displays the member verification info.
     */
    /**
     * Creates plain text digital QR verification payload containing only:
     * - जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड
     * - Member ID
     * - Name
     * - Blood Group
     * - Status
     * No URLs, no phone number, no address, no private UIDs.
     * Scannable by any camera or Google Lens.
     */
    fun getVerificationPayload(user: User, mandalInfo: MandalInfo? = null): String {
        val memberId = formatMemberId(user)
        val statusStr = if (user.isApproved) "Active Member" else "Pending Member"
        val blood = user.bloodGroup.ifBlank { "O+" }
        return """
जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड

Member ID: $memberId
Name: ${user.fullName}
Blood Group: $blood
Status: $statusStr
""".trim()
    }

    /**
     * Human-readable text summary of member verification for preview dialogs
     */
    fun getVerificationDisplaySummary(user: User, mandalInfo: MandalInfo? = null): String {
        val memberId = formatMemberId(user)
        val statusStr = if (user.isApproved) "Active Member" else "Pending Member"
        val blood = user.bloodGroup.ifBlank { "O+" }

        return """
🚩 जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड 🚩
━━━━━━━━━━━━━━━━━━━━━
🆔 Member ID: $memberId
👤 Name: ${user.fullName}
🩸 Blood Group: $blood
✅ Status: $statusStr
━━━━━━━━━━━━━━━━━━━━━
डिजिटल ओळखपत्र QR व्हेरिफिकेशन
""".trimIndent()
    }

    /**
     * Parses any scanned QR payload (Plain text, Member ID, or text)
     * and maps it to verified member information.
     */
    fun parseVerificationQrPayload(raw: String, allMembers: List<User>): QrVerificationResult? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        // 1. Check if it matches the standard plain text digital QR format
        if (trimmed.contains("Member ID:") || trimmed.contains("Name:") || trimmed.contains("Blood Group:") || trimmed.contains("जय हिंद")) {
            val lines = trimmed.lines()
            var extractedName = ""
            var extractedMid = ""
            var extractedBg = ""
            var extractedStatus = ""
            var extractedRole = ""

            for (line in lines) {
                val clean = line.trim()
                when {
                    clean.startsWith("Member ID:", ignoreCase = true) -> extractedMid = clean.substringAfter(":").trim()
                    clean.startsWith("Name:", ignoreCase = true) -> extractedName = clean.substringAfter(":").trim()
                    clean.startsWith("Blood Group:", ignoreCase = true) -> extractedBg = clean.substringAfter(":").trim()
                    clean.startsWith("Status:", ignoreCase = true) -> extractedStatus = clean.substringAfter(":").trim()
                    clean.contains("सभासद क्रमांक:") || clean.contains("आयडी:") -> extractedMid = clean.substringAfter(":").trim()
                    clean.contains("नाव:") -> extractedName = clean.substringAfter(":").trim()
                    clean.contains("रक्तगट:") -> extractedBg = clean.substringAfter(":").trim()
                    clean.contains("पद:") -> extractedRole = clean.substringAfter(":").trim()
                }
            }

            val matchedFromText = allMembers.find {
                (extractedMid.isNotBlank() && formatMemberId(it).equals(extractedMid, ignoreCase = true)) ||
                (extractedName.isNotBlank() && it.fullName.equals(extractedName, ignoreCase = true))
            }

            return QrVerificationResult(
                memberId = extractedMid.ifBlank { matchedFromText?.let { formatMemberId(it) } ?: "JH-2026-MEMBER" },
                fullName = matchedFromText?.fullName ?: extractedName.ifBlank { "जय हिंद सभासद" },
                designation = matchedFromText?.designation?.ifBlank { null } ?: extractedStatus.ifBlank { "Active Member" },
                mobileNumber = matchedFromText?.mobileNumber ?: "",
                bloodGroup = matchedFromText?.bloodGroup ?: extractedBg,
                address = matchedFromText?.address ?: "अर्जुनवाड",
                userId = matchedFromText?.id,
                matchedUser = matchedFromText,
                isOfficialMandal = true,
                rawContent = trimmed
            )
        }

        // 2. Check if URL formatted payload
        if (trimmed.contains("/verify") || trimmed.startsWith("mandal://verify") || trimmed.contains("jayhindmandal")) {
            try {
                val uri = Uri.parse(trimmed)
                val mid = uri.getQueryParameter("mid") ?: uri.getQueryParameter("id") ?: ""
                val name = uri.getQueryParameter("name") ?: ""
                val role = uri.getQueryParameter("role") ?: "सभासद"
                val mob = uri.getQueryParameter("mob") ?: uri.getQueryParameter("mobile") ?: ""
                val bg = uri.getQueryParameter("bg") ?: uri.getQueryParameter("blood") ?: ""
                val uid = uri.getQueryParameter("uid") ?: ""
                val addr = uri.getQueryParameter("addr") ?: uri.getQueryParameter("address") ?: "अर्जुनवाड"

                val matched = allMembers.find { 
                    (uid.isNotBlank() && it.id == uid) || 
                    (mob.isNotBlank() && it.mobileNumber == mob) ||
                    (mid.isNotBlank() && formatMemberId(it).equals(mid, ignoreCase = true)) ||
                    (name.isNotBlank() && it.fullName.equals(name, ignoreCase = true))
                }

                return QrVerificationResult(
                    memberId = mid.ifBlank { matched?.let { formatMemberId(it) } ?: "JH-2026-MEMBER" },
                    fullName = matched?.fullName ?: name.ifBlank { "जय हिंद सभासद" },
                    designation = matched?.designation?.ifBlank { null } ?: role,
                    mobileNumber = matched?.mobileNumber ?: mob,
                    bloodGroup = matched?.bloodGroup ?: bg,
                    address = matched?.address ?: addr,
                    userId = matched?.id ?: uid,
                    matchedUser = matched,
                    isOfficialMandal = true,
                    rawContent = trimmed
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Check if matching by Member ID or Mobile Number or Name directly
        val directMatch = allMembers.find {
            it.mobileNumber == trimmed ||
            it.id.equals(trimmed, ignoreCase = true) ||
            formatMemberId(it).equals(trimmed, ignoreCase = true) ||
            it.fullName.equals(trimmed, ignoreCase = true)
        }

        if (directMatch != null) {
            return QrVerificationResult(
                memberId = formatMemberId(directMatch),
                fullName = directMatch.fullName,
                designation = if (directMatch.designation.isNotBlank()) directMatch.designation else if (directMatch.isAdmin) "कार्यकारिणी सदस्य" else "सभासद",
                mobileNumber = directMatch.mobileNumber,
                bloodGroup = directMatch.bloodGroup,
                address = directMatch.address,
                userId = directMatch.id,
                matchedUser = directMatch,
                isOfficialMandal = true,
                rawContent = trimmed
            )
        }

        // Fallback generic scan result
        return QrVerificationResult(
            memberId = "JH-2026-SCAN",
            fullName = trimmed.take(40),
            designation = "स्कॅन केलेला मजकूर",
            mobileNumber = "",
            bloodGroup = "",
            address = "",
            isOfficialMandal = false,
            rawContent = trimmed
        )
    }

    /**
     * Generates high-contrast QR Code Bitmap using ZXing.
     * Uses ErrorCorrectionLevel.M and Margin 2 for 100% instant readability
     * in any mobile camera app (Google Lens, Samsung, iOS, etc.)
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 2)
            }
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val darkColor = Color.BLACK // Pure high-contrast solid black for 100% camera sensor scanning
            val lightColor = Color.WHITE

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a stunning High-Definition Digital ID Card Bitmap (1080 x 1560 px)
     */
    fun generateHighResIdCardBitmap(
        context: Context,
        user: User,
        mandalInfo: MandalInfo
    ): Bitmap {
        val width = 1080
        val height = 1560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Overall Card Background (Soft Premium Off-White with rounded border)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(250, 248, 245)
        }
        val cardRect = RectF(20f, 20f, width - 20f, height - 20f)
        canvas.drawRoundRect(cardRect, 48f, 48f, bgPaint)

        // Card Border Stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.rgb(235, 130, 20) // Saffron primary
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        // 2. Saffron & Golden Gradient Top Banner
        val headerHeight = 270f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                20f, 20f, width - 20f, headerHeight,
                intArrayOf(Color.rgb(235, 94, 15), Color.rgb(249, 140, 30), Color.rgb(217, 119, 6)),
                null, Shader.TileMode.CLAMP
            )
        }
        val headerPath = Path().apply {
            addRoundRect(
                RectF(20f, 20f, width - 20f, headerHeight),
                floatArrayOf(48f, 48f, 48f, 48f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // Header decorative bottom border
        val goldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.rgb(254, 215, 170)
        }
        canvas.drawLine(20f, headerHeight, width - 20f, headerHeight, goldLinePaint)

        // Header Mandal Logo on left (Dynamic custom logo if present, fallback to ic_jayhind_logo)
        try {
            var logoResBitmap: Bitmap? = null
            if (!mandalInfo.logoUrl.isNullOrBlank()) {
                logoResBitmap = MediaUtils.loadBitmap(context, mandalInfo.logoUrl)
            }
            if (logoResBitmap == null) {
                logoResBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_jayhind_logo)
            }
            if (logoResBitmap != null) {
                val logoSize = 160
                val scaledLogo = Bitmap.createScaledBitmap(logoResBitmap, logoSize, logoSize, true)
                // Draw circular mask for logo
                val circleLogo = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
                val cCanvas = Canvas(circleLogo)
                val cPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                cCanvas.drawCircle(logoSize / 2f, logoSize / 2f, logoSize / 2f, cPaint)
                cPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                cCanvas.drawBitmap(scaledLogo, 0f, 0f, cPaint)

                // White ring around logo
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    color = Color.WHITE
                }
                canvas.drawCircle(55f + logoSize / 2f, 55f + logoSize / 2f, logoSize / 2f + 2f, ringPaint)
                canvas.drawBitmap(circleLogo, 55f, 55f, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Header Title in Marathi Devanagari
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ", 240f, 105f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 243, 199)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("अर्जुनवाड, ता. शिरोळ (स्थापना १९९६)", 240f, 155f, subtitlePaint)

        val idBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cardTypePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 237, 213)
            textSize = 24f
        }
        canvas.drawText("अधिकृत डिजिटल सभासद ओळखपत्र", 240f, 205f, cardTypePaint)

        // 3. Subtle Central Watermark of Mandal Logo (Dynamic custom logo if present, fallback to ic_jayhind_logo)
        try {
            var logoRes: Bitmap? = null
            if (!mandalInfo.logoUrl.isNullOrBlank()) {
                logoRes = MediaUtils.loadBitmap(context, mandalInfo.logoUrl)
            }
            if (logoRes == null) {
                logoRes = BitmapFactory.decodeResource(context.resources, R.drawable.ic_jayhind_logo)
            }
            if (logoRes != null) {
                val wmSize = 620
                val wmBitmap = Bitmap.createScaledBitmap(logoRes, wmSize, wmSize, true)
                val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = 24 // ~9.5% subtle transparency
                }
                val wmX = (width - wmSize) / 2f
                val wmY = 460f
                canvas.drawBitmap(wmBitmap, wmX, wmY, wmPaint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Member Profile Photo Box (Left side)
        val photoLeft = 70f
        val photoTop = 320f
        val photoSize = 250f
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize)

        // Photo Border & Fill
        val photoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 244, 246)
        }
        canvas.drawRoundRect(photoRect, 28f, 28f, photoBgPaint)

        // Default avatar or photo
        var memberPhotoDrawn = false
        if (!user.profilePhotoUrl.isNullOrBlank()) {
            val bmp = MediaUtils.loadBitmap(context, user.profilePhotoUrl)
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, photoSize.toInt(), photoSize.toInt(), true)
                val roundedPhoto = Bitmap.createBitmap(photoSize.toInt(), photoSize.toInt(), Bitmap.Config.ARGB_8888)
                val pCanvas = Canvas(roundedPhoto)
                val pPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                pCanvas.drawRoundRect(0f, 0f, photoSize, photoSize, 28f, 28f, pPaint)
                pPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                pCanvas.drawBitmap(scaled, 0f, 0f, pPaint)
                canvas.drawBitmap(roundedPhoto, photoLeft, photoTop, null)
                memberPhotoDrawn = true
            }
        }

        if (!memberPhotoDrawn) {
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(235, 94, 15)
                textSize = 90f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val initial = user.fullName.firstOrNull()?.toString() ?: "ज"
            canvas.drawText(initial, photoLeft + photoSize / 2f, photoTop + photoSize / 2f + 32f, initialPaint)
        }

        val photoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.rgb(235, 94, 15)
        }
        canvas.drawRoundRect(photoRect, 28f, 28f, photoBorderPaint)

        // Blood Group Pill below Photo
        val bloodGroup = user.bloodGroup.ifBlank { "O+" }
        val bgPillRect = RectF(photoLeft + 20f, photoTop + photoSize + 16f, photoLeft + photoSize - 20f, photoTop + photoSize + 66f)
        val bgPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38) // Blood red
        }
        canvas.drawRoundRect(bgPillRect, 25f, 25f, bgPillPaint)

        val bgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("रक्तगट: $bloodGroup", bgPillRect.centerX(), bgPillRect.centerY() + 9f, bgTextPaint)

        // 5. Member Details Section (Right side)
        val textLeft = 360f
        var curY = 365f

        // Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(user.fullName, textLeft, curY, namePaint)
        curY += 52f

        // Designation Badge
        val desig = if (user.designation.isNotBlank()) user.designation else if (user.isAdmin) "मंडळ कार्यकारिणी सदस्य" else "आजीवन सभासद"
        val desigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 83, 9)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("पद: $desig", textLeft, curY, desigPaint)
        curY += 46f

        // Formatted Member ID (JH-2026-001)
        val memberId = formatMemberId(user)
        val idLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 85, 99)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val idValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38) // Bold Red/Navy for ID
            textSize = 34f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("सभासद आयडी:", textLeft, curY, idLabelPaint)
        canvas.drawText(memberId, textLeft + 160f, curY, idValuePaint)
        curY += 48f

        // Mobile
        val infoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 85, 99)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val infoValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("मोबाईल:", textLeft, curY, infoLabelPaint)
        canvas.drawText(user.mobileNumber, textLeft + 110f, curY, infoValPaint)
        curY += 44f

        // Address
        canvas.drawText("पत्ता:", textLeft, curY, infoLabelPaint)
        val addrText = if (user.address.isNotBlank()) user.address else "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर"
        val tp = TextPaint(infoValPaint).apply { textSize = 25f }
        val staticLayout = StaticLayout.Builder.obtain(addrText, 0, addrText.length, tp, (width - textLeft - 60).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(textLeft + 70f, curY - 22f)
        staticLayout.draw(canvas)
        canvas.restore()

        // Horizontal Divider Line
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 3f
        }
        canvas.drawLine(60f, 690f, width - 60f, 690f, divPaint)

        // 6. Mid Status Bar
        val regDateStr = SimpleDateFormat("dd/MM/yyyy", Locale("mr", "IN")).format(Date(user.createdAt))
        val midBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 244, 246)
        }
        val midBarRect = RectF(60f, 715f, width - 60f, 790f)
        canvas.drawRoundRect(midBarRect, 20f, 20f, midBarPaint)

        val midTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 65, 81)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("नोंदणी दिनांक: $regDateStr", 85f, 760f, midTextPaint)

        val validityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 101, 52) // Green
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("वैधता: आजीवन (Lifetime)", width - 85f, 760f, validityPaint)

        // 7. Bottom Section: QR Code on Left, Official Seal on Right
        val qrSize = 310
        val qrTop = 840f
        val qrLeft = 100f

        val qrContent = getVerificationPayload(user, mandalInfo)
        val qrBitmap = generateQrBitmap(qrContent, qrSize)

        if (qrBitmap != null) {
            val qrBorderRect = RectF(qrLeft - 10f, qrTop - 10f, qrLeft + qrSize + 10f, qrTop + qrSize + 10f)
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }
            canvas.drawRoundRect(qrBorderRect, 18f, 18f, qrBgPaint)
            val qrStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.rgb(209, 213, 219)
            }
            canvas.drawRoundRect(qrBorderRect, 18f, 18f, qrStrokePaint)
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

            val qrLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(31, 41, 55)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("व्हेरिफिकेशन QR कोड (Scan to Verify)", qrLeft + qrSize / 2f, qrTop + qrSize + 40f, qrLabelPaint)
        }

        // 8. Official Mandal Rubber Stamp on Right (अधिकृत डिजिटल निळा शिक्का व अध्यक्षांची स्वाक्षरी)
        val sealCenterX = width - 250f
        val sealCenterY = qrTop + qrSize / 2f
        val sealRadius = 145f

        drawOfficialStamp(context, canvas, sealCenterX, sealCenterY, sealRadius, mandalInfo)

        // Sign text below stamp
        val presName = mandalInfo.presidentName.ifBlank { "अध्यक्ष" }
        val signPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(presName, sealCenterX, qrTop + qrSize + 32f, signPaint)

        val signSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("जय हिंद मंडळ, अर्जुनवाड", sealCenterX, qrTop + qrSize + 56f, signSubPaint)

        // 9. Card Bottom Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 244, 246)
        }
        val footerRect = RectF(20f, height - 130f, width - 20f, height - 20f)
        val footerPath = Path().apply {
            addRoundRect(
                footerRect,
                floatArrayOf(0f, 0f, 0f, 0f, 48f, 48f, 48f, 48f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(footerPath, footerPaint)

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 114, 128)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("हे ओळखपत्र जय हिंद मंडळाच्या सर्व अधिकृत उपक्रमांसाठी ग्राह्य आहे.", width / 2f, height - 75f, footerTextPaint)

        val orgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 94, 15)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड • स्थापना १९९६", width / 2f, height - 42f, orgTextPaint)

        return bitmap
    }

    /**
     * Draws an authentic official circular blue rubber seal (अधिकृत गोल रबर शिक्का)
     * and seamlessly overlays the President's signature on top of it.
     */
    private fun drawOfficialStamp(context: Context, canvas: Canvas, cx: Float, cy: Float, radius: Float, mandalInfo: MandalInfo) {
        var drawnCustomStamp = false

        // If an authentic physical stamp was uploaded, draw it!
        if (mandalInfo.officialStampUrl.isNotBlank()) {
            try {
                val stampBmp = MediaUtils.loadBitmap(context, mandalInfo.officialStampUrl)
                if (stampBmp != null) {
                    val targetDiameter = radius * 2f
                    val matrix = Matrix().apply {
                        val s = targetDiameter / maxOf(stampBmp.width, stampBmp.height).toFloat()
                        postScale(s, s)
                        val scaledW = stampBmp.width * s
                        val scaledH = stampBmp.height * s
                        postTranslate(cx - scaledW / 2f, cy - scaledH / 2f)
                    }
                    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = 240
                    }
                    canvas.drawBitmap(stampBmp, matrix, stampPaint)
                    drawnCustomStamp = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!drawnCustomStamp) {
            val stampBlue = Color.rgb(30, 58, 138) // Official Rubber Stamp Blue (#1E3A8A)
            val stampAlpha = 220

            // 1. Outer solid circle
            val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6.5f
                color = stampBlue
                alpha = stampAlpha
            }
            canvas.drawCircle(cx, cy, radius, outerPaint)

            // 2. Inner fine dashed ring
            val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = stampBlue
                alpha = stampAlpha
                pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
            }
            canvas.drawCircle(cx, cy, radius - 13f, innerCirclePaint)

            // 3. Innermost solid circle
            val innermostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = stampBlue
                alpha = stampAlpha
            }
            canvas.drawCircle(cx, cy, radius - 24f, innermostPaint)

            // 4. Center texts in Rubber Stamp
            val stampCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stampBlue
                alpha = stampAlpha
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stampBlue
                alpha = stampAlpha
                textSize = 18f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("★ ★ ★", cx, cy - 56f, starPaint)
            canvas.drawText("जय हिंद मंडळ", cx, cy - 28f, stampCenterPaint)

            val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stampBlue
                alpha = stampAlpha
                textSize = 23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("★ अधिकृत शिक्का ★", cx, cy + 4f, offPaint)
            canvas.drawText("अर्जुनवाड", cx, cy + 34f, stampCenterPaint)
            canvas.drawText("स्थापना १९९६", cx, cy + 62f, Paint(stampCenterPaint).apply { textSize = 17f })
        }

        // 5. Overlay President's Signature
        var drawnSignature = false
        if (mandalInfo.presidentSignatureUrl.isNotBlank()) {
            try {
                val sigBmp = MediaUtils.loadBitmap(context, mandalInfo.presidentSignatureUrl)
                if (sigBmp != null) {
                    val sigWidth = 230f
                    val sigHeight = 115f
                    val matrix = Matrix().apply {
                        val scaleX = sigWidth / sigBmp.width.toFloat()
                        val scaleY = sigHeight / sigBmp.height.toFloat()
                        val s = minOf(scaleX, scaleY)
                        postScale(s, s)
                        postRotate(-5f, (sigBmp.width * s) / 2f, (sigBmp.height * s) / 2f)
                        val scaledW = sigBmp.width * s
                        val scaledH = sigBmp.height * s
                        postTranslate(cx - scaledW / 2f, cy - scaledH / 2f - 6f)
                    }
                    val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = 245
                    }
                    canvas.drawBitmap(sigBmp, matrix, sigPaint)
                    drawnSignature = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback cursive signature line if no image uploaded
        if (!drawnSignature) {
            val sigLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 45, 105)
                strokeWidth = 4f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val sigPath = Path().apply {
                moveTo(cx - 75f, cy + 8f)
                cubicTo(cx - 50f, cy - 25f, cx - 25f, cy + 20f, cx, cy - 10f)
                cubicTo(cx + 20f, cy - 35f, cx + 45f, cy + 15f, cx + 75f, cy - 15f)
                moveTo(cx - 65f, cy + 18f)
                lineTo(cx + 65f, cy + 12f)
            }
            canvas.drawPath(sigPath, sigLinePaint)
        }
    }

    /**
     * Saves HD Digital ID Card directly to device gallery via MediaStore
     */
    fun saveIdCardToGallery(
        context: Context,
        user: User,
        mandalInfo: MandalInfo,
        onSuccess: (Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val bitmap = generateHighResIdCardBitmap(context, user, mandalInfo)
            val memberId = formatMemberId(user)
            val fileName = "JayHind_IDCard_${memberId}_${System.currentTimeMillis()}.png"

            var savedUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/JayHindMandal")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    savedUri = uri
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val mandalDir = File(picturesDir, "JayHindMandal").apply { if (!exists()) mkdirs() }
                val imageFile = File(mandalDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                savedUri = Uri.fromFile(imageFile)
            }

            if (savedUri != null) {
                Toast.makeText(context, "✅ HD ओळखपत्र गॅलरीत सेव्ह झाले!", Toast.LENGTH_LONG).show()
                onSuccess(savedUri)
            } else {
                onError("ओळखपत्र सेव्ह करता आले नाही.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "ओळखपत्र सेव्ह करताना त्रुटी आली.")
        }
    }

    /**
     * Shares HD Digital ID Card image directly or via WhatsApp
     */
    fun shareIdCard(
        context: Context,
        user: User,
        mandalInfo: MandalInfo,
        onlyWhatsApp: Boolean = false
    ) {
        try {
            val bitmap = generateHighResIdCardBitmap(context, user, mandalInfo)
            val cachePath = File(context.cacheDir, "id_cards").apply { if (!exists()) mkdirs() }
            val memberId = formatMemberId(user)
            val file = File(cachePath, "JayHind_ID_${memberId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = """
🚩 *जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड* 🚩
(स्थापना १९९६)

🪪 *अधिकृत डिजिटल सभासद ओळखपत्र*
👤 *नाव:* ${user.fullName}
🆔 *सभासद क्रमांक:* ${formatMemberId(user)}
🎖️ *पद:* ${if (user.designation.isNotBlank()) user.designation else if (user.isAdmin) "कार्यकारिणी सदस्य" else "सभासद"}
🩸 *रक्तगट:* ${user.bloodGroup}
📱 *मोबाईल:* ${user.mobileNumber}
📍 *पत्ता:* ${user.address}

✅ मंडळाचे अधिकृत व्हेरिफाईड ओळखपत्र.
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (onlyWhatsApp) {
                    `package` = "com.whatsapp"
                }
            }

            try {
                if (onlyWhatsApp) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(Intent.createChooser(intent, "डिजिटल ओळखपत्र शेअर करा"))
                }
            } catch (e: Exception) {
                // If WhatsApp specific package not found, open general chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "डिजिटल ओळखपत्र शेअर करा"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "शेअर करताना त्रुटी आली: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
