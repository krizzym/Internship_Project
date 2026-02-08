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
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Handle login success based on role
    LaunchedEffect(state.loginSuccess, state.userRole, state.userId) {
        if (state.loginSuccess && state.userRole != null && state.userId != null) {
            when (state.userRole) {
                "student" -> onStudentLoginSuccess(state.userId!!)
                "company" -> onCompanyLoginSuccess(state.userId!!)
            }
        }
    }

    // Handle password reset success
    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) {
            showResetDialog = false
            viewModel.resetResetEmailSent()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientBrush)
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

                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { 
                                    resetEmail = state.email
                                    showResetDialog = true 
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 13.sp,
                                    color = PrimaryDeepBlueButton,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                                            text = "Error",
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
                                    text = "Register",
                                    fontSize = 14.sp,
                                    color = PrimaryDeepBlueButton,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { 
                showResetDialog = false 
                viewModel.clearErrorMessage()
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDeepBlueButton
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendPasswordReset(resetEmail) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton),
                    enabled = !state.isResetLoading
                ) {
                    if (state.isResetLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showResetDialog = false 
                    viewModel.clearErrorMessage()
                }) {
                    Text("Cancel")
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
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
        else -> errorMessage
    }
}
