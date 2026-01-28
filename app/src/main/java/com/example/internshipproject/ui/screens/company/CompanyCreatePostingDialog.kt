package com.example.internshipproject.ui.screens.company

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.repository.CompanyRepository
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.CreatePostingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyCreatePostingDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreatePostingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val repository = remember { CompanyRepository() }
    var company by remember { mutableStateOf<com.example.internshipproject.data.model.Company?>(null) }

    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // ✅ NEW: Stipend state
    var stipendType by remember { mutableStateOf("Unpaid") }
    var customStipendAmount by remember { mutableStateOf("") }
    var expandedStipend by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        scope.launch {
            repository.getCompanyProfile(userId).onSuccess { company = it }
        }
    }

    LaunchedEffect(state.createSuccess) {
        if (state.createSuccess) {
            viewModel.resetCreateSuccess()
            onSuccess()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Create Internship Posting",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                // Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Job Title
                    OutlinedTextField(
                        value = state.jobTitle,
                        onValueChange = { viewModel.updateJobTitle(it) },
                        label = { Text("Job Title *") },
                        placeholder = { Text("e.g., Software Developer Intern") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.errors.containsKey("jobTitle"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = state.jobDescription,
                        onValueChange = { viewModel.updateJobDescription(it) },
                        label = { Text("Description *") },
                        placeholder = { Text("Describe the role, responsibilities, and what the intern will learn") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 6,
                        isError = state.errors.containsKey("jobDescription"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleButton,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Requirements
                    Column {
                        OutlinedTextField(
                            value = state.requirements,
                            onValueChange = { viewModel.updateRequirements(it) },
                            label = { Text("Requirements *") },
                            placeholder = { Text("List required skills, qualifications, and experience") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 6,
                            isError = state.errors.containsKey("requirements"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        // ✅ NEW: Helper text for Requirements
                        Text(
                            text = "Example format:\n• Proficient in Java/Kotlin\n• Basic understanding of Android development\n• Strong problem-solving skills",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // Type Dropdown
                    var expandedType by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = it }
                    ) {
                        OutlinedTextField(
                            value = state.internshipType.ifEmpty { "Select type" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            isError = state.errors.containsKey("internshipType"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            listOf("On-site", "Remote", "Hybrid").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        viewModel.updateInternshipType(type)
                                        expandedType = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Location
                        OutlinedTextField(
                            value = state.location,
                            onValueChange = { viewModel.updateLocation(it) },
                            label = { Text("Location *") },
                            placeholder = { Text("e.g., Quezon City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = state.errors.containsKey("location"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Duration
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.duration,
                                onValueChange = { viewModel.updateDuration(it) },
                                label = { Text("Duration *") },
                                placeholder = { Text("e.g., 3-6 months") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = state.errors.containsKey("duration"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleButton,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            // ✅ NEW: Helper text for Duration
                            Text(
                                text = "Format: X-Y months",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Number of Slots
                        OutlinedTextField(
                            value = state.slots,
                            onValueChange = { viewModel.updateSlots(it) },
                            label = { Text("Slots *") },
                            placeholder = { Text("e.g., 5") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = state.errors.containsKey("slots"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Deadline with Date Picker
                        OutlinedTextField(
                            value = state.deadline,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deadline *") },
                            placeholder = { Text("MM/DD/YYYY") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Select date",
                                        tint = PurpleButton
                                    )
                                }
                            },
                            isError = state.errors.containsKey("deadline"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = PurpleButton
                            )
                        )
                    }

                    // ✅ NEW: Stipend Section
                    Text(
                        text = "Stipend",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Stipend Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedStipend,
                        onExpandedChange = { expandedStipend = it }
                    ) {
                        OutlinedTextField(
                            value = stipendType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Stipend Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStipend) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStipend,
                            onDismissRequest = { expandedStipend = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unpaid") },
                                onClick = {
                                    stipendType = "Unpaid"
                                    customStipendAmount = ""
                                    // Update ViewModel if you have updateSalaryRange method
                                    // viewModel.updateSalaryRange("Unpaid")
                                    expandedStipend = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom Amount") },
                                onClick = {
                                    stipendType = "Custom Amount"
                                    expandedStipend = false
                                }
                            )
                        }
                    }

                    // Show custom amount field if "Custom Amount" is selected
                    if (stipendType == "Custom Amount") {
                        OutlinedTextField(
                            value = customStipendAmount,
                            onValueChange = {
                                customStipendAmount = it
                                // Update ViewModel if you have updateSalaryRange method
                                // viewModel.updateSalaryRange(it)
                            },
                            label = { Text("Stipend Amount") },
                            placeholder = { Text("e.g., ₱5,000 - ₱8,000 per month") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleButton,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Text(
                            text = "Enter the stipend range or amount",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // Error Message
                    if (state.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = state.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            company?.let { comp ->
                                viewModel.createPosting(
                                    userId,
                                    comp.companyName,
                                    comp.companyAddress,
                                    comp.companyDescription,
                                    onSuccess
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Create Posting", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val formattedDate = dateFormat.format(Date(millis))
                            viewModel.updateDeadline(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = PurpleButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PurpleButton,
                    todayContentColor = PurpleButton,
                    todayDateBorderColor = PurpleButton
                )
            )
        }
    }
}