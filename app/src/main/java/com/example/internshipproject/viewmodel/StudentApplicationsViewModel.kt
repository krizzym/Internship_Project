//StudentApplicationsViewModel.kt
package com.example.internshipproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.repository.ApplicationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentApplicationsViewModel : ViewModel() {

    private val applicationRepository = ApplicationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    val applications: StateFlow<List<Application>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Observe applications in real-time for the current student
     */
    fun observeApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "User not authenticated"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Get student's email
                val studentEmail = currentUser.email ?: ""

                Log.d("StudentApplicationsVM", "Loading applications for: $studentEmail")

                // Fetch applications for this student
                val apps = applicationRepository.getApplicationsByStudent(studentEmail)

                Log.d("StudentApplicationsVM", "Loaded ${apps.size} applications")

                _applications.value = apps
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("StudentApplicationsVM", "Error loading applications: ${e.message}", e)
                _error.value = "Failed to load applications: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh applications
     */
    fun refresh() {
        observeApplications()
    }

    /**
     * Get application statistics
     */
    fun getApplicationStats(): Map<ApplicationStatus, Int> {
        val currentApps = _applications.value
        return mapOf(
            ApplicationStatus.PENDING to currentApps.count { it.status == ApplicationStatus.PENDING },
            ApplicationStatus.REVIEWED to currentApps.count { it.status == ApplicationStatus.REVIEWED },
            ApplicationStatus.SHORTLISTED to currentApps.count { it.status == ApplicationStatus.SHORTLISTED },
            ApplicationStatus.ACCEPTED to currentApps.count { it.status == ApplicationStatus.ACCEPTED },
            ApplicationStatus.REJECTED to currentApps.count { it.status == ApplicationStatus.REJECTED }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("StudentApplicationsVM", "ViewModel cleared")
    }
}