package com.example.internshipproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.internshipproject.ui.theme.CardWhite
import com.example.internshipproject.ui.theme.BackgroundGradientBrush

@Composable
fun SuccessScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "✓",
                fontSize = 80.sp,
                color = CardWhite
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Login Successful",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CardWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dashboard Coming Soon",
                fontSize = 18.sp,
                color = CardWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The main dashboard and post-login features will be available in the next phase.",
                fontSize = 14.sp,
                color = CardWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}