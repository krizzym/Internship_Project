//ReviewApplicationsScreen.kt - FIXED VERSION
package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.viewmodel.ApplicationDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewApplicationsScreen(
    navController: NavController,
    postingId: String,
    viewModel: ApplicationDetailViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    LaunchedEffect(postingId) {
        viewModel.loadData(postingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Applications") },
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
                    ErrorView(
                        message = state.errorMessage ?: "",
                        onRetry = { viewModel.refreshData(postingId) }
                    )
                }
                state.posting != null -> {
                    ApplicationsContent(
                        viewModel = viewModel,
                        context = context
                    )
                }
            }

            // Success message
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
private fun ApplicationsContent(
    viewModel: ApplicationDetailViewModel,
    context: Context
) {
    val state = viewModel.state
    val filteredApplications = viewModel.getFilteredApplications()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Posting info header
        state.posting?.let { posting ->
            PostingInfoCard(posting = posting)
        }

        // Status filters
        StatusFilterChips(
            statusCounts = state.statusCounts,
            selectedStatus = state.selectedStatus,
            onStatusSelected = viewModel::selectStatus
        )

        // Applications list
        if (filteredApplications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (state.selectedStatus != null) {
                            "No ${state.selectedStatus!!.name.lowercase()} applications"
                        } else {
                            "No applications yet"
                        },
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApplications) { application ->
                    ApplicationCard(
                        application = application,
                        onStatusChange = { newStatus ->
                            viewModel.updateApplicationStatus(application.id, newStatus)
                        },
                        onViewResume = { app ->
                            if (app.hasResume) {
                                coroutineScope.launch {
                                    openResume(context, app)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// FIXED: Added the missing openResume function with proper error handling
private suspend fun openResume(context: Context, application: Application) {
    try {
        if (application.resumeBase64.isNullOrEmpty()) {
            Log.e("ReviewApp", "Resume data is empty or null")
            // Show error to user if needed
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)
                val fileName = application.resumeFileName ?: "resume.pdf"
                val tempFile = File(context.cacheDir, fileName)

                // Write the file
                tempFile.writeBytes(bytes)
                Log.d("ReviewApp", "Resume saved to: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")

                withContext(Dispatchers.Main) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            tempFile
                        )

                        Log.d("ReviewApp", "FileProvider URI created: $uri")

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        // This will show the app chooser (including Google Drive option)
                        val chooser = Intent.createChooser(intent, "Open Resume")
                        context.startActivity(chooser)
                        Log.d("ReviewApp", "PDF chooser opened successfully")
                    } catch (e: Exception) {
                        Log.e("ReviewApp", "Failed to open PDF: ${e.message}", e)
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                Log.e("ReviewApp", "Error decoding or saving file: ${e.message}", e)
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        Log.e("ReviewApp", "Error opening resume: ${e.message}", e)
        e.printStackTrace()
    }
}

@Composable
private fun PostingInfoCard(posting: com.example.internshipproject.data.model.Internship) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = posting.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
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
                    text = posting.location,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StatusFilterChips(
    statusCounts: Map<ApplicationStatus, Int>,
    selectedStatus: ApplicationStatus?,
    onStatusSelected: (ApplicationStatus?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All applications chip
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = {
                Text("All (${statusCounts.values.sum()})")
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF6200EA),
                selectedLabelColor = Color.White
            )
        )

        // Status-specific chips
        ApplicationStatus.entries.forEach { status ->
            val count = statusCounts[status] ?: 0
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = {
                    Text("${status.name} ($count)")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = status.color,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationCard(
    application: Application,
    onStatusChange: (ApplicationStatus) -> Unit,
    onViewResume: (Application) -> Unit
) {
    var showStatusDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with email and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Display student name from profile if available, otherwise show email
                    Text(
                        text = application.studentProfile?.let {
                            "${it.firstName} ${it.surname}"
                        } ?: application.studentEmail.substringBefore("@"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = application.studentEmail,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                StatusChip(
                    status = application.status,
                    onClick = { showStatusDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Application details from student profile (if available)
            application.studentProfile?.let { profile ->
                InfoRow(icon = Icons.Default.School, text = profile.school)
                InfoRow(icon = Icons.Default.Book, text = profile.course)
                InfoRow(icon = Icons.Default.CalendarToday, text = "Year ${profile.yearLevel}")
                InfoRow(icon = Icons.Default.LocationOn, text = profile.city)

                // Skills
                if (profile.skills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Skills:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.skills.split(",").forEach { skill ->
                            SkillChip(skill.trim())
                        }
                    }
                }
            }

            // Cover Letter
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cover Letter:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = application.coverLetter,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 3
                )
            }

            // Resume section
            if (application.hasResume) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF6200EA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = application.resumeFileName ?: "document.pdf",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatFileSize(application.resumeSize ?: 0L),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Button(
                        onClick = { onViewResume(application) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EA)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View")
                    }
                }
            }

            // Applied date
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Applied: ${application.appliedDate}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }

    // Status change dialog
    if (showStatusDialog) {
        StatusChangeDialog(
            currentStatus = application.status,
            onDismiss = { showStatusDialog = false },
            onConfirm = { newStatus ->
                onStatusChange(newStatus)
                showStatusDialog = false
            }
        )
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun SkillChip(skill: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8DEF8)
    ) {
        Text(
            text = skill,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = Color(0xFF6200EA)
        )
    }
}

@Composable
private fun StatusChip(
    status: ApplicationStatus,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = status.color.copy(alpha = 0.2f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = status.color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StatusChangeDialog(
    currentStatus: ApplicationStatus,
    onDismiss: () -> Unit,
    onConfirm: (ApplicationStatus) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Application Status") },
        text = {
            Column {
                Text(
                    text = "Select new status:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                ApplicationStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = status.color
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status.name,
                            fontSize = 16.sp,
                            color = if (selectedStatus == status) status.color else Color.DarkGray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedStatus.color
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
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
            text = message,
            color = Color.Gray,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

// Extension property for ApplicationStatus color
val ApplicationStatus.color: Color
    get() = when (this) {
        ApplicationStatus.PENDING -> Color(0xFFBE7B0B)
        ApplicationStatus.REVIEWED -> Color(0xFF0067AD)
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }