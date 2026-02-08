package com.example.internshipproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Student
import com.example.internshipproject.data.repository.AuthRepository
import com.example.internshipproject.ui.components.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentRegistrationState(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val school: String = "",
    val course: String = "",
    val yearLevel: String = "",
    val city: String = "",
    val barangay: String = "",
    val selectedInternshipType: String = "", // Changed to single selection
    val skills: String = "",
    val resumeUri: Uri? = null,
    val agreedToTerms: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null
)

class StudentRegistrationViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StudentRegistrationState())
    val state: StateFlow<StudentRegistrationState> = _state.asStateFlow()

    fun updateFirstName(value: String) {
        _state.value = _state.value.copy(firstName = value)
        validateField("firstName")
    }

    fun updateMiddleName(value: String) {
        _state.value = _state.value.copy(middleName = value)
    }

    fun updateLastName(value: String) {
        _state.value = _state.value.copy(lastName = value)
        validateField("lastName")
    }

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value)
        validateField("email")
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value)
        validateField("password")
    }

    fun updateConfirmPassword(value: String) {
        _state.value = _state.value.copy(confirmPassword = value)
        validateField("confirmPassword")
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

    fun updateInternshipType(type: String) {
        _state.value = _state.value.copy(selectedInternshipType = type)
        validateField("internshipTypes")
    }

    fun updateSkills(value: String) {
        _state.value = _state.value.copy(skills = value)
    }

    fun toggleAgreement() {
        _state.value = _state.value.copy(agreedToTerms = !_state.value.agreedToTerms)
        validateField("terms")
    }

    fun resetRegistrationSuccess() {
        _state.value = _state.value.copy(registrationSuccess = false)
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
            "lastName" -> {
                if (currentState.lastName.isBlank()) {
                    newErrors["lastName"] = "Last name is required"
                } else {
                    newErrors.remove("lastName")
                }
            }
            "email" -> {
                if (currentState.email.isBlank()) {
                    newErrors["email"] = "Email is required"
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
                    newErrors["email"] = "Invalid email format"
                } else {
                    newErrors.remove("email")
                }
            }
            "password" -> {
                val validationResult = PasswordValidator.validate(currentState.password)
                if (!validationResult.isValid) {
                    newErrors["password"] = validationResult.errorMessage ?: "Invalid password"
                } else {
                    newErrors.remove("password")
                }
            }
            "confirmPassword" -> {
                if (currentState.confirmPassword != currentState.password) {
                    newErrors["confirmPassword"] = "Passwords do not match"
                } else {
                    newErrors.remove("confirmPassword")
                }
            }
            "school" -> {
                if (currentState.school.isBlank()) {
                    newErrors["school"] = "School/University is required"
                } else {
                    newErrors.remove("school")
                }
            }
            "course" -> {
                if (currentState.course.isBlank()) {
                    newErrors["course"] = "Course/Program is required"
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
                if (currentState.selectedInternshipType.isBlank()) {
                    newErrors["internshipTypes"] = "Please select your preferred internship type"
                } else {
                    newErrors.remove("internshipTypes")
                }
            }
            "terms" -> {
                if (!currentState.agreedToTerms) {
                    newErrors["terms"] = "You must agree to the Terms & Conditions and Privacy Policy"
                } else {
                    newErrors.remove("terms")
                }
            }
        }

        _state.value = currentState.copy(errors = newErrors)
    }

    fun register() {
        val currentState = _state.value

        // Validate all fields
        validateField("firstName")
        validateField("lastName")
        validateField("email")
        validateField("password")
        validateField("confirmPassword")
        validateField("school")
        validateField("course")
        validateField("yearLevel")
        validateField("city")
        validateField("barangay")
        validateField("internshipTypes")
        validateField("terms")

        if (_state.value.errors.isNotEmpty()) {
            return
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)

        val internshipTypes = listOf(currentState.selectedInternshipType)

        val student = Student(
            firstName = currentState.firstName,
            middleName = currentState.middleName.ifBlank { null },
            lastName = currentState.lastName,
            email = currentState.email,
            password = currentState.password,
            school = currentState.school,
            course = currentState.course,
            yearLevel = currentState.yearLevel,
            city = currentState.city,
            barangay = currentState.barangay,
            internshipTypes = internshipTypes,
            skills = currentState.skills,
            resumeUri = null
        )

        viewModelScope.launch {
            val result = repository.registerStudent(student)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        registrationSuccess = true
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Registration failed"
                    )
                }
            )
        }
    }
}
