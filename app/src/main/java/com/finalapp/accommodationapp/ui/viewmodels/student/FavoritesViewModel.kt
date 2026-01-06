package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.student.FavoritesRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Favorites Screen
 * Manages favorite properties list and remove/undo operations
 */
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val favoriteProperties: List<Property> = emptyList(),
        val isLoading: Boolean = true,
        val studentId: Int = 0,
        val isRefreshing: Boolean = false,
        val snackbarMessage: String? = null,
        val showUndoAction: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadFavorites(userId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true)

                // Get student ID
                val studentProfile = studentRepository.getStudentProfile(userId)

                if (studentProfile != null) {
                    val favorites = favoritesRepository.getStudentFavorites(studentProfile.studentId)
                    _uiState.value = _uiState.value.copy(
                        studentId = studentProfile.studentId,
                        favoriteProperties = favorites,
                        isLoading = false,
                        isRefreshing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        snackbarMessage = "Unable to load student profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    snackbarMessage = "Error loading favorites: ${e.message}"
                )
            }
        }
    }

    fun removeFromFavorites(propertyId: Int) {
        viewModelScope.launch {
            val removed = favoritesRepository.removeFromFavorites(
                _uiState.value.studentId,
                propertyId
            )

            if (removed) {
                // Remove from local list
                _uiState.value = _uiState.value.copy(
                    favoriteProperties = _uiState.value.favoriteProperties.filter {
                        it.propertyId != propertyId
                    },
                    snackbarMessage = "Removed from favorites",
                    showUndoAction = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to remove from favorites",
                    showUndoAction = false
                )
            }
        }
    }

    fun undoRemoveFavorite(propertyId: Int) {
        viewModelScope.launch {
            val added = favoritesRepository.addToFavorites(
                _uiState.value.studentId,
                propertyId
            )

            if (added) {
                // Reload favorites to get the property back
                val favorites = favoritesRepository.getStudentFavorites(_uiState.value.studentId)
                _uiState.value = _uiState.value.copy(
                    favoriteProperties = favorites
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(
            snackbarMessage = null,
            showUndoAction = false
        )
    }
}
