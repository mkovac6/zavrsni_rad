package com.finalapp.accommodationapp.ui.viewmodels.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.FavoritesRepository
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Property Detail Screen
 * Manages property details, ownership, favorites, and booking logic
 */
class PropertyDetailViewModel(
    private val propertyRepository: PropertyRepository,
    private val landlordRepository: LandlordRepository,
    private val favoritesRepository: FavoritesRepository,
    private val bookingRepository: BookingRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val property: Property? = null,
        val amenities: List<String> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isLandlordOwner: Boolean = false,
        val isFavorite: Boolean = false,
        val studentId: Int = 0,
        val showBookingDialog: Boolean = false,
        val isProcessingBooking: Boolean = false,
        val snackbarMessage: String? = null,
        val bookingSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadPropertyDetails(propertyId: Int, userId: Int?, userType: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Load property details
                val loadedProperty = propertyRepository.getPropertyById(propertyId)
                if (loadedProperty != null) {
                    // Load amenities
                    val loadedAmenities = propertyRepository.getPropertyAmenitiesAsStrings(propertyId)

                    var isOwner = false
                    var favorite = false
                    var stId = 0

                    // Check if current user is the landlord owner
                    if (userType == "landlord" && userId != null) {
                        try {
                            val landlord = landlordRepository.getLandlordByUserId(userId)
                            if (landlord != null) {
                                val propertyLandlordId = propertyRepository.getLandlordIdByPropertyId(propertyId)
                                isOwner = landlord.landlordId == propertyLandlordId
                            }
                        } catch (e: Exception) {
                            Log.e("PropertyDetailVM", "Error checking landlord ownership: ${e.message}")
                        }
                    } else if (userType == "student" && userId != null) {
                        Log.d("PropertyDetailVM", "Current user is student with userId: $userId")

                        // Get student profile
                        try {
                            val student = studentRepository.getStudentProfile(userId)
                            if (student != null) {
                                stId = student.studentId
                                Log.d("PropertyDetailVM", "Found student profile with studentId: $stId")
                                favorite = favoritesRepository.isFavorite(student.studentId, propertyId)
                            } else {
                                Log.e("PropertyDetailVM", "No student profile found for userId: $userId")
                            }
                        } catch (e: Exception) {
                            Log.e("PropertyDetailVM", "Error loading student profile: ${e.message}")
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        property = loadedProperty,
                        amenities = loadedAmenities,
                        isLandlordOwner = isOwner,
                        isFavorite = favorite,
                        studentId = stId,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Property not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("PropertyDetailVM", "Error loading property: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading property: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite(propertyId: Int) {
        viewModelScope.launch {
            val studentId = _uiState.value.studentId
            if (studentId <= 0) return@launch

            try {
                if (_uiState.value.isFavorite) {
                    val removed = favoritesRepository.removeFromFavorites(studentId, propertyId)
                    if (removed) {
                        _uiState.value = _uiState.value.copy(
                            isFavorite = false,
                            snackbarMessage = "Removed from favorites"
                        )
                    }
                } else {
                    val added = favoritesRepository.addToFavorites(studentId, propertyId)
                    if (added) {
                        _uiState.value = _uiState.value.copy(
                            isFavorite = true,
                            snackbarMessage = "Added to favorites"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating favorites"
                )
            }
        }
    }

    fun showBookingDialog() {
        _uiState.value = _uiState.value.copy(showBookingDialog = true)
    }

    fun hideBookingDialog() {
        _uiState.value = _uiState.value.copy(showBookingDialog = false)
    }

    fun createBooking(
        propertyId: Int,
        startDate: String,
        endDate: String,
        totalPrice: Double,
        messageToLandlord: String
    ) {
        viewModelScope.launch {
            val studentId = _uiState.value.studentId
            if (studentId <= 0) return@launch

            _uiState.value = _uiState.value.copy(
                isProcessingBooking = true,
                showBookingDialog = false
            )

            try {
                // Check availability first
                val isAvailable = bookingRepository.checkDateAvailability(propertyId, startDate, endDate)

                if (isAvailable) {
                    // Create booking with the studentId
                    val success = bookingRepository.createBooking(
                        propertyId = propertyId,
                        studentId = studentId,
                        startDate = startDate,
                        endDate = endDate,
                        totalPrice = totalPrice,
                        messageToLandlord = messageToLandlord
                    )

                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isProcessingBooking = false,
                            snackbarMessage = "Booking request sent successfully!",
                            bookingSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isProcessingBooking = false,
                            snackbarMessage = "Failed to create booking. Please try again."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessingBooking = false,
                        snackbarMessage = "These dates are no longer available. Please choose different dates."
                    )
                }
            } catch (e: Exception) {
                Log.e("PropertyDetailVM", "Error creating booking: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isProcessingBooking = false,
                    snackbarMessage = "Error creating booking: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun bookingNavigationHandled() {
        _uiState.value = _uiState.value.copy(bookingSuccess = false)
    }
}
