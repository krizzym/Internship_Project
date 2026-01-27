package com.example.internshipproject.data.model

data class Internship(
    val id: String,
    val title: String,
    val companyName: String,
    val companyLogo: String? = null,
    val location: String,
    val workType: String,
    val duration: String,
    val salaryRange: String,
    val availableSlots: Int,
    val description: String,
    val requirements: String,
    val aboutCompany: String,
    val companyAddress: String,
    val applicationDeadline: String,
    val isActive: Boolean = true
)