// ApplicationRepository.kt - UPDATED with Resume Base64 Support
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
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplicationRepository(
    private val context: Context? = null
) {

    private val firestore: FirebaseFirestore = FirebaseManager.firestore

    /**
     * ✅ UPDATED: Submit application with resume (Base64 encoded)
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
            // Check if already applied
            val existingApp = hasAppliedToInternship(internshipId, studentEmail)
            if (existingApp) {
                return Result.failure(Exception("You have already applied to this internship"))
            }

            val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            } else {
                android.text.format.DateFormat.format("MMM dd, yyyy", System.currentTimeMillis()).toString()
            }

            // ✅ NEW: Process resume if provided
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
                    // Continue without resume rather than failing the application
                    resumeBase64 = null
                }
            }

            val applicationData = hashMapOf(
                "internshipId" to internshipId,
                "internshipTitle" to internshipTitle,
                "companyName" to companyName,
                "studentEmail" to studentEmail,
                "studentId" to FirebaseManager.getCurrentUserId(),
                "coverLetter" to coverLetter,
                "status" to ApplicationStatus.PENDING.name,
                "appliedDate" to currentDate,
                "createdAt" to System.currentTimeMillis(),
                // ✅ NEW: Resume fields
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
                internship = null,
                resumeBase64 = resumeBase64,
                resumeFileName = resumeFileName,
                resumeSize = resumeSize,
                resumeMimeType = resumeMimeType
            )

            Log.d("ApplicationRepo", "Application submitted successfully with resume: ${resumeFileName ?: "No resume"}")
            Result.success(application)
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Failed to submit application: ${e.message}")
            Result.failure(Exception("Failed to submit application: ${e.message}"))
        }
    }

    /**
     * Get applications by student
     */
    suspend fun getApplicationsByStudent(studentEmail: String): Result<List<Application>> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("studentEmail", studentEmail)
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
                        status = ApplicationStatus.valueOf(
                            doc.getString("status") ?: ApplicationStatus.PENDING.name
                        ),
                        appliedDate = doc.getString("appliedDate") ?: "",
                        internship = null,
                        // ✅ Load resume fields
                        resumeBase64 = doc.getString("resumeBase64")?.takeIf { it.isNotEmpty() },
                        resumeFileName = doc.getString("resumeFileName")?.takeIf { it.isNotEmpty() },
                        resumeSize = doc.getLong("resumeSize"),
                        resumeMimeType = doc.getString("resumeMimeType")?.takeIf { it.isNotEmpty() }
                    )
                } catch (e: Exception) {
                    Log.e("ApplicationRepo", "Error parsing application: ${e.message}")
                    null
                }
            }

            // Sort by createdAt in memory (newest first)
            val sortedApps = applications.sortedByDescending {
                try {
                    snapshot.documents.find { it.id == it.id }?.getLong("createdAt") ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }

            Result.success(sortedApps)
        } catch (e: Exception) {
            Log.e("ApplicationRepo", "Failed to get applications: ${e.message}")
            Result.failure(Exception("Failed to get applications: ${e.message}"))
        }
    }

    /**
     * Check if student already applied to internship
     */
    suspend fun hasAppliedToInternship(internshipId: String, studentEmail: String): Boolean {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("internshipId", internshipId)
                .whereEqualTo("studentEmail", studentEmail)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get application statistics
     */
    suspend fun getApplicationStats(studentEmail: String): Map<ApplicationStatus, Int> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("studentEmail", studentEmail)
                .get()
                .await()

            val stats = ApplicationStatus.values().associateWith { 0 }.toMutableMap()

            snapshot.documents.forEach { doc ->
                try {
                    val statusString = doc.getString("status") ?: ApplicationStatus.PENDING.name
                    val status = ApplicationStatus.valueOf(statusString)
                    stats[status] = (stats[status] ?: 0) + 1
                } catch (e: Exception) {
                    // Skip invalid status
                }
            }

            stats
        } catch (e: Exception) {
            ApplicationStatus.values().associateWith { 0 }
        }
    }

    /**
     * ✅ NEW: Encode resume PDF to Base64
     */
    private fun encodeResumeToBase64(context: Context, uri: Uri): ResumeData {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")

        val bytes = inputStream.readBytes()
        inputStream.close()

        // Validate file size (max 500KB to stay under Firestore 1MB document limit)
        if (bytes.size > 500000) {
            throw Exception("Resume file too large. Maximum size is 500KB. Your file: ${bytes.size / 1024}KB")
        }

        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        val fileName = getFileName(context, uri) ?: "resume.pdf"
        val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"

        return ResumeData(
            base64String = base64String,
            fileName = fileName,
            fileSize = bytes.size.toLong(),
            mimeType = mimeType
        )
    }

    /**
     * Helper: Get filename from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName ?: uri.lastPathSegment
    }

    /**
     * Data class for resume encoding result
     */
    private data class ResumeData(
        val base64String: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    )
}