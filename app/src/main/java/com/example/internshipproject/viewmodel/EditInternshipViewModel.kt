package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditInternshipState(
    val title: String = "",
    val description: String = "",
    val requirements: String = "",
    val workType: String = "Hybrid",
    val location: String = "",
    val duration: String = "",
    val salaryRange: String = "",
    val availableSlots: String = "",
    val applicationDeadline: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updateSuccess: Boolean = false
)

class EditInternshipViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(EditInternshipState())
    val state: StateFlow<EditInternshipState> = _state.asStateFlow()

    fun loadInternship(internshipId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // ✅ OPTION 1: If you renamed the method to avoid conflicts
            // Uncomment this line and comment out the next one:
            // repository.getInternshipByIdForEdit(internshipId).fold(

            // ✅ OPTION 2: If you're using the original method name
            repository.getInternshipById(internshipId).fold(
                onSuccess = { internship ->
                    _state.value = _state.value.copy(
                        title = internship.title,
                        description = internship.description,
                        requirements = internship.requirements,
                        workType = internship.workType,
                        location = internship.location,
                        duration = internship.duration,
                        salaryRange = internship.salaryRange,
                        availableSlots = internship.availableSlots.toString(),
                        applicationDeadline = internship.applicationDeadline,
                        isActive = internship.isActive,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load internship data"
                    )
                }
            )
        }
    }

    fun updateInternship(internshipId: String) {
        viewModelScope.launch {
            val currentState = _state.value

            // Validation
            if (currentState.title.isBlank()) {
                _state.value = currentState.copy(errorMessage = "Job title is required")
                return@launch
            }
            if (currentState.description.isBlank()) {
                _state.value = currentState.copy(errorMessage = "Job description is required")
                return@launch
            }
            if (currentState.requirements.isBlank()) {
                _state.value = currentState.copy(errorMessage = "Requirements are required")
                return@launch
            }
            if (currentState.location.isBlank()) {
                _state.value = currentState.copy(errorMessage = "Location is required")
                return@launch
            }
            if (currentState.duration.isBlank()) {
                _state.value = currentState.copy(errorMessage = "Duration is required")
                return@launch
            }

            val slots = currentState.availableSlots.toIntOrNull()
            if (slots == null || slots <= 0) {
                _state.value = currentState.copy(errorMessage = "Please enter a valid number of slots")
                return@launch
            }

            _state.value = currentState.copy(isLoading = true, errorMessage = null)

            val updateData = mapOf(
                "title" to currentState.title.trim(),
                "description" to currentState.description.trim(),
                "requirements" to currentState.requirements.trim(),
                "workType" to currentState.workType,
                "location" to currentState.location.trim(),
                "duration" to currentState.duration.trim(),
                "salaryRange" to currentState.salaryRange.trim(),
                "availableSlots" to slots,
                "applicationDeadline" to currentState.applicationDeadline.trim(),
                "isActive" to currentState.isActive,
                "updatedAt" to System.currentTimeMillis()
            )

            repository.updateInternship(internshipId, updateData).fold(
                onSuccess = {
                    _state.value = currentState.copy(
                        isLoading = false,
                        updateSuccess = true
                    )
                },
                onFailure = { error ->
                    _state.value = currentState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to update internship"
                    )
                }
            )
        }
    }

    fun updateTitle(value: String) {
        _state.value = _state.value.copy(title = value, errorMessage = null)
    }

    fun updateDescription(value: String) {
        _state.value = _state.value.copy(description = value, errorMessage = null)
    }

    fun updateRequirements(value: String) {
        _state.value = _state.value.copy(requirements = value, errorMessage = null)
    }

    fun updateWorkType(value: String) {
        _state.value = _state.value.copy(workType = value, errorMessage = null)
    }

    fun updateLocation(value: String) {
        _state.value = _state.value.copy(location = value, errorMessage = null)
    }

    fun updateDuration(value: String) {
        _state.value = _state.value.copy(duration = value, errorMessage = null)
    }

    fun updateSalaryRange(value: String) {
        _state.value = _state.value.copy(salaryRange = value, errorMessage = null)
    }

    fun updateAvailableSlots(value: String) {
        _state.value = _state.value.copy(availableSlots = value, errorMessage = null)
    }

    fun updateApplicationDeadline(value: String) {
        _state.value = _state.value.copy(applicationDeadline = value, errorMessage = null)
    }

    fun updateIsActive(value: Boolean) {
        _state.value = _state.value.copy(isActive = value, errorMessage = null)
    }
}
