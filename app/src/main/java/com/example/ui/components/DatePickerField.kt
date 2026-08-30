package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

object DatePickerHelper {
    private val marathiMonths = arrayOf(
        "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल",
        "मे", "जून", "जुलै", "ऑगस्ट",
        "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
    )

    fun showDatePicker(
        context: Context,
        initialDateStr: String = "",
        isIsoFormat: Boolean = false,
        isDob: Boolean = false,
        onDateSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        // Set sensible default for Date of Birth (e.g. year 2000) if no valid date entered
        if (isDob && initialDateStr.isBlank()) {
            calendar.set(Calendar.YEAR, 2000)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        } else if (initialDateStr.isNotBlank()) {
            // Try parsing ISO format (yyyy-MM-dd)
            try {
                val parts = initialDateStr.trim().split("-")
                if (parts.size == 3) {
                    val y = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    val d = parts[2].toIntOrNull()
                    if (y != null && m != null && d != null) {
                        calendar.set(y, m - 1, d)
                    }
                }
            } catch (_: Exception) {}
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = if (isIsoFormat) {
                    String.format(Locale.ENGLISH, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                } else {
                    val monthName = marathiMonths.getOrElse(selectedMonth) { "${selectedMonth + 1}" }
                    "$selectedDay $monthName $selectedYear"
                }
                onDateSelected(formattedDate)
            },
            year,
            month,
            day
        )

        // For DOB, don't allow future dates
        if (isDob) {
            dialog.datePicker.maxDate = System.currentTimeMillis()
        }

        dialog.show()
    }
}

@Composable
fun MandalDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "तारीख निवडा",
    placeholder: String = "कॅलेंडरमधून तारीख निवडा",
    isIsoFormat: Boolean = false,
    isDob: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 12.sp, color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = if (isDob) Icons.Default.EditCalendar else Icons.Default.CalendarMonth,
                    contentDescription = "कॅलेंडर",
                    tint = SaffronPrimary
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        DatePickerHelper.showDatePicker(
                            context = context,
                            initialDateStr = value,
                            isIsoFormat = isIsoFormat,
                            isDob = isDob,
                            onDateSelected = onValueChange
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "कॅलेंडर उघडा",
                        tint = SaffronPrimary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Invisible clickable overlay covering the whole field to trigger the picker smoothly
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    DatePickerHelper.showDatePicker(
                        context = context,
                        initialDateStr = value,
                        isIsoFormat = isIsoFormat,
                        isDob = isDob,
                        onDateSelected = onValueChange
                    )
                }
        )
    }
}
