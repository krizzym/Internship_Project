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

data class CompanyApplicationsState(
    val applications: List<Application> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CompanyApplicationsViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyApplicationsState())
    val state: StateFlow<CompanyApplicationsState> = _state.asStateFlow()

    // ✅ FIXED: Now accepts companyId instead of companyName
    fun loadApplications(companyId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            println("🔍 CompanyApplicationsVM: Loading applications for company: $companyId")

            repository.getAllCompanyApplications(companyId).onSuccess { applications ->
                println("✅ CompanyApplicationsVM: Loaded ${applications.size} applications")
                applications.forEachIndexed { i, app ->
                    println("   [$i] ${app.studentEmail} - ${app.internshipTitle} - ${app.status}")
                }
                _state.value = _state.value.copy(applications = applications)
            }.onFailure { error ->
                println("❌ CompanyApplicationsVM: Error loading applications: ${error.message}")
                _state.value = _state.value.copy(errorMessage = error.message)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun setFilter(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
        println("🔍 Filter changed to: $filter")
    }

    fun getFilteredApplications(): List<Application> {
        val filtered = when (_state.value.selectedFilter) {
            "All" -> _state.value.applications
            else -> _state.value.applications.filter {
                it.status.name == _state.value.selectedFilter
            }
        }
        println("🔍 Filtered applications: ${filtered.size} (Filter: ${_state.value.selectedFilter})")
        return filtered
    }

    fun getPendingCount() = _state.value.applications.count {
        it.status == ApplicationStatus.PENDING
    }

    fun getReviewedCount() = _state.value.applications.count {
        it.status == ApplicationStatus.REVIEWED
    }

    fun getShortlistedCount() = _state.value.applications.count {
        it.status == ApplicationStatus.SHORTLISTED
    }

    fun getAcceptedCount() = _state.value.applications.count {
        it.status == ApplicationStatus.ACCEPTED
    }
}
