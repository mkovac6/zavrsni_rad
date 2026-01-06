package com.finalapp.accommodationapp.ui.viewmodels.landlord

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.model.PropertyImage
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.data.repository.PropertyImageRepository
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Landlord Edit Property Screen
 * Manages complex form state with 15+ fields, image management, and property deletion
 */
class LandlordEditPropertyViewModel(
    private val propertyRepository: PropertyRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class FormState(
        val title: String = "",
        val description: String = "",
        val propertyType: String = "apartment",
        val address: String = "",
        val city: String = "",
        val postalCode: String = "",
        val pricePerMonth: String = "",
        val bedrooms: String = "",
        val bathrooms: String = "",
        val totalCapacity: String = "",
        val availableFrom: String = "",
        val availableTo: String = "",
        val selectedAmenities: Set<Int> = emptySet(),
        val newImageUris: List<Uri> = emptyList()
    )

    data class UiState(
        val formState: FormState = FormState(),
        val property: Property? = null,
        val amenities: List<Amenity> = emptyList(),
        val existingImages: List<PropertyImage> = emptyList(),
        val isLoading: Boolean = true,
        val isUpdating: Boolean = false,
        val isUploadingImages: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val showDatePicker: Boolean = false,
        val datePickerTarget: String = "from",
        val errorMessage: String = "",
        val successMessage: String = "",
        val propertyUpdated: Boolean = false,
        val propertyDeleted: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed property for form validation
    val isFormValid: Boolean
        get() {
            val form = _uiState.value.formState
            return form.title.isNotBlank() &&
                    form.address.isNotBlank() &&
                    form.city.isNotBlank() &&
                    form.pricePerMonth.toDoubleOrNull() != null &&
                    form.bedrooms.toIntOrNull() != null &&
                    form.bathrooms.toIntOrNull() != null &&
                    form.totalCapacity.toIntOrNull() != null &&
                    form.availableFrom.isNotBlank()
        }

    fun loadProperty(propertyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                Log.d("LandlordEditProperty", "Loading property with ID: $propertyId")

                // Load property
                val loadedProperty = propertyRepository.getPropertyById(propertyId)
                Log.d("LandlordEditProperty", "Loaded property: $loadedProperty")

                if (loadedProperty != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    _uiState.value = _uiState.value.copy(
                        property = loadedProperty,
                        formState = _uiState.value.formState.copy(
                            title = loadedProperty.title,
                            description = loadedProperty.description,
                            propertyType = loadedProperty.propertyType,
                            address = loadedProperty.address,
                            city = loadedProperty.city,
                            postalCode = loadedProperty.postalCode,
                            pricePerMonth = loadedProperty.pricePerMonth.toString(),
                            bedrooms = loadedProperty.bedrooms.toString(),
                            bathrooms = loadedProperty.bathrooms.toString(),
                            totalCapacity = loadedProperty.totalCapacity.toString(),
                            availableFrom = dateFormat.format(loadedProperty.availableFrom),
                            availableTo = loadedProperty.availableTo?.let { dateFormat.format(it) } ?: ""
                        )
                    )
                } else {
                    Log.e("LandlordEditProperty", "Property not found for ID: $propertyId")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Property not found"
                    )
                }

                // Load amenities
                try {
                    val loadedAmenities = adminRepository.getAllAmenities()
                    Log.d("LandlordEditProperty", "Loaded ${loadedAmenities.size} amenities")
                    _uiState.value = _uiState.value.copy(amenities = loadedAmenities)
                } catch (e: Exception) {
                    Log.e("LandlordEditProperty", "Error loading amenities", e)
                }

                // Load selected amenities
                try {
                    val propertyAmenityIds = propertyRepository.getPropertyAmenities(propertyId)
                    Log.d("LandlordEditProperty", "Loaded ${propertyAmenityIds.size} selected amenities")
                    _uiState.value = _uiState.value.copy(
                        formState = _uiState.value.formState.copy(
                            selectedAmenities = propertyAmenityIds.toSet()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("LandlordEditProperty", "Error loading property amenities", e)
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error in loadProperty", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading property: ${e.message}"
                )
            }
        }
    }

    fun loadPropertyImages(context: Context, propertyId: Int) {
        viewModelScope.launch {
            try {
                val imageRepository = PropertyImageRepository(context)
                val images = imageRepository.getPropertyImages(propertyId)
                Log.d("LandlordEditProperty", "Loaded ${images.size} images")
                _uiState.value = _uiState.value.copy(existingImages = images)
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error loading images", e)
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

    fun updateAvailableTo(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(availableTo = value)
        )
    }

    fun updateSelectedAmenities(amenities: Set<Int>) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedAmenities = amenities)
        )
    }

    fun updateNewImageUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(newImageUris = uris)
        )
    }

    fun deleteExistingImage(context: Context, imageId: Int) {
        viewModelScope.launch {
            try {
                val imageRepository = PropertyImageRepository(context)
                // Find the image to get its URL
                val image = _uiState.value.existingImages.find { it.imageId == imageId }
                val success = if (image != null) {
                    imageRepository.deletePropertyImage(imageId, image.imageUrl)
                } else {
                    false
                }
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        existingImages = _uiState.value.existingImages.filter { it.imageId != imageId },
                        successMessage = "Image deleted"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete image"
                    )
                }
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error deleting image", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error deleting image"
                )
            }
        }
    }

    fun setPrimaryImage(context: Context, propertyId: Int, imageId: Int) {
        viewModelScope.launch {
            try {
                val imageRepository = PropertyImageRepository(context)
                val success = imageRepository.setPrimaryImage(propertyId, imageId)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        existingImages = _uiState.value.existingImages.map {
                            it.copy(isPrimary = it.imageId == imageId)
                        },
                        successMessage = "Primary image updated"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to set primary"
                    )
                }
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error setting primary image", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error setting primary image"
                )
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun showDatePicker(target: String) {
        _uiState.value = _uiState.value.copy(
            showDatePicker = true,
            datePickerTarget = target
        )
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "",
            successMessage = ""
        )
    }

    fun updateProperty(context: Context, propertyId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val form = state.formState

            _uiState.value = state.copy(
                isUpdating = true,
                errorMessage = "",
                successMessage = ""
            )

            // Validation
            when {
                form.title.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Title is required"
                    )
                    return@launch
                }
                form.address.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Address is required"
                    )
                    return@launch
                }
                form.city.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "City is required"
                    )
                    return@launch
                }
                form.bedrooms.toIntOrNull() == null -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Valid number of bedrooms required"
                    )
                    return@launch
                }
                form.bathrooms.toIntOrNull() == null -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Valid number of bathrooms required"
                    )
                    return@launch
                }
                form.totalCapacity.toIntOrNull() == null -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Valid capacity required"
                    )
                    return@launch
                }
                form.pricePerMonth.toDoubleOrNull() == null -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Valid price required"
                    )
                    return@launch
                }
                form.availableFrom.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Available from date is required"
                    )
                    return@launch
                }
            }

            try {
                val success = propertyRepository.updateProperty(
                    propertyId = propertyId,
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
                    availableFrom = form.availableFrom,
                    availableTo = form.availableTo.ifEmpty { null }
                )

                if (success) {
                    // Update amenities
                    propertyRepository.updatePropertyAmenities(
                        propertyId,
                        form.selectedAmenities.toList()
                    )

                    // Upload new images if any
                    if (form.newImageUris.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(isUploadingImages = true)
                        val imageRepository = PropertyImageRepository(context)
                        val startOrder = _uiState.value.existingImages.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0
                        val uploadedUrls = imageRepository.uploadMultipleImages(
                            propertyId,
                            form.newImageUris,
                            startOrder
                        )
                        _uiState.value = _uiState.value.copy(isUploadingImages = false)

                        if (uploadedUrls.isNotEmpty()) {
                            Log.d("LandlordEditProperty", "Uploaded ${uploadedUrls.size} new images")
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        successMessage = "Property updated successfully!",
                        propertyUpdated = true
                    )
                    delay(1000)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Failed to update property"
                    )
                }
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error updating property", e)
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun deleteProperty(propertyId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                isUpdating = true
            )

            try {
                val success = propertyRepository.deleteProperty(propertyId)

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        propertyDeleted = true
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        errorMessage = "Failed to delete property"
                    )
                }
            } catch (e: Exception) {
                Log.e("LandlordEditProperty", "Error deleting property", e)
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(
            propertyUpdated = false,
            propertyDeleted = false
        )
    }
}
