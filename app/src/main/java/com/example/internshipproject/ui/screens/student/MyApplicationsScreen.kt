package com.example.internshipproject.ui.screens.student

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.StudentApplicationsViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBackToDashboard: () -> Unit,
    onBrowseInternships: () -> Unit,
    onApplicationClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: StudentApplicationsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // Observe applications from ViewModel
    val applications by viewModel.applications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Filter state
    var selectedFilterStatus by remember { mutableStateOf<ApplicationStatus?>(null) }

    // Snackbar state for feedback messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Calculate stats dynamically from applications
    val applicationStats = remember(applications) {
        viewModel.getApplicationStats()
    }

    // Filtered list of applications
    val filteredApplications = remember(applications, selectedFilterStatus) {
        if (selectedFilterStatus == null) {
            applications
        } else {
            applications.filter { it.status == selectedFilterStatus }
        }
    }

    // Set up real-time listener when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.observeApplications()
    }

    // Error handling
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Delete handler
    fun handleDelete(applicationId: String) {
        viewModel.deleteApplication(applicationId) { _, message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundGradientBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Internship Connection Platform", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp, 
                    top = paddingValues.calculateTopPadding() + 12.dp, 
                    end = 12.dp, 
                    bottom = 100.dp // Added bottom padding so content scrolls behind nav bar
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "My Applications",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Select a status to filter your list",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Clickable Status Cards - COMPACT GRID
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // ALL CARD - THE LONG ONE
                        StatusFilterCard(
                            label = "All Submissions",
                            count = applications.size,
                            isSelected = selectedFilterStatus == null,
                            color = PrimaryDeepBlueButton,
                            onClick = { selectedFilterStatus = null },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusFilterCard(
                                label = "Pending",
                                count = applicationStats[ApplicationStatus.PENDING] ?: 0,
                                isSelected = selectedFilterStatus == ApplicationStatus.PENDING,
                                color = Color(0xFFBE7B0B),
                                onClick = { selectedFilterStatus = ApplicationStatus.PENDING },
                                modifier = Modifier.weight(1f)
                            )
                            StatusFilterCard(
                                label = "Accepted",
                                count = applicationStats[ApplicationStatus.ACCEPTED] ?: 0,
                                isSelected = selectedFilterStatus == ApplicationStatus.ACCEPTED,
                                color = Color(0xFF4CAF50),
                                onClick = { selectedFilterStatus = ApplicationStatus.ACCEPTED },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusFilterCard(
                                label = "Reviewed",
                                count = applicationStats[ApplicationStatus.REVIEWED] ?: 0,
                                isSelected = selectedFilterStatus == ApplicationStatus.REVIEWED,
                                color = Color(0xFF0067AD),
                                onClick = { selectedFilterStatus = ApplicationStatus.REVIEWED },
                                modifier = Modifier.weight(1f)
                            )
                            StatusFilterCard(
                                label = "Shortlisted",
                                count = applicationStats[ApplicationStatus.SHORTLISTED] ?: 0,
                                isSelected = selectedFilterStatus == ApplicationStatus.SHORTLISTED,
                                color = Color(0xFF66BB6A),
                                onClick = { selectedFilterStatus = ApplicationStatus.SHORTLISTED },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section Label
                item {
                    Text(
                        text = if (selectedFilterStatus == null) "All Submissions" else "${selectedFilterStatus!!.name.lowercase().replaceFirstChar { it.uppercase() }} Applications",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Loading state
                if (isLoading && applications.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // Empty state - EXPANDED TO OCCUPY SPACE
                if (filteredApplications.isEmpty() && !isLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.6f), // Occupy most of the remaining visible area
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.85f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Inbox, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No ${selectedFilterStatus?.name?.lowercase() ?: ""} applications.",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Your submitted internships will appear here.",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Application List
                items(filteredApplications) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onApplicationClick(application.id) },
                        onDelete = { handleDelete(it) }
                    )
                }
            }
        }

        // NAVIGATION BAR OVERLAPPING AT BOTTOM
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            containerColor = Color.White.copy(alpha = 0.9f),
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                label = { Text("Dashboard") },
                selected = false,
                onClick = { onBackToDashboard() },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepBlueButton,
                    selectedTextColor = PrimaryDeepBlueButton
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Description, contentDescription = "My Applications") },
                label = { Text("My Applications") },
                selected = true,
                onClick = { },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepBlueButton,
                    selectedTextColor = PrimaryDeepBlueButton,
                    indicatorColor = PrimaryDeepBlueButton.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = false,
                onClick = { onNavigateToProfile() },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepBlueButton,
                    selectedTextColor = PrimaryDeepBlueButton
                )
            )
        }
    }
}

@Composable
fun StatusFilterCard(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else CardWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (!isSelected) BorderStroke(1.dp, color.copy(alpha = 0.15f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.1f)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else color
                )
            }
        }
    }
}

@Composable
fun ApplicationCard(
    application: Application,
    onClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        application.internshipTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        application.companyName,
                        fontSize = 12.sp,
                        color = PrimaryDeepBlueButton,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Applied: ${application.appliedDate}", fontSize = 11.sp, color = TextSecondary)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getStatusColor(application.status).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, getStatusColor(application.status).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = application.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = getStatusColor(application.status)
                    )
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("View Details", fontSize = 12.sp)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Application?", fontSize = 18.sp) },
            text = { Text("This action cannot be undone.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    onDelete(application.id) 
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFBE7B0B)
        ApplicationStatus.REVIEWED -> Color(0xFF0067AD)
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }
}
