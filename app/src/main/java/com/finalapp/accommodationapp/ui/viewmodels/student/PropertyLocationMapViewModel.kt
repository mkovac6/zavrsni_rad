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
 * ViewModel for Property Location Map Screen
 * Shows single property location with distance from student's university
 */
class PropertyLocationMapViewModel(
    private val propertyRepository: PropertyRepository,
    private val universityRepository: UniversityRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val property: Property? = null,
        val university: University? = null,
        val isLoading: Boolean = true,
        val distance: Float? = null,
        val cameraLat: Double = 45.8150,
        val cameraLng: Double = 15.9819,
        val cameraZoom: Float = 13f
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadPropertyLocation(propertyId: Int, userId: Int?, isStudent: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load property
            val loadedProperty = propertyRepository.getPropertyById(propertyId)

            var loadedUniversity: University? = null
            var calculatedDistance: Float? = null
            var cameraLat = 45.8150
            var cameraLng = 15.9819
            var cameraZoom = 13f

            if (loadedProperty != null) {
                // Load student's university if student
                if (isStudent && userId != null) {
                    val studentProfile = studentRepository.getStudentProfile(userId)
                    if (studentProfile != null) {
                        loadedUniversity = universityRepository.getUniversityById(studentProfile.universityId)

                        // Calculate distance and camera position
                        loadedUniversity?.let { uni ->
                            if (uni.latitude != 0.0 && uni.longitude != 0.0) {
                                calculatedDistance = calculateDistance(
                                    uni.latitude,
                                    uni.longitude,
                                    loadedProperty.latitude,
                                    loadedProperty.longitude
                                )

                                // Calculate center point between property and university
                                cameraLat = (loadedProperty.latitude + uni.latitude) / 2
                                cameraLng = (loadedProperty.longitude + uni.longitude) / 2

                                // Calculate appropriate zoom level based on distance
                                cameraZoom = when {
                                    calculatedDistance!! < 2f -> 14f
                                    calculatedDistance!! < 5f -> 12f
                                    calculatedDistance!! < 10f -> 11f
                                    else -> 10f
                                }
                            } else {
                                // Just show property if no university coordinates
                                cameraLat = loadedProperty.latitude
                                cameraLng = loadedProperty.longitude
                                cameraZoom = 13f
                            }
                        } ?: run {
                            // Just show property if no university
                            cameraLat = loadedProperty.latitude
                            cameraLng = loadedProperty.longitude
                            cameraZoom = 13f
                        }
                    }
                } else {
                    // Not a student, just show property
                    cameraLat = loadedProperty.latitude
                    cameraLng = loadedProperty.longitude
                    cameraZoom = 13f
                }
            }

            _uiState.value = _uiState.value.copy(
                property = loadedProperty,
                university = loadedUniversity,
                distance = calculatedDistance,
                cameraLat = cameraLat,
                cameraLng = cameraLng,
                cameraZoom = cameraZoom,
                isLoading = false
            )
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371.0

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return (R * c).toFloat()
    }
}
