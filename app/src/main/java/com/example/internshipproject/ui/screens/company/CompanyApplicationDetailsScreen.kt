// CompanyApplicationDetailsScreen.kt
package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
        // Application Status Timeline Card
        ApplicationStatusTimelineCard(currentStatus = application.status)

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
        StudentInformationCard(application = application)

        // Educational Background Card (if available)
        application.studentProfile?.let { profile ->
            InfoCard(
                title = "Educational Background",
                content = {
                    InfoRow(label = "School/University", value = profile.school)
                    InfoRow(label = "Course/Program", value = profile.course)
                    InfoRow(label = "Year Level", value = profile.yearLevel)
                }
            )
        }

        // Internship Preferences Card (if available)
        application.studentProfile?.let { profile ->
            if (profile.internshipTypes.isNotEmpty()) {
                InfoCard(
                    title = "Internship Preferences",
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            profile.internshipTypes.forEach { type ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(text = type, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                )
            }
        }

        // Skills Card (if available)
        application.studentProfile?.let { profile ->
            if (profile.skills.isNotBlank()) {
                InfoCard(
                    title = "Skills",
                    content = {
                        Text(
                            text = profile.skills,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        // Cover Letter Card
        InfoCard(
            title = "Cover Letter",
            content = {
                Text(
                    text = application.coverLetter,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        )

        // Resume Card
        if (application.hasResume) {
            ResumeCard(
                fileName = application.resumeFileName ?: "Resume.pdf",
                fileSize = application.resumeSize ?: 0L,
                onViewClick = onResumeView
            )
        }

        // Application Timeline Card
        InfoCard(
            title = "Application Timeline",
            content = {
                InfoRow(label = "Applied", value = application.appliedDate)
            }
        )

        // COMPANY ACTIONS SECTION
        Text(
            text = "Company Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

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

                ExposedDropdownMenuBox(
                    expanded = showStatusMenu,
                    onExpandedChange = onShowStatusMenuChange
                ) {
                    OutlinedTextField(
                        value = selectedStatus.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusMenu)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { onShowStatusMenuChange(false) }
                    ) {
                        ApplicationStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = status.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onStatusSelected(status)
                                    onShowStatusMenuChange(false)
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = onStatusUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating && selectedStatus != application.status
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Update Status")
                }
            }
        }

        // Notes for Applicant Card
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
private fun ApplicationStatusTimelineCard(currentStatus: ApplicationStatus) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    color = currentStatus.getStatusColor(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = currentStatus.getDisplayName().uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Timeline
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimelineItem(
                    title = "Submitted",
                    description = "Application received",
                    isActive = true,
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.PENDING.ordinal
                )

                TimelineItem(
                    title = "Under Review",
                    description = "Company is reviewing your application",
                    isActive = currentStatus == ApplicationStatus.REVIEWED,
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.REVIEWED.ordinal
                )

                TimelineItem(
                    title = "Shortlisted",
                    description = "You've been shortlisted for interview",
                    isActive = currentStatus == ApplicationStatus.SHORTLISTED,
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.SHORTLISTED.ordinal
                )

                TimelineItem(
                    title = "Final Decision",
                    description = when (currentStatus) {
                        ApplicationStatus.ACCEPTED -> "Congratulations! Offer extended"
                        ApplicationStatus.REJECTED -> "Application not moving forward"
                        else -> "Awaiting final decision"
                    },
                    isActive = currentStatus == ApplicationStatus.ACCEPTED || currentStatus == ApplicationStatus.REJECTED,
                    isCompleted = currentStatus == ApplicationStatus.ACCEPTED || currentStatus == ApplicationStatus.REJECTED,
                    isLast = true
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    description: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isLast: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted || isActive) MaterialTheme.colorScheme.primary
                        else Color.LightGray
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted && !isActive) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else Color.LightGray
                        )
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isCompleted || isActive) Color.Black else Color.Gray
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun StudentInformationCard(application: com.example.internshipproject.data.model.Application) {
    InfoCard(
        title = "Student Information",
        content = {
            application.studentProfile?.let { profile ->
                InfoRow(label = "Name", value = profile.fullName)
                InfoRow(label = "Email", value = application.studentEmail)
                InfoRow(label = "Location", value = "${profile.barangay}, ${profile.city}")
            } ?: run {
                InfoRow(label = "Email", value = application.studentEmail)
            }
        }
    )
}

@Composable
private fun ResumeCard(
    fileName: String,
    fileSize: Long,
    onViewClick: () -> Unit
) {
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
                text = "Resume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatFileSize(fileSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Button(
                    onClick = onViewClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View")
                }
            }
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper functions
private fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
    }
}

private fun ApplicationStatus.getStatusColor(): Color {
    return when (this) {
        ApplicationStatus.PENDING -> Color(0xFFFFA726)      // Orange
        ApplicationStatus.REVIEWED -> Color(0xFF42A5F5)     // Blue
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)  // Green
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)     // Dark Green
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)     // Red
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun viewResumeFromBase64(context: Context, base64String: String, fileName: String) {
    try {
        val pdfBytes = Base64.decode(base64String, Base64.DEFAULT)
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { fos ->
            fos.write(pdfBytes)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("ResumeView", "Error viewing resume: ${e.message}")
        Toast.makeText(context, "Error opening resume", Toast.LENGTH_SHORT).show()
    }
}