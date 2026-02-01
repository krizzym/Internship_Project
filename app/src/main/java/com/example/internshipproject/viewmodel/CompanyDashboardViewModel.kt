package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.Company
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanyDashboardState(
    val company: Company? = null,
    val postings: List<Internship> = emptyList(),
    val applications: List<Application> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CompanyDashboardViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyDashboardState())
    val state: StateFlow<CompanyDashboardState> = _state.asStateFlow()

    fun loadDashboardData(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // Load company profile
            repository.getCompanyProfile(userId).onSuccess { company ->
                _state.value = _state.value.copy(company = company)

                // ✅ REAL-TIME: Collect company internships
                launch {
                    repository.getCompanyInternshipsFlow(userId).collect { postings ->
                        _state.value = _state.value.copy(postings = postings)
                    }
                }

                // Load applications
                repository.getAllCompanyApplications(userId).onSuccess { applications ->
                    _state.value = _state.value.copy(applications = applications)
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.message)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun getTotalPostings() = _state.value.postings.size

    fun getActivePostings() = _state.value.postings.count { it.isActive }

    fun getTotalApplications() = _state.value.applications.size

    fun getPendingReview() = _state.value.applications.count {
        it.status == ApplicationStatus.PENDING
    }

    fun getRecentApplications() = _state.value.applications.take(5)

    fun getRecentPostings() = _state.value.postings.take(3)
}
