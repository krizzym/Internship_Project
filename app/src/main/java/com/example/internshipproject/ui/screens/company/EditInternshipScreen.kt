package com.example.internshipproject.ui.screens.company

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.internshipproject.viewmodel.EditInternshipViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInternshipScreen(
    navController: NavController,
    internshipId: String,
    viewModel: EditInternshipViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Load internship data when screen opens
    LaunchedEffect(internshipId) {
        viewModel.loadInternship(internshipId)
    }

    // Show success message and navigate back
    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5B7BED),
                        Color(0xFF8B6EC7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        "FirstStep",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )

            // Main Content
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Text(
                            text = "Edit Internship Posting",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Update your internship opportunity details",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Error Message
                        state.errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Job Information Section
                        SectionHeader("Job Information")

                        OutlinedTextField(
                            value = state.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text("Job Title *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("Job Description *") },
                            placeholder = { Text("Provide a detailed description of the internship role") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(bottom = 16.dp),
                            maxLines = 6
                        )

                        OutlinedTextField(
                            value = state.requirements,
                            onValueChange = { viewModel.updateRequirements(it) },
                            label = { Text("Requirements *") },
                            placeholder = { Text("Specify what skills and qualifications are needed") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(bottom = 24.dp),
                            maxLines = 6
                        )

                        // Internship Details Section
                        SectionHeader("Internship Details")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Internship Type Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                var expanded by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = state.workType,
                                    onValueChange = { },
                                    label = { Text("Internship Type *") },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("On-site", "Remote", "Hybrid").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                viewModel.updateWorkType(type)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                                // Clickable surface to open dropdown
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    color = Color.Transparent,
                                    onClick = { expanded = !expanded }
                                ) {}
                            }

                            // Location
                            OutlinedTextField(
                                value = state.location,
                                onValueChange = { viewModel.updateLocation(it) },
                                label = { Text("Location *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Duration
                            OutlinedTextField(
                                value = state.duration,
                                onValueChange = { viewModel.updateDuration(it) },
                                label = { Text("Duration *") },
                                placeholder = { Text("e.g., 3-6 months") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Stipend
                            OutlinedTextField(
                                value = state.salaryRange,
                                onValueChange = { viewModel.updateSalaryRange(it) },
                                label = { Text("Stipend") },
                                placeholder = { Text("e.g., 5,000 - 8,000") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Number of Slots
                            OutlinedTextField(
                                value = state.availableSlots,
                                onValueChange = { viewModel.updateAvailableSlots(it) },
                                label = { Text("Number of Slots *") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            // Application Deadline with Date Picker
                            OutlinedTextField(
                                value = state.applicationDeadline,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Application Deadline") },
                                placeholder = { Text("MM/DD/YYYY") },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker = true },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = "Select date",
                                            tint = Color(0xFF7B68EE)
                                        )
                                    }
                                },
                                singleLine = true
                            )
                        }

                        // Posting Status Section
                        SectionHeader("Posting Status")

                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = if (state.isActive) "Active - Visible to students" else "Inactive - Hidden from students",
                                onValueChange = { },
                                label = { Text("Status *") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Active - Visible to students") },
                                    onClick = {
                                        viewModel.updateIsActive(true)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Inactive - Hidden from students") },
                                    onClick = {
                                        viewModel.updateIsActive(false)
                                        expanded = false
                                    }
                                )
                            }
                            // Clickable surface to open dropdown
                            Surface(
                                modifier = Modifier.matchParentSize(),
                                color = Color.Transparent,
                                onClick = { expanded = !expanded }
                            ) {}
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Gray
                                )
                            ) {
                                Text("Cancel")
                            }

                            // Update Button
                            Button(
                                onClick = { viewModel.updateInternship(internshipId) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7B68EE)
                                )
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text("Update Posting")
                                }
                            }
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
                            viewModel.updateApplicationDeadline(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = Color(0xFF7B68EE))
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
                    selectedDayContainerColor = Color(0xFF7B68EE),
                    todayContentColor = Color(0xFF7B68EE),
                    todayDateBorderColor = Color(0xFF7B68EE)
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
    )
}