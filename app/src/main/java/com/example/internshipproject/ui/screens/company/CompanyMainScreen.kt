package com.example.internshipproject.ui.company

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.internshipproject.navigation.Screen
import com.example.internshipproject.ui.screens.company.CompanyApplicationsScreen
import com.example.internshipproject.ui.screens.company.CompanyDashboardScreen
import com.example.internshipproject.ui.screens.company.CompanyMyPostingsScreen
import com.example.internshipproject.ui.screens.company.CompanyProfileScreen
import com.example.internshipproject.ui.theme.PurpleButton

@Composable
fun CompanyMainScreen(
    userId: String,
    navController: NavHostController,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Tab(0)) }

    Scaffold(
        bottomBar = {
            // Only show bottom bar when not in detail screens
            if (navigationState is NavigationState.Tab) {
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
                            navigationState = NavigationState.Tab(0)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleButton,
                            selectedTextColor = PurpleButton,
                            indicatorColor = PurpleButton.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Description, contentDescription = "My Postings") },
                        label = { Text("My Postings") },
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            navigationState = NavigationState.Tab(1)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleButton,
                            selectedTextColor = PurpleButton,
                            indicatorColor = PurpleButton.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Email, contentDescription = "Applications") },
                        label = { Text("Applications") },
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            navigationState = NavigationState.Tab(2)
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
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            navigationState = NavigationState.Tab(3)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleButton,
                            selectedTextColor = PurpleButton,
                            indicatorColor = PurpleButton.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (navigationState) {
                is NavigationState.Tab -> {
                    when (selectedTab) {
                        0 -> CompanyDashboardScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onViewApplications = { postingId ->
                                navigationState = NavigationState.ViewApplications(postingId)
                            },
                            onEditPosting = { postingId ->
                                // ✅ FIXED: Use NavGraph navigation instead of internal state
                                navController.navigate(Screen.EditInternship.createRoute(postingId))
                            },
                            onReviewApplication = { applicationId ->
                                navigationState = NavigationState.ReviewApplication(applicationId)
                            }
                        )
                        1 -> CompanyMyPostingsScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onViewApplications = { postingId ->
                                navigationState = NavigationState.ViewApplications(postingId)
                            },
                            onEditPosting = { postingId ->
                                // ✅ FIXED: Use NavGraph navigation instead of internal state
                                navController.navigate(Screen.EditInternship.createRoute(postingId))
                            }
                        )
                        2 -> CompanyApplicationsScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onReviewApplication = { applicationId ->
                                navigationState = NavigationState.ReviewApplication(applicationId)
                            }
                        )
                        3 -> CompanyProfileScreen(userId = userId, onLogout = onLogout)
                    }
                }
                is NavigationState.ViewApplications -> {
                    ViewApplicationsScreen(
                        postingId = (navigationState as NavigationState.ViewApplications).postingId,
                        onBack = {
                            navigationState = NavigationState.Tab(selectedTab)
                        }
                    )
                }
                is NavigationState.EditPosting -> {
                    // ✅ NOTE: This state is no longer used since we navigate via NavGraph
                    // Keeping it here for backward compatibility
                    LaunchedEffect(Unit) {
                        navigationState = NavigationState.Tab(selectedTab)
                    }
                }
                is NavigationState.ReviewApplication -> {
                    // TODO: Create ReviewApplicationScreen
                    // For now, just go back
                    LaunchedEffect(Unit) {
                        navigationState = NavigationState.Tab(selectedTab)
                    }
                }
            }
        }
    }
}

sealed class NavigationState {
    data class Tab(val index: Int) : NavigationState()
    data class ViewApplications(val postingId: String) : NavigationState()
    data class EditPosting(val postingId: String) : NavigationState()  // ✅ Kept for compatibility
    data class ReviewApplication(val applicationId: String) : NavigationState()
}