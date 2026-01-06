package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for University Selection Screen
 * Manages loading and selection of universities for student registration
 */
class UniversitySelectionViewModel(
    private val universityRepository: UniversityRepository
) : ViewModel() {

    data class UiState(
        val universities: List<University> = emptyList(),
        val selectedUniversity: University? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadUniversities()
    }

    private fun loadUniversities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val universities = universityRepository.getAllUniversities()
                if (universities.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No universities found",
                        snackbarMessage = "No universities available. Please contact support."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        universities = universities,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load universities",
                    snackbarMessage = "Error loading universities. Please try again."
                )
            }
        }
    }

    fun selectUniversity(university: University) {
        _uiState.value = _uiState.value.copy(selectedUniversity = university)
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
