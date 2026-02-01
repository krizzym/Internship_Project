package com.example.internshipproject.data.repository

import android.util.Log
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.StudentProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudentRepository {

    private val firestore: FirebaseFirestore = FirebaseManager.firestore

    /**
     * Get student profile by email
     */
    suspend fun getStudentByEmail(email: String): Result<StudentProfile> {
        return try {
            Log.d("StudentRepo", "Fetching student profile for: $email")

            val snapshot = firestore.collection(FirebaseManager.Collections.STUDENTS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents[0]

                // Parse internshipTypes - handle both String and List formats
                val internshipTypes = when (val types = doc.get("internshipTypes")) {
                    is String -> listOf(types)
                    is List<*> -> types.filterIsInstance<String>()
                    else -> emptyList()
                }

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
                    internshipTypes = internshipTypes,
                    skills = doc.getString("skills") ?: "",
                    resumeUri = doc.getString("resumeUri")
                )
                Log.d("StudentRepo", "Student profile found: ${profile.firstName} ${profile.surname}")
                Result.success(profile)
            } else {
                Log.w("StudentRepo", "Student profile not found for: $email")
                Result.failure(Exception("Student profile not found"))
            }
        } catch (e: Exception) {
            Log.e("StudentRepo", "Error fetching student profile: ${e.message}")
            Result.failure(e)
        }
    }
}
