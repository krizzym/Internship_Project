//CompanyApplicationDetailsScreen.kt
package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.viewmodel.CompanyApplicationsViewModel
import com.example.internshipproject.viewmodel.UpdateState
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyApplicationDetailsScreen(
    navController: NavController,
    applicationId: String,
    viewModel: CompanyApplicationsViewModel = viewModel()
) {
    val context = LocalContext.current
    val detailsState by viewModel.detailsState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // State for dialogs and UI
    var showStatusMenu by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ApplicationStatus?>(null) }

    // Load application on first composition
    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
    }

    // Update notes text and selected status when application loads
    LaunchedEffect(detailsState.application) {
        detailsState.application?.let {
            notesText = it.companyNotes ?: ""
            selectedStatus = it.status
        }
    }

    // Handle update state notifications
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.Success -> {
                Toast.makeText(
                    context,
                    (updateState as UpdateState.Success).message,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                Toast.makeText(
                    context,
                    (updateState as UpdateState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    // Clean up when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearDetailsState()
            viewModel.resetUpdateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Application Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when {
            detailsState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            detailsState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = detailsState.errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadApplicationDetails(applicationId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            detailsState.application != null -> {
                CompanyApplicationDetailsContent(
                    application = detailsState.application!!,
                    selectedStatus = selectedStatus ?: detailsState.application!!.status,
                    notesText = notesText,
                    showStatusMenu = showStatusMenu,
                    onShowStatusMenuChange = { showStatusMenu = it },
                    onStatusSelected = { selectedStatus = it },
                    onStatusUpdate = {
                        selectedStatus?.let { status ->
                            viewModel.updateApplicationStatus(applicationId, status)
                            showStatusMenu = false
                        }
                    },
                    onNotesTextChange = { notesText = it },
                    onNotesUpdate = {
                        viewModel.updateCompanyNotes(applicationId, notesText)
                        showNotesDialog = false
                    },
                    onResumeView = {
                        viewResumeFromBase64(
                            context = context,
                            base64String = detailsState.application!!.resumeBase64 ?: "",
                            fileName = detailsState.application!!.resumeFileName ?: "resume.pdf"
                        )
                    },
                    showNotesDialog = showNotesDialog,
                    onShowNotesDialogChange = { showNotesDialog = it },
                    isUpdating = updateState is UpdateState.Updating,
                    context = context,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyApplicationDetailsContent(
    application: com.example.internshipproject.data.model.Application,
    selectedStatus: ApplicationStatus,
    notesText: String,
    showStatusMenu: Boolean,
    onShowStatusMenuChange: (Boolean) -> Unit,
    onStatusSelected: (ApplicationStatus) -> Unit,
    onStatusUpdate: () -> Unit,
    onNotesTextChange: (String) -> Unit,
    onNotesUpdate: () -> Unit,
    onResumeView: () -> Unit,
    showNotesDialog: Boolean,
    onShowNotesDialogChange: (Boolean) -> Unit,
    isUpdating: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Position Applied For Card
        InfoCard(
            title = "Position Applied For",
            content = {
                Text(
                    text = application.internshipTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = application.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        )

        // Student Information Card
        InfoCard(
            title = "Student Information",
            content = {
                application.studentProfile?.let { profile ->
                    InfoRow(label = "Name", value = profile.fullName)
                    InfoRow(label = "Email", value = profile.email)
                    InfoRow(label = "Location", value = "${profile.barangay}, ${profile.city}")
                }
            }
        )

        // Educational Background Card
        InfoCard(
            title = "Educational Background",
            content = {
                application.studentProfile?.let { profile ->
                    InfoRow(label = "School/University", value = profile.school)
                    InfoRow(label = "Course/Program", value = profile.course)
                    InfoRow(label = "Year Level", value = profile.yearLevel)
                }
            }
        )

        // Cover Letter Card
        InfoCard(
            title = "Cover Letter",
            content = {
                Text(
                    text = application.coverLetter.ifBlank { "No cover letter provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        )

        // Resume Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Resume/CV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (application.hasResume) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = application.resumeFileName ?: "resume.pdf",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatFileSize(application.resumeSize ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = onResumeView,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View")
                        }
                    }
                } else {
                    Text(
                        text = "No resume uploaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Update Application Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Update Application Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Status:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Surface(
                        color = selectedStatus.getStatusColor(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = selectedStatus.getDisplayName().uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { onShowStatusMenuChange(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Status")
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { onShowStatusMenuChange(false) }
                    ) {
                        ApplicationStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = status.getStatusColor(),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                        Text(status.getDisplayName())
                                    }
                                },
                                onClick = {
                                    onStatusSelected(status)
                                }
                            )
                        }
                    }
                }

                if (selectedStatus != application.status) {
                    Button(
                        onClick = onStatusUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating...")
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update Status")
                        }
                    }
                }
            }
        }

        // Company Notes Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes for Applicant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = { onShowNotesDialogChange(true) }) {
                        Icon(
                            imageVector = if (application.companyNotes.isNullOrEmpty())
                                Icons.Filled.Add
                            else
                                Icons.Filled.Edit,
                            contentDescription = "Edit Notes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (application.companyNotes.isNullOrEmpty()) {
                    Text(
                        text = "No notes yet. Click + to add notes about this applicant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = application.companyNotes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Notes Dialog
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = {
                onNotesTextChange(application.companyNotes ?: "")
                onShowNotesDialogChange(false)
            },
            title = {
                Text(
                    "Company Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add feedback, interview notes, or remarks. These notes will be visible to the student.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = onNotesTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Add feedback or notes for the applicant...") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onNotesUpdate,
                    enabled = !isUpdating && notesText != application.companyNotes
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Save Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onNotesTextChange(application.companyNotes ?: "")
                    onShowNotesDialogChange(false)
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Extension functions for ApplicationStatus
private fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.REVIEWED -> "Under Review"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
    }
}

private fun ApplicationStatus.getStatusColor(): Color {
    return when (this) {
        ApplicationStatus.PENDING -> Color(0xFFFFA726)
        ApplicationStatus.REVIEWED -> Color(0xFF42A5F5)
        ApplicationStatus.SHORTLISTED -> Color(0xFF9C27B0)
        ApplicationStatus.ACCEPTED -> Color(0xFF66BB6A)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }
}

// Helper function to format file size
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

// Helper function to view resume from Base64
private fun viewResumeFromBase64(
    context: Context,
    base64String: String,
    fileName: String
) {
    try {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        val mimeType = when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
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