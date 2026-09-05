package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EmergencyBloodAlert
import com.example.ui.theme.*

@Composable
fun EmergencyBloodBanner(
    alert: EmergencyBloodAlert,
    isAdmin: Boolean,
    onResolve: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB91C1C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Emergency SOS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🚨 आणीबाणी: तातडीची रक्ताची गरज!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "कृपया तातडीने मदत करा किंवा शेअर करा",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Blood Group Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "🩸 ${alert.bloodGroup}",
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "👤 रुग्ण: ${alert.patientName}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "🏥 हॉस्पिटल: ${alert.hospital}",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📦 आवश्यक बाटल्या: ${alert.unitsNeeded} बाटल्या",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp
                    )
                    if (alert.additionalNote.isNotBlank()) {
                        Text(
                            text = "ℹ️ टीप: ${alert.additionalNote}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${alert.contactNumber}"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📞 त्वरित कॉल",
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // WhatsApp Button
                Button(
                    onClick = {
                        try {
                            val cleanNumber = alert.contactNumber.replace("+", "").replace(" ", "").trim()
                            val msg = Uri.encode("🚩 जय हिंद मंडळ: मी ${alert.patientName} यांच्यासाठी ${alert.bloodGroup} रक्तदान करण्यास तयार आहे.")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=91$cleanNumber&text=$msg"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // If Admin: Resolve Button
                if (isAdmin) {
                    IconButton(
                        onClick = { onResolve(alert.id) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Resolve",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEmergencyBloodAlertDialog(
    onDismiss: () -> Unit,
    onSend: (
        bloodGroup: String,
        patientName: String,
        hospital: String,
        unitsNeeded: String,
        contactPerson: String,
        contactNumber: String,
        additionalNote: String
    ) -> Unit
) {
    var bloodGroup by remember { mutableStateOf("O+") }
    var patientName by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var unitsNeeded by remember { mutableStateOf("1") }
    var contactPerson by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var additionalNote by remember { mutableStateOf("") }

    val bloodGroups = listOf("A+", "B+", "AB+", "O+", "A-", "B-", "AB-", "O-")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB91C1C).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "आणीबाणी रक्तदान अलर्ट 🚨",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "सर्व सदस्यांच्या मोबाईलवर सायरन नोटिफिकेशन जाईल",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Blood Group Picker
                    Text(
                        text = "आवश्यक रक्तगट निवडा *:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bloodGroups) { bg ->
                            val isSelected = bloodGroup == bg
                            FilterChip(
                                selected = isSelected,
                                onClick = { bloodGroup = bg },
                                label = { Text(bg, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFB91C1C),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Patient Name
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("रुग्णाचे नाव *") },
                        placeholder = { Text("उदा. रमेश पाटील") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Hospital Name
                    OutlinedTextField(
                        value = hospital,
                        onValueChange = { hospital = it },
                        label = { Text("हॉस्पिटलचे नाव व शहर *") },
                        placeholder = { Text("उदा. सिव्हिल हॉस्पिटल, कोल्हापूर") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Units Needed
                    OutlinedTextField(
                        value = unitsNeeded,
                        onValueChange = { unitsNeeded = it },
                        label = { Text("आवश्यक बाटल्यांची संख्या (Units) *") },
                        placeholder = { Text("उदा. 2 बाटल्या") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contact Person
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("संपर्क व्यक्तीचे नाव *") },
                        placeholder = { Text("उदा. सचिन शिंदे (नातेवाईक)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contact Mobile
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("संपर्क मोबाईल नंबर *") },
                        placeholder = { Text("उदा. 9876543210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Additional Note
                    OutlinedTextField(
                        value = additionalNote,
                        onValueChange = { additionalNote = it },
                        label = { Text("इतर माहिती / टीप (पर्यायी)") },
                        placeholder = { Text("उदा. तातडीची शस्त्रक्रिया आहे.") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                val isFormValid = patientName.isNotBlank() && hospital.isNotBlank() && contactNumber.isNotBlank()
                Button(
                    onClick = {
                        if (isFormValid) {
                            onSend(
                                bloodGroup,
                                patientName,
                                hospital,
                                unitsNeeded,
                                contactPerson,
                                contactNumber,
                                additionalNote
                            )
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "तातडीचा सायरन अलर्ट पाठवा 🚨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
