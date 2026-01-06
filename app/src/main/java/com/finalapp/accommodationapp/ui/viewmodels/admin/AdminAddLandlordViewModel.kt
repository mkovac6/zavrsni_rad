package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.repository.UserRepository
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin Add Landlord Screen
 * Manages landlord creation form with validation
 */
class AdminAddLandlordViewModel(
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class FormState(
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val companyName: String = "",
        val phone: String = "",
        val isVerified: Boolean = false
    )

    data class UiState(
        val formState: FormState = FormState(),
        val passwordVisible: Boolean = false,
        val confirmPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(email = email)
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(password = password)
        )
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(confirmPassword = confirmPassword)
        )
    }

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(firstName = firstName)
        )
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(lastName = lastName)
        )
    }

    fun updateCompanyName(companyName: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(companyName = companyName)
        )
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(phone = phone)
        )
    }

    fun toggleVerified() {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(isVerified = !_uiState.value.formState.isVerified)
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            confirmPasswordVisible = !_uiState.value.confirmPasswordVisible
        )
    }

    val isEmailValid: Boolean
        get() = _uiState.value.formState.email.contains("@") &&
                _uiState.value.formState.email.contains(".")

    val isPasswordValid: Boolean
        get() = _uiState.value.formState.password.length >= 6

    val doPasswordsMatch: Boolean
        get() = _uiState.value.formState.password == _uiState.value.formState.confirmPassword &&
                _uiState.value.formState.password.isNotEmpty()

    val isFormValid: Boolean
        get() = isEmailValid &&
                isPasswordValid &&
                doPasswordsMatch &&
                _uiState.value.formState.firstName.isNotBlank() &&
                _uiState.value.formState.lastName.isNotBlank() &&
                _uiState.value.formState.phone.isNotBlank()

    fun createLandlord(onSuccess: () -> Unit) {
        if (!isFormValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val formState = _uiState.value.formState

            try {
                // Check if email already exists
                val emailExists = userRepository.checkEmailExists(formState.email)
                if (emailExists) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "Email already exists"
                    )
                    return@launch
                }

                // Create user account
                val user = userRepository.register(formState.email, formState.password, "landlord")
                if (user != null) {
                    // Create landlord profile
                    val profileCreated = adminRepository.createLandlordProfile(
                        userId = user.userId,
                        firstName = formState.firstName.trim(),
                        lastName = formState.lastName.trim(),
                        companyName = formState.companyName.trim().ifEmpty { null },
                        phone = formState.phone.trim(),
                        isVerified = formState.isVerified
                    )

                    if (profileCreated) {
                        // Update profile completion status
                        userRepository.updateUserProfileStatus(user.userId, true)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "Landlord created successfully!"
                        )
                        onSuccess()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "Failed to create landlord profile"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "Failed to create user account"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminAddLandlord", "Error creating landlord", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
