package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for Landlord Home Screen
 * Manages properties list, booking counts, and property status toggling
 */
class LandlordHomeViewModel(
    private val propertyRepository: PropertyRepository,
    private val landlordRepository: LandlordRepository,
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    data class UiState(
        val properties: List<Property> = emptyList(),
        val isLoading: Boolean = true,
        val landlordName: String = "",
        val landlordId: Int? = null,
        val pendingBookingsCount: Int = 0,
        val newReviewsCount: Int = 0,
        val selectedTab: Int = 0,
        val showWarningDialog: Boolean = false,
        val propertyToToggle: Property? = null,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadLandlordData(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val landlordInfo = landlordRepository.getLandlordByUserId(userId)
                if (landlordInfo != null) {
                    val landlordFullName = "${landlordInfo.firstName} ${landlordInfo.lastName}"
                    val loadedProperties = propertyRepository.getPropertiesByLandlordId(landlordInfo.landlordId)
                    val pendingCount = bookingRepository.countPendingBookingsForLandlord(landlordInfo.landlordId)
                    val reviewsCount = reviewRepository.getNewReviewsCount(landlordInfo.landlordId)

                    _uiState.value = _uiState.value.copy(
                        landlordName = landlordFullName,
                        landlordId = landlordInfo.landlordId,
                        properties = loadedProperties,
                        pendingBookingsCount = pendingCount,
                        newReviewsCount = reviewsCount,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading data: ${e.message}"
                )
            }
        }
    }

    fun loadProperties() {
        viewModelScope.launch {
            val landlordId = _uiState.value.landlordId ?: return@launch

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val loadedProperties = propertyRepository.getPropertiesByLandlordId(landlordId)
                val pendingCount = bookingRepository.countPendingBookingsForLandlord(landlordId)
                val reviewsCount = reviewRepository.getNewReviewsCount(landlordId)

                _uiState.value = _uiState.value.copy(
                    properties = loadedProperties,
                    pendingBookingsCount = pendingCount,
                    newReviewsCount = reviewsCount,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading properties: ${e.message}"
                )
            }
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val landlordId = _uiState.value.landlordId ?: return@launch

            try {
                val pendingCount = bookingRepository.countPendingBookingsForLandlord(landlordId)
                val reviewsCount = reviewRepository.getNewReviewsCount(landlordId)

                _uiState.value = _uiState.value.copy(
                    pendingBookingsCount = pendingCount,
                    newReviewsCount = reviewsCount
                )
            } catch (e: Exception) {
                // Silently fail count refresh
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        if (tabIndex == 0) {
            refreshCounts()
        }
    }

    fun togglePropertyStatus(property: Property) {
        // Check if trying to deactivate a property before its availability date
        val today = Date()
        if (property.isActive && property.availableFrom != null && property.availableFrom.after(today)) {
            // Show warning when DEACTIVATING before availability date
            _uiState.value = _uiState.value.copy(
                showWarningDialog = true,
                propertyToToggle = property
            )
        } else {
            // Toggle directly (activating or deactivating after availability date)
            performToggle(property.propertyId, !property.isActive)
        }
    }

    fun confirmToggle() {
        viewModelScope.launch {
            val property = _uiState.value.propertyToToggle
            if (property != null) {
                performToggle(property.propertyId, false)
            }
            dismissWarningDialog()
        }
    }

    fun dismissWarningDialog() {
        _uiState.value = _uiState.value.copy(
            showWarningDialog = false,
            propertyToToggle = null
        )
    }

    private fun performToggle(propertyId: Int, newStatus: Boolean) {
        viewModelScope.launch {
            try {
                val success = propertyRepository.updatePropertyStatus(propertyId, newStatus)
                if (success) {
                    loadProperties()
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = if (newStatus) "Property activated" else "Property deactivated"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to update property status"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
