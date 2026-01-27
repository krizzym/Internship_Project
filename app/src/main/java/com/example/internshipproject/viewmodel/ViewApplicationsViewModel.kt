package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewApplicationsState(
    val posting: Internship? = null,
    val applications: List<Application> = emptyList(),
    val statusCounts: Map<ApplicationStatus, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ViewApplicationsViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ViewApplicationsState())
    val state: StateFlow<ViewApplicationsState> = _state.asStateFlow()

    fun loadData(postingId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // Load posting details
            repository.getInternshipById(postingId).onSuccess { posting ->
                _state.value = _state.value.copy(posting = posting)
            }

            // Load applications
            repository.getApplicationsByPosting(postingId).onSuccess { applications ->
                _state.value = _state.value.copy(applications = applications)
            }

            // Load status counts
            val statusCounts = repository.getApplicationStatusCounts(postingId)
            _state.value = _state.value.copy(
                statusCounts = statusCounts,
                isLoading = false
            )
        }
    }

    fun refreshData(postingId: String) {
        loadData(postingId)
    }
}