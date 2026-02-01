package com.example.internshipproject.ui.screens.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.ApplicationRepository
import com.example.internshipproject.data.repository.InternshipRepository
import kotlinx.coroutines.launch

// ViewModel
class ApplicationDetailViewModel : ViewModel() {
    private val applicationRepository = ApplicationRepository()
    private val internshipRepository = InternshipRepository()

    var state by mutableStateOf(ApplicationDetailState())
        private set

    fun loadApplicationDetail(internshipId: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val userEmail = FirebaseManager.getCurrentUserEmail()
                if (userEmail == null) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "User not logged in"
                    )
                    return@launch
                }

                // Load internship
                val internship = internshipRepository.getInternshipById(internshipId)

                // Load existing application
                val application = applicationRepository.getApplicationByInternshipAndStudent(
                    internshipId = internshipId,
                    studentEmail = userEmail
                )

                state = state.copy(
                    isLoading = false,
                    internship = internship,
                    application = application
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Error loading details: ${e.message}"
                )
            }
        }
    }

    fun submitApplication(
        context: Context,
        internshipId: String,
        internshipTitle: String,
        companyName: String,
        coverLetter: String,
        resumeUri: Uri?
    ) {
        viewModelScope.launch {
            state = state.copy(isSaving = true, errorMessage = null, successMessage = null)
            try {
                val userEmail = FirebaseManager.getCurrentUserEmail()
                if (userEmail == null) {
                    state = state.copy(
                        isSaving = false,
                        errorMessage = "User not logged in"
                    )
                    return@launch
                }

                val repo = ApplicationRepository(context)
                val result = repo.submitApplication(
                    internshipId = internshipId,
                    internshipTitle = internshipTitle,
                    companyName = companyName,
                    studentEmail = userEmail,
                    coverLetter = coverLetter,
                    resumeUri = resumeUri
                )

                result.fold(
                    onSuccess = { application ->
                        state = state.copy(
                            isSaving = false,
                            successMessage = "Application submitted successfully!",
                            application = application
                        )
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to submit application"
                        )
                    }
                )
            } catch (e: Exception) {
                state = state.copy(
                    isSaving = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateNotes(notes: String) {
        state = state.copy(notes = notes)
    }

    fun clearMessages() {
        state = state.copy(successMessage = null, errorMessage = null)
    }
}

data class ApplicationDetailState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val internship: Internship? = null,
    val application: Application? = null,
    val notes: String = "",
    val successMessage: String? = null,
    val errorMessage: String? = null
)

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailsScreen(
    navController: NavController,
    internshipId: String,
    viewModel: ApplicationDetailViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(internshipId) {
        viewModel.loadApplicationDetail(internshipId)
    }

    // Handle messages
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EA),
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
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadApplicationDetail(internshipId) }) {
                            Text("Retry")
                        }
                    }
                }
                state.internship != null -> {
                    ApplicationDetailContent(
                        internship = state.internship!!,
                        application = state.application,
                        notes = state.notes,
                        onNotesChange = viewModel::updateNotes,
                        isSaving = state.isSaving,
                        onSubmit = { coverLetter, resumeUri ->
                            viewModel.submitApplication(
                                context = context,
                                internshipId = state.internship!!.id,
                                internshipTitle = state.internship!!.title,
                                companyName = state.internship!!.companyName,
                                coverLetter = coverLetter,
                                resumeUri = resumeUri
                            )
                        }
                    )
                }
            }

            // Success/Error messages
            state.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text(message, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ApplicationDetailContent(
    internship: Internship,
    application: Application?,
    notes: String,
    onNotesChange: (String) -> Unit,
    isSaving: Boolean,
    onSubmit: (coverLetter: String, resumeUri: Uri?) -> Unit
) {
    var showApplicationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Internship Header
        InternshipHeaderCard(internship)

        Spacer(modifier = Modifier.height(16.dp))

        // Application Status or Apply Button
        if (application != null) {
            ApplicationStatusCard(application)
        } else {
            Button(
                onClick = { showApplicationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EA)
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Internship Details
        InternshipDetailsSection(internship)

        Spacer(modifier = Modifier.height(16.dp))

        // Notes Section
        NotesSection(notes, onNotesChange)
    }

    // Application Dialog
    if (showApplicationDialog) {
        ApplicationDialog(
            internship = internship,
            onDismiss = { showApplicationDialog = false },
            onSubmit = { coverLetter, resumeUri ->
                onSubmit(coverLetter, resumeUri)
                showApplicationDialog = false
            }
        )
    }
}

@Composable
private fun InternshipHeaderCard(internship: Internship) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = internship.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = internship.companyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = internship.location,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ApplicationStatusCard(application: Application) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                ApplicationStatus.PENDING -> Color(0xFFFFF3E0)
                ApplicationStatus.REVIEWED -> Color(0xFFE3F2FD)
                ApplicationStatus.SHORTLISTED -> Color(0xFFF3E5F5)
                ApplicationStatus.ACCEPTED -> Color(0xFFE8F5E9)
                ApplicationStatus.REJECTED -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (application.status) {
                        ApplicationStatus.PENDING -> Icons.Default.Schedule
                        ApplicationStatus.REVIEWED -> Icons.Default.Visibility
                        ApplicationStatus.SHORTLISTED -> Icons.Default.Star
                        ApplicationStatus.ACCEPTED -> Icons.Default.CheckCircle
                        ApplicationStatus.REJECTED -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (application.status) {
                        ApplicationStatus.PENDING -> Color(0xFFF57C00)
                        ApplicationStatus.REVIEWED -> Color(0xFF1976D2)
                        ApplicationStatus.SHORTLISTED -> Color(0xFF7B1FA2)
                        ApplicationStatus.ACCEPTED -> Color(0xFF388E3C)
                        ApplicationStatus.REJECTED -> Color(0xFFD32F2F)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: ${application.status.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Applied on: ${application.appliedDate}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cover Letter:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = application.coverLetter,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun InternshipDetailsSection(internship: Internship) {
    Column {
        Text(
            text = "Internship Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6200EA)
        )
        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(Icons.Default.Work, "Work Type", internship.workType)
        InfoRow(Icons.Default.Schedule, "Duration", internship.duration)
        InfoRow(Icons.Default.AttachMoney, "Salary Range", internship.salaryRange)
        InfoRow(Icons.Default.Event, "Deadline", internship.applicationDeadline)
        InfoRow(Icons.Default.People, "Available Slots", internship.availableSlots.toString())

        Spacer(modifier = Modifier.height(16.dp))

        DetailCard("Description", internship.description)
        Spacer(modifier = Modifier.height(12.dp))
        DetailCard("Requirements", internship.requirements)
        Spacer(modifier = Modifier.height(12.dp))
        DetailCard("About Company", internship.aboutCompany)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6200EA),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun DetailCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun NotesSection(notes: String, onNotesChange: (String) -> Unit) {
    Column {
        Text(
            text = "My Notes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6200EA)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Add your notes here...") },
            maxLines = 5
        )
    }
}

@Composable
private fun ApplicationDialog(
    internship: Internship,
    onDismiss: () -> Unit,
    onSubmit: (coverLetter: String, resumeUri: Uri?) -> Unit
) {
    var coverLetter by remember { mutableStateOf("") }
    var selectedResumeUri by remember { mutableStateOf<Uri?>(null) }
    var resumeFileName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val resumeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedResumeUri = it
            // Get file name
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                resumeFileName = cursor.getString(nameIndex)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Application") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Applying for: ${internship.title}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = coverLetter,
                    onValueChange = { coverLetter = it },
                    label = { Text("Cover Letter") },
                    placeholder = { Text("Tell us why you're interested...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { resumeLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (resumeFileName != null) "Change Resume" else "Attach Resume (Optional)")
                }

                if (resumeFileName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selected: $resumeFileName",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (coverLetter.isNotBlank()) {
                        onSubmit(coverLetter, selectedResumeUri)
                    }
                },
                enabled = coverLetter.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
