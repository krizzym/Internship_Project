package com.example.internshipproject.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.ApplicationRepository
import com.example.internshipproject.data.repository.InternshipRepository
import kotlinx.coroutines.launch

data class ApplicationDetailViewState(
    val posting: Internship? = null,
    val applications: List<Application> = emptyList(),
    val statusCounts: Map<ApplicationStatus, Int> = emptyMap(),
    val selectedStatus: ApplicationStatus? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ApplicationDetailViewModel : ViewModel() {

    private val applicationRepository = ApplicationRepository()
    private val internshipRepository = InternshipRepository()

    var state by mutableStateOf(ApplicationDetailViewState())
        private set

    fun loadData(postingId: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)

            try {
                // Load internship posting
                val posting = internshipRepository.getInternshipById(postingId)

                if (posting == null) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "Internship not found"
                    )
                    return@launch
                }

                // Load applications for this posting
                val applications = applicationRepository.getApplicationsByInternship(postingId)

                // Calculate status counts
                val counts = applications.groupingBy { it.status }.eachCount()

                state = state.copy(
                    isLoading = false,
                    posting = posting,
                    applications = applications,
                    statusCounts = counts
                )

            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    fun selectStatus(status: ApplicationStatus?) {
        state = state.copy(selectedStatus = status)
    }

    fun updateApplicationStatus(
        applicationId: String,
        newStatus: ApplicationStatus
    ) {
        viewModelScope.launch {
            try {
                applicationRepository.updateApplicationStatus(applicationId, newStatus)

                // Reload data after update
                state.posting?.let { loadData(it.id) }

                state = state.copy(
                    successMessage = "Application status updated successfully"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                state = state.copy(successMessage = null)

            } catch (e: Exception) {
                state = state.copy(
                    errorMessage = "Error updating status: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        state = state.copy(successMessage = null, errorMessage = null)
    }

    fun refreshData(postingId: String) {
        loadData(postingId)
    }

    fun getFilteredApplications(): List<Application> {
        return if (state.selectedStatus != null) {
            state.applications.filter { it.status == state.selectedStatus }
        } else {
            state.applications
        }
    }
}
