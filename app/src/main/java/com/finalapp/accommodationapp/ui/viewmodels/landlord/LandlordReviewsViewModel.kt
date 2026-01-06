package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Review
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import com.finalapp.accommodationapp.data.repository.student.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Landlord Reviews Screen
 * Manages reviews list and average ratings calculation
 */
class LandlordReviewsViewModel(
    private val reviewRepository: ReviewRepository,
    private val landlordRepository: LandlordRepository
) : ViewModel() {

    data class UiState(
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val landlordId: Int? = null,
        val averagePropertyRating: Double = 0.0,
        val averageLandlordRating: Double = 0.0,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadReviews(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val landlordInfo = landlordRepository.getLandlordByUserId(userId)
                if (landlordInfo != null) {
                    val loadedReviews = reviewRepository.getReviewsForLandlord(landlordInfo.landlordId)

                    // Calculate average ratings
                    val avgPropertyRating = if (loadedReviews.isNotEmpty()) {
                        loadedReviews.map { it.propertyRating }.average()
                    } else {
                        0.0
                    }

                    val avgLandlordRating = if (loadedReviews.isNotEmpty()) {
                        loadedReviews.map { it.landlordRating }.average()
                    } else {
                        0.0
                    }

                    _uiState.value = _uiState.value.copy(
                        landlordId = landlordInfo.landlordId,
                        reviews = loadedReviews,
                        averagePropertyRating = avgPropertyRating,
                        averageLandlordRating = avgLandlordRating,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading reviews: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
