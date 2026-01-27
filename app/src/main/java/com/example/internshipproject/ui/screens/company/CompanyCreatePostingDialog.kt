package com.example.internshipproject.ui.screens.company

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("Create Internship Posting", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.jobTitle,
                    onValueChange = { viewModel.updateJobTitle(it) },
                    label = { Text("Job Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.errors.containsKey("jobTitle"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.jobDescription,
                    onValueChange = { viewModel.updateJobDescription(it) },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = state.errors.containsKey("jobDescription"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.requirements,
                    onValueChange = { viewModel.updateRequirements(it) },
                    label = { Text("Requirements *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = state.errors.containsKey("requirements"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                var expandedType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = it }) {
                    OutlinedTextField(
                        value = state.internshipType.ifEmpty { "Select type" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        listOf("On-site", "Remote", "Hybrid").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { viewModel.updateInternshipType(type); expandedType = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = { viewModel.updateLocation(it) },
                        label = { Text("Location *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = state.duration,
                        onValueChange = { viewModel.updateDuration(it) },
                        label = { Text("Duration *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.slots,
                        onValueChange = { viewModel.updateSlots(it) },
                        label = { Text("Slots *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Date Picker Field for Application Deadline
                    OutlinedTextField(
                        value = state.deadline,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Deadline *") },
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
                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            company?.let { comp ->
                                viewModel.createPosting(userId, comp.companyName, comp.companyAddress, comp.companyDescription, onSuccess)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("Create")
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