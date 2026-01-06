package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Landlord Edit Profile Screen
 * Manages form state and profile update logic
 */
class LandlordEditProfileViewModel(
    private val landlordRepository: LandlordRepository
) : ViewModel() {

    data class FormState(
        val landlordId: Int = 0,
        val firstName: String = "",
        val lastName: String = "",
        val phone: String = "",
        val companyName: String = ""
    )

    data class UiState(
        val formState: FormState = FormState(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val snackbarMessage: String? = null,
        val profileUpdated: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val landlord = landlordRepository.getLandlordByUserId(userId)

            if (landlord != null) {
                _uiState.value = _uiState.value.copy(
                    formState = FormState(
                        landlordId = landlord.landlordId,
                        firstName = landlord.firstName,
                        lastName = landlord.lastName,
                        phone = landlord.phone,
                        companyName = landlord.companyName ?: ""
                    ),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
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

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(phone = value)
        )
    }

    fun updateCompanyName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(companyName = value)
        )
    }

    fun saveProfile() {
        viewModelScope.launch {
            val form = _uiState.value.formState

            // Validate required fields
            when {
                form.firstName.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "First name is required"
                    )
                }
                form.lastName.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Last name is required"
                    )
                }
                form.phone.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Phone number is required"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSaving = true)

                    val success = landlordRepository.updateLandlordProfile(
                        landlordId = form.landlordId,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        companyName = form.companyName.trim().ifEmpty { null },
                        phone = form.phone.trim()
                    )

                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            snackbarMessage = "Profile updated successfully",
                            profileUpdated = true
                        )
                        delay(1000)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            snackbarMessage = "Failed to update profile"
                        )
                    }
                }
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(profileUpdated = false)
    }
}
