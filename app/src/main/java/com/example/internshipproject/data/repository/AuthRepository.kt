// AuthRepository
package com.example.internshipproject.data.repository

import android.net.Uri
import android.util.Log
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Company
import com.example.internshipproject.data.model.Student
import com.example.internshipproject.data.model.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseManager.auth
    private val firestore: FirebaseFirestore = FirebaseManager.firestore
    private val storage: FirebaseStorage = FirebaseManager.storage

    suspend fun registerStudent(student: Student): Result<UserSession> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(
                student.email,
                student.password
            ).await()

            val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            var resumeUrl: String? = null
            if (!student.resumeUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(student.resumeUri)
                    resumeUrl = uploadFile(
                        uri,
                        "${FirebaseManager.StoragePaths.RESUMES}/${userId}_resume.pdf"
                    )
                    Log.d("FirebaseAuth", "Resume uploaded successfully: $resumeUrl")
                } catch (e: Exception) {
                    Log.e("FirebaseAuth", "Resume upload failed, continuing without resume: ${e.message}")
                    resumeUrl = null
                }
            }

            val studentData = hashMapOf(
                "userId" to userId,
                "firstName" to student.firstName,
                "middleName" to (student.middleName ?: ""),
                "lastName" to student.lastName,
                "email" to student.email,
                "school" to student.school,
                "course" to student.course,
                "yearLevel" to student.yearLevel,
                "city" to student.city,
                "barangay" to student.barangay,
                "internshipTypes" to student.internshipTypes,
                "skills" to student.skills,
                "resumeUri" to (resumeUrl ?: ""),
                "userRole" to "student",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection(FirebaseManager.Collections.STUDENTS)
                .document(userId)
                .set(studentData)
                .await()

            Log.d("FirebaseAuth", "Student registered successfully: $userId")

            Result.success(
                UserSession(
                    userRole = "student",
                    token = userId,
                    email = student.email
                )
            )
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Registration failed: ${e.message}", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun registerCompany(company: Company): Result<UserSession> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(
                company.companyEmail,
                company.password
            ).await()

            val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            var logoUrl: String? = null
            if (!company.logoUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(company.logoUri)
                    logoUrl = uploadFile(
                        uri,
                        "${FirebaseManager.StoragePaths.COMPANY_LOGOS}/${userId}_logo.jpg"
                    )
                    Log.d("FirebaseAuth", "Logo uploaded successfully: $logoUrl")
                } catch (e: Exception) {
                    Log.e("FirebaseAuth", "Logo upload failed, continuing without logo: ${e.message}")
                    logoUrl = null
                }
            }

            val companyData = hashMapOf(
                "userId" to userId,
                "companyEmail" to company.companyEmail,
                "contactNumber" to company.contactNumber,
                "companyName" to company.companyName,
                "contactPerson" to company.contactPerson,
                "industryType" to company.industryType,
                "companyAddress" to company.companyAddress,
                "companyDescription" to company.companyDescription,
                "logoUri" to (logoUrl ?: ""),
                "userRole" to "company",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection(FirebaseManager.Collections.COMPANIES)
                .document(userId)
                .set(companyData)
                .await()

            Log.d("FirebaseAuth", "Company registered successfully: $userId")

            Result.success(
                UserSession(
                    userRole = "company",
                    token = userId,
                    email = company.companyEmail
                )
            )
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Company registration failed: ${e.message}", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun login(email: String, password: String): Result<UserSession> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val studentDoc = firestore.collection(FirebaseManager.Collections.STUDENTS)
                .document(userId)
                .get()
                .await()

            if (studentDoc.exists()) {
                Log.d("FirebaseAuth", "Student logged in: $userId")
                return Result.success(
                    UserSession(
                        userRole = "student",
                        token = userId,
                        email = email
                    )
                )
            }

            val companyDoc = firestore.collection(FirebaseManager.Collections.COMPANIES)
                .document(userId)
                .get()
                .await()

            if (companyDoc.exists()) {
                Log.d("FirebaseAuth", "Company logged in: $userId")
                return Result.success(
                    UserSession(
                        userRole = "company",
                        token = userId,
                        email = email
                    )
                )
            }

            Result.failure(Exception("User profile not found"))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Login failed: ${e.message}", e)
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    suspend fun getStudentProfile(userId: String): Student? {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.STUDENTS)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                Student(
                    firstName = doc.getString("firstName") ?: "",
                    middleName = doc.getString("middleName")?.takeIf { it.isNotEmpty() },
                    lastName = doc.getString("lastName") ?: "",
                    email = doc.getString("email") ?: "",
                    password = "",
                    school = doc.getString("school") ?: "",
                    course = doc.getString("course") ?: "",
                    yearLevel = doc.getString("yearLevel") ?: "",
                    city = doc.getString("city") ?: "",
                    barangay = doc.getString("barangay") ?: "",
                    internshipTypes = doc.get("internshipTypes") as? List<String> ?: emptyList(),
                    skills = doc.getString("skills") ?: "",
                    resumeUri = doc.getString("resumeUri")?.takeIf { it.isNotEmpty() }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Failed to get student profile: ${e.message}", e)
            null
        }
    }

    suspend fun getCompanyProfile(userId: String): Company? {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.COMPANIES)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                Company(
                    companyEmail = doc.getString("companyEmail") ?: "",
                    contactNumber = doc.getString("contactNumber") ?: "",
                    password = "",
                    companyName = doc.getString("companyName") ?: "",
                    contactPerson = doc.getString("contactPerson") ?: "",
                    industryType = doc.getString("industryType") ?: "",
                    companyAddress = doc.getString("companyAddress") ?: "",
                    companyDescription = doc.getString("companyDescription") ?: "",
                    logoUri = doc.getString("logoUri")?.takeIf { it.isNotEmpty() }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Failed to get company profile: ${e.message}", e)
            null
        }
    }

    suspend fun updateStudentProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.STUDENTS)
                .document(userId)
                .update(updates)
                .await()
            Log.d("FirebaseAuth", "Student profile updated: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Failed to update profile: ${e.message}", e)
            Result.failure(Exception("Update failed: ${e.message}"))
        }
    }

    private suspend fun uploadFile(fileUri: Uri, storagePath: String): String {
        return try {
            val storageRef = storage.reference.child(storagePath)
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "File upload failed: ${e.message}", e)
            throw Exception("File upload failed: ${e.message}")
        }
    }

    suspend fun uploadResume(userId: String, fileUri: Uri): Result<String> {
        return try {
            val url = uploadFile(
                fileUri,
                "${FirebaseManager.StoragePaths.RESUMES}/${userId}_resume.pdf"
            )

            updateStudentProfile(userId, mapOf("resumeUri" to url))

            Log.d("FirebaseAuth", "Resume uploaded and profile updated: $url")
            Result.success(url)
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Resume upload failed: ${e.message}", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        }
    }
}