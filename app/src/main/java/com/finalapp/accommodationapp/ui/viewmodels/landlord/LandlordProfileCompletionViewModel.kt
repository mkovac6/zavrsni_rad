package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Landlord Profile Completion Screen
 * Handles initial profile creation for new landlords
 */
class LandlordProfileCompletionViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class FormState(
        val firstName: String = "",
        val lastName: String = "",
        val companyName: String = "",
        val phone: String = ""
    )

    data class UiState(
        val formState: FormState = FormState(),
        val isLoading: Boolean = false,
        val showSuccessMessage: Boolean = false,
        val profileComplete: Boolean = false,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isFormValid: Boolean
        get() = with(_uiState.value.formState) {
            firstName.isNotBlank() && lastName.isNotBlank() && phone.isNotBlank()
        }

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(firstName = value)
        )
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(lastName = value)
        )
    }

    fun updateCompanyName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(companyName = value)
        )
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(phone = value)
        )
    }

    fun completeProfile(userId: Int) {
        viewModelScope.launch {
            val form = _uiState.value.formState

            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = adminRepository.createLandlordProfile(
                userId = userId,
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                companyName = form.companyName.trim().ifEmpty { null },
                phone = form.phone.trim(),
                isVerified = false,
                rating = 0.0
            )

            if (success) {
                // Update profile complete status
                adminRepository.updateUserProfileStatus(userId, true)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showSuccessMessage = true,
                    snackbarMessage = "Profile completed successfully!",
                    profileComplete = true
                )
                delay(1000)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Failed to complete profile. Please try again."
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(profileComplete = false)
    }
}
