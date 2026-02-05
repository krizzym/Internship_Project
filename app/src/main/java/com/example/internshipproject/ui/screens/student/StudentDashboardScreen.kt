package com.example.internshipproject.ui.screens.student

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.model.StudentProfile
import com.example.internshipproject.data.repository.InternshipRepository
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.StudentApplicationsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    studentProfile: StudentProfile,
    internships: List<Internship>,
    onInternshipClick: (String) -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: StudentApplicationsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentInternships by remember { mutableStateOf<List<Internship>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val repository = remember { InternshipRepository() }

    // Observe applications for statistics
    val applications by viewModel.applications.collectAsState()

    // Calculate dashboard stats dynamically
    val dashboardStats = remember(applications) {
        viewModel.getDashboardStats()
    }

    // Set up real-time listener for applications and load internships immediately
    LaunchedEffect(Unit) {
        viewModel.observeApplications()
        // Load internships immediately on screen load
        isRefreshing = true
        try {
            val loadedInternships = repository.getActiveInternships()
            currentInternships = loadedInternships
            Log.d("StudentDashboard", "Loaded ${loadedInternships.size} internships")
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Error loading internships", e)
        } finally {
            isRefreshing = false
        }
    }

    // Manual refresh function
    fun refreshInternships() {
        scope.launch {
            isRefreshing = true
            try {
                val loadedInternships = repository.getActiveInternships()
                currentInternships = loadedInternships
                Log.d("StudentDashboard", "Refresh: Loaded ${loadedInternships.size} internships")
                delay(300) // Small delay for visual feedback
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error refreshing internships", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "FirstStep",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Internship Connection Platform",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PurpleButton,
                        selectedTextColor = PurpleButton,
                        indicatorColor = PurpleButton.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "My Applications"
                        )
                    },
                    label = { Text("My Applications") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToApplications()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Welcome, ${studentProfile.firstName}!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Find your perfect internship opportunity",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Stats Cards using ViewModel data
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Applications",
                        count = dashboardStats["total"] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pending Review",
                        count = dashboardStats["pending"] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Accepted Applications",
                        count = dashboardStats["accepted"] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Available Internship Opportunities Section
            item {
                Text(
                    text = if (currentInternships.isNotEmpty()) {
                        "Available Internship Opportunities (${currentInternships.size})"
                    } else {
                        "Available Internship Opportunities"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Loading Indicator
            if (isRefreshing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleButton)
                    }
                }
            }

            // Internship Cards
            if (currentInternships.isEmpty() && !isRefreshing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Internships Available Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Companies will post internship opportunities here. Check back soon!",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { refreshInternships() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PurpleButton
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    }
                }
            } else {
                items(currentInternships) { internship ->
                    InternshipCard(
                        internship = internship,
                        onClick = { onInternshipClick(internship.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleButton
            )
        }
    }
}

@Composable
fun InternshipCard(
    internship: Internship,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = internship.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = internship.companyName,
                        fontSize = 14.sp,
                        color = PurpleButton,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = internship.location
                )
                InfoChip(
                    icon = Icons.Default.BusinessCenter,
                    text = internship.workType
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Schedule,
                    text = internship.duration
                )
                InfoChip(
                    icon = Icons.Default.AttachMoney,
                    text = internship.salaryRange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apply Button
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Details", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}