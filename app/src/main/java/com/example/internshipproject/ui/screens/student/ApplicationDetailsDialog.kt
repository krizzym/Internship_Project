//ApplicationDetailsDialog.kt
package com.example.internshipproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.theme.*

@Composable
fun ApplicationDetailsDialog(
    application: Application,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PurpleButton,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Application Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Status: ${getStatusText(application.status)}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = getStatusBackgroundColor(application.status)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = getStatusColor(application.status)
                            ) {
                                Text(
                                    text = application.status.name,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = getStatusMessage(application.status),
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Internship Information
                    SectionHeader("Internship Information")

                    InfoRow(label = "Position", value = application.internshipTitle)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                    InfoRow(label = "Company", value = application.companyName)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                    InfoRow(label = "Applied Date", value = application.appliedDate)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Application Details
                    SectionHeader("Your Application")

                    InfoRow(label = "Applicant Email", value = application.studentEmail)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cover Letter
                    Text(
                        text = "Cover Letter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundPurple.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = application.coverLetter,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resume Status
                    if (application.hasResume) {
                        InfoRow(
                            label = "Resume",
                            value = "✓ Attached (${application.resumeFileName ?: "Resume.pdf"})"
                        )
                    } else {
                        InfoRow(
                            label = "Resume",
                            value = "No resume attached"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status Timeline
                    SectionHeader("Application Timeline")

                    TimelineItem(
                        status = "Submitted",
                        date = application.appliedDate,
                        isCompleted = true
                    )

                    TimelineItem(
                        status = "Under Review",
                        date = if (application.status != ApplicationStatus.PENDING) "Reviewed" else "Pending",
                        isCompleted = application.status != ApplicationStatus.PENDING
                    )

                    TimelineItem(
                        status = "Decision",
                        date = when (application.status) {
                            ApplicationStatus.ACCEPTED -> "Accepted"
                            ApplicationStatus.REJECTED -> "Rejected"
                            ApplicationStatus.SHORTLISTED -> "Shortlisted"
                            else -> "Pending"
                        },
                        isCompleted = application.status == ApplicationStatus.ACCEPTED ||
                                application.status == ApplicationStatus.REJECTED ||
                                application.status == ApplicationStatus.SHORTLISTED,
                        isLast = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleButton
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TimelineItem(
    status: String,
    date: String,
    isCompleted: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isCompleted) PurpleButton else Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            ) {}
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(if (isCompleted) PurpleButton else Color.Gray.copy(alpha = 0.3f))
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = status,
                fontSize = 14.sp,
                fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCompleted) TextPrimary else TextSecondary
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

private fun getStatusText(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
    }
}

private fun getStatusMessage(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Your application is awaiting review"
        ApplicationStatus.REVIEWED -> "Your application has been reviewed"
        ApplicationStatus.SHORTLISTED -> "Congratulations! You've been shortlisted"
        ApplicationStatus.ACCEPTED -> "Congratulations! Your application was accepted"
        ApplicationStatus.REJECTED -> "Unfortunately, your application was not selected"
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFFA726)
        ApplicationStatus.REVIEWED -> Color(0xFF42A5F5)
        ApplicationStatus.SHORTLISTED -> Color(0xFF8B5CF6)
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFEF5350)
    }
}

private fun getStatusBackgroundColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFFF3E0)
        ApplicationStatus.REVIEWED -> Color(0xFFE3F2FD)
        ApplicationStatus.SHORTLISTED -> Color(0xFFF3E5F5)
        ApplicationStatus.ACCEPTED -> Color(0xFFE8F5E9)
        ApplicationStatus.REJECTED -> Color(0xFFFFEBEE)
    }
}