package com.example.internshipproject.ui.screens.company

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.internshipproject.data.model.ApplicationStatus
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.viewmodel.CompanyApplicationsViewModel
import com.example.internshipproject.viewmodel.UpdateState

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

    // Validation states
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var hasStatusBeenTouched by remember { mutableStateOf(false) }
    var hasNotesBeenTouched by remember { mutableStateOf(false) }

    val charCount = notesText.length
    val trimmedNotesLength = notesText.trim().length
    val hasStatusChanged = selectedStatus != detailsState.application?.status
    val hasValidNotes = trimmedNotesLength >= 20
    val isFormValid = hasStatusChanged && hasValidNotes

    // Helper text should only show after interaction
    val shouldShowStatusHelper = hasStatusBeenTouched || hasAttemptedSubmit
    val shouldShowNotesHelper = hasNotesBeenTouched || hasAttemptedSubmit

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
                // Reset validation states after successful update
                hasAttemptedSubmit = false
                hasStatusBeenTouched = false
                hasNotesBeenTouched = false
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
                    charCount = charCount,
                    trimmedNotesLength = trimmedNotesLength,
                    showStatusMenu = showStatusMenu,
                    onShowStatusMenuChange = { showStatusMenu = it },
                    onStatusSelected = {
                        selectedStatus = it
                        hasStatusBeenTouched = true
                    },
                    onStatusUpdate = {
                        selectedStatus?.let { status ->
                            viewModel.updateApplicationStatus(applicationId, status)
                            showStatusMenu = false
                        }
                    },
                    onNotesTextChange = {
                        notesText = it
                        hasNotesBeenTouched = true
                    },
                    onNotesUpdate = {
                        viewModel.updateCompanyNotes(applicationId, notesText)
                        showNotesDialog = false
                    },
                    showNotesDialog = showNotesDialog,
                    onShowNotesDialogChange = { showNotesDialog = it },
                    isUpdating = updateState is UpdateState.Updating,
                    context = context,
                    isFormValid = isFormValid,
                    hasAttemptedSubmit = hasAttemptedSubmit,
                    shouldShowStatusHelper = shouldShowStatusHelper,
                    shouldShowNotesHelper = shouldShowNotesHelper,
                    hasStatusChanged = hasStatusChanged,
                    hasValidNotes = hasValidNotes,
                    // Callbacks for Cancel/confirm
                    onCancelChanges = {
                        // Reset to original values from database
                        detailsState.application?.let { app ->
                            selectedStatus = app.status
                            notesText = app.companyNotes ?: ""
                        }
                        // Reset validation states
                        hasAttemptedSubmit = false
                        hasStatusBeenTouched = false
                        hasNotesBeenTouched = false
                        Toast.makeText(context, "Changes discarded", Toast.LENGTH_SHORT).show()
                    },
                    onConfirmChanges = {
                        hasAttemptedSubmit = true

                        // Only proceed if form is valid
                        if (isFormValid) {
                            selectedStatus?.let { status ->
                                viewModel.updateApplicationStatusAndNotes(
                                    applicationId = applicationId,
                                    newStatus = status,
                                    notes = notesText.trim()
                                )
                            }
                        }
                    },
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
    charCount: Int,
    trimmedNotesLength: Int,
    showStatusMenu: Boolean,
    onShowStatusMenuChange: (Boolean) -> Unit,
    onStatusSelected: (ApplicationStatus) -> Unit,
    onStatusUpdate: () -> Unit,
    onNotesTextChange: (String) -> Unit,
    onNotesUpdate: () -> Unit,
    showNotesDialog: Boolean,
    onShowNotesDialogChange: (Boolean) -> Unit,
    isUpdating: Boolean,
    context: Context,
    isFormValid: Boolean,
    hasAttemptedSubmit: Boolean,
    shouldShowStatusHelper: Boolean,
    shouldShowNotesHelper: Boolean,
    hasStatusChanged: Boolean,
    hasValidNotes: Boolean,
    onCancelChanges: () -> Unit,
    onConfirmChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Determine when to show validation messages and their colors
    val showStatusError = shouldShowStatusHelper && !hasStatusChanged
    val showNotesError = shouldShowNotesHelper && !hasValidNotes

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

        // Application Timeline Card
        InfoCard(
            title = "Application Timeline",
            content = {
                InfoRow(label = "Applied", value = application.appliedDate)
            }
        )

        Text(
            text = "Company Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Single unified card with status, notes, and action buttons
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
                // Application Status Section
                Text(
                    text = "Application Status",
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
                        colors = OutlinedTextFieldDefaults.colors(),
                        supportingText = {
                            if (showStatusError) {
                                Text(
                                    text = "Required.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = showStatusError
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

                Spacer(modifier = Modifier.height(4.dp))

                // Notes for Applicant Section
                Text(
                    text = "Notes for Applicant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = onNotesTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text("Add feedback, interview notes, or remarks. These notes will be visible to the student.")
                    },
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(),
                    supportingText = {
                        if (shouldShowNotesHelper) {
                            Text(
                                text = "Minimum of 20 characters required.",
                                color = if (showNotesError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Gray
                                }
                            )
                        }
                    },
                    isError = showNotesError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$charCount characters (min. 20)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shouldShowNotesHelper && showNotesError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Gray
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onCancelChanges,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cancel")
                    }

                    // Confirm Button
                    Button(
                        onClick = onConfirmChanges,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating && isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm")
                        }
                    }
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Status Timeline
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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header Row with Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Surface(
                    color = currentStatus.getStatusColor(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = currentStatus.getDisplayName().uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Timeline
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                TimelineItem(
                    title = "Submitted",
                    description = "Application received",
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.PENDING.ordinal,
                    isLast = false
                )

                TimelineItem(
                    title = "Under Review",
                    description = "Company is reviewing your application",
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.REVIEWED.ordinal,
                    isLast = false
                )

                TimelineItem(
                    title = "Shortlisted",
                    description = "You've been shortlisted for interview",
                    isCompleted = currentStatus.ordinal >= ApplicationStatus.SHORTLISTED.ordinal,
                    isLast = false
                )

                TimelineItem(
                    title = "Final Decision",
                    description = when (currentStatus) {
                        ApplicationStatus.ACCEPTED -> "Congratulations! Offer extended"
                        ApplicationStatus.REJECTED -> "Application not moving forward"
                        else -> "Awaiting final decision"
                    },
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
    isCompleted: Boolean,
    isLast: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = if (isLast) 0.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circle with checkmark or empty
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            Color(0xFF5C6BC0) // Material Blue-ish color matching screenshot
                        } else {
                            Color(0xFFE0E0E0) // Light gray for incomplete
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    // Empty circle
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontSize = 15.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

// Student information
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


// Generic info card / row
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


// Utility / extension helpers
private fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.PENDING    -> "Pending"
        ApplicationStatus.REVIEWED   -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.ACCEPTED   -> "Accepted"
        ApplicationStatus.REJECTED   -> "Rejected"
    }
}

private fun ApplicationStatus.getStatusColor(): Color {
    return when (this) {
        ApplicationStatus.PENDING     -> Color(0xFFFFA726)      // Orange
        ApplicationStatus.REVIEWED    -> Color(0xFF42A5F5)      // Blue
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)      // Green
        ApplicationStatus.ACCEPTED    -> Color(0xFF4CAF50)      // Dark Green
        ApplicationStatus.REJECTED    -> Color(0xFFEF5350)      // Red
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024            -> "$bytes B"
        bytes < 1024 * 1024     -> "${bytes / 1024} KB"
        else                    -> "${bytes / (1024 * 1024)} MB"
    }
}