package com.finalapp.accommodationapp.ui.viewmodels.landlord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.landlord.Landlord
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Landlord Profile Screen
 * Manages landlord profile data display
 */
class LandlordProfileViewModel(
    private val landlordRepository: LandlordRepository
) : ViewModel() {

    data class UiState(
        val landlord: Landlord? = null,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val landlord = landlordRepository.getLandlordByUserId(userId)

            _uiState.value = _uiState.value.copy(
                landlord = landlord,
                isLoading = false
            )
        }
    }
}
