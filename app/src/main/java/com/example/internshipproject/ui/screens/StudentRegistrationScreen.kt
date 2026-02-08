package com.example.internshipproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.ui.components.*
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.StudentRegistrationViewModel
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRegistrationScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: StudentRegistrationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }
    var showOtherCourseInput by remember { mutableStateOf(false) }

    val courses = listOf(
        "Computer Science",
        "Information Technology",
        "Computer Engineering",
        "Software Engineering",
        "Business Administration",
        "Accountancy",
        "Marketing",
        "Nursing",
        "Psychology",
        "Civil Engineering",
        "Electrical Engineering",
        "Mechanical Engineering",
        "Architecture",
        "Education",
        "Mass Communication",
        "Others"
    )

    LaunchedEffect(state.registrationSuccess) {
        if (state.registrationSuccess) {
            onRegistrationSuccess()
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

            Card(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Student Registration",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Create your student account to find internship opportunities",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    SectionTitle("Account Information")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = state.firstName,
                            onValueChange = { viewModel.updateFirstName(it) },
                            label = "First Name *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("firstName"),
                            errorMessage = state.errors["firstName"] ?: ""
                        )

                        InputField(
                            value = state.middleName,
                            onValueChange = { viewModel.updateMiddleName(it) },
                            label = "Middle Name",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InputField(
                        value = state.lastName,
                        onValueChange = { viewModel.updateLastName(it) },
                        label = "Last Name *",
                        isError = state.errors.containsKey("lastName"),
                        errorMessage = state.errors["lastName"] ?: ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InputField(
                        value = state.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = "Email Address *",
                        hint = "Use a valid email format (e.g., name@example.com)",
                        keyboardType = KeyboardType.Email,
                        isError = state.errors.containsKey("email"),
                        errorMessage = state.errors["email"] ?: ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordTextField(
                        value = state.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = "Password *",
                        isError = state.errors.containsKey("password"),
                        errorMessage = state.errors["password"],
                        helperText = "Password must be at least 12 characters long.",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordTextField(
                        value = state.confirmPassword,
                        onValueChange = { viewModel.updateConfirmPassword(it) },
                        label = "Confirm Password *",
                        isError = state.errors.containsKey("confirmPassword"),
                        errorMessage = state.errors["confirmPassword"],
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Educational Information")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = state.school,
                            onValueChange = { viewModel.updateSchool(it) },
                            label = "School / University *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("school"),
                            errorMessage = state.errors["school"] ?: ""
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedCourse,
                            onExpandedChange = { expandedCourse = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (showOtherCourseInput) "Others" else state.course.ifEmpty { "Select course" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Course / Program *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourse) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                isError = state.errors.containsKey("course"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryDeepBlueButton,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expandedCourse,
                                onDismissRequest = { expandedCourse = false }
                            ) {
                                courses.forEach { course ->
                                    DropdownMenuItem(
                                        text = { Text(course) },
                                        onClick = {
                                            if (course == "Others") {
                                                showOtherCourseInput = true
                                                viewModel.updateCourse("")
                                            } else {
                                                showOtherCourseInput = false
                                                viewModel.updateCourse(course)
                                            }
                                            expandedCourse = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.errors.containsKey("course") && !showOtherCourseInput) {
                        Text(
                            text = state.errors["course"] ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    if (showOtherCourseInput) {
                        Spacer(modifier = Modifier.height(16.dp))

                        InputField(
                            value = state.course,
                            onValueChange = { viewModel.updateCourse(it) },
                            label = "Please specify your course/program *",
                            hint = "Enter your course or program",
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.errors.containsKey("course"),
                            errorMessage = state.errors["course"] ?: ""
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var expandedYearLevel by remember { mutableStateOf(false) }
                    val yearLevels = listOf("2nd Year", "3rd Year", "4th Year", "5th Year")

                    ExposedDropdownMenuBox(
                        expanded = expandedYearLevel,
                        onExpandedChange = { expandedYearLevel = it }
                    ) {
                        OutlinedTextField(
                            value = state.yearLevel.ifEmpty { "Select your level" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year Level *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYearLevel) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            isError = state.errors.containsKey("yearLevel"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryDeepBlueButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expandedYearLevel,
                            onDismissRequest = { expandedYearLevel = false }
                        ) {
                            yearLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        viewModel.updateYearLevel(level)
                                        expandedYearLevel = false
                                    }
                                )
                            }
                        }
                    }

                    if (state.errors.containsKey("yearLevel")) {
                        Text(
                            text = state.errors["yearLevel"] ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Location")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = state.city,
                            onValueChange = { viewModel.updateCity(it) },
                            label = "City *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("city"),
                            errorMessage = state.errors["city"] ?: ""
                        )

                        InputField(
                            value = state.barangay,
                            onValueChange = { viewModel.updateBarangay(it) },
                            label = "Barangay *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("barangay"),
                            errorMessage = state.errors["barangay"] ?: ""
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Internship Preferences")

                    Text(
                        text = "Preferred Internship Types * (Select all that apply)",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.onsite, onCheckedChange = { viewModel.toggleOnsite() })
                        Text("On-site", fontSize = 14.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.remote, onCheckedChange = { viewModel.toggleRemote() })
                        Text("Remote", fontSize = 14.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.hybrid, onCheckedChange = { viewModel.toggleHybrid() })
                        Text("Hybrid", fontSize = 14.sp)
                    }

                    if (state.errors.containsKey("internshipTypes")) {
                        Text(
                            text = state.errors["internshipTypes"] ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Terms & Conditions Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = state.agreedToTerms,
                            onCheckedChange = { viewModel.toggleAgreement() },
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        val annotatedText = buildAnnotatedString {
                            append("I agree to the ")

                            pushStringAnnotation(tag = "TERMS", annotation = "terms")
                            withStyle(style = SpanStyle(color = PrimaryDeepBlueButton, fontWeight = FontWeight.Medium)) {
                                append("Terms & Conditions")
                            }
                            pop()

                            append(" and ")

                            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                            withStyle(style = SpanStyle(color = PrimaryDeepBlueButton, fontWeight = FontWeight.Medium)) {
                                append("Privacy Policy")
                            }
                            pop()
                        }

                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = TextSecondary
                            ),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                                    .firstOrNull()?.let {
                                        showTermsDialog = true
                                    }

                                annotatedText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                                    .firstOrNull()?.let {
                                        showPrivacyDialog = true
                                    }
                            }
                        )
                    }

                    if (showTermsDialog) {
                        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
                    }

                    if (showPrivacyDialog) {
                        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
                    }

                    if (state.errors.containsKey("terms")) {
                        Text(
                            text = state.errors["terms"] ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Register",
                        onClick = { viewModel.register() },
                        isLoading = state.isLoading
                    )

                    state.errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Already have an account? ", fontSize = 14.sp, color = TextSecondary)
                        TextButton(onClick = onLoginClick) {
                            Text(
                                text = "Log in",
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
