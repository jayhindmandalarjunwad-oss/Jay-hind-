package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.BusinessLeadClick
import com.example.data.model.BusinessListing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to export Business Directory Analytics & Monthly/Custom Date Lead Reports
 * into an Excel-compatible CSV file with UTF-8 BOM for accurate Marathi Devanagari font rendering,
 * proper Excel text formatting for 10-digit mobile numbers (avoiding scientific notation),
 * and clean column structure.
 */
object BusinessAnalyticsExcelExporter {

    fun formatMonthLabel(monthYear: String, dateRangeLabel: String? = null): String {
        if (!dateRangeLabel.isNullOrBlank()) {
            return dateRangeLabel
        }
        return try {
            if (monthYear.isBlank() || monthYear.equals("ALL", ignoreCase = true) || monthYear == "सर्व") {
                "सर्व महिने (All Time)"
            } else {
                val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val date = inputFormat.parse(monthYear)
                if (date != null) {
                    val marathiMonth = SimpleDateFormat("MMMM yyyy", Locale("mr", "IN")).format(date)
                    val engMonth = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(date)
                    "$marathiMonth ($engMonth)"
                } else monthYear
            }
        } catch (_: Exception) {
            monthYear
        }
    }

    fun generateCsvContent(
        monthYear: String,
        businesses: List<BusinessListing>,
        clicks: List<BusinessLeadClick>,
        dateRangeLabel: String? = null
    ): String {
        val sb = StringBuilder()
        // UTF-8 Byte Order Mark (BOM) so Excel/WPS opens Marathi Devanagari text correctly
        sb.append("\uFEFF")

        val nowFormatted = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date())
        val periodLabel = formatMonthLabel(monthYear, dateRangeLabel)

        val isAllTime = (monthYear.isBlank() || monthYear.equals("ALL", ignoreCase = true) || monthYear == "सर्व") && dateRangeLabel.isNullOrBlank()

        val (totalCalls, totalWhatsApp) = if (isAllTime) {
            val clickCalls = clicks.count { it.clickType == "CALL" }
            val clickWa = clicks.count { it.clickType == "WHATSAPP" }
            if (clickCalls > 0 || clickWa > 0) {
                Pair(clickCalls, clickWa)
            } else {
                Pair(businesses.sumOf { it.callClicks }, businesses.sumOf { it.whatsappClicks })
            }
        } else {
            Pair(clicks.count { it.clickType == "CALL" }, clicks.count { it.clickType == "WHATSAPP" })
        }

        val totalLeads = totalCalls + totalWhatsApp

        // Report Title & Metadata Header
        sb.append(escapeCsv("जय हिंद कला क्रीडा व सांस्कृतिक मंडळ अर्जुनवाड - व्यावसायिक डिरेक्टरी अहवाल")).append("\n")
        sb.append("अहवाल कालावधी,").append(escapeCsv(periodLabel)).append("\n")
        sb.append("अहवाल निर्मिती तारीख व वेळ,").append(escapeCsv(nowFormatted)).append("\n")
        sb.append("नोंदणीकृत एकूण व्यवसाय,").append(businesses.size).append("\n")
        sb.append("एकूण थेट कॉल (Call Clicks),").append(totalCalls).append("\n")
        sb.append("एकूण WhatsApp संपर्क (WhatsApp Clicks),").append(totalWhatsApp).append("\n")
        sb.append("एकूण संपर्क चौकशी (Total Inquiries),").append(totalLeads).append("\n\n")

        // SECTION 1: BUSINESS-WISE LEADS SUMMARY TABLE
        sb.append(escapeCsv("--- भाग १: व्यवसायनिहाय संपर्क लीड्स सारांश (Business Wise Summary) ---")).append("\n")
        sb.append("अ. क्र.,व्यवसायाचे / दुकानाचे नाव,वर्गवारी (कॅटेगरी),संचालकाचे नाव,संपर्क नंबर,WhatsApp नंबर,पत्ता,कॉल क्लिक्स,WhatsApp क्लिक्स,एकूण संपर्क लीड्स,नोंदणी तारीख\n")

        val regDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

        // Sort businesses by total leads descending
        val sortedBusinesses = businesses.sortedByDescending { biz ->
            if (isAllTime) {
                val bClicks = clicks.filter { it.businessId == biz.id }
                if (bClicks.isNotEmpty()) bClicks.size else biz.totalClicks
            } else {
                clicks.count { it.businessId == biz.id }
            }
        }

