package com.example.internshipproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internshipproject.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userRole: String? = null,  // Added
    val userId: String? = null     // Added
)

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value)
        validateField("email")
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value)
        validateField("password")
    }

    private fun validateField(fieldName: String) {
        val currentState = _state.value
        val newErrors = currentState.errors.toMutableMap()

        when (fieldName) {
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
                if (currentState.password.isBlank()) {
                    newErrors["password"] = "Password is required"
                } else {
                    newErrors.remove("password")
                }
            }
        }

        _state.value = currentState.copy(errors = newErrors)
    }

    fun login() {
        validateField("email")
        validateField("password")

        if (_state.value.errors.isNotEmpty()) {
            return
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.login(_state.value.email, _state.value.password)
            result.fold(
                onSuccess = { userSession ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        userRole = userSession.userRole,  // Capture role
                        userId = userSession.token         // Capture userId
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login failed"
                    )
                }
            )
        }
    }
}
