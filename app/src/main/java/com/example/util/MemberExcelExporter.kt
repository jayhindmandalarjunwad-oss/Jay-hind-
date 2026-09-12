package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.User
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to export Mandal Members register into an Excel compatible CSV file
 * with UTF-8 BOM for perfect Marathi Devanagari font rendering in MS Excel and Google Sheets.
 */
object MemberExcelExporter {

    const val FILE_NAME = "जय हिंद मंडळ ॲप्लिकेशन सदस्य नोंदणी.csv"

    fun generateCsvContent(members: List<User>): String {
        val sb = StringBuilder()
        // UTF-8 Byte Order Mark (BOM) so Excel opens Marathi Devanagari text correctly
        sb.append("\uFEFF")

        // 10 Detailed Columns requested by Admin
        sb.append("अ. क्र.,डिजिटल आयडी नंबर,सदस्याचे पूर्ण नाव,पद / भूमिका,मोबाईल नंबर,जन्मतारीख,रक्तगट,सदस्याचा पत्ता,नोंदणी दिनांक,खाते स्थिती\n")

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("mr", "IN"))

        members.forEachIndexed { index, user ->
            val srNo = (index + 1).toString()
            val memberId = IdCardUtils.formatMemberId(user)
            val name = escapeCsv(user.fullName.ifBlank { "सभासद" })
            val designation = escapeCsv(
                if (user.designation.isNotBlank()) user.designation 
                else if (user.isAdmin) "मंडळ पदाधिकारी" 
                else "सभासद"
            )
            val mobile = escapeCsv(user.mobileNumber)
            val dob = escapeCsv(user.dateOfBirth.ifBlank { "-" })
            val bloodGroup = escapeCsv(user.bloodGroup.ifBlank { "-" })
            val address = escapeCsv(user.address.ifBlank { "अर्जुनवाड, ता. शिरोळ, जि. कोल्हापूर" })
            val regDate = try {
                dateFormat.format(Date(user.createdAt))
            } catch (_: Exception) {
                "-"
            }
            val status = when (user.status.uppercase()) {
                "APPROVED" -> "सक्रिय (मंजूर)"
                "PENDING_APPROVAL" -> "प्रलंबित"
                "REJECTED" -> "नाकारलेले"
                "BLOCKED" -> "ब्लॉक"
                else -> user.status
            }

            sb.append("$srNo,$memberId,$name,$designation,$mobile,$dob,$bloodGroup,$address,$regDate,$status\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\n", " ").replace("\r", " ").trim()
        if (clean.contains(",") || clean.contains("\"")) {
            clean = "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return clean
    }

    fun saveAndGetUri(context: Context, members: List<User>): Pair<File, Uri>? {
        return try {
            val csvContent = generateCsvContent(members)

            // 1. Save to app external downloads directory (guaranteed accessible via FileProvider)
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(downloadsDir, FILE_NAME)
            file.writeText(csvContent, Charsets.UTF_8)

            // 2. Also save to public Downloads folder for user convenience
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            os.write(csvContent.toByteArray(Charsets.UTF_8))
                        }
                    }
                } else {
                    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (publicDir.exists() || publicDir.mkdirs()) {
                        val pubFile = File(publicDir, FILE_NAME)
                        pubFile.writeText(csvContent, Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {
                // Secondary copy error ignored
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Pair(file, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "जय हिंद मंडळ ॲप्लिकेशन सदस्य नोंदणी")
            putExtra(
                Intent.EXTRA_TEXT, 
                "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड - अधिकृत सभासद डिजिटल नोंदणी वही (Excel/CSV Data)."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "सदस्य नोंदणी एक्सेल फाईल शेअर करा"))
    }

    fun openCsvFile(context: Context, fileUri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(openIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "एक्सेल फाईल उघडण्यासाठी ॲप सापडले नाही. कृपया फाईल शेअर करा.", Toast.LENGTH_LONG).show()
        }
    }
}
