// StudentApplicationDetailsScreen.kt
package com.example.internshipproject.ui.screens.company

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.StudentProfile
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentApplicationDetailsScreen(
    applicationId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Application details
    var studentName by remember { mutableStateOf("") }
    var positionAppliedFor by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var yearLevel by remember { mutableStateOf("") }
    var internshipTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var skills by remember { mutableStateOf("") }
    var coverLetter by remember { mutableStateOf("") }
    var appliedDate by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf(ApplicationStatus.PENDING) }
    var notesForApplicant by remember { mutableStateOf("") }

    // Resume details
    var resumeBase64 by remember { mutableStateOf<String?>(null) }
    var resumeFileName by remember { mutableStateOf<String?>(null) }
    var resumeMimeType by remember { mutableStateOf<String?>(null) }

    var selectedStatus by remember { mutableStateOf(ApplicationStatus.PENDING) }
    var isUpdating by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Load application data
    LaunchedEffect(applicationId) {
        try {
            isLoading = true
            val applicationDoc = FirebaseManager.firestore
                .collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .get()
                .await()

            if (applicationDoc.exists()) {
                val data = applicationDoc.data ?: throw Exception("Application data not found")

                // Get basic application info
                positionAppliedFor = data["internshipTitle"] as? String ?: ""
                email = data["studentEmail"] as? String ?: ""
                coverLetter = data["coverLetter"] as? String ?: ""
                appliedDate = data["appliedDate"] as? String ?: ""

                // Get status
                val statusString = data["status"] as? String ?: "PENDING"
                currentStatus = try {
                    ApplicationStatus.valueOf(statusString)
                } catch (e: Exception) {
                    ApplicationStatus.PENDING
                }
                selectedStatus = currentStatus

                // Get notes
                notesForApplicant = data["notesForApplicant"] as? String ?: ""

                // Get resume data
                resumeBase64 = data["resumeBase64"] as? String
                resumeFileName = data["resumeFileName"] as? String
                resumeMimeType = data["resumeMimeType"] as? String

                // Fetch student details
                val studentId = data["studentId"] as? String
                if (studentId != null) {
                    val studentDoc = FirebaseManager.firestore
                        .collection(FirebaseManager.Collections.STUDENTS)
                        .document(studentId)
                        .get()
                        .await()

                    if (studentDoc.exists()) {
                        val studentData = studentDoc.data ?: emptyMap()

                        val firstName = studentData["firstName"] as? String ?: ""
                        val middleName = studentData["middleName"] as? String
                        val lastName = studentData["lastName"] as? String ?: ""

                        studentName = if (middleName.isNullOrBlank()) {
                            "$firstName $lastName"
                        } else {
                            "$firstName $middleName $lastName"
                        }

                        school = studentData["school"] as? String ?: ""
                        course = studentData["course"] as? String ?: ""
                        yearLevel = studentData["yearLevel"] as? String ?: ""

                        val city = studentData["city"] as? String ?: ""
                        val barangay = studentData["barangay"] as? String ?: ""
                        location = if (barangay.isNotBlank() && city.isNotBlank()) {
                            "$barangay, $city"
                        } else if (city.isNotBlank()) {
                            city
                        } else {
                            "Not specified"
                        }

                        @Suppress("UNCHECKED_CAST")
                        internshipTypes = studentData["internshipTypes"] as? List<String> ?: emptyList()
                        skills = studentData["skills"] as? String ?: ""
                    }
                }
            } else {
                errorMessage = "Application not found"
            }
        } catch (e: Exception) {
            Log.e("StudentAppDetails", "Error loading application: ${e.message}", e)
            errorMessage = "Failed to load application: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Status Badge
                        StatusBadge(status = currentStatus)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Student Name and Position
                        Text(
                            text = studentName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Applied for: $positionAppliedFor",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Contact Information
                        SectionCard(title = "Contact Information") {
                            InfoRow(label = "Email", value = email)
                            InfoRow(label = "Location", value = location)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Educational Background
                        SectionCard(title = "Educational Background") {
                            InfoRow(label = "School/University", value = school)
                            InfoRow(label = "Course/Program", value = course)
                            InfoRow(label = "Year Level", value = yearLevel)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Internship Preferences
                        SectionCard(title = "Internship Preferences") {
                            Text(
                                text = "Preferred Types:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            internshipTypes.forEach { type ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF6366F1).copy(alpha = 0.1f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = type,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            color = Color(0xFF6366F1)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Skills
                        SectionCard(title = "Skills") {
                            Text(
                                text = skills.ifBlank { "No skills listed" },
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cover Letter
                        SectionCard(title = "Cover Letter") {
                            Text(
                                text = coverLetter.ifBlank { "No cover letter provided" },
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resume Section (View Resume Button)
                        SectionCard(title = "Resume") {
                            if (!resumeBase64.isNullOrEmpty()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            downloadResume(
                                                context = context,
                                                base64String = resumeBase64!!,
                                                fileName = resumeFileName ?: "resume.pdf",
                                                mimeType = resumeMimeType ?: "application/pdf"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Resume (${resumeFileName ?: "resume.pdf"})")
                                }
                            } else {
                                Text(
                                    text = "No resume uploaded",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Application Timeline
                        SectionCard(title = "Application Timeline") {
                            InfoRow(label = "Applied", value = appliedDate)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Update Application Status
                        SectionCard(title = "Update Application Status") {
                            Text(
                                text = "Status *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = showStatusDropdown,
                                onExpandedChange = { showStatusDropdown = !showStatusDropdown }
                            ) {
                                OutlinedTextField(
                                    value = formatStatusForDisplay(selectedStatus),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors()
                                )

                                ExposedDropdownMenu(
                                    expanded = showStatusDropdown,
                                    onDismissRequest = { showStatusDropdown = false }
                                ) {
                                    ApplicationStatus.values().forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(formatStatusForDisplay(status)) },
                                            onClick = {
                                                selectedStatus = status
                                                showStatusDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Notes for Applicant
                        SectionCard(title = "Notes for Applicant") {
                            OutlinedTextField(
                                value = notesForApplicant,
                                onValueChange = { notesForApplicant = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                placeholder = { Text("Add feedback or notes for the applicant...") },
                                minLines = 4,
                                maxLines = 8
                            )
                            Text(
                                text = "These notes will be visible to the student",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isUpdating = true
                                        try {
                                            FirebaseManager.firestore
                                                .collection(FirebaseManager.Collections.APPLICATIONS)
                                                .document(applicationId)
                                                .update(
                                                    mapOf(
                                                        "status" to selectedStatus.name,
                                                        "notesForApplicant" to notesForApplicant,
                                                        "lastUpdated" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                                .await()

                                            currentStatus = selectedStatus
                                            Toast.makeText(
                                                context,
                                                "Application status updated successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            Log.e("StudentAppDetails", "Error updating status: ${e.message}", e)
                                            Toast.makeText(
                                                context,
                                                "Failed to update status: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            isUpdating = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                )
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Update Status")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ApplicationStatus) {
    val (backgroundColor, textColor) = when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFA16207)
        ApplicationStatus.REVIEWED -> Color(0xFFDDD6FE) to Color(0xFF6B21A8)
        ApplicationStatus.SHORTLISTED -> Color(0xFFBFDBFE) to Color(0xFF1E40AF)
        ApplicationStatus.ACCEPTED -> Color(0xFFBBF7D0) to Color(0xFF166534)
        ApplicationStatus.REJECTED -> Color(0xFFFECDD3) to Color(0xFF9F1239)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = formatStatusForDisplay(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value.ifBlank { "Not specified" },
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

fun formatStatusForDisplay(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending - Awaiting review"
        ApplicationStatus.REVIEWED -> "Reviewed - Applicant has been reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted - Candidate is shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted - Offer extended"
        ApplicationStatus.REJECTED -> "Rejected - Not moving forward"
    }
}

suspend fun downloadResume(
    context: Context,
    base64String: String,
    fileName: String,
    mimeType: String
) {
    try {
        // Decode Base64 string to byte array
        val pdfBytes = Base64.decode(base64String, Base64.DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(pdfBytes)
                }
                Toast.makeText(
                    context,
                    "Resume downloaded to Downloads folder",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                outputStream.write(pdfBytes)
            }

            Toast.makeText(
                context,
                "Resume downloaded to ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        Log.e("DownloadResume", "Error downloading resume: ${e.message}", e)
        Toast.makeText(
            context,
            "Failed to download resume: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}