        sortedBusinesses.forEachIndexed { index, biz ->
            val srNo = (index + 1).toString()
            val bName = escapeCsv(biz.businessName)
            val cat = escapeCsv(biz.category)
            val owner = escapeCsv(biz.ownerName.ifBlank { "-" })
            val contact = formatPhoneNumber(biz.contactNumber)
            val wa = formatPhoneNumber(biz.whatsappNumber.ifBlank { biz.contactNumber })
            val addr = escapeCsv(biz.address.ifBlank { "-" })

            val (bizCalls, bizWa) = if (isAllTime) {
                val bClicks = clicks.filter { it.businessId == biz.id }
                if (bClicks.isNotEmpty()) {
                    Pair(bClicks.count { it.clickType == "CALL" }, bClicks.count { it.clickType == "WHATSAPP" })
                } else {
                    Pair(biz.callClicks, biz.whatsappClicks)
                }
            } else {
                val bClicks = clicks.filter { it.businessId == biz.id }
                Pair(bClicks.count { it.clickType == "CALL" }, bClicks.count { it.clickType == "WHATSAPP" })
            }
            val bizTotal = bizCalls + bizWa

            val regDate = try {
                escapeCsv(regDateFormat.format(Date(biz.timestamp)))
            } catch (_: Exception) {
                "\"-\""
            }

            sb.append("$srNo,$bName,$cat,$owner,$contact,$wa,$addr,$bizCalls,$bizWa,$bizTotal,$regDate\n")
        }

        sb.append("\n")

        // SECTION 2: DETAILED CLICK LOG (If clicks are available)
        sb.append(escapeCsv("--- भाग २: तपशीलवार संपर्क लॉग (Detailed Lead Activity Log) ---")).append("\n")
        if (clicks.isNotEmpty()) {
            sb.append("अ. क्र.,तारीख व वेळ,व्यवसाय / दुकानाचे नाव,संचालकाचे नाव,वर्गवारी,कृती प्रकार (Action),वापरलेला नंबर\n")

            val logDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH)
            clicks.forEachIndexed { index, click ->
                val srNo = (index + 1).toString()
                val logTime = try {
                    escapeCsv(logDateFormat.format(Date(click.timestamp)))
                } catch (_: Exception) {
                    "\"-\""
                }
                val bName = escapeCsv(click.businessName)
                val owner = escapeCsv(click.ownerName.ifBlank { "-" })
                val cat = escapeCsv(click.category.ifBlank { "-" })
                val action = if (click.clickType == "CALL") escapeCsv("📞 कॉल (Phone Call)") else escapeCsv("💬 WhatsApp चौकशी")
                val num = formatPhoneNumber(click.contactNumber.ifBlank { "-" })

                sb.append("$srNo,$logTime,$bName,$owner,$cat,$action,$num\n")
            }
        } else {
            sb.append(escapeCsv("टीप: निवडलेल्या कालावधीतील सर्व लीड्सचा सारांश वरील भाग १ मध्ये दिला आहे.")).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ").trim()
        return "\"" + clean.replace("\"", "\"\"") + "\""
    }

    private fun formatPhoneNumber(phone: String): String {
        val clean = phone.replace(Regex("[^0-9+]"), "").trim()
        return if (clean.isNotBlank()) "=\"$clean\"" else "\"-\""
    }

    fun saveAndGetUri(
        context: Context,
        monthYear: String,
        businesses: List<BusinessListing>,
        clicks: List<BusinessLeadClick>,
        dateRangeLabel: String? = null
    ): Pair<File, Uri>? {
        return try {
            val isAllTime = (monthYear.isBlank() || monthYear.equals("ALL", ignoreCase = true) || monthYear == "सर्व") && dateRangeLabel.isNullOrBlank()
            val fileName = if (!dateRangeLabel.isNullOrBlank()) {
                val cleanRange = dateRangeLabel.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").replace(Regex("_+"), "_")
                "जय_हिंद_मंडळ_व्यवसाय_अहवाल_${cleanRange}.csv"
            } else {
                val cleanMonth = if (isAllTime) "सर्व_काळ" else monthYear
                "जय_हिंद_मंडळ_व्यवसाय_अहवाल_${cleanMonth}.csv"
            }

            val csvContent = generateCsvContent(monthYear, businesses, clicks, dateRangeLabel)

            // 1. Save to app internal external files directory
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(downloadsDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)

            // 2. Also save copy to Public MediaStore Downloads folder
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
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
                        val pubFile = File(publicDir, fileName)
                        pubFile.writeText(csvContent, Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {
                // Secondary public copy error ignored
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Pair(file, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, fileUri: Uri, monthYear: String, dateRangeLabel: String? = null) {
        val periodLabel = formatMonthLabel(monthYear, dateRangeLabel)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "जय हिंद मंडळ - व्यावसायिक अहवाल ($periodLabel)")
            putExtra(
                Intent.EXTRA_TEXT,
                "जय हिंद कला, क्रीडा व सांस्कृतिक मंडळ, अर्जुनवाड\nव्यावसायिक डिरेक्टरी संपर्क व लीड्स अहवाल ($periodLabel) Excel/CSV डेटा."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "अहवाल शेअर करा"))
    }

    fun openCsvFile(context: Context, fileUri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(openIntent)
        } catch (e: Exception) {
            // No app to view CSV
        }
    }
}
