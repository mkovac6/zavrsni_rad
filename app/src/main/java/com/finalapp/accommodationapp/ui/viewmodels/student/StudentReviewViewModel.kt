package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Booking
import com.finalapp.accommodationapp.data.repository.student.BookingRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Review Screen
 * Manages review form state and submission for completed bookings
 */
class StudentReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val bookingRepository: BookingRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val booking: Booking? = null,
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val propertyRating: Int = 0,
        val landlordRating: Int = 0,
        val comment: String = "",
        val studentId: Int = 0,
        val snackbarMessage: String? = null,
        val reviewSubmitted: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadBooking(bookingId: Int, userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    val bookings = bookingRepository.getStudentBookings(studentProfile.studentId)
                    val foundBooking = bookings.find { it.bookingId == bookingId }

                    if (foundBooking == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "Booking not found"
                        )
                    } else if (foundBooking.status.lowercase() != "completed") {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            booking = foundBooking,
                            snackbarMessage = "Can only review completed bookings"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            studentId = studentProfile.studentId,
                            booking = foundBooking,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "Unable to load student profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading booking: ${e.message}"
                )
            }
        }
    }

    fun updatePropertyRating(rating: Int) {
        _uiState.value = _uiState.value.copy(propertyRating = rating)
    }

    fun updateLandlordRating(rating: Int) {
        _uiState.value = _uiState.value.copy(landlordRating = rating)
    }

    fun updateComment(newComment: String) {
        _uiState.value = _uiState.value.copy(comment = newComment)
    }

    fun submitReview(bookingId: Int) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.propertyRating == 0 || state.landlordRating == 0) {
                _uiState.value = state.copy(
                    snackbarMessage = "Please provide both property and landlord ratings"
                )
                return@launch
            }

            _uiState.value = state.copy(isSubmitting = true)

            try {
                val landlordId = state.booking?.landlordId

                if (landlordId == null) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Unable to identify landlord for this booking"
                    )
                    return@launch
                }

                val success = reviewRepository.submitReview(
                    bookingId = bookingId,
                    propertyId = state.booking.propertyId,
                    studentId = state.studentId,
                    landlordId = landlordId,
                    propertyRating = state.propertyRating,
                    landlordRating = state.landlordRating,
                    comment = state.comment.ifBlank { null }
                )

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Review submitted successfully!",
                        reviewSubmitted = true
                    )
                    // Delay before triggering navigation
                    delay(1000)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        snackbarMessage = "Failed to submit review. You may have already reviewed this booking."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    snackbarMessage = "Error submitting review: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(reviewSubmitted = false)
    }
}
