package com.example.internshipproject.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush


// Primary Colors
val PrimaryDeepBlueButton = Color(0xFF234353)
val BackgroundPurple = Color(0xFF7C7FED)
val CardWhite = Color(0xFFFFFFFF)

// Text Colors
val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)

// Additional Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// In Color.kt or Theme.kt
val BackgroundGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF234353), // Deep Blue
        Color(0xFF5E6F46)  // Olive Green
    ),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)