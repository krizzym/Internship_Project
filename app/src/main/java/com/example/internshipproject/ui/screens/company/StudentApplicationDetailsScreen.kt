package com.example.internshipproject.ui.screens.company

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.ApplicationStatus
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentApplicationDetailsScreen(
    applicationId: String,
    navController: NavController
) {
    //Check if this screen is loading
    LaunchedEffect(Unit) {
        Log.d("ScreenDebug", "✅ StudentApplicationDetailsScreen loaded with ID: $applicationId")
    }

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

    // Original values from database
    var originalStatus by remember { mutableStateOf(ApplicationStatus.PENDING) }
    var originalNotes by remember { mutableStateOf("") }

    // Current/working values
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
            Log.d("DataLoading", "🔄 Loading application data...")

            val applicationDoc = FirebaseManager.firestore
                .collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .get()
                .await()

            if (applicationDoc.exists()) {
                val data = applicationDoc.data ?: throw Exception("Application data not found")

                Log.d("DataLoading", "✅ Application data loaded successfully")

                positionAppliedFor = data["internshipTitle"] as? String ?: ""
                email = data["studentEmail"] as? String ?: ""
                coverLetter = data["coverLetter"] as? String ?: ""
                appliedDate = data["appliedDate"] as? String ?: ""

                val statusString = data["status"] as? String ?: "PENDING"
                val loadedStatus = try {
                    ApplicationStatus.valueOf(statusString)
                } catch (e: Exception) {
                    ApplicationStatus.PENDING
                }

                originalStatus = loadedStatus
                currentStatus = loadedStatus
                selectedStatus = loadedStatus

                val loadedNotes = data["notesForApplicant"] as? String ?: ""
                originalNotes = loadedNotes
                notesForApplicant = loadedNotes

                Log.d("DataLoading", "📊 Status: $loadedStatus, Notes: ${if(loadedNotes.isEmpty()) "empty" else "has content"}")

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
                Log.e("DataLoading", "❌ Application document does not exist")
            }
        } catch (e: Exception) {
            Log.e("DataLoading", "❌ Error loading application: ${e.message}", e)
            errorMessage = "Failed to load application: ${e.message}"
        } finally {
            isLoading = false
            Log.d("DataLoading", "🏁 Loading complete")
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6366F1))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading application...", color = Color.Gray)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    // Log when content starts rendering
                    LaunchedEffect(Unit) {
                        Log.d("UIRender", "🎨 Rendering main content")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5))
                            .verticalScroll(rememberScrollState()) // ← CRITICAL: Make sure this is here!
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Student Information
                        SectionCard(title = "Student Information") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow(label = "Name", value = studentName)
                                InfoRow(label = "Email", value = email)
                                InfoRow(label = "Position Applied", value = positionAppliedFor)
                                InfoRow(label = "Location", value = location)
                            }
                        }

                        // Academic Information
                        SectionCard(title = "Academic Information") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow(label = "School", value = school)
                                InfoRow(label = "Course", value = course)
                                InfoRow(label = "Year Level", value = yearLevel)
                            }
                        }

                        // Skills and Preferences
                        SectionCard(title = "Skills & Preferences") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow(label = "Skills", value = skills)
                                InfoRow(
                                    label = "Internship Types",
                                    value = internshipTypes.joinToString(", ")
                                )
                            }
                        }

                        // Cover Letter
                        SectionCard(title = "Cover Letter") {
                            Text(
                                text = coverLetter.ifEmpty { "No cover letter provided" },
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF1F2937)
                            )
                        }

                        // Resume Section
                        SectionCard(title = "Resume") {
                            if (resumeBase64 != null && resumeFileName != null) {
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            "Resume viewing functionality",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Resume ($resumeFileName)")
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

                        // Application Timeline
                        SectionCard(title = "Application Timeline") {
                            InfoRow(label = "Applied", value = appliedDate)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Check if we reach this point
                        LaunchedEffect(Unit) {
                            Log.d("UIRender", "🎯 Reached Company Actions section")
                        }


                        // COMPANY ACTIONS SECTION WITH CANCEL & CONFIRM BUTTONS
                        SectionCard(title = "Company Actions") {
                            // Log when Company Actions card renders
                            LaunchedEffect(Unit) {
                                Log.d("UIRender", "📋 Company Actions card is rendering")
                            }

                            Column {
                                // Application Status Dropdown
                                Text(
                                    text = "Application Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = showStatusDropdown,
                                    onExpandedChange = {
                                        showStatusDropdown = !showStatusDropdown
                                        Log.d("UIInteraction", "Dropdown toggled: $showStatusDropdown")
                                    }
                                ) {
                                    OutlinedTextField(
                                        value = formatStatusText(selectedStatus),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = showStatusDropdown
                                            )
                                        },
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
                                                    Log.d("UIInteraction", "Status selected: $status")
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
                                    onValueChange = {
                                        notesForApplicant = it
                                        Log.d("UIInteraction", "Notes updated, length: ${it.length}")
                                    },
                                    placeholder = {
                                        Text("Add feedback, interview notes, or remarks...")
                                    },
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

                                // 🔍 DIAGNOSTIC: Check if we reach buttons section
                                LaunchedEffect(Unit) {
                                    Log.d("UIRender", "🔘 About to render Cancel & Confirm buttons")
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF3CD).copy(alpha = 0.3f)) // Subtle yellow highlight
                                        .padding(4.dp), // Small padding for visibility
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 🔍 DIAGNOSTIC: Log when buttons compose
                                    LaunchedEffect(Unit) {
                                        Log.d("UIRender", "✅ Buttons Row is composing!")
                                    }

                                    // CANCEL BUTTON
                                    OutlinedButton(
                                        onClick = {
                                            Log.d("ButtonClick", "🔴 CANCEL button clicked")
                                            selectedStatus = originalStatus
                                            notesForApplicant = originalNotes
                                            Toast.makeText(
                                                context,
                                                "Changes discarded",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF6366F1)
                                        ),
                                        enabled = !isUpdating
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.Bold)
                                    }

                                    // CONFIRM BUTTON
                                    Button(
                                        onClick = {
                                            Log.d("ButtonClick", "🟢 CONFIRM button clicked")
                                            scope.launch {
                                                isUpdating = true
                                                try {
                                                    Log.d("FirebaseUpdate", "📤 Updating Firebase...")

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

                                                    Log.d("FirebaseUpdate", "✅ Update successful")

                                                    originalStatus = selectedStatus
                                                    originalNotes = notesForApplicant
                                                    currentStatus = selectedStatus

                                                    Toast.makeText(
                                                        context,
                                                        "Application updated successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    navController.popBackStack()
                                                } catch (e: Exception) {
                                                    Log.e("FirebaseUpdate", "❌ Update failed: ${e.message}", e)
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
                                            Text("Confirm", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // ═══════ END OF BUTTONS ═══════

                                // 🔍 DIAGNOSTIC: Confirm buttons rendered
                                LaunchedEffect(Unit) {
                                    Log.d("UIRender", "✅ Buttons Row completed")
                                }
                            }
                        }
                        // ═══════ END OF COMPANY ACTIONS SECTION ═══════

                        // Add extra bottom padding for scrolling
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value.ifEmpty { "Not specified" },
            fontSize = 14.sp,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatStatusText(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Under Review"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
    }
}