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
import androidx.navigation.NavController
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
    navController: NavController
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
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                        Button(onClick = {
                            navController.popBackStack()
                        }) {
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

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoRow(label = "Skills", value = skills.ifBlank { "Not specified" })
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ✅ MOVED: Cover Letter Section (now before Resume)
                        SectionCard(title = "Cover Letter") {
                            Text(
                                text = coverLetter.ifBlank { "No cover letter provided" },
                                fontSize = 14.sp,
                                color = if (coverLetter.isBlank()) Color.Gray else Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resume Section (now after Cover Letter)
                        SectionCard(title = "Resume") {
                            if (resumeBase64 != null) {
                                Button(
                                    onClick = {
                                        resumeBase64?.let { base64 ->
                                            openResumeFromBase64(
                                                context = context,
                                                base64Data = base64,
                                                fileName = resumeFileName ?: "resume.pdf",
                                                mimeType = resumeMimeType ?: "application/pdf"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Resume (${resumeFileName ?: "resume.pdf"})")
                                }
                            } else {
                                Text(
                                    text = "No resume attached",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(8.dp)
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
                            Column {
                                Text(
                                    text = "Status *",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Status Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = showStatusDropdown,
                                    onExpandedChange = { showStatusDropdown = !showStatusDropdown }
                                ) {
                                    OutlinedTextField(
                                        value = formatStatusText(selectedStatus),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6366F1),
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showStatusDropdown,
                                        onDismissRequest = { showStatusDropdown = false }
                                    ) {
                                        ApplicationStatus.values().forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(formatStatusText(status)) },
                                                onClick = {
                                                    selectedStatus = status
                                                    showStatusDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Notes for Applicant
                                Text(
                                    text = "Notes for Applicant",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = notesForApplicant,
                                    onValueChange = { notesForApplicant = it },
                                    placeholder = { Text("Add feedback or notes for the applicant...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    maxLines = 5,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )

                                Text(
                                    text = "These notes will be visible to the student",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            navController.popBackStack()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF6366F1)
                                        )
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

                                                    navController.popBackStack()
                                                } catch (e: Exception) {
                                                    Log.e("StudentAppDetails", "Error updating: ${e.message}", e)
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to update: ${e.message}",
                                                        Toast.LENGTH_SHORT
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
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Helper composables
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun StatusBadge(status: ApplicationStatus) {
    val (backgroundColor, textColor) = when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFF59E0B)
        ApplicationStatus.REVIEWED -> Color(0xFFDDD6FE) to Color(0xFF3B82F6)
        ApplicationStatus.SHORTLISTED -> Color(0xFFEDE9FE) to Color(0xFF8B5CF6)
        ApplicationStatus.ACCEPTED -> Color(0xFFD1FAE5) to Color(0xFF10B981)
        ApplicationStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFE53935)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = formatStatusText(status),
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun formatStatusText(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending - Application submitted"
        ApplicationStatus.REVIEWED -> "Reviewed - Applicant has been reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted - Applicant has been shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted - Applicant has been accepted"
        ApplicationStatus.REJECTED -> "Rejected - Application rejected"
    }
}

private fun openResumeFromBase64(
    context: Context,
    base64Data: String,
    fileName: String,
    mimeType: String
) {
    try {
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                    outputStream.write(decodedBytes)
                }

                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Open Resume"))
            }
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            FileOutputStream(file).use { it.write(decodedBytes) }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Open Resume"))
        }
    } catch (e: Exception) {
        Log.e("OpenResume", "Error opening resume: ${e.message}", e)
        Toast.makeText(context, "Failed to open resume: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
