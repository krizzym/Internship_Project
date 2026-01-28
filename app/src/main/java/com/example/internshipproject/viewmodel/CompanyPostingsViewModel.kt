package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Internship
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanyPostingsState(
    val postings: List<Internship> = emptyList(),
    val applicationCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CompanyPostingsViewModel(
    private val repository: CompanyRepository = CompanyRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyPostingsState())
    val state: StateFlow<CompanyPostingsState> = _state.asStateFlow()

    fun loadPostings(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // ✅ REAL-TIME: Collect company internships
            repository.getCompanyInternshipsFlow(userId).collect { postings ->
                _state.value = _state.value.copy(
                    postings = postings,
                    isLoading = false
                )

                // Load application counts for each posting
                val counts = mutableMapOf<String, Int>()
                postings.forEach { posting ->
                    repository.getApplicationCountForPosting(posting.id).onSuccess { count ->
                        counts[posting.id] = count
                    }
                }
                _state.value = _state.value.copy(applicationCounts = counts)
            }
        }
    }

    fun closePosting(postingId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val posting = _state.value.postings.find { it.id == postingId } ?: return@launch
            repository.updateInternship(postingId, posting.copy(isActive = false))
                .onSuccess {
                    onComplete()
                }
        }
    }

    // ✅ NEW: Reopen functionality
    fun reopenPosting(postingId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val posting = _state.value.postings.find { it.id == postingId } ?: return@launch
            repository.updateInternship(postingId, posting.copy(isActive = true))
                .onSuccess {
                    onComplete()
                }
        }
    }

    fun deletePosting(postingId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteInternship(postingId).onSuccess {
                onComplete()
            }
        }
    }

    fun getApplicationCount(postingId: String): Int {
        return _state.value.applicationCounts[postingId] ?: 0
    }
}