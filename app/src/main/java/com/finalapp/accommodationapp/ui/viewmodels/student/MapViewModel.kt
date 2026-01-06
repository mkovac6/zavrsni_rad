package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.Property
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.PropertyRepository
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Map Screen
 * Manages property map display with university-based filtering
 */
class MapViewModel(
    private val propertyRepository: PropertyRepository,
    private val universityRepository: UniversityRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val properties: List<Property> = emptyList(),
        val allProperties: List<Property> = emptyList(),
        val university: University? = null,
        val isLoading: Boolean = true,
        val selectedProperty: Property? = null,
        val selectedDistance: Float? = null,
        val showUniversityMarker: Boolean = true,
        val showFilters: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadMapData(userId: Int, isStudent: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            var loadedUniversity: University? = null

            if (isStudent) {
                val studentProfile = studentRepository.getStudentProfile(userId)
                if (studentProfile != null) {
                    loadedUniversity = universityRepository.getUniversityById(studentProfile.universityId)
                }
            }

            val allProps = propertyRepository.getAllProperties()

            _uiState.value = _uiState.value.copy(
                university = loadedUniversity,
                allProperties = allProps,
                properties = allProps,
                isLoading = false
            )
        }
    }

    fun selectProperty(property: Property?) {
        _uiState.value = _uiState.value.copy(selectedProperty = property)
    }

    fun selectDistance(distance: Float?) {
        _uiState.value = _uiState.value.copy(selectedDistance = distance)
        applyDistanceFilter()
    }

    fun toggleUniversityMarker() {
        _uiState.value = _uiState.value.copy(
            showUniversityMarker = !_uiState.value.showUniversityMarker
        )
    }

    fun toggleFilters() {
        _uiState.value = _uiState.value.copy(
            showFilters = !_uiState.value.showFilters
        )
    }

    private fun applyDistanceFilter() {
        val state = _uiState.value
        val uni = state.university
        val distFilter = state.selectedDistance

        val filtered = if (distFilter == null || uni == null) {
            state.allProperties
        } else {
            state.allProperties.filter { property ->
                val distance = calculateDistance(
                    uni.latitude,
                    uni.longitude,
                    property.latitude,
                    property.longitude
                )
                distance <= distFilter
            }
        }

        _uiState.value = state.copy(properties = filtered)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}
