package com.finalapp.accommodationapp.ui.viewmodels.landlord

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.repository.PropertyImageRepository
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Landlord Add Property Screen
 * Manages complex form state with 15+ fields, image uploads, and date validation
 */
class LandlordAddPropertyViewModel(
    private val adminRepository: AdminRepository,
    private val landlordRepository: LandlordRepository
) : ViewModel() {

    data class FormState(
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
        val selectedAmenities: Set<Int> = emptySet(),
        val selectedImageUris: List<Uri> = emptyList()
    )

    data class UiState(
        val formState: FormState = FormState(),
        val landlordId: Int? = null,
        val landlordName: String = "",
        val amenities: List<Amenity> = emptyList(),
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val isUploadingImages: Boolean = false,
        val showDatePicker: Boolean = false,
        val showPastDateWarning: Boolean = false,
        val snackbarMessage: String? = null,
        val propertyCreated: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed property for form validation
    val isFormValid: Boolean
        get() {
            val state = _uiState.value
            val form = state.formState
            return state.landlordId != null &&
                    form.title.isNotBlank() &&
                    form.address.isNotBlank() &&
                    form.city.isNotBlank() &&
                    form.pricePerMonth.toDoubleOrNull() != null &&
                    form.bedrooms.toIntOrNull() != null &&
                    form.bathrooms.toIntOrNull() != null &&
                    form.totalCapacity.toIntOrNull() != null
        }

    fun loadLandlordData(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                Log.d("LandlordAddProperty", "Attempting to fetch landlord for userId: $userId")

                val landlord = landlordRepository.getLandlordByUserId(userId)

                if (landlord != null) {
                    val landlordFullName = "${landlord.firstName} ${landlord.lastName}"
                    Log.d("LandlordAddProperty", "Successfully loaded landlord: $landlordFullName (ID: ${landlord.landlordId})")

                    _uiState.value = _uiState.value.copy(
                        landlordId = landlord.landlordId,
                        landlordName = landlordFullName
                    )
                } else {
                    Log.e("LandlordAddProperty", "getLandlordByUserId returned null for userId: $userId")
                }

                // Load amenities
                val loadedAmenities = adminRepository.getAllAmenities()
                _uiState.value = _uiState.value.copy(
                    amenities = loadedAmenities,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("LandlordAddProperty", "Error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    // Form field update methods
    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(title = value)
        )
    }

    fun updateDescription(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(description = value)
        )
    }

    fun updatePropertyType(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(propertyType = value)
        )
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(address = value)
        )
    }

    fun updateCity(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(city = value)
        )
    }

    fun updatePostalCode(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(postalCode = value)
        )
    }

    fun updatePricePerMonth(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(pricePerMonth = value)
        )
    }

    fun updateBedrooms(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(bedrooms = value)
        )
    }

    fun updateBathrooms(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(bathrooms = value)
        )
    }

    fun updateTotalCapacity(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(totalCapacity = value)
        )
    }

    fun updateAvailableFrom(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(availableFrom = value)
        )
    }

    fun updateSelectedAmenities(amenities: Set<Int>) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedAmenities = amenities)
        )
    }

    fun updateSelectedImageUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedImageUris = uris)
        )
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun dismissPastDateWarning() {
        _uiState.value = _uiState.value.copy(showPastDateWarning = false)
    }

    fun submitProperty(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.formState

            // Check if available date is in the past
            val today = Calendar.getInstance()
            val todayDate = Date(today.timeInMillis)

            val selectedDate = if (form.availableFrom.isNotEmpty()) {
                try {
                    val parts = form.availableFrom.split("-")
                    if (parts.size == 3) {
                        val cal = Calendar.getInstance()
                        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.time
                    } else null
                } catch (e: Exception) {
                    null
                }
            } else null

            if (selectedDate != null && selectedDate.before(todayDate)) {
                // Show warning dialog for past date
                _uiState.value = state.copy(showPastDateWarning = true)
            } else {
                // Create property normally
                createProperty(context, onSuccess, isActive = true)
            }
        }
    }

    fun submitPropertyInactive(context: Context, onSuccess: () -> Unit) {
        createProperty(context, onSuccess, isActive = false)
    }

    private fun createProperty(context: Context, onSuccess: () -> Unit, isActive: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.formState

            _uiState.value = state.copy(isSubmitting = true, showPastDateWarning = false)

            try {
                Log.d("LandlordAddProperty", "Creating property for landlord: ${state.landlordId}")

                val propertyId = adminRepository.createProperty(
                    landlordId = state.landlordId!!,
                    title = form.title.trim(),
                    description = form.description.trim(),
                    propertyType = form.propertyType,
                    address = form.address.trim(),
                    city = form.city.trim(),
                    postalCode = form.postalCode.trim(),
                    pricePerMonth = form.pricePerMonth.toDouble(),
                    bedrooms = form.bedrooms.toInt(),
                    bathrooms = form.bathrooms.toInt(),
                    totalCapacity = form.totalCapacity.toInt(),
                    availableFrom = form.availableFrom.ifEmpty {
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        formatter.format(Date())
                    }
                )

                if (propertyId > 0) {
                    // Add amenities
                    if (form.selectedAmenities.isNotEmpty()) {
                        adminRepository.addPropertyAmenities(propertyId, form.selectedAmenities.toList())
                    }

                    // Upload images
                    if (form.selectedImageUris.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(isUploadingImages = true)
                        val imageRepository = PropertyImageRepository(context)
                        imageRepository.uploadMultipleImages(propertyId, form.selectedImageUris)
                        _uiState.value = _uiState.value.copy(isUploadingImages = false)
                    }

                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Property created successfully!",
                        propertyCreated = true
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Failed to create property"
                    )
                }
            } catch (e: Exception) {
                Log.e("LandlordAddProperty", "Error creating property", e)
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
