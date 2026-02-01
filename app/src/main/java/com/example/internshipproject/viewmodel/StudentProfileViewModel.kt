package com.example.internshipproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.firebase.FirebaseManager
import com.example.internshipproject.data.model.StudentProfile
import com.example.internshipproject.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ✅ UPDATED: Enhanced state with proper feedback mechanism
data class StudentProfileState(
    val profile: StudentProfile? = null,
    val firstName: String = "",
    val middleName: String = "",
    val surname: String = "",
    val email: String = "",
    val school: String = "",
    val course: String = "",
    val yearLevel: String = "",
    val city: String = "",
    val barangay: String = "",
    val onsite: Boolean = false,
    val remote: Boolean = false,
    val hybrid: Boolean = false,
    val skills: String = "",
    val resumeUri: Uri? = null,
    val newResumeUri: Uri? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,  // ✅ NEW: Success message for user feedback
    val errors: Map<String, String> = emptyMap()
)

class StudentProfileViewModel(
    private val repository: FirebaseAuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StudentProfileState())
    val state: StateFlow<StudentProfileState> = _state.asStateFlow()

    fun loadProfile(profile: StudentProfile) {
        _state.value = _state.value.copy(
            profile = profile,
            firstName = profile.firstName,
            middleName = profile.middleName ?: "",
            surname = profile.surname,
            email = profile.email,
            school = profile.school,
            course = profile.course,
            yearLevel = profile.yearLevel,
            city = profile.city,
            barangay = profile.barangay,
            onsite = profile.internshipTypes.contains("On-site"),
            remote = profile.internshipTypes.contains("Remote"),
            hybrid = profile.internshipTypes.contains("Hybrid"),
            skills = profile.skills,
            resumeUri = profile.resumeUri?.let { Uri.parse(it) }
        )
    }

    fun updateFirstName(value: String) {
        _state.value = _state.value.copy(firstName = value)
        validateField("firstName")
    }

    fun updateMiddleName(value: String) {
        _state.value = _state.value.copy(middleName = value)
    }

    fun updateSurname(value: String) {
        _state.value = _state.value.copy(surname = value)
        validateField("surname")
    }

    fun updateSchool(value: String) {
        _state.value = _state.value.copy(school = value)
        validateField("school")
    }

    fun updateCourse(value: String) {
        _state.value = _state.value.copy(course = value)
        validateField("course")
    }

    fun updateYearLevel(value: String) {
        _state.value = _state.value.copy(yearLevel = value)
        validateField("yearLevel")
    }

    fun updateCity(value: String) {
        _state.value = _state.value.copy(city = value)
        validateField("city")
    }

    fun updateBarangay(value: String) {
        _state.value = _state.value.copy(barangay = value)
        validateField("barangay")
    }

    fun toggleOnsite() {
        _state.value = _state.value.copy(onsite = !_state.value.onsite)
        validateField("internshipTypes")
    }

    fun toggleRemote() {
        _state.value = _state.value.copy(remote = !_state.value.remote)
        validateField("internshipTypes")
    }

    fun toggleHybrid() {
        _state.value = _state.value.copy(hybrid = !_state.value.hybrid)
        validateField("internshipTypes")
    }

    fun updateSkills(value: String) {
        _state.value = _state.value.copy(skills = value)
    }

    fun updateResumeUri(uri: Uri?) {
        _state.value = _state.value.copy(newResumeUri = uri)
    }

    private fun validateField(fieldName: String) {
        val currentState = _state.value
        val newErrors = currentState.errors.toMutableMap()

        when (fieldName) {
            "firstName" -> {
                if (currentState.firstName.isBlank()) {
                    newErrors["firstName"] = "First name is required"
                } else {
                    newErrors.remove("firstName")
                }
            }
            "surname" -> {
                if (currentState.surname.isBlank()) {
                    newErrors["surname"] = "Surname is required"
                } else {
                    newErrors.remove("surname")
                }
            }
            "school" -> {
                if (currentState.school.isBlank()) {
                    newErrors["school"] = "School is required"
                } else {
                    newErrors.remove("school")
                }
            }
            "course" -> {
                if (currentState.course.isBlank()) {
                    newErrors["course"] = "Course is required"
                } else {
                    newErrors.remove("course")
                }
            }
            "yearLevel" -> {
                if (currentState.yearLevel.isBlank()) {
                    newErrors["yearLevel"] = "Year level is required"
                } else {
                    newErrors.remove("yearLevel")
                }
            }
            "city" -> {
                if (currentState.city.isBlank()) {
                    newErrors["city"] = "City is required"
                } else {
                    newErrors.remove("city")
                }
            }
            "barangay" -> {
                if (currentState.barangay.isBlank()) {
                    newErrors["barangay"] = "Barangay is required"
                } else {
                    newErrors.remove("barangay")
                }
            }
            "internshipTypes" -> {
                if (!currentState.onsite && !currentState.remote && !currentState.hybrid) {
                    newErrors["internshipTypes"] = "Select at least one internship type"
                } else {
                    newErrors.remove("internshipTypes")
                }
            }
        }

        _state.value = currentState.copy(errors = newErrors)
    }

    // ✅ UPDATED: Enhanced updateProfile with proper feedback mechanism
    fun updateProfile() {
        // Clear previous messages
        _state.value = _state.value.copy(
            successMessage = null,
            errorMessage = null
        )

        // Validate all fields
        listOf(
            "firstName", "surname", "school", "course",
            "yearLevel", "city", "barangay", "internshipTypes"
        ).forEach { validateField(it) }

        // If there are validation errors, stop and show error
        if (_state.value.errors.isNotEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "Please fix all errors before updating"
            )
            return
        }

        // ✅ Set loading state to prevent duplicate submissions
        _state.value = _state.value.copy(isUpdating = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val currentState = _state.value
                val userId = FirebaseManager.getCurrentUserId()

                if (userId == null) {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        errorMessage = "User not logged in. Please log in again."
                    )
                    return@launch
                }

                val internshipTypes = buildList {
                    if (currentState.onsite) add("On-site")
                    if (currentState.remote) add("Remote")
                    if (currentState.hybrid) add("Hybrid")
                }

                val updates = mapOf(
                    "firstName" to currentState.firstName,
                    "middleName" to (currentState.middleName.ifBlank { null } ?: ""),
                    "lastName" to currentState.surname,
                    "school" to currentState.school,
                    "course" to currentState.course,
                    "yearLevel" to currentState.yearLevel,
                    "city" to currentState.city,
                    "barangay" to currentState.barangay,
                    "internshipTypes" to internshipTypes,
                    "skills" to currentState.skills
                )

                // ✅ Execute Firebase update asynchronously
                val result = repository.updateStudentProfile(userId, updates)

                result.fold(
                    onSuccess = {
                        // ✅ Update successful - create updated profile
                        val updatedProfile = StudentProfile(
                            firstName = currentState.firstName,
                            middleName = currentState.middleName.ifBlank { null },
                            surname = currentState.surname,
                            email = currentState.email,
                            school = currentState.school,
                            course = currentState.course,
                            yearLevel = currentState.yearLevel,
                            city = currentState.city,
                            barangay = currentState.barangay,
                            internshipTypes = internshipTypes,
                            skills = currentState.skills,
                            resumeUri = (currentState.newResumeUri ?: currentState.resumeUri)?.toString()
                        )

                        // ✅ Set success state with message
                        _state.value = _state.value.copy(
                            profile = updatedProfile,
                            isUpdating = false,
                            updateSuccess = true,
                            successMessage = "Profile updated successfully!"
                        )
                    },
                    onFailure = { error ->
                        // ✅ Update failed - show error message
                        _state.value = _state.value.copy(
                            isUpdating = false,
                            errorMessage = error.message ?: "Failed to update profile. Please try again."
                        )
                    }
                )
            } catch (e: Exception) {
                // ✅ Handle unexpected errors
                _state.value = _state.value.copy(
                    isUpdating = false,
                    errorMessage = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    fun updateResume() {
        val currentState = _state.value
        val newResumeUri = currentState.newResumeUri

        if (newResumeUri == null) {
            _state.value = _state.value.copy(errorMessage = "Please select a resume file")
            return
        }

        _state.value = _state.value.copy(isUpdating = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val userId = FirebaseManager.getCurrentUserId()

                if (userId == null) {
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        errorMessage = "User not logged in. Please log in again."
                    )
                    return@launch
                }

                val result = repository.uploadResume(userId, newResumeUri)

                result.fold(
                    onSuccess = { downloadUrl ->
                        _state.value = _state.value.copy(
                            resumeUri = Uri.parse(downloadUrl),
                            newResumeUri = null,
                            isUpdating = false,
                            updateSuccess = true,
                            successMessage = "Resume uploaded successfully!"
                        )
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            isUpdating = false,
                            errorMessage = error.message ?: "Failed to upload resume. Please try again."
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUpdating = false,
                    errorMessage = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    // ✅ NEW: Clear success message after displaying
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    // ✅ NEW: Clear error message after displaying
    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }
}
