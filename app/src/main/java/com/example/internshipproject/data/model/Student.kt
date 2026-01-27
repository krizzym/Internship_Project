package com.example.internshipproject.data.model

data class Student(
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val email: String,
    val password: String,
    val school: String,
    val course: String,
    val yearLevel: String,
    val city: String,
    val barangay: String,
    val internshipTypes: List<String>,
    val skills: String,
    val resumeUri: String?
)