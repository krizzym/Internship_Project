// StudentProfile
package com.example.internshipproject.data.model

data class StudentProfile(
    val firstName: String,
    val middleName: String?,
    val surname: String,
    val email: String,
    val school: String,
    val course: String,
    val yearLevel: String,
    val city: String,
    val barangay: String,
    val internshipTypes: List<String>,
    val skills: String,
    val resumeUri: String?
) {
    val fullName: String
        get() = if (middleName.isNullOrBlank()) {
            "$firstName $surname"
        } else {
            "$firstName $middleName $surname"
        }
}