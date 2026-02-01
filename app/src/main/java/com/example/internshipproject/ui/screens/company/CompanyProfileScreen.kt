package com.example.internshipproject.ui.screens.company

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.internshipproject.ui.components.PrimaryButton
import com.example.internshipproject.ui.components.SectionTitle
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.CompanyProfileViewModel
import kotlinx.coroutines.launch
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    userId: String,
    onLogout: () -> Unit,
    viewModel: CompanyProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showLogoDialog by remember { mutableStateOf(false) }

    // ✅ NEW: Get context for taking persistent permissions
    val context = LocalContext.current

    // ✅ NEW: Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // ✅ FIXED: Take persistent read permission to prevent "Object does not exist" error
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.updateNewLogoUri(it)
            } catch (e: SecurityException) {
                // If persistent permission fails, still try to use the URI
                // (it might work for immediate upload)
                viewModel.updateNewLogoUri(it)
            }
        }
    }

    LaunchedEffect(userId) {
        scope.launch {
            viewModel.loadProfile(userId)
        }
    }

    // ✅ NEW: Show success message in Snackbar
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            // Clear the message after showing
            viewModel.clearSuccessMessage()
        }
    }

    // ✅ NEW: Show error message in Snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            // Clear the message after showing
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            viewModel.resetUpdateSuccess()
        }
    }

    Scaffold(
        snackbarHost = {
            // ✅ NEW: SnackbarHost for displaying feedback messages
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = if (snackbarData.visuals.message.contains("successfully", ignoreCase = true)) {
                            Color(0xFF4CAF50) // Green for success
                        } else {
                            Color(0xFFE53935) // Red for errors
                        },
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Internship Connection Platform",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // NEW: Logout Button
                    TextButton(onClick = onLogout) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Logout",
                                color = Color.Red,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPurple)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Company Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Manage your company information",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Success Message (existing verification status card - kept for compatibility)
            if (state.updateSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Your account is verified",
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Account Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionTitle("Account Information")

                    Text(
                        text = "Company Email",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.companyEmail,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                            disabledTextColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        text = "Email cannot be changed",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Company Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionTitle("Company Information")

                    Text(
                        text = "Contact Number *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.contactNumber,
                        onValueChange = { viewModel.updateContactNumber(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.errors.containsKey("contactNumber"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Company Name *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.companyName,
                        onValueChange = { viewModel.updateCompanyName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.errors.containsKey("companyName"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Contact Person *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.contactPerson,
                        onValueChange = { viewModel.updateContactPerson(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.errors.containsKey("contactPerson"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Industry Type *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.industryType,
                        onValueChange = { viewModel.updateIndustryType(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.errors.containsKey("industryType"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Company Address *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.companyAddress,
                        onValueChange = { viewModel.updateCompanyAddress(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2,
                        isError = state.errors.containsKey("companyAddress"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Company Description *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.companyDescription,
                        onValueChange = { viewModel.updateCompanyDescription(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 5,
                        isError = state.errors.containsKey("companyDescription"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        text = "Minimum 50 characters",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Update Profile",
                        onClick = { viewModel.updateProfile(userId) },
                        isLoading = state.isUpdating
                    )
                }
            }

            // Company Logo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionTitle("Company Logo")

                    if (state.logoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = BackgroundPurple.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PurpleButton,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Company logo uploaded",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // NEW: View Logo Button
                        OutlinedButton(
                            onClick = { showLogoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PurpleButton
                            )
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Current Logo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "Upload New Logo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedButton(
                        onClick = { logoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (state.newLogoUri != null) "File selected ✓" else "Choose File",
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Supported formats: PNG, JPG, SVG | Maximum file size: 2MB",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Update Logo",
                        onClick = { viewModel.uploadLogo(userId) },
                        isLoading = state.isUpdating,
                        enabled = state.newLogoUri != null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // NEW: Logo Preview Dialog
    if (showLogoDialog && state.logoUri != null) {
        Dialog(onDismissRequest = { showLogoDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Company Logo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showLogoDialog = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Logo Image
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundPurple.copy(alpha = 0.1f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = state.logoUri,
                                contentDescription = "Company Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Close",
                        onClick = { showLogoDialog = false }
                    )
                }
            }
        }
    }
}
