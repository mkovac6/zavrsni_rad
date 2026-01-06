package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Home Screen
 * Manages property listing and search functionality
 */
class StudentHomeViewModel(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    data class UiState(
        val properties: List<Property> = emptyList(),
        val isLoading: Boolean = true,
        val searchQuery: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Computed property for filtered properties
    val filteredProperties: List<Property>
        get() {
            val state = _uiState.value
            return if (state.searchQuery.isEmpty()) {
                state.properties
            } else {
                state.properties.filter { property ->
                    property.title.contains(state.searchQuery, ignoreCase = true) ||
                    property.city.contains(state.searchQuery, ignoreCase = true) ||
                    property.address.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }

    init {
        loadProperties()
    }

    private fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val properties = propertyRepository.getAllProperties()

            _uiState.value = _uiState.value.copy(
                properties = properties,
                isLoading = false
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
