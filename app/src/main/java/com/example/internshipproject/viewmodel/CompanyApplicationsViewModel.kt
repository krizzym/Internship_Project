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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.internshipproject.data.firebase.FirebaseManager

data class CompanyApplicationsState(
    val applications: List<Application> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// State for individual application details view
data class ApplicationDetailsState(
    val application: Application? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// State for update operations (status/notes)
sealed class UpdateState {
    object Idle : UpdateState()
    object Updating : UpdateState()     // ← Change to "Updating"
    data class Success(val message: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class CompanyApplicationsViewModel(
    private val companyRepository: CompanyRepository = CompanyRepository(),
    private val applicationRepository: ApplicationRepository = ApplicationRepository(),
    private val studentRepository: StudentRepository = StudentRepository()
) : ViewModel() {

    // cached companyId so refresh() can re-use it without a parameter
    private var _currentCompanyId: String = ""

    // raw state (full list + selected filter)
    private val _state = MutableStateFlow(CompanyApplicationsState())
    val state: StateFlow<CompanyApplicationsState> = _state.asStateFlow()

    val filteredApplications: StateFlow<List<Application>> = combine(
        _state.map { it.applications },
        _state.map { it.selectedFilter }
    ) { applications, filter ->
        when (filter) {

            "All"        -> applications
            else         -> {
                // Safe: if the string doesn't match any enum constant the
                // filter just returns an empty list instead of crashing.
                val status = ApplicationStatus.values().firstOrNull { it.name == filter }
                if (status != null) applications.filter { it.status == status }
                else applications   // fallback – should never happen
            }
        }
    }.stateIn(
        scope     = viewModelScope,
        started   = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // State for application details
    private val _detailsState = MutableStateFlow(ApplicationDetailsState())
    val detailsState: StateFlow<ApplicationDetailsState> = _detailsState.asStateFlow()

    // State for update operations
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun loadApplicationsForCompany(companyId: String) {
        _currentCompanyId = companyId          // cache for refresh()
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

    // Load details for a specific application
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

    fun updateApplicationStatusAndNotes(
        applicationId: String,
        newStatus: ApplicationStatus,
        notes: String
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Updating
            try {
                // Update both fields atomically in Firebase
                FirebaseManager.firestore
                    .collection(FirebaseManager.Collections.APPLICATIONS)
                    .document(applicationId)
                    .update(
                        mapOf(
                            "status" to newStatus.name,
                            "companyNotes" to notes,
                            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )
                    .await()

                // Success - update UI state
                _updateState.value = UpdateState.Success("Application updated successfully")

                // Reload the application details to reflect the changes
                loadApplicationDetails(applicationId)

                Log.d("CompanyAppsViewModel", "✅ Successfully updated status to $newStatus and notes")
            } catch (e: Exception) {
                Log.e("CompanyAppsViewModel", "❌ Error updating application", e)
                _updateState.value = UpdateState.Error("Failed to update: ${e.message}")
            }
        }
    }


    // Update application status
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

   //Update company notes
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

    fun filterApplications(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun refresh() {
        if (_currentCompanyId.isNotEmpty()) {
            loadApplicationsForCompany(_currentCompanyId)
        }
    }

    fun getApplicationCountByStatus(status: ApplicationStatus): Int {
        return _state.value.applications.count { it.status == status }
    }

    fun clearDetailsState() {
        _detailsState.value = ApplicationDetailsState()
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}

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