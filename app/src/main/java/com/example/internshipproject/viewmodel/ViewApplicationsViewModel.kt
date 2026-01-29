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

            try {
                // ✅ DEBUG: Log what we're loading
                println("🔍 ViewApplicationsViewModel: Loading data for posting: $postingId")

                // Load posting details
                repository.getInternshipById(postingId).onSuccess { posting ->
                    println("✅ Posting loaded: ${posting?.title}")
                    _state.value = _state.value.copy(posting = posting)
                }.onFailure { e ->
                    println("❌ Failed to load posting: ${e.message}")
                }

                // Load applications
                repository.getApplicationsByPosting(postingId).onSuccess { applications ->
                    println("✅ Applications loaded: ${applications.size} applications")
                    applications.forEachIndexed { index, app ->
                        println("   [$index] ${app.studentEmail} - ${app.status}")
                    }
                    _state.value = _state.value.copy(applications = applications)
                }.onFailure { e ->
                    println("❌ Failed to load applications: ${e.message}")
                    _state.value = _state.value.copy(errorMessage = "Failed to load applications: ${e.message}")
                }

                // Load status counts
                val statusCounts = repository.getApplicationStatusCounts(postingId)
                println("✅ Status counts loaded: $statusCounts")

                _state.value = _state.value.copy(
                    statusCounts = statusCounts,
                    isLoading = false
                )

                // ✅ DEBUG: Final state check
                println("🎯 Final state:")
                println("   Posting: ${_state.value.posting?.title}")
                println("   Applications: ${_state.value.applications.size}")
                println("   Status counts: ${_state.value.statusCounts}")

            } catch (e: Exception) {
                println("❌ EXCEPTION in loadData: ${e.message}")
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    fun refreshData(postingId: String) {
        loadData(postingId)
    }
}
