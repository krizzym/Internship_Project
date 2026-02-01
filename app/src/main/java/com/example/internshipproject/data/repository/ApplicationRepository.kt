package com.example.internshipproject.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplicationRepository(
    private val context: Context? = null
) {

    private val firestore: FirebaseFirestore = FirebaseManager.firestore

    /**
     * Submit application with resume (Base64 encoded)
     */
    suspend fun submitApplication(
        internshipId: String,
        internshipTitle: String,
        companyName: String,
        studentEmail: String,
        coverLetter: String,
        resumeUri: Uri? = null
    ): Result<Application> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
            if (currentUserId == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Check if already applied - using studentId for consistency
            val existingApp = hasAppliedToInternship(internshipId, currentUserId)
            if (existingApp) {
                return Result.failure(Exception("You have already applied to this internship"))
            }

            val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            } else {
                android.text.format.DateFormat.format("MMM dd, yyyy", System.currentTimeMillis()).toString()
            }

            // Process resume if provided
            var resumeBase64: String? = null
            var resumeFileName: String? = null
            var resumeSize: Long? = null
            var resumeMimeType: String? = null

            if (resumeUri != null && context != null) {
                try {
                    val resumeData = encodeResumeToBase64(context, resumeUri)
                    resumeBase64 = resumeData.base64String
                    resumeFileName = resumeData.fileName
                    resumeSize = resumeData.fileSize
                    resumeMimeType = resumeData.mimeType

                    Log.d("ApplicationRepo", "Resume encoded: $resumeFileName, Size: $resumeSize bytes")
                } catch (e: Exception) {
                    Log.e("ApplicationRepo", "Resume encoding failed: ${e.message}")
                    resumeBase64 = null
                }
            }

            val applicationData = hashMapOf(
                "internshipId" to internshipId,
                "internshipTitle" to internshipTitle,
                "companyName" to companyName,
                "studentEmail" to studentEmail,
                "studentId" to currentUserId, // ✅ CRITICAL: Store studentId
                "coverLetter" to coverLetter,
                "status" to ApplicationStatus.PENDING.name, // ✅ Single source of truth for status
                "appliedDate" to currentDate,
                "createdAt" to System.currentTimeMillis(),
                "resumeBase64" to (resumeBase64 ?: ""),
                "resumeFileName" to (resumeFileName ?: ""),
                "resumeSize" to (resumeSize ?: 0L),
                "resumeMimeType" to (resumeMimeType ?: "")
            )

            val docRef = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .add(applicationData)
                .await()

            val application = Application(
                id = docRef.id,
                internshipId = internshipId,
                internshipTitle = internshipTitle,
                companyName = companyName,
                studentEmail = studentEmail,
                coverLetter = coverLetter,
                status = ApplicationStatus.PENDING,
                appliedDate = currentDate,
                resumeBase64 = resumeBase64,
                resumeFileName = resumeFileName,
                resumeSize = resumeSize,
                resumeMimeType = resumeMimeType
            )

            Result.success(application)

        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Submit application error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * ✅ NEW: Get all applications by studentId (primary method)
     * This is the correct way to query applications for the logged-in student
     */
    suspend fun getApplicationsByStudentId(studentId: String): List<Application> {
        return try {
            Log.d("ApplicationRepo", "Fetching applications for studentId: $studentId")

            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("studentId", studentId) // ✅ Query by studentId
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d("ApplicationRepo", "Found ${snapshot.documents.size} applications")

            snapshot.documents.mapNotNull { doc ->
                try {
                    Application(
                        id = doc.id,
                        internshipId = doc.getString("internshipId") ?: "",
                        internshipTitle = doc.getString("internshipTitle") ?: "",
                        companyName = doc.getString("companyName") ?: "",
                        studentEmail = doc.getString("studentEmail") ?: "",
                        coverLetter = doc.getString("coverLetter") ?: "",
                        status = ApplicationStatus.valueOf(
                            doc.getString("status") ?: ApplicationStatus.PENDING.name
                        ), // ✅ Read status from Firestore (single source of truth)
                        appliedDate = doc.getString("appliedDate") ?: "",
                        resumeBase64 = doc.getString("resumeBase64"),
                        resumeFileName = doc.getString("resumeFileName"),
                        resumeSize = doc.getLong("resumeSize"),
                        resumeMimeType = doc.getString("resumeMimeType")
                    )
                } catch (e: Exception) {
                    Log.e("ApplicationRepo", "Error parsing application: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error fetching applications: ${e.message}")
            emptyList()
        }
    }

    /**
     * ✅ NEW: Real-time listener for student applications
     * Returns a Flow that emits updates whenever applications change
     */
    fun observeApplicationsByStudentId(studentId: String): Flow<List<Application>> = callbackFlow {
        Log.d("ApplicationRepo", "Setting up real-time listener for studentId: $studentId")

        val listener = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ApplicationRepo", "Listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val applications = snapshot.documents.mapNotNull { doc ->
                        try {
                            Application(
                                id = doc.id,
                                internshipId = doc.getString("internshipId") ?: "",
                                internshipTitle = doc.getString("internshipTitle") ?: "",
                                companyName = doc.getString("companyName") ?: "",
                                studentEmail = doc.getString("studentEmail") ?: "",
                                coverLetter = doc.getString("coverLetter") ?: "",
                                status = ApplicationStatus.valueOf(
                                    doc.getString("status") ?: ApplicationStatus.PENDING.name
                                ),
                                appliedDate = doc.getString("appliedDate") ?: "",
                                resumeBase64 = doc.getString("resumeBase64"),
                                resumeFileName = doc.getString("resumeFileName"),
                                resumeSize = doc.getLong("resumeSize"),
                                resumeMimeType = doc.getString("resumeMimeType")
                            )
                        } catch (e: Exception) {
                            Log.e("ApplicationRepo", "Error parsing application: ${e.message}")
                            null
                        }
                    }

                    Log.d("ApplicationRepo", "Real-time update: ${applications.size} applications")
                    trySend(applications)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all applications by student email (kept for backward compatibility)
     */
    suspend fun getApplicationsByStudent(studentEmail: String): List<Application> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Application(
                        id = doc.id,
                        internshipId = doc.getString("internshipId") ?: "",
                        internshipTitle = doc.getString("internshipTitle") ?: "",
                        companyName = doc.getString("companyName") ?: "",
                        studentEmail = doc.getString("studentEmail") ?: "",
                        coverLetter = doc.getString("coverLetter") ?: "",
                        status = ApplicationStatus.valueOf(
                            doc.getString("status") ?: ApplicationStatus.PENDING.name
                        ),
                        appliedDate = doc.getString("appliedDate") ?: "",
                        resumeBase64 = doc.getString("resumeBase64"),
                        resumeFileName = doc.getString("resumeFileName"),
                        resumeSize = doc.getLong("resumeSize"),
                        resumeMimeType = doc.getString("resumeMimeType")
                    )
                } catch (e: Exception) {
                    Log.e("ApplicationRepo", "Error parsing application: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error fetching applications: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get all applications for a specific internship
     */
    suspend fun getApplicationsByInternship(internshipId: String): List<Application> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", internshipId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Application(
                        id = doc.id,
                        internshipId = doc.getString("internshipId") ?: "",
                        internshipTitle = doc.getString("internshipTitle") ?: "",
                        companyName = doc.getString("companyName") ?: "",
                        studentEmail = doc.getString("studentEmail") ?: "",
                        coverLetter = doc.getString("coverLetter") ?: "",
                        status = ApplicationStatus.valueOf(
                            doc.getString("status") ?: ApplicationStatus.PENDING.name
                        ),
                        appliedDate = doc.getString("appliedDate") ?: "",
                        resumeBase64 = doc.getString("resumeBase64"),
                        resumeFileName = doc.getString("resumeFileName"),
                        resumeSize = doc.getLong("resumeSize"),
                        resumeMimeType = doc.getString("resumeMimeType")
                    )
                } catch (e: Exception) {
                    Log.e("ApplicationRepo", "Error parsing application: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error fetching applications: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get specific application by internship and student
     */
    suspend fun getApplicationByInternshipAndStudent(
        internshipId: String,
        studentEmail: String
    ): Application? {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", internshipId)
                .whereEqualTo("studentEmail", studentEmail)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                null
            } else {
                val doc = snapshot.documents[0]
                Application(
                    id = doc.id,
                    internshipId = doc.getString("internshipId") ?: "",
                    internshipTitle = doc.getString("internshipTitle") ?: "",
                    companyName = doc.getString("companyName") ?: "",
                    studentEmail = doc.getString("studentEmail") ?: "",
                    coverLetter = doc.getString("coverLetter") ?: "",
                    status = ApplicationStatus.valueOf(
                        doc.getString("status") ?: ApplicationStatus.PENDING.name
                    ),
                    appliedDate = doc.getString("appliedDate") ?: "",
                    resumeBase64 = doc.getString("resumeBase64"),
                    resumeFileName = doc.getString("resumeFileName"),
                    resumeSize = doc.getLong("resumeSize"),
                    resumeMimeType = doc.getString("resumeMimeType")
                )
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error fetching application: ${e.message}")
            null
        }
    }

    /**
     * ✅ UPDATED: Check if already applied using studentId
     */
    private suspend fun hasAppliedToInternship(internshipId: String, studentId: String): Boolean {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", internshipId)
                .whereEqualTo("studentId", studentId) // ✅ Check by studentId
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error checking application: ${e.message}")
            false
        }
    }

    /**
     * Update application status (used by company)
     */
    suspend fun updateApplicationStatus(
        applicationId: String,
        newStatus: ApplicationStatus
    ): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .document(applicationId)
                .update("status", newStatus.name) // ✅ Update the single source of truth
                .await()

            Log.d("ApplicationRepo", "Application $applicationId status updated to $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error updating application status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * ✅ UPDATED: Get application stats by studentId
     */
    suspend fun getApplicationStatsByStudentId(studentId: String): Map<ApplicationStatus, Int> {
        return try {
            val applications = getApplicationsByStudentId(studentId)
            val stats = mutableMapOf<ApplicationStatus, Int>()

            ApplicationStatus.values().forEach { status ->
                stats[status] = applications.count { it.status == status }
            }

            stats
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error getting application stats: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Get application stats by email (kept for backward compatibility)
     */
    suspend fun getApplicationStats(studentEmail: String): Map<ApplicationStatus, Int> {
        return try {
            val applications = getApplicationsByStudent(studentEmail)
            val stats = mutableMapOf<ApplicationStatus, Int>()

            ApplicationStatus.values().forEach { status ->
                stats[status] = applications.count { it.status == status }
            }

            stats
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Error getting application stats: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Encode resume to Base64
     */
    private fun encodeResumeToBase64(context: Context, uri: Uri): ResumeData {
        val contentResolver = context.contentResolver
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")

        val bytes = inputStream.readBytes()
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

        val fileName = uri.lastPathSegment ?: "resume.pdf"
        val fileSize = bytes.size.toLong()
        val mimeType = contentResolver.getType(uri) ?: "application/pdf"

        inputStream.close()

        return ResumeData(
            base64String = base64String,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType
        )
    }

    data class ResumeData(
        val base64String: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    )
}
