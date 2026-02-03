package com.example.internshipproject.ui.screens.company

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.company.ImprovedStatCard
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.CompanyApplicationsViewModel

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
                    actions = {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !state.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh applications",
                                tint = if (state.isLoading) Color.Gray else TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // Thin progress bar that slides in while data is being fetched
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PurpleButton,
                        trackColor = PurpleButton.copy(alpha = 0.15f)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPurple)
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
                                color = PurpleButton.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${state.applications.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleButton,
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
                        color = Color(0xFFFBBF24),
                        backgroundColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    ImprovedStatCard(
                        title = "Reviewed",
                        count = viewModel.getApplicationCountByStatus(ApplicationStatus.REVIEWED),
                        color = Color(0xFF3B82F6),
                        backgroundColor = Color(0xFFDBEAFE),
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
                                    focusedBorderColor = PurpleButton,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                // value keys use the exact enum .name
                                // strings so they match the when-branch in the
                                // ViewModel.  Labels are purely for the UI.
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
                                                        tint = PurpleButton,
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
                    // use the reactively-collected list, NOT a one-shot
                    // imperative call.  Both the count badge and the loop below
                    // now recompose whenever the filter changes.
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
                                color = PurpleButton.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${filteredApplications.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleButton,
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
                                        // Title-case the enum name for the message
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
                                    onReview = { onReviewApplication(application.id) }
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
    onReview: () -> Unit
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
                        ApplicationStatus.PENDING    -> Color(0xFFFBBF24)
                        ApplicationStatus.REVIEWED   -> Color(0xFF3B82F6)
                        ApplicationStatus.SHORTLISTED -> Color(0xFF8B5CF6)
                        ApplicationStatus.ACCEPTED   -> Color(0xFF10B981)
                        ApplicationStatus.REJECTED   -> Color(0xFFEF4444)
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

            // Action Button
            Button(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Review Application",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}