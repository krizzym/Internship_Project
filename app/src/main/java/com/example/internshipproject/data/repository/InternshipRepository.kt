package com.example.internshipproject.data.repository

import android.util.Log
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Internship
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InternshipRepository {

    private val firestore: FirebaseFirestore = FirebaseManager.firestore

    suspend fun getInternshipById(internshipId: String): Internship? {
        return try {
            val doc = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(internshipId)
                .get()
                .await()

            if (doc.exists()) {
                Internship(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
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
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error getting internship: ${e.message}")
            null
        }
    }

    suspend fun getAllInternships(): List<Internship> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Internship(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
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
                    Log.e("InternshipRepo", "Error parsing internship: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error fetching internships: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActiveInternships(): List<Internship> {
        return getAllInternships()
    }

    fun getActiveInternshipsFlow(): Flow<List<Internship>> = callbackFlow {
        val listener = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InternshipRepo", "Error listening to internships: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val internships = snapshot.documents.mapNotNull { doc ->
                        try {
                            Internship(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
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
                            Log.e("InternshipRepo", "Error parsing internship: ${e.message}")
                            null
                        }
                    }
                    trySend(internships)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getInternshipsByCompany(companyName: String): List<Internship> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("companyName", companyName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Internship(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
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
                    Log.e("InternshipRepo", "Error parsing internship: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error fetching company internships: ${e.message}")
            emptyList()
        }
    }

    suspend fun createInternship(internship: Internship): Result<String> {
        return try {
            val internshipData = hashMapOf(
                "title" to internship.title,
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
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to (FirebaseManager.getCurrentUserId() ?: "")
            )

            val docRef = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .add(internshipData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error creating internship: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateInternship(internshipId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(internshipId)
                .update(updates + mapOf("updatedAt" to System.currentTimeMillis()))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error updating internship: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun deactivateInternship(internshipId: String): Result<Unit> {
        return try {
            firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(internshipId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error deactivating internship: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun searchInternships(query: String): List<Internship> {
        return try {
            val allInternships = getAllInternships()

            // Filter by search query
            allInternships.filter { internship ->
                internship.title.contains(query, ignoreCase = true) ||
                        internship.companyName.contains(query, ignoreCase = true) ||
                        internship.location.contains(query, ignoreCase = true) ||
                        internship.description.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error searching internships: ${e.message}")
            emptyList()
        }
    }
}