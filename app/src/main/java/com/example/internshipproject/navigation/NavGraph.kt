package com.example.internshipproject.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.model.StudentProfile
import com.example.internshipproject.data.repository.ApplicationRepository
import com.example.internshipproject.data.repository.AuthRepository
import com.example.internshipproject.data.repository.InternshipRepository
import com.example.internshipproject.ui.screens.*
import com.example.internshipproject.ui.screens.student.*
import com.example.internshipproject.ui.screens.company.CompanyMainScreen
import com.example.internshipproject.ui.screens.company.EditInternshipScreen
import com.example.internshipproject.ui.screens.company.StudentApplicationDetailsScreen
import com.example.internshipproject.viewmodel.StudentApplicationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String) {
    object Join : Screen("join")
    object StudentRegistration : Screen("student_registration")
    object CompanyRegistration : Screen("company_registration")
    object Login : Screen("login")

    object StudentDashboard : Screen("student_dashboard/{userId}") {
        fun createRoute(userId: String) = "student_dashboard/$userId"
    }
    object InternshipDetails : Screen("internship_details/{internshipId}") {
        fun createRoute(internshipId: String) = "internship_details/$internshipId"
    }
    object MyApplications : Screen("my_applications/{userId}") {
        fun createRoute(userId: String) = "my_applications/$userId"
    }
    object StudentProfile : Screen("student_profile/{userId}") {
        fun createRoute(userId: String) = "student_profile/$userId"
    }

    object CompanyMain : Screen("company_main/{userId}") {
        fun createRoute(userId: String) = "company_main/$userId"
    }

    object EditInternship : Screen("edit_internship/{internshipId}") {
        fun createRoute(internshipId: String) = "edit_internship/$internshipId"
    }

    // ✅ NEW: Student Application Details Route
    object StudentApplicationDetails : Screen("student_application_details/{applicationId}") {
        fun createRoute(applicationId: String) = "student_application_details/$applicationId"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authRepository = remember { AuthRepository() }
    val internshipRepository = remember { InternshipRepository() }

    // ✅ UPDATED: Get context and pass to ApplicationRepository
    val context = LocalContext.current
    val applicationRepository = remember(context) { ApplicationRepository(context) }

    NavHost(
        navController = navController,
        startDestination = Screen.Join.route
    ) {
        // ============================================
        // AUTH SCREENS
        // ============================================

        composable(Screen.Join.route) {
            JoinScreen(
                onStudentClick = { navController.navigate(Screen.StudentRegistration.route) },
                onCompanyClick = { navController.navigate(Screen.CompanyRegistration.route) },
                onLoginClick = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.StudentRegistration.route) {
            StudentRegistrationScreen(
                onBackClick = { navController.popBackStack() },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Join.route)
                    }
                },
                onRegistrationSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Join.route)
                    }
                }
            )
        }

        composable(Screen.CompanyRegistration.route) {
            CompanyRegistrationScreen(
                onBackClick = { navController.popBackStack() },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Join.route)
                    }
                },
                onRegistrationSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Join.route)
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterClick = {
                    navController.navigate(Screen.Join.route) {
                        popUpTo(Screen.Join.route) { inclusive = true }
                    }
                },
                onStudentLoginSuccess = { userId ->
                    navController.navigate(Screen.StudentDashboard.createRoute(userId)) {
                        popUpTo(Screen.Join.route) { inclusive = true }
                    }
                },
                onCompanyLoginSuccess = { userId ->
                    navController.navigate(Screen.CompanyMain.createRoute(userId)) {
                        popUpTo(Screen.Join.route) { inclusive = true }
                    }
                }
            )
        }

        // ============================================
        // ✅ UPDATED: STUDENT DASHBOARD WITH VIEWMODEL
        // ============================================
        composable(
            route = Screen.StudentDashboard.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            // ✅ NEW: Create ViewModel instance
            val viewModel: StudentApplicationsViewModel = viewModel()

            var profile by remember { mutableStateOf<StudentProfile?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            // ✅ Collect real-time internships using Flow
            val internships by internshipRepository.getActiveInternshipsFlow()
                .collectAsState(initial = emptyList())

            LaunchedEffect(userId) {
                withContext(Dispatchers.IO) {
                    isLoading = true

                    val student = authRepository.getStudentProfile(userId)
                    profile = student?.let {
                        StudentProfile(
                            firstName = it.firstName,
                            middleName = it.middleName,
                            surname = it.lastName,
                            email = it.email,
                            school = it.school,
                            course = it.course,
                            yearLevel = it.yearLevel,
                            city = it.city,
                            barangay = it.barangay,
                            internshipTypes = it.internshipTypes,
                            skills = it.skills,
                            resumeUri = it.resumeUri
                        )
                    }

                    isLoading = false
                }
            }

            profile?.let { studentProfile ->
                StudentDashboardScreen(
                    studentProfile = studentProfile,
                    internships = internships, // ✅ Real-time updates
                    viewModel = viewModel, // ✅ NEW: Pass ViewModel
                    onInternshipClick = { internshipId ->
                        navController.navigate(Screen.InternshipDetails.createRoute(internshipId))
                    },
                    onNavigateToApplications = {
                        navController.navigate(Screen.MyApplications.createRoute(userId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.StudentProfile.createRoute(userId))
                    },
                    onLogout = {
                        FirebaseManager.signOut()
                        navController.navigate(Screen.Join.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ============================================
        // ✅ UPDATED: INTERNSHIP DETAILS WITH RESUME UPLOAD
        // ============================================
        composable(
            route = Screen.InternshipDetails.route,
            arguments = listOf(navArgument("internshipId") { type = NavType.StringType })
        ) { backStackEntry ->
            val internshipId = backStackEntry.arguments?.getString("internshipId") ?: ""
            var internship by remember { mutableStateOf<Internship?>(null) }
            var isSubmitting by remember { mutableStateOf(false) }
            var showSuccessDialog by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(internshipId) {
                withContext(Dispatchers.IO) {
                    internship = internshipRepository.getInternshipById(internshipId)
                }
            }

            // Success Dialog
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Application Submitted! ✅") },
                    text = {
                        Text("Your application has been submitted successfully. You can track its status in My Applications.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSuccessDialog = false
                                navController.popBackStack()
                            }
                        ) {
                            Text("OK", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // Error Dialog
            errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Submission Failed") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    }
                )
            }

            internship?.let { internshipData ->
                InternshipDetailsScreen(
                    internship = internshipData,
                    onBackClick = { navController.popBackStack() },
                    isSubmitting = isSubmitting,
                    // ✅ UPDATED: Now receives both coverLetter and resumeUri
                    onSubmitApplication = { coverLetter, resumeUri ->
                        isSubmitting = true

                        // Get current user info
                        val userId = FirebaseManager.getCurrentUserId()

                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    // Get student email
                                    val student = authRepository.getStudentProfile(userId ?: "")
                                    val studentEmail = student?.email ?: ""

                                    if (studentEmail.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            errorMessage = "Unable to get your profile information"
                                            isSubmitting = false
                                        }
                                        return@withContext
                                    }

                                    // ✅ UPDATED: Submit application with resume
                                    val result = applicationRepository.submitApplication(
                                        internshipId = internshipData.id,
                                        internshipTitle = internshipData.title,
                                        companyName = internshipData.companyName,
                                        studentEmail = studentEmail,
                                        coverLetter = coverLetter,
                                        resumeUri = resumeUri  // ✅ NEW: Pass resume URI
                                    )

                                    withContext(Dispatchers.Main) {
                                        result.fold(
                                            onSuccess = { _ ->
                                                isSubmitting = false
                                                showSuccessDialog = true
                                            },
                                            onFailure = { exception ->
                                                isSubmitting = false
                                                errorMessage = exception.message ?: "Failed to submit application"
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        errorMessage = e.message ?: "An error occurred"
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        // ============================================
        // ✅ UPDATED: MY APPLICATIONS WITH VIEWMODEL
        // ============================================
        composable(
            route = Screen.MyApplications.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            // ✅ NEW: Create ViewModel instance
            val viewModel: StudentApplicationsViewModel = viewModel()

            MyApplicationsScreen(
                viewModel = viewModel, // ✅ NEW: Pass ViewModel instead of individual states
                onBackToDashboard = {
                    navController.navigate(Screen.StudentDashboard.createRoute(userId)) {
                        popUpTo(Screen.StudentDashboard.createRoute(userId)) { inclusive = true }
                    }
                },
                onBrowseInternships = {
                    navController.navigate(Screen.StudentDashboard.createRoute(userId)) {
                        popUpTo(Screen.StudentDashboard.createRoute(userId)) { inclusive = true }
                    }
                },
                onApplicationClick = { applicationId ->
                    // ✅ FIXED: Navigate to student view of application details
                    navController.navigate(Screen.StudentApplicationDetails.createRoute(applicationId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.StudentProfile.createRoute(userId))
                }
            )
        }

        // ============================================
        // STUDENT PROFILE (Unchanged)
        // ============================================
        composable(
            route = Screen.StudentProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            var profile by remember { mutableStateOf<StudentProfile?>(null) }

            LaunchedEffect(userId) {
                withContext(Dispatchers.IO) {
                    val student = authRepository.getStudentProfile(userId)
                    profile = student?.let {
                        StudentProfile(
                            firstName = it.firstName,
                            middleName = it.middleName,
                            surname = it.lastName,
                            email = it.email,
                            school = it.school,
                            course = it.course,
                            yearLevel = it.yearLevel,
                            city = it.city,
                            barangay = it.barangay,
                            internshipTypes = it.internshipTypes,
                            skills = it.skills,
                            resumeUri = it.resumeUri
                        )
                    }
                }
            }

            profile?.let { studentProfile ->
                StudentProfileScreen(
                    profile = studentProfile,
                    onBackToDashboard = {
                        navController.navigate(Screen.StudentDashboard.createRoute(userId)) {
                            popUpTo(Screen.StudentDashboard.createRoute(userId)) { inclusive = true }
                        }
                    },
                    onNavigateToApplications = {
                        navController.navigate(Screen.MyApplications.createRoute(userId)) {
                            popUpTo(Screen.StudentProfile.createRoute(userId)) { inclusive = true }
                        }
                    },
                    onLogout = {
                        FirebaseManager.signOut()
                        navController.navigate(Screen.Join.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ============================================
        // COMPANY ROUTES (Unchanged)
        // ============================================
        composable(
            route = Screen.CompanyMain.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CompanyMainScreen(
                userId = userId,
                navController = navController,
                onLogout = {
                    FirebaseManager.signOut()
                    navController.navigate(Screen.Join.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.EditInternship.route,
            arguments = listOf(navArgument("internshipId") { type = NavType.StringType })
        ) { backStackEntry ->
            val internshipId = backStackEntry.arguments?.getString("internshipId") ?: ""
            EditInternshipScreen(
                navController = navController,
                internshipId = internshipId
            )
        }

        // ============================================
        // ✅ UPDATED: STUDENT APPLICATION DETAILS (Student View - Read Only)
        // ============================================
        composable(
            route = Screen.StudentApplicationDetails.route,
            arguments = listOf(navArgument("applicationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""

            // ✅ FIXED: Use StudentViewApplicationScreen for students to view their own applications (read-only)
            StudentViewApplicationScreen(
                applicationId = applicationId,
                navController = navController
            )
        }
    }
}