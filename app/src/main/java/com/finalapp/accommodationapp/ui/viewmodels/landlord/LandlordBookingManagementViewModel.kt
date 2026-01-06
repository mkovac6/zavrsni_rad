package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Booking
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Landlord Booking Management Screen
 * Manages booking list, filtering, and approval/rejection actions
 */
class LandlordBookingManagementViewModel(
    private val bookingRepository: BookingRepository,
    private val landlordRepository: LandlordRepository
) : ViewModel() {

    data class UiState(
        val bookings: List<Booking> = emptyList(),
        val isLoading: Boolean = true,
        val selectedTab: Int = 0,
        val processingBookingId: Int? = null,
        val showSuccessMessage: String = "",
        val pendingCount: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed property for filtered bookings
    val filteredBookings: List<Booking>
        get() = when (_uiState.value.selectedTab) {
            0 -> _uiState.value.bookings.filter { it.status == "pending" } // Pending
            1 -> _uiState.value.bookings.filter { it.status == "approved" } // Approved
            2 -> _uiState.value.bookings.filter { it.status in listOf("rejected", "cancelled", "completed") } // History
            else -> _uiState.value.bookings
        }

    fun loadBookings(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val landlordProfile = landlordRepository.getLandlordByUserId(userId)

                if (landlordProfile != null) {
                    val loadedBookings = bookingRepository.getLandlordBookings(landlordProfile.landlordId)
                    val pending = loadedBookings.count { it.status == "pending" }

                    _uiState.value = _uiState.value.copy(
                        bookings = loadedBookings,
                        pendingCount = pending,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun approveBooking(bookingId: Int, userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingBookingId = bookingId)

            try {
                val success = bookingRepository.updateBookingStatus(bookingId, "approved")
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        showSuccessMessage = "Booking approved successfully"
                    )
                    // Reload bookings
                    loadBookings(userId)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.value = _uiState.value.copy(processingBookingId = null)
            }
        }
    }

    fun rejectBooking(bookingId: Int, userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingBookingId = bookingId)

            try {
                val success = bookingRepository.updateBookingStatus(bookingId, "rejected")
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        showSuccessMessage = "Booking rejected"
                    )
                    // Reload bookings
                    loadBookings(userId)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.value = _uiState.value.copy(processingBookingId = null)
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = "")
    }
}
