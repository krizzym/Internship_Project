package com.example.internshipproject.ui.company

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.ui.theme.*
import com.example.internshipproject.viewmodel.ViewApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewApplicationsScreen(
    postingId: String,
    onBack: () -> Unit,
    viewModel: ViewApplicationsViewModel = viewModel()
) {
    LaunchedEffect(postingId) {
        viewModel.loadData(postingId)
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("FirstStep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Internship Connection Platform", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPurple)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Posting Summary Card
            item {
                state.posting?.let { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = post.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Applications for this posting",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(post.location, fontSize = 13.sp, color = TextSecondary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(post.duration, fontSize = 13.sp, color = TextSecondary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.availableSlots} slots", fontSize = 13.sp, color = TextSecondary)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (post.isActive) Color(0xFF10B981) else Color.Gray
                                ) {
                                    Text(
                                        text = if (post.isActive) "Active" else "Closed",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ✅ IMPROVED: Stat Cards with Better Visibility
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImprovedStatCard(
                        title = "Pending",
                        count = state.statusCounts[ApplicationStatus.PENDING] ?: 0,
                        color = Color(0xFFFBBF24),
                        backgroundColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    ImprovedStatCard(
                        title = "Reviewed",
                        count = state.statusCounts[ApplicationStatus.REVIEWED] ?: 0,
                        color = Color(0xFF3B82F6),
                        backgroundColor = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImprovedStatCard(
                        title = "Shortlisted",
                        count = state.statusCounts[ApplicationStatus.SHORTLISTED] ?: 0,
                        color = Color(0xFF8B5CF6),
                        backgroundColor = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f)
                    )
                    ImprovedStatCard(
                        title = "Accepted",
                        count = state.statusCounts[ApplicationStatus.ACCEPTED] ?: 0,
                        color = Color(0xFF10B981),
                        backgroundColor = Color(0xFFD1FAE5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Applications Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All Applications",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PurpleButton.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${state.applications.size}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleButton,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.applications.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Mail,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Applications Yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No students have applied to this posting yet.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ✅ FIXED: Show applications using items() for better list handling
            if (state.applications.isNotEmpty()) {
                items(state.applications) { application ->
                    ApplicationCardDetail(application = application)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ✅ IMPROVED: StatCard with better contrast and visibility
@Composable
fun ImprovedStatCard(
    title: String,
    count: Int,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardWhite
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ApplicationCardDetail(application: Application) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.studentEmail.substringBefore("@"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = application.studentEmail,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (application.status) {
                        ApplicationStatus.PENDING -> Color(0xFFFBBF24)
                        ApplicationStatus.REVIEWED -> Color(0xFF3B82F6)
                        ApplicationStatus.SHORTLISTED -> Color(0xFF8B5CF6)
                        ApplicationStatus.ACCEPTED -> Color(0xFF10B981)
                        ApplicationStatus.REJECTED -> Color(0xFFEF4444)
                    }
                ) {
                    Text(
                        text = application.status.name.replace("_", " "),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextSecondary
                )
                Text(
                    text = "Applied on: ${application.appliedDate}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { /* TODO: Review */ },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Review", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { /* TODO: View Resume */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PurpleButton
                    ),
                    border = BorderStroke(1.5.dp, PurpleButton),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resume", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}