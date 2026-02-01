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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.core.content.FileProvider
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.navigation.Screen
import com.example.internshipproject.ui.company.ImprovedStatCard
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.ApplicationWithStudent
import com.example.internshipproject.viewmodel.ViewApplicationsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewApplicationsScreen(
    navController: NavController,
    postingId: String,
    onNavigateBack: () -> Unit,  // ✅ NEW: Callback to navigate back to CompanyMainScreen tabs
    viewModel: ViewApplicationsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(postingId) {
        viewModel.loadData(postingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Applications") },
                navigationIcon = {
                    // ✅ FIXED: Use callback instead of navController.popBackStack()
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
                else -> {
                    ApplicationsContent(
                        state = state,
                        context = context,
                        postingId = postingId,
                        onUpdateStatus = { applicationId, newStatus ->
                            viewModel.updateApplicationStatus(applicationId, newStatus)
                        },
                        // ✅ Navigate to Student Application Details screen
                        onReviewApplication = { applicationId ->
                            navController.navigate(
                                Screen.StudentApplicationDetails.createRoute(applicationId)
                            )
                        }
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
    state: com.example.internshipproject.viewmodel.ViewApplicationsState,
    context: Context,
    postingId: String,
    onUpdateStatus: (String, ApplicationStatus) -> Unit,
    onReviewApplication: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPurple)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            state.posting?.let { posting ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = posting.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = posting.location,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Status Overview
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
                    backgroundColor = Color(0xFFDDD6FE),
                    modifier = Modifier.weight(1f)
                )
            }
        }

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

                }
            }
        }

        // Applications List
        if (state.applicationsWithStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Applications Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Applications will appear here once students start applying",
                            fontSize = 14.sp,
                            color = TextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.applicationsWithStudents) { appWithStudent ->
                ApplicationCard(
                    applicationWithStudent = appWithStudent,
                    onReview = { onReviewApplication(appWithStudent.application.id) },
                    onViewResume = {
                        if (appWithStudent.application.hasResume) {
                            openResume(context, appWithStudent.application)
                        }
                    }
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ApplicationCard(
    applicationWithStudent: ApplicationWithStudent,
    onReview: () -> Unit,
    onViewResume: () -> Unit
) {
    val app = applicationWithStudent.application
    val studentProfile = applicationWithStudent.studentProfile

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentProfile?.fullName ?: "Unknown Applicant",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = app.studentEmail,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                StatusChip(status = app.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Student Info
            studentProfile?.let {
                InfoChip(
                    icon = Icons.Default.School,
                    text = "${it.school} - ${it.course}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoChip(
                    icon = Icons.Default.Star,
                    text = it.yearLevel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            InfoChip(
                icon = Icons.Default.CalendarToday,
                text = "Applied on ${app.appliedDate}"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Review Button
                Button(
                    onClick = onReview,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleButton
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review")
                }

                // Resume Button
                OutlinedButton(
                    onClick = onViewResume,
                    modifier = Modifier.weight(1f),
                    enabled = app.hasResume,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PurpleButton
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: ApplicationStatus) {
    val statusColor = getStatusColor(status)
    val backgroundColor = when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFEF3C7)
        ApplicationStatus.REVIEWED -> Color(0xFFDDD6FE)
        ApplicationStatus.SHORTLISTED -> Color(0xFFEDE9FE)
        ApplicationStatus.ACCEPTED -> Color(0xFFD1FAE5)
        ApplicationStatus.REJECTED -> Color(0xFFFFEBEE)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = PurpleButton
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 20.sp,
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
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleButton
            )
        ) {
            Text("Retry")
        }
    }
}

private fun openResume(context: Context, application: Application) {
    try {
        if (application.resumeBase64.isNullOrEmpty()) {
            Log.e("ViewApplications", "Resume data is empty")
            return
        }

        val decodedBytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)
        val file = File(context.cacheDir, application.resumeFileName ?: "resume_${application.id}.pdf")
        file.writeBytes(decodedBytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, application.resumeMimeType ?: "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("ViewApplications", "Error opening resume: ${e.message}", e)
    }
}