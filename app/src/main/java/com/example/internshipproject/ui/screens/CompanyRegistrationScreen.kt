package com.example.internshipproject.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.ui.components.*
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.CompanyRegistrationViewModel
import androidx.compose.foundation.text.ClickableText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyRegistrationScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: CompanyRegistrationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Add state variables for the dialogs
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateLogoUri(uri)
    }

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
                        text = "Company Registration",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Create your company account to post internship opportunities",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    SectionTitle("Account Information")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = state.companyEmail,
                            onValueChange = { viewModel.updateCompanyEmail(it) },
                            label = "Company Email *",
                            hint = "Use your official company email address",
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("companyEmail"),
                            errorMessage = state.errors["companyEmail"] ?: ""
                        )

                        InputField(
                            value = state.contactNumber,
                            onValueChange = { viewModel.updateContactNumber(it) },
                            label = "Contact Number *",
                            hint = "Please enter your valid contact number",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("contactNumber"),
                            errorMessage = state.errors["contactNumber"] ?: ""
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simplified password field with helper text (matching student registration)
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

                    SectionTitle("Company Details")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField(
                            value = state.companyName,
                            onValueChange = { viewModel.updateCompanyName(it) },
                            label = "Company Name *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("companyName"),
                            errorMessage = state.errors["companyName"] ?: ""
                        )

                        InputField(
                            value = state.contactPerson,
                            onValueChange = { viewModel.updateContactPerson(it) },
                            label = "Contact Person *",
                            modifier = Modifier.weight(1f),
                            isError = state.errors.containsKey("contactPerson"),
                            errorMessage = state.errors["contactPerson"] ?: ""
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var expandedIndustry by remember { mutableStateOf(false) }
                    val industries = listOf(
                        "Technology", "Healthcare", "Finance", "Education",
                        "Manufacturing", "Retail", "Hospitality", "Other"
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedIndustry,
                        onExpandedChange = { expandedIndustry = it }
                    ) {
                        OutlinedTextField(
                            value = state.industryType.ifEmpty { "Select industry" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Industry Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndustry) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            isError = state.errors.containsKey("industryType"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryDeepBlueButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expandedIndustry,
                            onDismissRequest = { expandedIndustry = false }
                        ) {
                            industries.forEach { industry ->
                                DropdownMenuItem(
                                    text = { Text(industry) },
                                    onClick = {
                                        viewModel.updateIndustryType(industry)
                                        expandedIndustry = false
                                    }
                                )
                            }
                        }
                    }

                    if (state.errors.containsKey("industryType")) {
                        Text(
                            text = state.errors["industryType"] ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InputField(
                        value = state.companyAddress,
                        onValueChange = { viewModel.updateCompanyAddress(it) },
                        label = "Company Address *",
                        singleLine = false,
                        maxLines = 2,
                        isError = state.errors.containsKey("companyAddress"),
                        errorMessage = state.errors["companyAddress"] ?: ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InputField(
                        value = state.companyDescription,
                        onValueChange = { viewModel.updateCompanyDescription(it) },
                        label = "Company Description *",
                        hint = "Tell us about your company, mission, and what you do...",
                        singleLine = false,
                        maxLines = 5,
                        isError = state.errors.containsKey("companyDescription"),
                        errorMessage = state.errors["companyDescription"] ?: ""
                    )

                    Text(
                        text = "Minimum 50 characters - Describe your company's mission and services",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("Company Branding")

                    Text(
                        text = "Upload Company Logo *",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = { logoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepBlueButton)
                    ) {
                        Text("Upload Logo")
                    }

                    state.logoUri?.let {
                        Text(
                            text = "Logo selected: ${it.path}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Supported formats: PNG, JPG, SVG | Maximum file size: 2MB",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    if (state.errors.containsKey("logo")) {
                        Text(
                            text = state.errors["logo"] ?: "",
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
                            withStyle(
                                style = SpanStyle(
                                    color = PrimaryDeepBlueButton,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline  // ✅ Added underline
                                )
                            ) {
                                append("Terms & Conditions")
                            }
                            pop()

                            append(" and ")

                            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                            withStyle(
                                style = SpanStyle(
                                    color = PrimaryDeepBlueButton,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline  // ✅ Added underline
                                )
                            ) {
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

                    // Add the actual dialog components
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

                    if (state.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = state.errorMessage!!,
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Button is disabled until form is valid
                    PrimaryButton(
                        text = "Create Company Account",
                        onClick = { viewModel.register() },
                        isLoading = state.isLoading,
                        enabled = !state.isLoading && viewModel.isFormValid()
                    )

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

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}