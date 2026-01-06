package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin Property List Screen
 * Manages property list display, deletion, and status toggling
 */
class AdminPropertyListViewModel(
    private val propertyRepository: PropertyRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class UiState(
        val properties: List<Property> = emptyList(),
        val isLoading: Boolean = true,
        val showDeleteDialog: Boolean = false,
        val propertyToDelete: Property? = null,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Pass includeInactive = true for admin view
                val properties = propertyRepository.getAllProperties(includeInactive = true)
                _uiState.value = _uiState.value.copy(
                    properties = properties,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AdminPropertyList", "Error loading properties", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading properties: ${e.message}"
                )
            }
        }
    }

    fun showDeleteDialog(property: Property) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            propertyToDelete = property
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            propertyToDelete = null
        )
    }

    fun deleteProperty() {
        viewModelScope.launch {
            val property = _uiState.value.propertyToDelete ?: return@launch

            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                propertyToDelete = null
            )

            try {
                val success = adminRepository.deleteProperty(property.propertyId)
                if (success) {
                    // Reload properties list
                    val updatedProperties = propertyRepository.getAllProperties(includeInactive = true)
                    _uiState.value = _uiState.value.copy(
                        properties = updatedProperties,
                        snackbarMessage = "Property deleted successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to delete property"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminPropertyList", "Error deleting property", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error deleting property: ${e.message}"
                )
            }
        }
    }

    fun togglePropertyStatus(propertyId: Int, currentStatus: Boolean) {
        viewModelScope.launch {
            try {
                val success = propertyRepository.updatePropertyStatus(propertyId, !currentStatus)
                if (success) {
                    // Reload properties list
                    val updatedProperties = propertyRepository.getAllProperties(includeInactive = true)
                    _uiState.value = _uiState.value.copy(
                        properties = updatedProperties,
                        snackbarMessage = if (!currentStatus) "Property activated" else "Property deactivated"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to update property status"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminPropertyList", "Error toggling property status", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating property status: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
