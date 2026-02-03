package com.example.internshipproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.repository.ApplicationRepository
import com.example.internshipproject.data.repository.CompanyRepository
import com.example.internshipproject.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CompanyApplicationsState(
    val applications: List<Application> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ✅ NEW: State for individual application details view
data class ApplicationDetailsState(
    val application: Application? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ✅ NEW: State for update operations (status/notes)
sealed class UpdateState {
    object Idle : UpdateState()
    object Updating : UpdateState()
    data class Success(val message: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class CompanyApplicationsViewModel(
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val applicationRepository: ApplicationRepository = ApplicationRepository(),
    private val studentRepository: StudentRepository = StudentRepository()
) : ViewModel() {

    // Existing state for applications list
    private val _state = MutableStateFlow(CompanyApplicationsState())
    val state: StateFlow<CompanyApplicationsState> = _state.asStateFlow()

    // ✅ NEW: State for application details
    private val _detailsState = MutableStateFlow(ApplicationDetailsState())
    val detailsState: StateFlow<ApplicationDetailsState> = _detailsState.asStateFlow()

    // ✅ NEW: State for update operations
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * Load all applications for a specific company
     */
    fun loadApplicationsForCompany(companyId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            try {
                // Use CompanyRepository's getAllCompanyApplications method
                val result = companyRepository.getAllCompanyApplications(companyId)

                result.onSuccess { applications ->
                    // Fetch student profiles for each application
                    val appsWithProfiles = applications.map { app ->
                        try {
                            val profileResult = studentRepository.getStudentByEmail(app.studentEmail)
                            profileResult.fold(
                                onSuccess = { profile: com.example.internshipproject.data.model.StudentProfile ->
                                    app.copy(studentProfile = profile)
                                },
                                onFailure = { _: Throwable -> app } // Return app without profile if fetch fails
                            )
                        } catch (e: Exception) {
                            Log.e("CompanyAppsVM", "Error fetching profile for ${app.studentEmail}", e)
                            app // Return app without profile if fetch fails
                        }
                    }

                    _state.value = _state.value.copy(
                        applications = appsWithProfiles,
                        isLoading = false
                    )
                }

                result.onFailure { e ->
                    Log.e("CompanyAppsVM", "Error loading applications", e)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load applications"
                    )
                }
            } catch (e: Exception) {
                Log.e("CompanyAppsVM", "Error loading applications", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    /**
     * ✅ NEW: Load details for a specific application
     */
    fun loadApplicationDetails(applicationId: String) {
        viewModelScope.launch {
            _detailsState.value = ApplicationDetailsState(isLoading = true)

            try {
                // Use CompanyRepository's getApplicationById method
                val result = companyRepository.getApplicationById(applicationId)

                result.onSuccess { application ->
                    // Fetch student profile
                    val profileResult = studentRepository.getStudentByEmail(application.studentEmail)
                    val appWithProfile = profileResult.fold(
                        onSuccess = { profile: com.example.internshipproject.data.model.StudentProfile ->
                            application.copy(studentProfile = profile)
                        },
                        onFailure = { _: Throwable -> application } // Return app without profile if fetch fails
                    )

                    _detailsState.value = ApplicationDetailsState(
                        application = appWithProfile,
                        isLoading = false
                    )
                }

                result.onFailure { e ->
                    Log.e("CompanyAppsVM", "Error loading application details", e)
                    _detailsState.value = ApplicationDetailsState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load application details"
                    )
                }
            } catch (e: Exception) {
                Log.e("CompanyAppsVM", "Error loading application details", e)
                _detailsState.value = ApplicationDetailsState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load application details"
                )
            }
        }
    }

    /**
     * ✅ NEW: Update application status
     */
    fun updateApplicationStatus(applicationId: String, newStatus: ApplicationStatus) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Updating

            try {
                // Use CompanyRepository's updateApplicationStatus method
                val result = companyRepository.updateApplicationStatus(applicationId, newStatus)

                result.onSuccess {
                    // Update the details state
                    _detailsState.value.application?.let { app ->
                        _detailsState.value = _detailsState.value.copy(
                            application = app.copy(status = newStatus)
                        )
                    }

                    // Update in the list state as well
                    _state.value = _state.value.copy(
                        applications = _state.value.applications.map { app ->
                            if (app.id == applicationId) app.copy(status = newStatus) else app
                        }
                    )

                    _updateState.value = UpdateState.Success("Status updated successfully")
                }

                result.onFailure { e ->
                    Log.e("CompanyAppsVM", "Error updating status", e)
                    _updateState.value = UpdateState.Error(e.message ?: "Failed to update status")
                }
            } catch (e: Exception) {
                Log.e("CompanyAppsVM", "Error updating status", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update status")
            }
        }
    }

    /**
     * ✅ NEW: Update company notes
     */
    fun updateCompanyNotes(applicationId: String, notes: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Updating

            try {
                // Update via Firestore directly since updateCompanyNotes is an extension function
                val result = applicationRepository.updateCompanyNotes(applicationId, notes)

                result.onSuccess {
                    // Update the details state
                    _detailsState.value.application?.let { app ->
                        _detailsState.value = _detailsState.value.copy(
                            application = app.copy(companyNotes = notes)
                        )
                    }

                    // Update in the list state as well
                    _state.value = _state.value.copy(
                        applications = _state.value.applications.map { app ->
                            if (app.id == applicationId) app.copy(companyNotes = notes) else app
                        }
                    )

                    _updateState.value = UpdateState.Success("Notes saved successfully")
                }

                result.onFailure { e ->
                    Log.e("CompanyAppsVM", "Error updating notes", e)
                    _updateState.value = UpdateState.Error(e.message ?: "Failed to save notes")
                }
            } catch (e: Exception) {
                Log.e("CompanyAppsVM", "Error updating notes", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to save notes")
            }
        }
    }

    /**
     * Filter applications by status
     */
    fun filterApplications(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    /**
     * Get filtered applications based on selected filter
     */
    fun getFilteredApplications(): List<Application> {
        val filter = _state.value.selectedFilter
        return when (filter) {
            "All" -> _state.value.applications
            "Pending" -> _state.value.applications.filter { it.status == ApplicationStatus.PENDING }
            "Reviewed" -> _state.value.applications.filter { it.status == ApplicationStatus.REVIEWED }
            "Shortlisted" -> _state.value.applications.filter { it.status == ApplicationStatus.SHORTLISTED }
            "Accepted" -> _state.value.applications.filter { it.status == ApplicationStatus.ACCEPTED }
            "Rejected" -> _state.value.applications.filter { it.status == ApplicationStatus.REJECTED }
            else -> _state.value.applications
        }
    }

    /**
     * Get count of applications by status
     */
    fun getApplicationCountByStatus(status: ApplicationStatus): Int {
        return _state.value.applications.count { it.status == status }
    }

    /**
     * ✅ NEW: Clear details state (when leaving details screen)
     */
    fun clearDetailsState() {
        _detailsState.value = ApplicationDetailsState()
    }

    /**
     * ✅ NEW: Reset update state
     */
    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}

/**
 * Extension function to handle updateCompanyNotes on ApplicationRepository
 * This matches the extension function defined in ApplicationRepository
 */
private suspend fun ApplicationRepository.updateCompanyNotes(
    applicationId: String,
    notes: String
): Result<Unit> {
    return try {
        val firestore = com.example.internshipproject.data.firebase.FirebaseManager.firestore
        firestore.collection(com.example.internshipproject.data.firebase.FirebaseManager.Collections.APPLICATIONS)
            .document(applicationId)
            .update(
                mapOf(
                    "companyNotes" to notes,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}