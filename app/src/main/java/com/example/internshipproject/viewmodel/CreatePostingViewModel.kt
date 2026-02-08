package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostingState(
    val jobTitle: String = "",
    val category: String = "Engineering and technology", // Added category
    val jobDescription: String = "",
    val requirements: String = "",
    val internshipType: String = "",
    val location: String = "",
    val duration: String = "",
    val stipend: String = "",
    val slots: String = "",
    val deadline: String = "",
    val status: String = "Active",
    val isLoading: Boolean = false,
    val createSuccess: Boolean = false,
    val errorMessage: String? = null,
    val errors: Map<String, String> = emptyMap()
)

class CreatePostingViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CreatePostingState())
    val state: StateFlow<CreatePostingState> = _state.asStateFlow()

    fun updateJobTitle(value: String) {
        _state.value = _state.value.copy(jobTitle = value)
        validateField("jobTitle")
    }

    fun updateCategory(value: String) {
        _state.value = _state.value.copy(category = value)
    }

    fun updateJobDescription(value: String) {
        _state.value = _state.value.copy(jobDescription = value)
        validateField("jobDescription")
    }

    fun updateRequirements(value: String) {
        _state.value = _state.value.copy(requirements = value)
        validateField("requirements")
    }

    fun updateInternshipType(value: String) {
        _state.value = _state.value.copy(internshipType = value)
        validateField("internshipType")
    }

    fun updateLocation(value: String) {
        _state.value = _state.value.copy(location = value)
        validateField("location")
    }

    fun updateDuration(value: String) {
        _state.value = _state.value.copy(duration = value)
        validateField("duration")
    }

    fun updateStipend(value: String) {
        _state.value = _state.value.copy(stipend = value)
    }

    fun updateSlots(value: String) {
        _state.value = _state.value.copy(slots = value)
        validateField("slots")
    }

    fun updateDeadline(value: String) {
        _state.value = _state.value.copy(deadline = value)
        validateField("deadline")
    }

    fun updateStatus(value: String) {
        _state.value = _state.value.copy(status = value)
    }

    private fun validateField(fieldName: String) {
        val currentState = _state.value
        val newErrors = currentState.errors.toMutableMap()

        when (fieldName) {
            "jobTitle" -> {
                if (currentState.jobTitle.isBlank()) {
                    newErrors["jobTitle"] = "Job title is required"
                } else {
                    newErrors.remove("jobTitle")
                }
            }
            "jobDescription" -> {
                if (currentState.jobDescription.isBlank()) {
                    newErrors["jobDescription"] = "Job description is required"
                } else {
                    newErrors.remove("jobDescription")
                }
            }
            "requirements" -> {
                if (currentState.requirements.isBlank()) {
                    newErrors["requirements"] = "Requirements are required"
                } else {
                    newErrors.remove("requirements")
                }
            }
            "internshipType" -> {
                if (currentState.internshipType.isBlank()) {
                    newErrors["internshipType"] = "Work mode is required"
                } else {
                    newErrors.remove("internshipType")
                }
            }
            "location" -> {
                if (currentState.location.isBlank()) {
                    newErrors["location"] = "Location is required"
                } else {
                    newErrors.remove("location")
                }
            }
            "duration" -> {
                if (currentState.duration.isBlank()) {
                    newErrors["duration"] = "Duration is required"
                } else {
                    newErrors.remove("duration")
                }
            }
            "slots" -> {
                if (currentState.slots.isBlank()) {
                    newErrors["slots"] = "Number of slots is required"
                } else if (currentState.slots.toIntOrNull() == null || currentState.slots.toInt() <= 0) {
                    newErrors["slots"] = "Please enter a valid number of slots"
                } else {
                    newErrors.remove("slots")
                }
            }
            "deadline" -> {
                if (currentState.deadline.isBlank()) {
                    newErrors["deadline"] = "Application deadline is required"
                } else {
                    newErrors.remove("deadline")
                }
            }
        }

        _state.value = _state.value.copy(errors = newErrors)
    }

    // Function to validate all fields and return if form is valid
    fun validateAllFields(): Boolean {
        listOf(
            "jobTitle", "jobDescription", "requirements", "internshipType",
            "location", "duration", "slots", "deadline"
        ).forEach { validateField(it) }

        return _state.value.errors.isEmpty()
    }

    fun createPosting(userId: String, companyName: String, companyAddress: String, aboutCompany: String, onComplete: () -> Unit) {
        listOf(
            "jobTitle", "jobDescription", "requirements", "internshipType",
            "location", "duration", "slots", "deadline"
        ).forEach { validateField(it) }

        if (_state.value.errors.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val internship = Internship(
                id = "",
                title = _state.value.jobTitle,
                companyName = companyName,
                category = _state.value.category, // Pass category
                companyLogo = null,
                location = _state.value.location,
                workType = _state.value.internshipType,
                duration = _state.value.duration,
                salaryRange = _state.value.stipend.ifBlank { "Unpaid" },
                availableSlots = _state.value.slots.toIntOrNull() ?: 0,
                description = _state.value.jobDescription,
                requirements = _state.value.requirements,
                aboutCompany = aboutCompany,
                companyAddress = companyAddress,
                applicationDeadline = _state.value.deadline,
                isActive = _state.value.status == "Active"
            )

            repository.createInternship(userId, internship).onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    createSuccess = true
                )
                onComplete()
                // Clear the form after successful creation
                clearForm()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message
                )
            }
        }
    }

    fun resetCreateSuccess() {
        _state.value = _state.value.copy(createSuccess = false)
    }

    // NEW FUNCTION: Clear all form fields
    fun clearForm() {
        _state.value = CreatePostingState() // Reset to default empty state
    }
}