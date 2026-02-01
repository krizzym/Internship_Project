package com.example.internshipproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.ui.components.*
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onStudentLoginSuccess: (String) -> Unit,
    onCompanyLoginSuccess: (String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Handle login success based on role
    LaunchedEffect(state.loginSuccess, state.userRole, state.userId) {
        if (state.loginSuccess && state.userRole != null && state.userId != null) {
            when (state.userRole) {
                "student" -> onStudentLoginSuccess(state.userId!!)
                "company" -> onCompanyLoginSuccess(state.userId!!)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPurple)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = "FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Internship Connection Platform", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Log in to FirstStep",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Access your account",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        InputField(
                            value = state.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = "Email Address",
                            keyboardType = KeyboardType.Email,
                            isError = state.errors.containsKey("email"),
                            errorMessage = state.errors["email"] ?: ""
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PasswordTextField(
                            value = state.password,
                            onValueChange = { viewModel.updatePassword(it) },
                            label = "Password",
                            isError = state.errors.containsKey("password"),
                            errorMessage = state.errors["password"],
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Improved Error Message
                        if (state.errorMessage != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFEE2E2)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "❌",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Login Failed",
                                            color = Color(0xFFDC2626),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = getFormattedErrorMessage(state.errorMessage),
                                            color = Color(0xFF991B1B),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        PrimaryButton(
                            text = "Log in",
                            onClick = { viewModel.login() },
                            isLoading = state.isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Don't have an account? ", fontSize = 14.sp, color = TextSecondary)
                            TextButton(onClick = onRegisterClick) {
                                Text(
                                    text = "Register now",
                                    fontSize = 14.sp,
                                    color = PurpleButton,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to format error messages
private fun getFormattedErrorMessage(errorMessage: String?): String {
    return when {
        errorMessage == null -> "An unexpected error occurred. Please try again."
        errorMessage.contains("password", ignoreCase = true) ->
            "Incorrect email or password. Please check your credentials and try again."
        errorMessage.contains("user", ignoreCase = true) ->
            "No account found with this email address. Please register first."
        errorMessage.contains("network", ignoreCase = true) ->
            "Network error. Please check your internet connection and try again."
        errorMessage.contains("malformed", ignoreCase = true) ->
            "Invalid email or password format. Please check your input."
        errorMessage.contains("credential", ignoreCase = true) ->
            "Invalid email or password. Please try again."
        else -> "Login failed. Please check your email and password and try again."
    }
}
