package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.viewmodel.ApplicationDetailViewModel
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
                                openResume(context, app)
                            }
                        }
                    )
                }
            }
        }
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
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = posting.workType,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Filter by Status:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All chip
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = {
                    Text("All (${statusCounts.values.sum()})")
                }
            )

            // Status chips
            ApplicationStatus.values().forEach { status ->
                val count = statusCounts[status] ?: 0
                if (count > 0) {
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = {
                            Text("${status.name} ($count)")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getStatusColor(status).copy(alpha = 0.3f),
                            selectedLabelColor = getStatusColor(status)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: Application,
    onStatusChange: (ApplicationStatus) -> Unit,
    onViewResume: (Application) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.studentEmail,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Applied: ${application.appliedDate}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = getStatusColor(application.status).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = application.status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(application.status)
                    )
                }
            }

            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Cover Letter
                if (application.coverLetter.isNotEmpty()) {
                    Text(
                        text = "Cover Letter:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = application.coverLetter,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View Resume button
                    OutlinedButton(
                        onClick = { onViewResume(application) },
                        modifier = Modifier.weight(1f),
                        enabled = application.hasResume
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (application.hasResume) "View Resume" else "No Resume",
                            fontSize = 12.sp
                        )
                    }

                    // Change Status button
                    Button(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EA)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update Status", fontSize = 12.sp)
                    }
                }
            }
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
private fun StatusChangeDialog(
    currentStatus: ApplicationStatus,
    onDismiss: () -> Unit,
    onConfirm: (ApplicationStatus) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Application Status") },
        text = {
            Column {
                Text("Select new status:")
                Spacer(modifier = Modifier.height(16.dp))

                ApplicationStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status.name,
                            color = getStatusColor(status),
                            fontWeight = if (selectedStatus == status) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus) },
                enabled = selectedStatus != currentStatus
            ) {
                Text("Update")
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
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF57C00)
        ApplicationStatus.REVIEWED -> Color(0xFF1976D2)
        ApplicationStatus.SHORTLISTED -> Color(0xFF7B1FA2)
        ApplicationStatus.ACCEPTED -> Color(0xFF388E3C)
        ApplicationStatus.REJECTED -> Color(0xFFD32F2F)
    }
}

private fun openResume(context: Context, application: Application) {
    try {
        if (application.resumeBase64.isNullOrEmpty()) {
            // Show toast or error message
            return
        }

        // Decode Base64 to bytes
        val bytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)

        // Create temp file
        val fileName = application.resumeFileName ?: "resume.pdf"
        val file = File(context.cacheDir, fileName)
        file.writeBytes(bytes)

        // Get URI using FileProvider
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Open with intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, application.resumeMimeType ?: "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(Intent.createChooser(intent, "Open Resume"))

    } catch (e: Exception) {
        e.printStackTrace()
        // Show error toast
    }
}
