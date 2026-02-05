//ReviewApplicationViewModel.kt - CORRECTED VERSION
package com.example.internshipproject.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.model.Application
import com.example.internshipproject.data.model.ApplicationStatus
import com.example.internshipproject.data.model.StudentProfile
import com.example.internshipproject.data.repository.CompanyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReviewApplicationState(
    val application: Application? = null,
    val studentProfile: StudentProfile? = null,
    val selectedStatus: String = "PENDING",  // Changed from "Pending" to match enum
    val isLoading: Boolean = false,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
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

                // Load student profile
                loadStudentProfile(application.studentEmail)
            }.onFailure { error ->
                _state.value = _state.value.copy(errorMessage = error.message)
            }

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    // Load student profile by email
    private fun loadStudentProfile(studentEmail: String) {
        viewModelScope.launch {
            repository.getStudentProfileByEmail(studentEmail).onSuccess { profile ->
                _state.value = _state.value.copy(studentProfile = profile)
                Log.d("ReviewAppVM", "Student profile loaded: ${profile.firstName} ${profile.surname}")
            }.onFailure { error ->
                Log.w("ReviewAppVM", "Failed to load student profile: ${error.message}")
            }
        }
    }

    fun updateSelectedStatus(status: String) {
        _state.value = _state.value.copy(selectedStatus = status)
    }

    fun updateStatus(applicationId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val status = try {
                ApplicationStatus.valueOf(_state.value.selectedStatus.uppercase())
            } catch (e: IllegalArgumentException) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid status selected"
                )
                return@launch
            }

            repository.updateApplicationStatus(applicationId, status).onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    updateSuccess = true,
                    successMessage = "Application status updated successfully"
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

    fun viewResume(context: Context, application: Application) {
        viewModelScope.launch {
            try {
                if (application.resumeBase64.isNullOrEmpty()) {
                    _state.value = _state.value.copy(
                        errorMessage = "Resume not available"
                    )
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val bytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)
                    val fileName = application.resumeFileName ?: "resume.pdf"
                    val tempFile = File(context.cacheDir, fileName)
                    tempFile.writeBytes(bytes)

                    Log.d("ReviewAppVM", "Resume saved to temp: ${tempFile.absolutePath}")

                    withContext(Dispatchers.Main) {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                tempFile
                            )

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }

                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                                Log.d("ReviewAppVM", "PDF viewer opened")
                            } else {
                                _state.value = _state.value.copy(
                                    errorMessage = "No PDF viewer found. Please install a PDF reader app."
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("ReviewAppVM", "Failed to open PDF: ${e.message}")
                            _state.value = _state.value.copy(
                                errorMessage = "No PDF viewer found. Please install a PDF reader app."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReviewAppVM", "Error viewing resume: ${e.message}")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to open resume: ${e.message}"
                )
            }
        }
    }

    fun downloadResume(context: Context, application: Application) {
        viewModelScope.launch {
            try {
                if (application.resumeBase64.isNullOrEmpty()) {
                    _state.value = _state.value.copy(
                        errorMessage = "Resume not available"
                    )
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val bytes = Base64.decode(application.resumeBase64, Base64.DEFAULT)
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                    val studentName = application.studentEmail.substringBefore("@")
                    val fileName = "${studentName}_${application.resumeFileName ?: "resume.pdf"}"
                    val file = File(downloadsDir, fileName)

                    file.writeBytes(bytes)

                    Log.d("ReviewAppVM", "Resume downloaded to: ${file.absolutePath}")

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            successMessage = "Resume downloaded to Downloads folder"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ReviewAppVM", "Error downloading resume: ${e.message}")
                _state.value = _state.value.copy(
                    errorMessage = "Failed to download resume: ${e.message}"
                )
            }
        }
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}