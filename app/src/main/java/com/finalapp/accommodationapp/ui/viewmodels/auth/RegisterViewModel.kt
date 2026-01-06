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
 * ViewModel for Register Screen
 * Manages registration logic with form validation and AuthStateManager integration
 */
class RegisterViewModel(
    private val userRepository: UserRepository,
    private val authStateManager: AuthStateManager
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val selectedAccountType: String = "student", // Default to student
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val showSuccessMessage: Boolean = false,
        val snackbarMessage: String? = null,
        val navigationTarget: String? = null // "university_selection", "landlord_profile", or "login"
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed validation properties
    val isEmailValid: Boolean
        get() = _uiState.value.email.contains("@") && _uiState.value.email.contains(".")

    val isPasswordValid: Boolean
        get() = _uiState.value.password.length >= 6

    val doPasswordsMatch: Boolean
        get() = _uiState.value.password == _uiState.value.confirmPassword &&
                _uiState.value.password.isNotEmpty()

    val isFormValid: Boolean
        get() = isEmailValid && isPasswordValid && doPasswordsMatch && !_uiState.value.isLoading

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

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            errorMessage = null
        )
    }

    fun selectAccountType(accountType: String) {
        _uiState.value = _uiState.value.copy(selectedAccountType = accountType)
    }

    fun register(isAdminCreating: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val accountType = if (isAdminCreating) "student" else _uiState.value.selectedAccountType
            val user = userRepository.register(
                _uiState.value.email,
                _uiState.value.password,
                accountType
            )

            if (user != null) {
                // Replace UserSession with AuthStateManager
                authStateManager.setUser(user)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showSuccessMessage = true,
                    snackbarMessage = "Registration successful! Welcome to Student Accommodation!"
                )

                // Small delay to show success message
                delay(1500)

                // Set navigation target based on context
                val target = if (isAdminCreating) {
                    "login" // Go back to admin panel
                } else {
                    when (accountType) {
                        "student" -> "university_selection"
                        "landlord" -> "landlord_profile"
                        else -> "login"
                    }
                }

                _uiState.value = _uiState.value.copy(navigationTarget = target)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Registration failed. Email might already be in use."
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(navigationTarget = null)
    }
}
