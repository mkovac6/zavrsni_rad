package com.finalapp.accommodationapp.ui.viewmodels.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.AuthStateManager
import com.finalapp.accommodationapp.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Login Screen
 * Manages authentication logic and replaces UserSession with AuthStateManager
 */
class LoginViewModel(
    private val userRepository: UserRepository,
    private val authStateManager: AuthStateManager
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val snackbarMessage: String? = null,
        val loginSuccessUserType: String? = null // Triggers navigation when set
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    fun login() {
        val currentState = _uiState.value

        // Validation
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Please enter email and password"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )

            val user = userRepository.debugLogin(
                currentState.email.trim(),
                currentState.password
            )

            if (user != null) {
                // Replace UserSession with AuthStateManager
                authStateManager.setUser(user)

                val firstName = when (user.email) {
                    "ana.kovac@student.hr" -> "Ana"
                    "marko.novak@gmail.com" -> "Marko"
                    "admin@accommodation.com" -> "Admin"
                    else -> user.email.substringBefore("@")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Welcome back, $firstName!",
                    snackbarMessage = "Login successful! Welcome $firstName",
                    loginSuccessUserType = user.userType
                )

                // Small delay to show success message before navigation
                delay(1000)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid email or password",
                    snackbarMessage = "Login failed. Please check your credentials."
                )
            }
        }
    }

    fun quickLogin(email: String, password: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            password = password
        )
        login()
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(loginSuccessUserType = null)
    }
}
