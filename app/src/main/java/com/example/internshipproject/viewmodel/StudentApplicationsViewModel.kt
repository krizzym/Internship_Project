// StudentApplicationsViewModel.kt - UPDATED with better delete handling
package com.example.internshipproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ✅ UPDATED: Better handling of delete operations
 */
class StudentApplicationsViewModel(
    private val applicationRepository: ApplicationRepository = ApplicationRepository()
) : ViewModel() {

    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    val applications: StateFlow<List<Application>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load applications for the current user
     */
    fun loadApplications() {
        val studentId = FirebaseManager.getCurrentUserId()
        if (studentId == null) {
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("StudentAppVM", "Loading applications for student: $studentId")
                val apps = applicationRepository.getApplicationsByStudentId(studentId)
                _applications.value = apps
                Log.d("StudentAppVM", "Loaded ${apps.size} applications")
            } catch (e: Exception) {
                Log.e("StudentAppVM", "Error loading applications: ${e.message}")
                _error.value = "Failed to load applications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Set up real-time listener for applications
     */
    fun observeApplications() {
        val studentId = FirebaseManager.getCurrentUserId()
        if (studentId == null) {
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                Log.d("StudentAppVM", "Setting up real-time observer for student: $studentId")

                applicationRepository.observeApplicationsByStudentId(studentId)
                    .collect { apps ->
                        _applications.value = apps
                        _isLoading.value = false
                        Log.d("StudentAppVM", "Real-time update: ${apps.size} applications")
                    }
            } catch (e: Exception) {
                Log.e("StudentAppVM", "Error observing applications: ${e.message}")
                _error.value = "Failed to observe applications: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Calculate statistics from current applications
     */
    fun getApplicationStats(): Map<ApplicationStatus, Int> {
        val stats = mutableMapOf<ApplicationStatus, Int>()
        ApplicationStatus.values().forEach { status ->
            stats[status] = _applications.value.count { it.status == status }
        }
        return stats
    }

    /**
     * ✅ Get dashboard statistics
     */
    fun getDashboardStats(): Map<String, Int> {
        val apps = _applications.value
        return mapOf(
            "total" to apps.size,
            "pending" to apps.count { it.status == ApplicationStatus.PENDING },
            "accepted" to apps.count { it.status == ApplicationStatus.ACCEPTED }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * ✅ UPDATED: Refresh without clearing existing data
     */
    fun refresh() {
        val studentId = FirebaseManager.getCurrentUserId()
        if (studentId == null) {
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            // ✅ Don't set loading to true - keeps UI stable
            try {
                Log.d("StudentAppVM", "Manual refresh triggered")
                val apps = applicationRepository.getApplicationsByStudentId(studentId)
                _applications.value = apps
                Log.d("StudentAppVM", "Refreshed: ${apps.size} applications")
            } catch (e: Exception) {
                Log.e("StudentAppVM", "Refresh error: ${e.message}")
                // Don't update error state for refresh failures - real-time listener will handle it
            }
        }
    }

    /**
     * ✅ UPDATED: Delete with better state management
     */
    fun deleteApplication(applicationId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // ✅ DON'T set isLoading - prevents UI from clearing
            try {
                Log.d("StudentAppVM", "Deleting application: $applicationId")

                applicationRepository.deleteApplication(applicationId)
                    .onSuccess {
                        Log.d("StudentAppVM", "Successfully deleted application")
                        // ✅ Real-time listener will automatically update the list
                        // No need to manually refresh
                        onComplete(true, "Application deleted successfully")
                    }
                    .onFailure { error ->
                        Log.e("StudentAppVM", "Failed to delete: ${error.message}")
                        _error.value = error.message
                        onComplete(false, error.message ?: "Failed to delete application")
                    }
            } catch (e: Exception) {
                Log.e("StudentAppVM", "Error in deleteApplication: ${e.message}")
                _error.value = e.message
                onComplete(false, e.message ?: "An error occurred")
            }
        }
    }
}