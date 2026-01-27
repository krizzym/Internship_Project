package com.example.internshipproject.data.model

data class Application(
    val id: String,
    val internshipId: String,
    val internshipTitle: String,
    val companyName: String,
    val studentEmail: String,
    val coverLetter: String,
    val status: ApplicationStatus,
    val appliedDate: String,
    val internship: Internship? = null
)

enum class ApplicationStatus {
    PENDING,
    REVIEWED,
    SHORTLISTED,
    ACCEPTED,
    REJECTED
}