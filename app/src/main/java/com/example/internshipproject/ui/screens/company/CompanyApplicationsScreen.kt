package com.example.internshipproject.ui.screens.company

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.company.ImprovedStatCard
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.CompanyApplicationsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyApplicationsScreen(
    userId: String,
    onLogout: () -> Unit,
    onReviewApplication: (String) -> Unit = {},
    viewModel: CompanyApplicationsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val filteredApplications by viewModel.filteredApplications.collectAsState()
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadApplicationsForCompany(userId)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Internship Connection Platform",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // Thin progress bar that slides in while data is being fetched
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryDeepBlueButton,
                        trackColor = PrimaryDeepBlueButton.copy(alpha = 0.15f)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradientBrush)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "All Applications",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Review and manage student applications",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            // Total count badge — always shows the FULL list size
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PrimaryDeepBlueButton.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${state.applications.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDeepBlueButton,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stats Cards – counts always reflect the full (unfiltered) list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImprovedStatCard(
                        title = "Pending",
                        count = viewModel.getApplicationCountByStatus(ApplicationStatus.PENDING),
                        color = Color(0xFFF59E0B),
                        backgroundColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    ImprovedStatCard(
                        title = "Reviewed",
                        count = viewModel.getApplicationCountByStatus(ApplicationStatus.REVIEWED),
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
                        count = viewModel.getApplicationCountByStatus(ApplicationStatus.SHORTLISTED),
                        color = Color(0xFF8B5CF6),
                        backgroundColor = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f)
                    )
                    ImprovedStatCard(
                        title = "Accepted",
                        count = viewModel.getApplicationCountByStatus(ApplicationStatus.ACCEPTED),
                        color = Color(0xFF10B981),
                        backgroundColor = Color(0xFFD1FAE5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Filter Applications",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            val displayLabel = if (state.selectedFilter == "All") "All Applications"
                            else state.selectedFilter.lowercase().replaceFirstChar { it.uppercase() }

                            OutlinedTextField(
                                value = displayLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = {
                                    Icon(
                                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryDeepBlueButton,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                val filters = listOf(
                                    "All"         to "All Applications",
                                    "PENDING"     to "Pending",
                                    "REVIEWED"    to "Reviewed",
                                    "SHORTLISTED" to "Shortlisted",
                                    "ACCEPTED"    to "Accepted",
                                    "REJECTED"    to "Rejected"
                                )

                                filters.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label)
                                                if (state.selectedFilter == value) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = PrimaryDeepBlueButton,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.filterApplications(value)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Applications List
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
                                "Applications",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PrimaryDeepBlueButton.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${filteredApplications.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDeepBlueButton,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredApplications.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Mail,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No applications found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (state.selectedFilter == "All") {
                                        "Students will appear here when they apply"
                                    } else {
                                        "No ${state.selectedFilter.lowercase()} applications yet"
                                    },
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            filteredApplications.forEach { application ->
                                ApplicationDetailsCard(
                                    application = application,
                                    onReview = { onReviewApplication(application.id) },
                                    onViewResume = {
                                        if (application.hasResume) {
                                            openResume(context, application)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ApplicationDetailsCard(
    application: Application,
    onReview: () -> Unit,
    onViewResume: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with Student Info and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.studentEmail.substringBefore("@"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            application.studentEmail,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (application.status) {
                        ApplicationStatus.PENDING -> Color(0xFFBE7B0B)
                        ApplicationStatus.REVIEWED -> Color(0xFF0067AD)
                        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)
                        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
                        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
                    }
                ) {
                    Text(
                        text = application.status.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Internship Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    application.internshipTitle,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Applied: ${application.appliedDate}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Review",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Resume Button
                OutlinedButton(
                    onClick = onViewResume,
                    modifier = Modifier.weight(1f),
                    enabled = application.hasResume,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryDeepBlueButton
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Resume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Resume viewing function
private fun openResume(context: Context, application: Application) {
    try {
        if (application.resumeBase64.isNullOrEmpty()) {
            Log.e("CompanyApps", "Resume data is empty or null")
            android.widget.Toast.makeText(
                context,
                "Resume not available",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Decode Base64 and save to temp file
        val bytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)
        val fileName = application.resumeFileName ?: "resume.pdf"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeBytes(bytes)

        Log.d("CompanyApps", "Resume saved to: ${tempFile.absolutePath}")

        // Create URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )

        // Create intent to open PDF
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Show app chooser (including Google Drive)
        val chooser = Intent.createChooser(intent, "Open Resume")
        context.startActivity(chooser)

        Log.d("CompanyApps", "Resume opened successfully")

    } catch (e: Exception) {
        Log.e("CompanyApps", "Error opening resume: ${e.message}", e)
        android.widget.Toast.makeText(
            context,
            "Failed to open resume",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}