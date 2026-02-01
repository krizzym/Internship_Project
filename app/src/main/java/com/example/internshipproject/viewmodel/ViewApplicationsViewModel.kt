//ViewApplicationsViewModel.kt - FIXED VERSION
package com.example.internshipproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.model.StudentProfile
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationWithStudent(
    val application: Application,
    val studentProfile: StudentProfile?
)

data class ViewApplicationsState(
    val posting: Internship? = null,
    val applicationsWithStudents: List<ApplicationWithStudent> = emptyList(),
    val statusCounts: Map<ApplicationStatus, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ViewApplicationsViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ViewApplicationsState())
    val state: StateFlow<ViewApplicationsState> = _state.asStateFlow()

    fun loadData(postingId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            try {
                Log.d("ViewApplicationsVM", "🔍 Loading data for posting: $postingId")

                // Load posting details
                repository.getInternshipById(postingId).onSuccess { posting ->
                    Log.d("ViewApplicationsVM", "✅ Posting loaded: ${posting?.title}")
                    _state.value = _state.value.copy(posting = posting)
                }.onFailure { e ->
                    Log.e("ViewApplicationsVM", "❌ Failed to load posting: ${e.message}")
                }

                // Load applications
                repository.getApplicationsByPosting(postingId).onSuccess { applications ->
                    Log.d("ViewApplicationsVM", "✅ Applications loaded: ${applications.size} applications")

                    // Fetch student profiles for each application
                    val applicationsWithStudents = applications.map { application ->
                        val studentProfile = repository.getStudentProfile(application.studentEmail).getOrNull()
                        Log.d("ViewApplicationsVM", "   - ${application.studentEmail}: ${studentProfile?.fullName ?: "profile not found"}")
                        ApplicationWithStudent(application, studentProfile)
                    }

                    _state.value = _state.value.copy(applicationsWithStudents = applicationsWithStudents)

                }.onFailure { e ->
                    Log.e("ViewApplicationsVM", "❌ Failed to load applications: ${e.message}")
                    _state.value = _state.value.copy(errorMessage = "Failed to load applications: ${e.message}")
                }

                // Load status counts
                val statusCounts = repository.getApplicationStatusCounts(postingId)
                Log.d("ViewApplicationsVM", "✅ Status counts loaded: $statusCounts")

                _state.value = _state.value.copy(
                    statusCounts = statusCounts,
                    isLoading = false
                )

                Log.d("ViewApplicationsVM", "🎯 Final state: ${_state.value.applicationsWithStudents.size} applications with student data")

            } catch (e: Exception) {
                Log.e("ViewApplicationsVM", "❌ EXCEPTION in loadData: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    fun updateApplicationStatus(applicationId: String, newStatus: ApplicationStatus) {
        viewModelScope.launch {
            try {
                Log.d("ViewApplicationsVM", "🔄 Updating application $applicationId to $newStatus")

                repository.updateApplicationStatus(applicationId, newStatus).onSuccess {
                    Log.d("ViewApplicationsVM", "✅ Status updated successfully")

                    // Update local state
                    val updatedList = _state.value.applicationsWithStudents.map { appWithStudent ->
                        if (appWithStudent.application.id == applicationId) {
                            appWithStudent.copy(
                                application = appWithStudent.application.copy(status = newStatus)
                            )
                        } else {
                            appWithStudent
                        }
                    }

                    _state.value = _state.value.copy(
                        applicationsWithStudents = updatedList,
                        successMessage = "Application status updated to ${newStatus.name}"
                    )

                    // Reload status counts
                    _state.value.posting?.let { posting ->
                        val statusCounts = repository.getApplicationStatusCounts(posting.id)
                        _state.value = _state.value.copy(statusCounts = statusCounts)
                    }

                }.onFailure { e ->
                    Log.e("ViewApplicationsVM", "❌ Failed to update status: ${e.message}")
                    _state.value = _state.value.copy(
                        errorMessage = "Failed to update status: ${e.message}"
                    )
                }

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _state.value = _state.value.copy(successMessage = null)

            } catch (e: Exception) {
                Log.e("ViewApplicationsVM", "❌ Error updating status: ${e.message}", e)
                _state.value = _state.value.copy(
                    errorMessage = "Error updating status: ${e.message}"
                )
            }
        }
    }

    fun refreshData(postingId: String) {
        loadData(postingId)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, errorMessage = null)
    }
}