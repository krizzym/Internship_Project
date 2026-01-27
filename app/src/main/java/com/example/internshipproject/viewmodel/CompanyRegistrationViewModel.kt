package com.example.internshipproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Company
import com.example.internshipproject.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanyRegistrationState(
    val companyEmail: String = "",
    val contactNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val companyName: String = "",
    val contactPerson: String = "",
    val industryType: String = "",
    val companyAddress: String = "",
    val companyDescription: String = "",
    val logoUri: Uri? = null,
    val agreedToTerms: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CompanyRegistrationViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyRegistrationState())
    val state: StateFlow<CompanyRegistrationState> = _state.asStateFlow()

    fun updateCompanyEmail(value: String) {
        _state.value = _state.value.copy(companyEmail = value)
        validateField("companyEmail")
    }

    fun updateContactNumber(value: String) {
        _state.value = _state.value.copy(contactNumber = value)
        validateField("contactNumber")
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value)
        validateField("password")
    }

    fun updateConfirmPassword(value: String) {
        _state.value = _state.value.copy(confirmPassword = value)
        validateField("confirmPassword")
    }

    fun updateCompanyName(value: String) {
        _state.value = _state.value.copy(companyName = value)
        validateField("companyName")
    }

    fun updateContactPerson(value: String) {
        _state.value = _state.value.copy(contactPerson = value)
        validateField("contactPerson")
    }

    fun updateIndustryType(value: String) {
        _state.value = _state.value.copy(industryType = value)
        validateField("industryType")
    }

    fun updateCompanyAddress(value: String) {
        _state.value = _state.value.copy(companyAddress = value)
        validateField("companyAddress")
    }

    fun updateCompanyDescription(value: String) {
        _state.value = _state.value.copy(companyDescription = value)
        validateField("companyDescription")
    }

    fun updateLogoUri(uri: Uri?) {
        _state.value = _state.value.copy(logoUri = uri)
        validateField("logo")
    }

    fun toggleAgreement() {
        _state.value = _state.value.copy(agreedToTerms = !_state.value.agreedToTerms)
        validateField("terms")
    }

    private fun validateField(fieldName: String) {
        val currentState = _state.value
        val newErrors = currentState.errors.toMutableMap()

        when (fieldName) {
            "companyEmail" -> {
                when {
                    currentState.companyEmail.isBlank() ->
                        newErrors["companyEmail"] = "Company email is required"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.companyEmail).matches() ->
                        newErrors["companyEmail"] = "Invalid email format"
                    else -> newErrors.remove("companyEmail")
                }
            }
            "contactNumber" -> {
                if (currentState.contactNumber.isBlank()) {
                    newErrors["contactNumber"] = "Contact number is required"
                } else {
                    newErrors.remove("contactNumber")
                }
            }
            "password" -> {
                if (currentState.password.length < 12) {
                    newErrors["password"] = "Password must be at least 12 characters long"
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
            "companyName" -> {
                if (currentState.companyName.isBlank()) {
                    newErrors["companyName"] = "Company name is required"
                } else {
                    newErrors.remove("companyName")
                }
            }
            "contactPerson" -> {
                if (currentState.contactPerson.isBlank()) {
                    newErrors["contactPerson"] = "Contact person is required"
                } else {
                    newErrors.remove("contactPerson")
                }
            }
            "industryType" -> {
                if (currentState.industryType.isBlank() || currentState.industryType == "Select industry") {
                    newErrors["industryType"] = "Industry type is required"
                } else {
                    newErrors.remove("industryType")
                }
            }
            "companyAddress" -> {
                if (currentState.companyAddress.isBlank()) {
                    newErrors["companyAddress"] = "Company address is required"
                } else {
                    newErrors.remove("companyAddress")
                }
            }
            "companyDescription" -> {
                if (currentState.companyDescription.length < 50) {
                    newErrors["companyDescription"] = "Description must be at least 50 characters"
                } else {
                    newErrors.remove("companyDescription")
                }
            }
            "logo" -> {
                if (currentState.logoUri == null) {
                    newErrors["logo"] = "Company logo is required"
                } else {
                    newErrors.remove("logo")
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

        listOf(
            "companyEmail", "contactNumber", "password", "confirmPassword",
            "companyName", "contactPerson", "industryType", "companyAddress",
            "companyDescription", "logo", "terms"
        ).forEach { validateField(it) }

        if (_state.value.errors.isNotEmpty()) {
            return
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)

        val company = Company(
            companyEmail = currentState.companyEmail,
            contactNumber = currentState.contactNumber,
            password = currentState.password,
            companyName = currentState.companyName,
            contactPerson = currentState.contactPerson,
            industryType = currentState.industryType,
            companyAddress = currentState.companyAddress,
            companyDescription = currentState.companyDescription,
            logoUri = currentState.logoUri?.toString()
        )

        viewModelScope.launch {
            val result = repository.registerCompany(company)
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