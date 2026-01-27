package com.example.internshipproject.data.model

data class UserSession(
    val userRole: String,
    val token: String,
    val email: String
)