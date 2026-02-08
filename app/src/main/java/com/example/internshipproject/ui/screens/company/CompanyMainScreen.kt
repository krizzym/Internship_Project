package com.example.internshipproject.ui.screens.company

import androidx.compose.foundation.background
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
import com.example.internshipproject.ui.screens.company.ViewApplicationsScreen
import com.example.internshipproject.ui.theme.BackgroundGradientBrush
import com.example.internshipproject.ui.theme.PrimaryDeepBlueButton

@Composable
fun CompanyMainScreen(
    userId: String,
    navController: NavHostController,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Tab(0)) }

    Scaffold(
        containerColor = Color.Transparent, // Show background gradient
        bottomBar = {
            if (navigationState is NavigationState.Tab) {
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
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
                            selectedIconColor = PrimaryDeepBlueButton,
                            selectedTextColor = PrimaryDeepBlueButton
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
                            selectedIconColor = PrimaryDeepBlueButton,
                            selectedTextColor = PrimaryDeepBlueButton
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
                            selectedIconColor = PrimaryDeepBlueButton,
                            selectedTextColor = PrimaryDeepBlueButton
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
                            selectedIconColor = PrimaryDeepBlueButton,
                            selectedTextColor = PrimaryDeepBlueButton
                        )
                    )
                }
            }
        },
        modifier = Modifier.background(BackgroundGradientBrush)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (navigationState) {
                is NavigationState.Tab -> {
                    val contentModifier = Modifier.padding(
                        bottom = if (navigationState is NavigationState.Tab) 0.dp else paddingValues.calculateBottomPadding()
                    )
                    
                    when (selectedTab) {
                        0 -> CompanyDashboardScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onViewApplications = { postingId ->
                                navigationState = NavigationState.ViewApplications(postingId)
                            },
                            onEditPosting = { postingId ->
                                navController.navigate(Screen.EditInternship.createRoute(postingId))
                            },
                            onReviewApplication = { applicationId ->
                                navController.navigate(Screen.CompanyApplicationDetails.createRoute(applicationId))
                            }
                        )
                        1 -> CompanyMyPostingsScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onViewApplications = { postingId ->
                                navigationState = NavigationState.ViewApplications(postingId)
                            },
                            onEditPosting = { postingId ->
                                navController.navigate(Screen.EditInternship.createRoute(postingId))
                            }
                        )
                        2 -> CompanyApplicationsScreen(
                            userId = userId,
                            onLogout = onLogout,
                            onReviewApplication = { applicationId ->
                                navController.navigate(Screen.CompanyApplicationDetails.createRoute(applicationId))
                            }
                        )
                        3 -> CompanyProfileScreen(userId = userId, onLogout = onLogout)
                    }
                }
                is NavigationState.ViewApplications -> {
                    ViewApplicationsScreen(
                        navController = navController,
                        postingId = (navigationState as NavigationState.ViewApplications).postingId,
                        onNavigateBack = {
                            selectedTab = 1
                            navigationState = NavigationState.Tab(1)
                        }
                    )
                }
                else -> {}
            }
        }
    }
}

sealed class NavigationState {
    data class Tab(val index: Int) : NavigationState()
    data class ViewApplications(val postingId: String) : NavigationState()
    data class EditPosting(val postingId: String) : NavigationState()
    data class ReviewApplication(val applicationId: String) : NavigationState()
}
