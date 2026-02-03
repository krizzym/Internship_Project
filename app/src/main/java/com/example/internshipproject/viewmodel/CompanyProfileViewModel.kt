package com.example.internshipproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Company
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanyProfileState(
    val company: Company? = null,
    val companyEmail: String = "",
    val contactNumber: String = "",
    val companyName: String = "",
    val contactPerson: String = "",
    val industryType: String = "",
    val companyAddress: String = "",
    val companyDescription: String = "",
    val logoUri: String? = null,
    val newLogoUri: Uri? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val errors: Map<String, String> = emptyMap()
)

class CompanyProfileViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyProfileState())
    val state: StateFlow<CompanyProfileState> = _state.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            repository.getCompanyProfile(userId).onSuccess { company ->
                _state.value = _state.value.copy(
                    company = company,
                    companyEmail = company.companyEmail,
                    contactNumber = company.contactNumber,
                    companyName = company.companyName,
                    contactPerson = company.contactPerson,
                    industryType = company.industryType,
                    companyAddress = company.companyAddress,
                    companyDescription = company.companyDescription,
                    logoUri = company.logoUri
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.message)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun updateContactNumber(value: String) {
        _state.value = _state.value.copy(contactNumber = value)
        validateField("contactNumber")
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

    fun updateNewLogoUri(uri: Uri?) {
        _state.value = _state.value.copy(newLogoUri = uri)
    }

    private fun validateField(fieldName: String) {
        val currentState = _state.value
        val newErrors = currentState.errors.toMutableMap()

        when (fieldName) {
            "contactNumber" -> {
                if (currentState.contactNumber.isBlank()) {
                    newErrors["contactNumber"] = "Contact number is required"
                } else {
                    newErrors.remove("contactNumber")
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
                if (currentState.industryType.isBlank()) {
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
        }

        _state.value = currentState.copy(errors = newErrors)
    }

    // Enhanced updateProfile with proper success feedback
    fun updateProfile(userId: String) {
        // Clear previous messages
        _state.value = _state.value.copy(
            successMessage = null,
            errorMessage = null
        )

        // Validate all required fields
        listOf(
            "contactNumber", "companyName", "contactPerson",
            "industryType", "companyAddress", "companyDescription"
        ).forEach { validateField(it) }

        // If there are validation errors, stop and show error
        if (_state.value.errors.isNotEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "Please fix all errors before updating"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, errorMessage = null)

            val updates = mapOf(
                "contactNumber" to _state.value.contactNumber,
                "companyName" to _state.value.companyName,
                "contactPerson" to _state.value.contactPerson,
                "industryType" to _state.value.industryType,
                "companyAddress" to _state.value.companyAddress,
                "companyDescription" to _state.value.companyDescription
            )

            repository.updateCompanyProfile(userId, updates).onSuccess {
                // Set success state with message
                _state.value = _state.value.copy(
                    isUpdating = false,
                    updateSuccess = true,
                    successMessage = "Profile updated successfully!"  // ✅ NEW: Success message
                )
            }.onFailure { error ->
                // Update failed - show error message
                _state.value = _state.value.copy(
                    isUpdating = false,
                    errorMessage = error.message ?: "Failed to update profile. Please try again."
                )
            }
        }
    }

    // Enhanced uploadLogo with proper success feedback
    fun uploadLogo(userId: String) {
        // Clear previous messages
        _state.value = _state.value.copy(
            successMessage = null,
            errorMessage = null
        )

        val newLogoUri = _state.value.newLogoUri

        if (newLogoUri == null) {
            _state.value = _state.value.copy(errorMessage = "Please select a logo file")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, errorMessage = null)

            repository.uploadCompanyLogo(userId, newLogoUri).onSuccess { downloadUrl ->
                // Update logo URL in profile
                val updates = mapOf("logoUri" to downloadUrl)
                repository.updateCompanyProfile(userId, updates).onSuccess {
                    // Set success state with message
                    _state.value = _state.value.copy(
                        logoUri = downloadUrl,
                        newLogoUri = null,
                        isUpdating = false,
                        updateSuccess = true,
                        successMessage = "Logo uploaded successfully!"
                    )
                }.onFailure { error ->
                    // Failed to update profile with logo URL
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        errorMessage = error.message ?: "Failed to update logo. Please try again."
                    )
                }
            }.onFailure { error ->
                // Failed to upload logo
                _state.value = _state.value.copy(
                    isUpdating = false,
                    errorMessage = error.message ?: "Failed to upload logo. Please try again."
                )
            }
        }
    }

    // Clear success message after displaying
    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    // Clear error message after displaying
    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }
}
