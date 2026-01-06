package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Booking
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Bookings Screen
 * Manages booking list, filtering, and review status
 */
class StudentBookingsViewModel(
    private val bookingRepository: BookingRepository,
    private val studentRepository: StudentRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    data class UiState(
        val bookings: List<Booking> = emptyList(),
        val isLoading: Boolean = true,
        val selectedTab: Int = 0,
        val studentId: Int = 0,
        val reviewedBookingIds: Set<Int> = emptySet(),
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed property for filtered bookings
    val filteredBookings: List<Booking>
        get() = when (_uiState.value.selectedTab) {
            0 -> _uiState.value.bookings // All
            1 -> _uiState.value.bookings.filter { it.status.lowercase() == "pending" }
            2 -> _uiState.value.bookings.filter { it.status.lowercase() == "approved" }
            3 -> _uiState.value.bookings.filter { it.status.lowercase() in listOf("rejected", "cancelled") }
            4 -> _uiState.value.bookings.filter { it.status.lowercase() == "completed" }
            else -> _uiState.value.bookings
        }

    fun loadBookings(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Get student ID
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    val loadedBookings = bookingRepository.getStudentBookings(studentProfile.studentId)

                    // Check which bookings have been reviewed
                    val completedBookingIds = loadedBookings
                        .filter { it.status.lowercase() == "completed" }
                        .map { it.bookingId }

                    val reviewed = if (completedBookingIds.isNotEmpty()) {
                        completedBookingIds.filter { bookingId ->
                            reviewRepository.isBookingReviewed(bookingId)
                        }.toSet()
                    } else {
                        emptySet()
                    }

                    _uiState.value = _uiState.value.copy(
                        studentId = studentProfile.studentId,
                        bookings = loadedBookings,
                        reviewedBookingIds = reviewed,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "Unable to load student profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading bookings: ${e.message}"
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun cancelBooking(bookingId: Int, userId: Int) {
        viewModelScope.launch {
            val success = bookingRepository.updateBookingStatus(bookingId, "cancelled")

            if (success) {
                // Reload bookings
                loadBookings(userId)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Booking cancelled successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to cancel booking"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
