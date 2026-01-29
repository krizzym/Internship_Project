// InternshipRepository
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

    /**
     * Get active internships with real-time updates
     * FIXED: Removed orderBy to avoid composite index requirement
     * Results will be ordered in-memory after fetching
     */
    fun getActiveInternshipsFlow(): Flow<List<Internship>> = callbackFlow {
        val listener = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
            .whereEqualTo("isActive", true)
            // REMOVED: .orderBy("createdAt", Query.Direction.DESCENDING)
            // This was causing the issue - Firestore needs a composite index for whereEqualTo + orderBy
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InternshipRepo", "Error listening to active internships: ${error.message}")
                    trySend(emptyList())
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
                        // Sort in memory by createdAt (newest first)
                        .sortedByDescending {
                            // This will work if createdAt exists, otherwise just maintain order
                            try {
                                snapshot.documents.find { it.id == it.id }?.getLong("createdAt") ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }

                    Log.d("InternshipRepo", "Loaded ${internships.size} active internships (real-time)")
                    trySend(internships)
                }
            }

        awaitClose {
            Log.d("InternshipRepo", "Closing active internships listener")
            listener.remove()
        }
    }

    /**
     * Get internships by company with real-time updates
     * FIXED: Removed orderBy to avoid composite index requirement
     */
    fun getCompanyInternshipsFlow(companyId: String): Flow<List<Internship>> = callbackFlow {
        val listener = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
            .whereEqualTo("companyId", companyId)
            // REMOVED: .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InternshipRepo", "Error listening to company internships: ${error.message}")
                    trySend(emptyList())
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

                    Log.d("InternshipRepo", "Loaded ${internships.size} internships for company $companyId (real-time)")
                    trySend(internships)
                }
            }

        awaitClose {
            Log.d("InternshipRepo", "Closing company internships listener")
            listener.remove()
        }
    }

    /**
     * Get a single internship by ID
     */
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
                Log.w("InternshipRepo", "Internship not found: $internshipId")
                null
            }
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error getting internship: ${e.message}")
            null
        }
    }

    /**
     * Get all active internships (one-time fetch, not real-time)
     */
    suspend fun getActiveInternships(): List<Internship> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("isActive", true)
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
            Log.e("InternshipRepo", "Error fetching active internships: ${e.message}")
            emptyList()
        }
    }
}