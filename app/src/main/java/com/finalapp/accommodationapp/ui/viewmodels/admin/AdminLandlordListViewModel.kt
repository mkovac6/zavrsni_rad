package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin Landlord List Screen
 * Manages landlord list display and deletion
 */
class AdminLandlordListViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class UiState(
        val landlords: List<LandlordWithUser> = emptyList(),
        val isLoading: Boolean = true,
        val showDeleteDialog: Boolean = false,
        val landlordToDelete: LandlordWithUser? = null,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadLandlords() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val landlords = adminRepository.getAllLandlords()
                _uiState.value = _uiState.value.copy(
                    landlords = landlords,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AdminLandlordList", "Error loading landlords", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading landlords: ${e.message}"
                )
            }
        }
    }

    fun showDeleteDialog(landlord: LandlordWithUser) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            landlordToDelete = landlord
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            landlordToDelete = null
        )
    }

    fun deleteLandlord() {
        viewModelScope.launch {
            val landlord = _uiState.value.landlordToDelete ?: return@launch

            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                landlordToDelete = null
            )

            try {
                val success = adminRepository.deleteLandlord(landlord.userId)
                if (success) {
                    // Reload landlords list
                    val updatedLandlords = adminRepository.getAllLandlords()
                    _uiState.value = _uiState.value.copy(
                        landlords = updatedLandlords,
                        snackbarMessage = "Landlord deleted successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to delete landlord"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminLandlordList", "Error deleting landlord", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error deleting landlord: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
