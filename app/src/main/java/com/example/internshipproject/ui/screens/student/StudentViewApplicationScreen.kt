package com.example.internshipproject.ui.screens.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.theme.*
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentViewApplicationScreen(
    applicationId: String,
    navController: NavController
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Application details
    var studentName by remember { mutableStateOf("") }
    var positionAppliedFor by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
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
    var applicationTimeline by remember { mutableStateOf<List<TimelineEvent>>(emptyList()) }

    // Resume details
    var resumeBase64 by remember { mutableStateOf<String?>(null) }
    var resumeFileName by remember { mutableStateOf<String?>(null) }
    var resumeMimeType by remember { mutableStateOf<String?>(null) }

    // Real-time listener
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Load and observe application data in real-time
    LaunchedEffect(applicationId) {
        try {
            isLoading = true

            // Set up real-time listener for status updates
            listenerRegistration = FirebaseManager.firestore
                .collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("StudentViewApp", "Listen failed: ${error.message}", error)
                        errorMessage = "Failed to load application updates"
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data ?: throw Exception("Application data not found")

                            // Get basic application info
                            positionAppliedFor = data["internshipTitle"] as? String ?: ""
                            companyName = data["companyName"] as? String ?: ""
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

                            // Get notes
                            notesForApplicant = data["companyNotes"] as? String ?: ""

                            // Get resume data
                            resumeBase64 = data["resumeBase64"] as? String
                            resumeFileName = data["resumeFileName"] as? String
                            resumeMimeType = data["resumeMimeType"] as? String

                            // Build timeline
                            applicationTimeline = buildTimelineFromStatus(currentStatus, appliedDate)

                            // Fetch student details (only once)
                            if (studentName.isEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
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
                                    } catch (e: Exception) {
                                        Log.e("StudentViewApp", "Error loading student: ${e.message}", e)
                                    }
                                }
                            }

                            isLoading = false
                        } catch (e: Exception) {
                            Log.e("StudentViewApp", "Error processing application: ${e.message}", e)
                            errorMessage = "Failed to process application: ${e.message}"
                            isLoading = false
                        }
                    } else {
                        errorMessage = "Application not found"
                        isLoading = false
                    }
                }
        } catch (e: Exception) {
            Log.e("StudentViewApp", "Error setting up listener: ${e.message}", e)
            errorMessage = "Failed to load application: ${e.message}"
            isLoading = false
        }
    }

    // Clean up listener
    DisposableEffect(applicationId) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Application Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("View your application status", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
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
                    containerColor = PrimaryDeepBlueButton,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradientBrush)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryDeepBlueButton
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            errorMessage ?: "Unknown error",
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status Card with Timeline
                        item {
                            StatusTimelineCard(
                                currentStatus = currentStatus,
                                timeline = applicationTimeline,
                                appliedDate = appliedDate
                            )
                        }

                        // Company Notes (if any)
                        if (notesForApplicant.isNotBlank()) {
                            item {
                                InfoCard(title = "Notes from Company") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFFFFF9C4),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Note",
                                            tint = Color(0xFFF57C00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            notesForApplicant,
                                            fontSize = 14.sp,
                                            color = Color(0xFF5D4037),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Position & Company Card
                        item {
                            InfoCard(title = "Position Applied For") {
                                Text(
                                    positionAppliedFor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    companyName,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Student Information
                        item {
                            InfoCard(title = "Student Information") {
                                DetailRow("Name", studentName)
                                DetailRow("Email", email)
                                DetailRow("Location", location)
                            }
                        }

                        // Educational Background
                        item {
                            InfoCard(title = "Educational Background") {
                                DetailRow("School", school)
                                DetailRow("Course", course)
                                DetailRow("Year Level", yearLevel)
                            }
                        }

                        // Internship Preferences
                        if (internshipTypes.isNotEmpty()) {
                            item {
                                InfoCard(title = "Internship Preferences") {
                                    internshipTypes.forEachIndexed { index, type ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PrimaryDeepBlueButton.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    type,
                                                    fontSize = 13.sp,
                                                    color = PrimaryDeepBlueButton,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Skills
                        if (skills.isNotBlank()) {
                            item {
                                InfoCard(title = "Skills") {
                                    Text(
                                        skills,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // Cover Letter
                        item {
                            InfoCard(title = "Cover Letter") {
                                Text(
                                    coverLetter,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Resume
                        if (!resumeBase64.isNullOrBlank() && !resumeFileName.isNullOrBlank()) {
                            item {
                                InfoCard(title = "Resume") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                resumeFileName ?: "Resume.pdf",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                "Attached file",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewResume(
                                                    context,
                                                    resumeBase64 ?: "",
                                                    resumeFileName ?: "Resume.pdf",
                                                    resumeMimeType
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Visibility,
                                                contentDescription = "View Resume",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTimelineCard(
    currentStatus: ApplicationStatus,
    timeline: List<TimelineEvent>,
    appliedDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Application Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = getApplicationStatusColor(currentStatus)
                ) {
                    Text(
                        currentStatus.name,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline
            timeline.forEach { event ->
                TimelineItem(
                    event = event,
                    isLast = event == timeline.last()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Applied on $appliedDate",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TimelineItem(event: TimelineEvent, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (event.isCompleted) PrimaryDeepBlueButton else Color.LightGray,
                modifier = Modifier.size(16.dp)
            ) {}
            if (!isLast) {
                Divider(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp),
                    color = if (event.isCompleted) PrimaryDeepBlueButton else Color.LightGray
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                event.title,
                fontSize = 14.sp,
                fontWeight = if (event.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (event.isCompleted) TextPrimary else TextSecondary
            )
            Text(
                event.description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// Helper data class for timeline
data class TimelineEvent(
    val title: String,
    val description: String,
    val isCompleted: Boolean
)

// Helper function to build timeline based on status
fun buildTimelineFromStatus(status: ApplicationStatus, appliedDate: String): List<TimelineEvent> {
    val allSteps = listOf(
        TimelineEvent(
            "Submitted",
            "Application received",
            isCompleted = true
        ),
        TimelineEvent(
            "Under Review",
            "Company is reviewing your application",
            isCompleted = status.ordinal >= ApplicationStatus.REVIEWED.ordinal
        ),
        TimelineEvent(
            "Shortlisted",
            "You've been shortlisted for interview",
            isCompleted = status.ordinal >= ApplicationStatus.SHORTLISTED.ordinal
        ),
        TimelineEvent(
            when (status) {
                ApplicationStatus.ACCEPTED -> "Accepted"
                ApplicationStatus.REJECTED -> "Not Selected"
                else -> "Final Decision"
            },
            when (status) {
                ApplicationStatus.ACCEPTED -> "Congratulations! You've been accepted"
                ApplicationStatus.REJECTED -> "Application was not selected"
                else -> "Awaiting final decision"
            },
            isCompleted = status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED
        )
    )

    return allSteps
}

// Helper function to get status color
private fun getApplicationStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFBE7B0B)
        ApplicationStatus.REVIEWED -> Color(0xFF0067AD)
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }
}

// Helper function to view resume
private fun viewResume(
    context: Context,
    base64String: String,
    fileName: String,
    mimeType: String?
) {
    try {
        // Decode Base64
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

        // Create temp file
        val file = File(context.cacheDir, fileName)
        file.writeBytes(decodedBytes)

        // Get URI using FileProvider
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Open with Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open Resume"))
    } catch (e: Exception) {
        Log.e("ViewResume", "Error viewing resume: ${e.message}", e)
    }
}