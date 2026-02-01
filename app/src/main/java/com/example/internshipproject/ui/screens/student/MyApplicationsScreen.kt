//MyApplicationsScreen.kt - FINAL VERSION with Dialog
package com.example.internshipproject.ui.screens.student

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
import com.example.internshipproject.ui.components.ApplicationDetailsDialog
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.StudentApplicationsViewModel

/**
 * ✅ FINAL VERSION: MyApplicationsScreen with ViewModel + Working Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBackToDashboard: () -> Unit,
    onBrowseInternships: () -> Unit,
    onApplicationClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: StudentApplicationsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(1) }

    // ✅ NEW: State to show application details dialog
    var selectedApplication by remember { mutableStateOf<Application?>(null) }

    // ✅ Observe applications from ViewModel
    val applications by viewModel.applications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // ✅ Calculate stats dynamically from applications
    val applicationStats = remember(applications) {
        viewModel.getApplicationStats()
    }

    // ✅ Set up real-time listener when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.observeApplications()  // This matches your ViewModel's method
    }

    // ✅ Error handling
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You could show a snackbar here
            viewModel.clearError()
        }
    }

    // ✅ NEW: Show dialog when an application is selected
    selectedApplication?.let { application ->
        ApplicationDetailsDialog(
            application = application,
            onDismiss = { selectedApplication = null }
        )
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
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isLoading) PurpleButton else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onBackToDashboard()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleButton,
                        selectedTextColor = PurpleButton,
                        indicatorColor = PurpleButton.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = "My Applications") },
                    label = { Text("My Applications") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        // Already on applications, no navigation needed
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleButton,
                        selectedTextColor = PurpleButton,
                        indicatorColor = PurpleButton.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onNavigateToProfile()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleButton,
                        selectedTextColor = PurpleButton,
                        indicatorColor = PurpleButton.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPurple)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "My Applications",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Track all your internship applications",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // ✅ Status Cards - using computed stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Application Status",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        StatusRow("Pending", applicationStats[ApplicationStatus.PENDING] ?: 0)
                        StatusRow("Reviewed", applicationStats[ApplicationStatus.REVIEWED] ?: 0)
                        StatusRow("Shortlisted", applicationStats[ApplicationStatus.SHORTLISTED] ?: 0)
                        StatusRow("Accepted", applicationStats[ApplicationStatus.ACCEPTED] ?: 0)
                    }
                }
            }

            // ✅ Loading indicator
            if (isLoading && applications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleButton)
                    }
                }
            }

            // All Applications Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "All Applications (${applications.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (applications.isEmpty() && !isLoading) {
                            // Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "You haven't applied to any internships yet.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(
                                    onClick = onBrowseInternships,
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
                                ) {
                                    Text("Browse Internships")
                                }
                            }
                        } else {
                            // Application Cards
                            applications.forEach { application ->
                                ApplicationCard(
                                    application = application,
                                    // ✅ FIXED: Opens dialog instead of using empty callback
                                    onClick = { selectedApplication = application }
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
fun StatusRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(
            count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PurpleButton
        )
    }
}

@Composable
fun ApplicationCard(
    application: Application,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundPurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                application.internshipTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                application.companyName,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Applied: ${application.appliedDate}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getStatusColor(application.status)
                ) {
                    Text(
                        application.status.name,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("View Application", fontSize = 13.sp)
            }
        }
    }
}

fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFFA726)
        ApplicationStatus.REVIEWED -> Color(0xFF42A5F5)
        ApplicationStatus.SHORTLISTED -> Color(0xFF66BB6A)
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }
}