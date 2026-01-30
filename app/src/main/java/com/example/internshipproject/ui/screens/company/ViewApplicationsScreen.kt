//ViewApplicationsScreen.kt - FIXED VERSION
package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.viewmodel.ApplicationWithStudent
import com.example.internshipproject.viewmodel.ViewApplicationsState
import com.example.internshipproject.viewmodel.ViewApplicationsViewModel
import java.io.File

// Color Definitions
private val BackgroundPurple = Color(0xFFF5F3FF)
private val PurpleButton = Color(0xFF7C3AED)
private val CardWhite = Color.White
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewApplicationsScreen(
    postingId: String,
    onBack: () -> Unit,
    onReviewApplication: (String) -> Unit = {},
    viewModel: ViewApplicationsViewModel = viewModel()
) {
    val context = LocalContext.current

    // Load data when screen is first shown
    LaunchedEffect(Unit) {
        Log.d("ViewApplicationsScreen", "🎨 Screen composed with postingId: $postingId")
    }

    LaunchedEffect(postingId) {
        Log.d("ViewApplicationsScreen", "🔄 Loading data for postingId: $postingId")
        viewModel.loadData(postingId)
    }

    val state by viewModel.state.collectAsState()

    // Debug state changes
    LaunchedEffect(state) {
        Log.d("ViewApplicationsScreen", "📊 State: loading=${state.isLoading}, apps=${state.applicationsWithStudents.size}, error=${state.errorMessage}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Internship Connection Platform", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PurpleButton)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading applications...", color = TextSecondary)
                    }
                }
                state.errorMessage != null -> {
                    ErrorView(
                        message = state.errorMessage ?: "",
                        onRetry = {
                            Log.d("ViewApplicationsScreen", "🔄 Retry button clicked")
                            viewModel.loadData(postingId)
                        }
                    )
                }
                else -> {
                    ViewApplicationsContent(
                        state = state,
                        context = context,
                        onUpdateStatus = viewModel::updateApplicationStatus,
                        onReviewApplication = onReviewApplication,
                        postingId = postingId
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
private fun ViewApplicationsContent(
    state: ViewApplicationsState,
    context: Context,
    onUpdateStatus: (String, ApplicationStatus) -> Unit,
    onReviewApplication: (String) -> Unit,
    postingId: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPurple)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Posting Summary Card
        item {
            state.posting?.let { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = post.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Applications for this posting",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Company and Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = post.companyName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = post.location,
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (post.isActive) Color(0xFF10B981) else Color(0xFF9CA3AF)
                            ) {
                                Text(
                                    text = if (post.isActive) "Active" else "Inactive",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Status Cards Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImprovedStatCard(
                    title = "Pending",
                    count = state.statusCounts[ApplicationStatus.PENDING] ?: 0,
                    color = Color(0xFFF59E0B),
                    backgroundColor = Color(0xFFFEF3C7),
                    modifier = Modifier.weight(1f)
                )
                ImprovedStatCard(
                    title = "Reviewed",
                    count = state.statusCounts[ApplicationStatus.REVIEWED] ?: 0,
                    color = Color(0xFF3B82F6),
                    backgroundColor = Color(0xFFDBEAFE),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Status Cards Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImprovedStatCard(
                    title = "Shortlisted",
                    count = state.statusCounts[ApplicationStatus.SHORTLISTED] ?: 0,
                    color = Color(0xFF8B5CF6),
                    backgroundColor = Color(0xFFEDE9FE),
                    modifier = Modifier.weight(1f)
                )
                ImprovedStatCard(
                    title = "Accepted",
                    count = state.statusCounts[ApplicationStatus.ACCEPTED] ?: 0,
                    color = Color(0xFF10B981),
                    backgroundColor = Color(0xFFD1FAE5),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Applications Section Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Applications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PurpleButton.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${state.applicationsWithStudents.size} Total",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleButton,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Empty State
                    if (state.applicationsWithStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No applications yet",
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Students haven't applied to this internship posting yet",
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                // Debug info
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Posting ID: $postingId",
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Application Items
        items(state.applicationsWithStudents) { appWithStudent ->
            ApplicationCardImproved(
                applicationWithStudent = appWithStudent,
                context = context,
                onUpdateStatus = onUpdateStatus,
                onReviewApplication = onReviewApplication
            )
        }
    }
}

@Composable
private fun ApplicationCardImproved(
    applicationWithStudent: ApplicationWithStudent,
    context: Context,
    onUpdateStatus: (String, ApplicationStatus) -> Unit,
    onReviewApplication: (String) -> Unit
) {
    val application = applicationWithStudent.application
    val studentProfile = applicationWithStudent.studentProfile

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Student Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentProfile?.fullName ?: application.studentEmail,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = application.studentEmail,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = getStatusColor(application.status).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = application.status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getStatusColor(application.status),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Info
            if (studentProfile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.School,
                        text = studentProfile.course
                    )
                    InfoChip(
                        icon = Icons.Default.Star,
                        text = studentProfile.yearLevel
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = "${studentProfile.city}, ${studentProfile.barangay}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cover Letter Preview
            Text(
                text = "Cover Letter:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (application.coverLetter.length > 150) {
                    application.coverLetter.take(150) + "..."
                } else {
                    application.coverLetter
                },
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Applied Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Applied on ${application.appliedDate}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = TextSecondary.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Review Button
                Button(
                    onClick = { onReviewApplication(application.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleButton
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review", fontSize = 14.sp)
                }

                // Resume Button
                if (application.hasResume) {
                    Button(
                        onClick = {
                            openResume(context, application.id, application.resumeBase64, application.resumeFileName)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume", fontSize = 14.sp)
                    }
                }
            }

            // Status Update Dropdown
            if (application.status != ApplicationStatus.ACCEPTED && application.status != ApplicationStatus.REJECTED) {
                Spacer(modifier = Modifier.height(12.dp))

                var expanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PurpleButton
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Status", fontSize = 14.sp)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ApplicationStatus.values().filter { it != application.status }.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    onUpdateStatus(application.id, status)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PurpleButton,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B)
        ApplicationStatus.REVIEWED -> Color(0xFF3B82F6)
        ApplicationStatus.SHORTLISTED -> Color(0xFF8B5CF6)
        ApplicationStatus.ACCEPTED -> Color(0xFF10B981)
        ApplicationStatus.REJECTED -> Color(0xFFEF4444)
    }
}

private fun openResume(context: Context, applicationId: String, resumeBase64: String?, fileName: String?) {
    try {
        if (resumeBase64.isNullOrEmpty()) {
            Log.e("ViewApplications", "Resume data is empty")
            return
        }

        val decodedBytes = Base64.decode(resumeBase64, Base64.DEFAULT)
        val file = File(context.cacheDir, fileName ?: "resume_$applicationId.pdf")
        file.writeBytes(decodedBytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("ViewApplications", "Error opening resume: ${e.message}", e)
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPurple)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun ImprovedStatCard(
    title: String,
    count: Int,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}