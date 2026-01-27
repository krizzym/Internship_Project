package com.example.internshipproject.data.repository

import android.os.Build
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplicationRepository {

    private val firestore: FirebaseFirestore = FirebaseManager.firestore

    suspend fun submitApplication(
        internshipId: String,
        internshipTitle: String,
        companyName: String,
        studentEmail: String,
        coverLetter: String
    ): Result<Application> {
        return try {
            val existingApp = hasAppliedToInternship(internshipId, studentEmail)
            if (existingApp) {
                return Result.failure(Exception("You have already applied to this internship"))
            }

            val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            } else {
                android.text.format.DateFormat.format("MMM dd, yyyy", System.currentTimeMillis()).toString()
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
                "createdAt" to System.currentTimeMillis()
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
                internship = null
            )

            Result.success(application)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit application: ${e.message}"))
        }
    }

    suspend fun getApplicationsByStudent(studentEmail: String): Result<List<Application>> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.APPLICATIONS)
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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
                        internship = null
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get applications: ${e.message}"))
        }
    }

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
}