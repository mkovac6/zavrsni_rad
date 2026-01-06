package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for Admin Add Property Screen
 * Manages property creation form with landlord selection and amenities
 */
class AdminAddPropertyViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class FormState(
        val selectedLandlordId: Int? = null,
        val title: String = "",
        val description: String = "",
        val propertyType: String = "apartment",
        val address: String = "",
        val city: String = "",
        val postalCode: String = "",
        val pricePerMonth: String = "",
        val bedrooms: String = "1",
        val bathrooms: String = "1",
        val totalCapacity: String = "1",
        val availableFrom: String = "",
        val selectedAmenities: Set<Int> = emptySet()
    )

    data class UiState(
        val formState: FormState = FormState(),
        val landlords: List<LandlordWithUser> = emptyList(),
        val amenities: List<Amenity> = emptyList(),
        val showLandlordDropdown: Boolean = false,
        val showDatePicker: Boolean = false,
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val landlords = adminRepository.getAllLandlords()
                val amenities = adminRepository.getAllAmenities()

                _uiState.value = _uiState.value.copy(
                    landlords = landlords,
                    amenities = amenities,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AdminAddProperty", "Error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    fun selectLandlord(landlordId: Int) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedLandlordId = landlordId),
            showLandlordDropdown = false
        )
    }

    fun toggleLandlordDropdown() {
        _uiState.value = _uiState.value.copy(
            showLandlordDropdown = !_uiState.value.showLandlordDropdown
        )
    }

    fun toggleDatePicker() {
        _uiState.value = _uiState.value.copy(
            showDatePicker = !_uiState.value.showDatePicker
        )
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(title = title)
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(description = description)
        )
    }

    fun updatePropertyType(propertyType: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(propertyType = propertyType)
        )
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(address = address)
        )
    }

    fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(city = city)
        )
    }

    fun updatePostalCode(postalCode: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(postalCode = postalCode)
        )
    }

    fun updatePricePerMonth(price: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(pricePerMonth = price)
        )
    }

    fun updateBedrooms(bedrooms: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(bedrooms = bedrooms)
        )
    }

    fun updateBathrooms(bathrooms: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(bathrooms = bathrooms)
        )
    }

    fun updateTotalCapacity(capacity: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(totalCapacity = capacity)
        )
    }

    fun updateAvailableFrom(date: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(availableFrom = date)
        )
    }

    fun toggleAmenity(amenityId: Int) {
        val currentAmenities = _uiState.value.formState.selectedAmenities
        val newAmenities = if (currentAmenities.contains(amenityId)) {
            currentAmenities - amenityId
        } else {
            currentAmenities + amenityId
        }
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedAmenities = newAmenities)
        )
    }

    val isFormValid: Boolean
        get() {
            val formState = _uiState.value.formState
            return formState.selectedLandlordId != null &&
                    formState.title.isNotBlank() &&
                    formState.address.isNotBlank() &&
                    formState.city.isNotBlank() &&
                    formState.pricePerMonth.toDoubleOrNull() != null &&
                    formState.bedrooms.toIntOrNull() != null &&
                    formState.bathrooms.toIntOrNull() != null &&
                    formState.totalCapacity.toIntOrNull() != null
        }

    fun createProperty(onSuccess: () -> Unit) {
        if (!isFormValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val formState = _uiState.value.formState

            try {
                val propertyId = adminRepository.createProperty(
                    landlordId = formState.selectedLandlordId!!,
                    title = formState.title.trim(),
                    description = formState.description.trim().ifEmpty { "" },
                    propertyType = formState.propertyType,
                    address = formState.address.trim(),
                    city = formState.city.trim(),
                    postalCode = formState.postalCode.trim().ifEmpty { "" },
                    pricePerMonth = formState.pricePerMonth.toDouble(),
                    bedrooms = formState.bedrooms.toInt(),
                    bathrooms = formState.bathrooms.toInt(),
                    totalCapacity = formState.totalCapacity.toInt(),
                    availableFrom = formState.availableFrom.trim().ifEmpty { "" }
                )

                if (propertyId > 0) {
                    // Add selected amenities
                    if (formState.selectedAmenities.isNotEmpty()) {
                        adminRepository.addPropertyAmenities(propertyId, formState.selectedAmenities.toList())
                    }

                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Property created successfully!"
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Failed to create property"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminAddProperty", "Error creating property", e)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    snackbarMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
