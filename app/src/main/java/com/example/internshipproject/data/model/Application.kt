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
    val internship: Internship? = null,
    // Resume fields (stored as Base64 in Firestore)
    val resumeBase64: String? = null,
    val resumeFileName: String? = null,
    val resumeSize: Long? = null,
    val resumeMimeType: String? = null,
    // Company notes field
    val companyNotes: String? = null,
    // Student profile data (fetched separately, not stored in application doc)
    val studentProfile: StudentProfile? = null
) {
    // Helper to check if resume exists
    val hasResume: Boolean
        get() = !resumeBase64.isNullOrEmpty()
}

enum class ApplicationStatus {
    PENDING,
    REVIEWED,
    SHORTLISTED,
    ACCEPTED,
    REJECTED
}