package com.example.internshipproject.ui.screens.student

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    studentProfile: StudentProfile,
    onInternshipClick: (String) -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: StudentApplicationsViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentInternships by remember { mutableStateOf<List<Internship>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val repository = remember { InternshipRepository() }

    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val categories = listOf(
        "All",
        "Engineering and technology",
        "Business & Management",
        "Healthcare & Medical",
        "Education",
        "Criminology"
    )

    val filteredInternships = remember(searchQuery, selectedCategory, currentInternships) {
        currentInternships.filter { internship ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                internship.title.contains(searchQuery, ignoreCase = true) ||
                internship.companyName.contains(searchQuery, ignoreCase = true) ||
                internship.description.contains(searchQuery, ignoreCase = true) ||
                internship.requirements.contains(searchQuery, ignoreCase = true)
            }

            val matchesCategory = if (selectedCategory == "All") {
                true
            } else {
                internship.category == selectedCategory
            }

            matchesSearch && matchesCategory
        }
    }

    // Observe applications for statistics
    val applications by viewModel.applications.collectAsState()

    // Calculate dashboard stats dynamically
    val dashboardStats = remember(applications) {
        viewModel.getDashboardStats()
    }

    // Set up real-time listener for applications and load internships immediately
    LaunchedEffect(Unit) {
        viewModel.observeApplications()
        isRefreshing = true
        try {
            val loadedInternships = repository.getActiveInternships()
            currentInternships = loadedInternships
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
                delay(300)
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error refreshing internships", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    // ROOT BOX TO ENSURE GRADIENT COVERS EVERYTHING INCLUDING BEHIND TABS
    Box(modifier = Modifier
        .fillMaxSize()
        .background(BackgroundGradientBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent, // MUST BE TRANSPARENT
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // DISABLE AUTO INSETS
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Internship Connection Platform", fontSize = 11.sp, color = TextSecondary)
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
                    start = 16.dp, 
                    top = paddingValues.calculateTopPadding() + 12.dp, 
                    end = 16.dp, 
                    bottom = 100.dp // PADDING FOR THE UNDERLAP EFFECT
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp) // MINIMAL DEAD SPACE
            ) {
                // Welcome Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Welcome, ${studentProfile.firstName}!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Find your perfect internship opportunity",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Stats Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total Apps",
                            count = dashboardStats["total"] ?: 0,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Pending",
                            count = dashboardStats["pending"] ?: 0,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Accepted",
                            count = dashboardStats["accepted"] ?: 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search and Filter Bar - NO DEAD SPACE
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search internships...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PrimaryDeepBlueButton,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                singleLine = true
                            )

                            Box {
                                IconButton(
                                    onClick = { showFilterMenu = true },
                                    modifier = Modifier
                                        .background(
                                            if (selectedCategory != "All") PrimaryDeepBlueButton else Color.White,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedCategory != "All") PrimaryDeepBlueButton else Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = "Filter",
                                        tint = if (selectedCategory != "All") Color.White else TextPrimary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    category,
                                                    color = if (selectedCategory == category) PrimaryDeepBlueButton else TextPrimary,
                                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                                ) 
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                showFilterMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // CLEARLY VISIBLE FILTER CHIP WITH HIGH CONTRAST
                        if (selectedCategory != "All") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, PrimaryDeepBlueButton)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedCategory,
                                        fontSize = 12.sp,
                                        color = PrimaryDeepBlueButton, // HIGH VISIBILITY
                                        fontWeight = FontWeight.Black
                                    )
                                    IconButton(
                                        onClick = { selectedCategory = "All" },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove filter",
                                            tint = PrimaryDeepBlueButton,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Opportunities Label - White text for better visibility on dark background
                        Text(
                            text = if (filteredInternships.isNotEmpty()) {
                                "Available Opportunities (${filteredInternships.size})"
                            } else {
                                "Available Opportunities"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Internship Cards
                if (filteredInternships.isEmpty() && !isRefreshing) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 250.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.9f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(if (searchQuery.isEmpty() && selectedCategory == "All") "📋" else "🔍", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isEmpty() && selectedCategory == "All") "No Internships Available Yet" else "No matching internships found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredInternships) { internship ->
                        InternshipCard(
                            internship = internship,
                            onClick = { onInternshipClick(internship.id) }
                        )
                    }
                }
            }
        }

        // TABS - PLACED IN ROOT BOX TO OVERLAP CONTENT
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            containerColor = Color.White.copy(alpha = 0.95f), // SEMI-TRANSPARENT
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                label = { Text("Dashboard") },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepBlueButton,
                    selectedTextColor = PrimaryDeepBlueButton
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Description, contentDescription = "My Applications") },
                label = { Text("My Applications") },
                selected = selectedTab == 1,
                onClick = { 
                    selectedTab = 1
                    onNavigateToApplications() 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepBlueButton,
                    selectedTextColor = PrimaryDeepBlueButton
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedTab == 2,
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
                .padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 1
            )
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDeepBlueButton
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = internship.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = internship.companyName,
                        fontSize = 13.sp,
                        color = PrimaryDeepBlueButton,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(icon = Icons.Default.LocationOn, text = internship.location)
                InfoChip(icon = Icons.Default.BusinessCenter, text = internship.workType)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(icon = Icons.Default.Schedule, text = internship.duration)
                InfoChip(icon = Icons.Default.AttachMoney, text = internship.salaryRange)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("View Details", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}
