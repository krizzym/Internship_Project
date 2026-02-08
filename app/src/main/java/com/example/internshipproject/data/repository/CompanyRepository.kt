package com.example.internshipproject.data.repository

import android.net.Uri
import android.util.Log
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Company
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.model.StudentProfile
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CompanyRepository {
    private val firestore = FirebaseManager.firestore
    private val auth = FirebaseManager.auth
    private val storage = FirebaseStorage.getInstance()

    fun getCurrentCompanyId(): String? = auth.currentUser?.uid

// Company Profile
    suspend fun getCompanyProfile(companyId: String): Result<Company> {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.COMPANIES)
                .document(companyId)
                .get()
                .await()

            if (doc.exists()) {
                val company = Company(
                    companyEmail = doc.getString("companyEmail") ?: "",
                    contactNumber = doc.getString("contactNumber") ?: "",
                    password = "",
                    companyName = doc.getString("companyName") ?: "",
                    contactPerson = doc.getString("contactPerson") ?: "",
                    industryType = doc.getString("industryType") ?: "",
                    companyAddress = doc.getString("companyAddress") ?: "",
                    companyDescription = doc.getString("companyDescription") ?: "",
                    logoUri = doc.getString("logoUri")
                )
                Result.success(company)
            } else {
                Result.failure(Exception("Company not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCompanyProfile(companyId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.COMPANIES)
                .document(companyId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCompanyLogo(companyId: String, uri: Uri): Result<String> {
        return try {
            // Validate URI
            if (uri.toString().isEmpty()) {
                Log.e("CompanyRepository", "Invalid URI: empty")
                return Result.failure(Exception("Invalid file selected. Please try again."))
            }

            Log.d("CompanyRepository", "Starting logo upload for company: $companyId")
            Log.d("CompanyRepository", "URI: $uri")
            Log.d("CompanyRepository", "URI Scheme: ${uri.scheme}")

            // Create storage reference with timestamp to ensure uniqueness
            val timestamp = System.currentTimeMillis()
            val fileName = "${companyId}_${timestamp}.jpg"
            val ref = storage.reference.child("${FirebaseManager.StoragePaths.COMPANY_LOGOS}/$fileName")

            Log.d("CompanyRepository", "Storage path: ${FirebaseManager.StoragePaths.COMPANY_LOGOS}/$fileName")

            // Upload file
            val uploadTask = ref.putFile(uri).await()
            Log.d("CompanyRepository", "Upload completed. Bytes transferred: ${uploadTask.bytesTransferred}")

            // Get download URL
            val url = ref.downloadUrl.await().toString()
            Log.d("CompanyRepository", "Download URL obtained: $url")

            Result.success(url)
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Logo upload failed", e)
            Log.e("CompanyRepository", "Error message: ${e.message}")
            Log.e("CompanyRepository", "Error type: ${e.javaClass.simpleName}")

            // Provide user-friendly error message
            val errorMessage = when {
                e.message?.contains("Object does not exist") == true ->
                    "File not found. Please select the image again."
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Permission denied. Please check storage permissions."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else ->
                    "Upload failed: ${e.message ?: "Unknown error"}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    // Internship Posting
    suspend fun createInternship(userId: String, internship: Internship): Result<String> {
        return try {
            val internshipData = hashMapOf(
                "companyId" to userId,
                "title" to internship.title,
                "category" to internship.category,
                "companyName" to internship.companyName,
                "companyLogo" to (internship.companyLogo ?: ""),
                "location" to internship.location,
                "workType" to internship.workType,
                "duration" to internship.duration,
                "salaryRange" to internship.salaryRange,
                "availableSlots" to internship.availableSlots,
                "description" to internship.description,
                "requirements" to internship.requirements,
                "aboutCompany" to internship.aboutCompany,
                "companyAddress" to internship.companyAddress,
                "applicationDeadline" to internship.applicationDeadline,
                "isActive" to internship.isActive,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .add(internshipData)
                .await()

            Log.d("CompanyRepo", "Internship created with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Failed to create internship: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateInternship(postingId: String, internship: Internship): Result<Unit> {
        return try {
            val updates = hashMapOf(
                "title" to internship.title,
                "category" to internship.category,
                "location" to internship.location,
                "workType" to internship.workType,
                "duration" to internship.duration,
                "salaryRange" to internship.salaryRange,
                "availableSlots" to internship.availableSlots,
                "description" to internship.description,
                "requirements" to internship.requirements,
                "aboutCompany" to internship.aboutCompany,
                "companyAddress" to internship.companyAddress,
                "applicationDeadline" to internship.applicationDeadline,
                "isActive" to internship.isActive,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(postingId)
                .update(updates as Map<String, Any>)
                .await()

            Log.d("CompanyRepo", "Internship updated: $postingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Failed to update internship: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateInternship(internshipId: String, updateData: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(internshipId)
                .update(updateData)
                .await()

            Log.d("CompanyRepo", "Internship updated: $internshipId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Failed to update internship: ${e.message}")
            Result.failure(Exception("Failed to update internship: ${e.message}"))
        }
    }

    suspend fun deleteInternship(postingId: String): Result<Unit> {
        return try {
            Log.d("CompanyRepo", "Starting deletion of internship: $postingId")

            // Start a batch write for atomic operation
            // This ensures either ALL operations succeed or ALL fail (no partial state)
            val batch = firestore.batch()

            // Reference to the internship document
            val internshipRef = firestore
                .collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(postingId)

            // Reference to applications collection
            val applicationsRef = firestore
                .collection(FirebaseManager.Collections.APPLICATIONS)

            // Step 1: Delete the internship document
            batch.delete(internshipRef)
            Log.d("CompanyRepo", "Added internship deletion to batch")

            // Step 2: Query all applications related to this internship
            val relatedApplications = applicationsRef
                .whereEqualTo("internshipId", postingId)
                .get()
                .await()

            val applicationCount = relatedApplications.size()
            Log.d("CompanyRepo", "Found $applicationCount related applications to delete")

            // Step 3: Add all application deletions to the batch
            relatedApplications.documents.forEach { applicationDoc ->
                batch.delete(applicationDoc.reference)
                Log.d("CompanyRepo", "Added application ${applicationDoc.id} to deletion batch")
            }

            // Step 4: Commit the entire batch operation atomically
            batch.commit().await()

            Log.d("CompanyRepo", "✅ Successfully deleted internship $postingId and $applicationCount applications")

            // Return success
            Result.success(Unit)

        } catch (e: Exception) {
            // Log detailed error information for debugging
            Log.e("CompanyRepo", "❌ Failed to delete internship: ${e.message}", e)

            // Return failure with the exception
            Result.failure(e)
        }
    }

    fun getCompanyInternshipsFlow(companyId: String): Flow<List<Internship>> = callbackFlow {
        val listener = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
            .whereEqualTo("companyId", companyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CompanyRepo", "Error listening to company internships: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val internships = snapshot.documents.mapNotNull { doc ->
                        try {
                            Internship(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                category = doc.getString("category") ?: "Engineering and technology",
                                companyName = doc.getString("companyName") ?: "",
                                companyLogo = doc.getString("companyLogo"),
                                location = doc.getString("location") ?: "",
                                workType = doc.getString("workType") ?: "",
                                duration = doc.getString("duration") ?: "",
                                salaryRange = doc.getString("salaryRange") ?: "",
                                availableSlots = doc.getLong("availableSlots")?.toInt() ?: 0,
                                description = doc.getString("description") ?: "",
                                requirements = doc.getString("requirements") ?: "",
                                aboutCompany = doc.getString("aboutCompany") ?: "",
                                companyAddress = doc.getString("companyAddress") ?: "",
                                applicationDeadline = doc.getString("applicationDeadline") ?: "",
                                isActive = doc.getBoolean("isActive") ?: true
                            )
                        } catch (e: Exception) {
                            Log.e("CompanyRepo", "Error parsing internship: ${e.message}")
                            null
                        }
                    }
                    Log.d("CompanyRepo", "Company internships updated: ${internships.size}")
                    trySend(internships)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getCompanyInternships(companyId: String): Result<List<Internship>> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("companyId", companyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val internships = snapshot.documents.mapNotNull { doc ->
                try {
                    Internship(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "Engineering and technology",
                        companyName = doc.getString("companyName") ?: "",
                        companyLogo = doc.getString("companyLogo"),
                        location = doc.getString("location") ?: "",
                        workType = doc.getString("workType") ?: "",
                        duration = doc.getString("duration") ?: "",
                        salaryRange = doc.getString("salaryRange") ?: "",
                        availableSlots = doc.getLong("availableSlots")?.toInt() ?: 0,
                        description = doc.getString("description") ?: "",
                        requirements = doc.getString("requirements") ?: "",
                        aboutCompany = doc.getString("aboutCompany") ?: "",
                        companyAddress = doc.getString("companyAddress") ?: "",
                        applicationDeadline = doc.getString("applicationDeadline") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(internships)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInternshipById(postingId: String): Result<Internship> {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(postingId)
                .get()
                .await()

            if (doc.exists()) {
                val internship = Internship(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    category = doc.getString("category") ?: "Engineering and technology",
                    companyName = doc.getString("companyName") ?: "",
                    companyLogo = doc.getString("companyLogo"),
                    location = doc.getString("location") ?: "",
                    workType = doc.getString("workType") ?: "",
                    duration = doc.getString("duration") ?: "",
                    salaryRange = doc.getString("salaryRange") ?: "",
                    availableSlots = doc.getLong("availableSlots")?.toInt() ?: 0,
                    description = doc.getString("description") ?: "",
                    requirements = doc.getString("requirements") ?: "",
                    aboutCompany = doc.getString("aboutCompany") ?: "",
                    companyAddress = doc.getString("companyAddress") ?: "",
                    applicationDeadline = doc.getString("applicationDeadline") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true
                )
                Result.success(internship)
            } else {
                Result.failure(Exception("Posting not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByPosting(postingId: String): Result<List<Application>> {
        return try {
            Log.d("CompanyRepo", "Querying applications for posting: $postingId")

            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", postingId)
                .get()
                .await()

            Log.d("CompanyRepo", "Found ${snapshot.documents.size} application documents")

            val applications = snapshot.documents.mapNotNull { doc ->
                try {
                    Application(
                        id = doc.id,
                        internshipId = doc.getString("internshipId") ?: "",
                        internshipTitle = doc.getString("internshipTitle") ?: "",
                        companyName = doc.getString("companyName") ?: "",
                        studentEmail = doc.getString("studentEmail") ?: "",
                        coverLetter = doc.getString("coverLetter") ?: "",
                        resumeBase64 = doc.getString("resumeBase64"),
                        resumeFileName = doc.getString("resumeFileName"),
                        resumeSize = doc.getLong("resumeSize"),
                        resumeMimeType = doc.getString("resumeMimeType"),
                        status = ApplicationStatus.valueOf(
                            doc.getString("status") ?: ApplicationStatus.PENDING.name
                        ),
                        appliedDate = doc.getString("appliedDate") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("CompanyRepo", "Error mapping application: ${e.message}")
                    null
                }
            }

            val sortedApps = applications.sortedByDescending { it.appliedDate }

            Log.d("CompanyRepo", "Successfully mapped ${sortedApps.size} applications")
            Result.success(sortedApps)
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Error getting applications: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAllCompanyApplications(companyId: String): Result<List<Application>> {
        return try {
            val internshipsSnapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("companyId", companyId)
                .get()
                .await()

            val internshipIds = internshipsSnapshot.documents.map { it.id }

            if (internshipIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val allApplications = mutableListOf<Application>()

            for (internshipId in internshipIds) {
                val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                    .whereEqualTo("internshipId", internshipId)
                    .get()
                    .await()

                val applications = snapshot.documents.mapNotNull { doc ->
                    try {
                        Application(
                            id = doc.id,
                            internshipId = doc.getString("internshipId") ?: "",
                            internshipTitle = doc.getString("internshipTitle") ?: "",
                            companyName = doc.getString("companyName") ?: "",
                            studentEmail = doc.getString("studentEmail") ?: "",
                            coverLetter = doc.getString("coverLetter") ?: "",
                            resumeBase64 = doc.getString("resumeBase64"),
                            resumeFileName = doc.getString("resumeFileName"),
                            resumeSize = doc.getLong("resumeSize"),
                            resumeMimeType = doc.getString("resumeMimeType"),
                            status = ApplicationStatus.valueOf(
                                doc.getString("status") ?: ApplicationStatus.PENDING.name
                            ),
                            appliedDate = doc.getString("appliedDate") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                allApplications.addAll(applications)
            }

            Result.success(allApplications.sortedByDescending { it.appliedDate })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationById(applicationId: String): Result<Application> {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .get()
                .await()

            if (doc.exists()) {
                val application = Application(
                    id = doc.id,
                    internshipId = doc.getString("internshipId") ?: "",
                    internshipTitle = doc.getString("internshipTitle") ?: "",
                    companyName = doc.getString("companyName") ?: "",
                    studentEmail = doc.getString("studentEmail") ?: "",
                    coverLetter = doc.getString("coverLetter") ?: "",
                    resumeBase64 = doc.getString("resumeBase64"),
                    resumeFileName = doc.getString("resumeFileName"),
                    resumeSize = doc.getLong("resumeSize"),
                    resumeMimeType = doc.getString("resumeMimeType"),
                    status = ApplicationStatus.valueOf(
                        doc.getString("status") ?: ApplicationStatus.PENDING.name
                    ),
                    appliedDate = doc.getString("appliedDate") ?: ""
                )
                Result.success(application)
            } else {
                Result.failure(Exception("Application not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(
        applicationId: String,
        newStatus: ApplicationStatus,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to System.currentTimeMillis()
            )

            // Add notes if provided
            if (!notes.isNullOrBlank()) {
                updateData["reviewerNotes"] = notes
            }

            firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .update(updateData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationCountForPosting(postingId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", postingId)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationStatusCounts(postingId: String): Map<ApplicationStatus, Int> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", postingId)
                .get()
                .await()

            val counts = mutableMapOf<ApplicationStatus, Int>()
            ApplicationStatus.values().forEach { counts[it] = 0 }

            snapshot.documents.forEach { doc ->
                try {
                    val status = ApplicationStatus.valueOf(
                        doc.getString("status") ?: ApplicationStatus.PENDING.name
                    )
                    counts[status] = (counts[status] ?: 0) + 1
                } catch (e: Exception) {
                    // Skip invalid status
                }
            }

            counts
        } catch (e: Exception) {
            ApplicationStatus.values().associateWith { 0 }
        }
    }

    suspend fun getStudentProfileByEmail(email: String): Result<StudentProfile> {
        return try {
            Log.d("CompanyRepo", "Fetching student profile for: $email")

            val snapshot = firestore.collection(FirebaseManager.Collections.STUDENTS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]
                val profile = StudentProfile(
                    firstName = doc.getString("firstName") ?: "",
                    middleName = doc.getString("middleName") ?: "",
                    surname = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    school = doc.getString("school") ?: "",
                    course = doc.getString("course") ?: "",
                    yearLevel = doc.getString("yearLevel") ?: "",
                    city = doc.getString("city") ?: "",
                    barangay = doc.getString("barangay") ?: "",
                    internshipTypes = listOf(doc.getString("internshipTypes") ?: ""),
                    skills = doc.getString("skills") ?: "",
                    resumeUri = doc.getString("resumeUri")
                )
                Log.d("CompanyRepo", "Student profile found: ${profile.firstName} ${profile.surname}")
                Result.success(profile)
            } else {
                Log.w("CompanyRepo", "Student profile not found for: $email")
                Result.failure(Exception("Student profile not found"))
            }
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Error fetching student profile: ${e.message}")
            Result.failure(e)
        }
    }
    suspend fun getStudentProfile(studentEmail: String): Result<StudentProfile?> {
        return try {
            Log.d("CompanyRepo", "Fetching student profile for: $studentEmail")

            val snapshot = firestore.collection(FirebaseManager.Collections.STUDENTS)
                .whereEqualTo("email", studentEmail)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isEmpty()) {
                Log.w("CompanyRepo", "No student profile found for: $studentEmail")
                return Result.success(null)
            }

            val doc = snapshot.documents.first()
            val profile = StudentProfile(
                firstName = doc.getString("firstName") ?: "",
                middleName = doc.getString("middleName"),
                surname = doc.getString("lastName") ?: "",
                email = doc.getString("email") ?: "",
                school = doc.getString("school") ?: "",
                course = doc.getString("course") ?: "",
                yearLevel = doc.getString("yearLevel") ?: "",
                city = doc.getString("city") ?: "",
                barangay = doc.getString("barangay") ?: "",
                internshipTypes = (doc.get("internshipTypes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                skills = doc.getString("skills") ?: "",
                resumeUri = doc.getString("resumeUri")
            )

            Log.d("CompanyRepo", "Student profile found: ${profile.fullName}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("CompanyRepo", "Error fetching student profile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
