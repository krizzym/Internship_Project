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
            Log.d("InternshipRepo", "Fetching all internships...")
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            Log.d("InternshipRepo", "Retrieved ${snapshot.documents.size} documents")

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
                    Log.e("InternshipRepo", "Error parsing internship ${doc.id}: ${e.message}")
                    null
                }
            }

            // Sort by createdAt in memory to avoid needing Firestore index
            val sortedInternships = internships.sortedByDescending {
                it.id // If you have createdAt timestamp, use that instead
            }

            Log.d("InternshipRepo", "Successfully parsed ${sortedInternships.size} internships")
            sortedInternships
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error fetching internships: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getActiveInternships(): List<Internship> {
        return try {
            Log.d("InternshipRepo", "Getting active internships...")
            val result = getAllInternships()
            Log.d("InternshipRepo", "Returning ${result.size} active internships")
            result
        } catch (e: Exception) {
            Log.e("InternshipRepo", "Error in getActiveInternships: ${e.message}", e)
            emptyList()
        }
    }

    fun getActiveInternshipsFlow(): Flow<List<Internship>> = callbackFlow {
        val listener = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("InternshipRepo", "Error listening to internships: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("InternshipRepo", "Flow received ${snapshot.documents.size} internships")
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
                            Log.e("InternshipRepo", "Error parsing internship: ${e.message}")
                            null
                        }
                    }
                    trySend(internships)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun getInternshipsByCompany(companyName: String): List<Internship> {
        return try {
            val snapshot = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .whereEqualTo("companyName", companyName)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
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
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to (FirebaseManager.getCurrentUserId() ?: "")
            )

            val docRef = firestore.collection(FirebaseManager.Collections.INTERNSHIPS)
                .add(internshipData)
                .await()

            Log.d("InternshipRepo", "Created internship with ID: ${docRef.id}")
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
    suspend fun deleteInternship(internshipId: String): Result<Unit> {
        return try {
            // Verify user is authenticated
            val currentUserId = FirebaseManager.getCurrentUserId()
            if (currentUserId == null) {
                return Result.failure(Exception("User not logged in"))
            }

            Log.d("InternshipRepository", "Starting deletion of internship: $internshipId")

            // Start a batch write for atomic operation
            // This ensures either ALL operations succeed or ALL fail (no partial state)
            val batch = firestore.batch()

            // Reference to the internship document
            val internshipRef = firestore
                .collection(FirebaseManager.Collections.INTERNSHIPS)
                .document(internshipId)

            // Reference to applications collection
            val applicationsRef = firestore
                .collection(FirebaseManager.Collections.APPLICATIONS)

            // Step 1: Delete the internship document
            batch.delete(internshipRef)
            Log.d("InternshipRepository", "Added internship deletion to batch")

            // Step 2: Query all applications related to this internship
            val relatedApplications = applicationsRef
                .whereEqualTo("internshipId", internshipId)
                .get()
                .await()

            val applicationCount = relatedApplications.size()
            Log.d("InternshipRepository", "Found $applicationCount related applications to delete")

            // Step 3: Add all application deletions to the batch
            relatedApplications.documents.forEach { applicationDoc ->
                batch.delete(applicationDoc.reference)
                Log.d("InternshipRepository", "Added application ${applicationDoc.id} to deletion batch")
            }

            // Step 4: Commit the entire batch operation atomically
            batch.commit().await()

            Log.d("InternshipRepository", "✅ Successfully deleted internship $internshipId and $applicationCount related applications")

            // Return success
            Result.success(Unit)

        } catch (e: Exception) {
            // Log detailed error information for debugging
            Log.e("InternshipRepository", "❌ Error deleting internship $internshipId: ${e.message}", e)

            // Return failure with the exception
            Result.failure(e)
        }
    }
}