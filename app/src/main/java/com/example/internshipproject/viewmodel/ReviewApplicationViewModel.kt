package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewApplicationState(
    val application: Application? = null,
    val selectedStatus: String = "Pending",
    val isLoading: Boolean = false,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ReviewApplicationViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewApplicationState())
    val state: StateFlow<ReviewApplicationState> = _state.asStateFlow()

    fun loadApplication(applicationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            repository.getApplicationById(applicationId).onSuccess { application ->
                _state.value = _state.value.copy(
                    application = application,
                    selectedStatus = application.status.name
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.message)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun updateSelectedStatus(status: String) {
        _state.value = _state.value.copy(selectedStatus = status)
    }

    fun updateStatus(applicationId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val status = ApplicationStatus.valueOf(_state.value.selectedStatus)

            repository.updateApplicationStatus(applicationId, status).onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    updateSuccess = true
                )
                onComplete()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message
                )
            }
        }
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }
}