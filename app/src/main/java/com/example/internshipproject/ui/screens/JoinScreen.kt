package com.example.internshipproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.internshipproject.ui.components.PrimaryButton
import com.example.internshipproject.ui.theme.CardWhite
import com.example.internshipproject.ui.theme.PrimaryDeepBlueButton
import com.example.internshipproject.ui.theme.TextPrimary
import com.example.internshipproject.ui.theme.TextSecondary
import com.example.internshipproject.ui.theme.BackgroundGradientBrush

@Composable
fun JoinScreen(
    onStudentClick: () -> Unit,
    onCompanyClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp), // Maximum width for larger screens
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Join FirstStep",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose your account type to get started",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Student Card
                    AccountTypeCard(
                        title = "I'm a Student",
                        description = "Looking for internship opportunities to gain experience and develop my skills",
                        benefits = listOf(
                            "Browse internship opportunities",
                            "Apply to multiple companies",
                            "Build your professional profile"
                        ),
                        buttonText = "Register as Student →",
                        onClick = onStudentClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Company Card
                    AccountTypeCard(
                        title = "I'm a Company",
                        description = "Looking to hire talented interns and provide valuable work experience",
                        benefits = listOf(
                            "Post internship opportunities",
                            "Review qualified candidates",
                            "Manage your hiring process"
                        ),
                        buttonText = "Register as Company →",
                        onClick = onCompanyClick
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        TextButton(
                            onClick = onLoginClick,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Log in",
                                fontSize = 13.sp,
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
fun AccountTypeCard(
    title: String,
    description: String,
    benefits: List<String>,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Benefits
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                benefits.forEach { benefit ->
                    BenefitItem(text = "✓ $benefit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = buttonText,
                onClick = onClick
            )
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}