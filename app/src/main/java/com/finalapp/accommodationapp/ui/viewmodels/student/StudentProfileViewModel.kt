package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.student.StudentProfile
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Profile Screen
 * Manages student profile data display
 */
class StudentProfileViewModel(
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class UiState(
        val profile: StudentProfile? = null,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val profile = studentRepository.getStudentProfile(userId)

            _uiState.value = _uiState.value.copy(
                profile = profile,
                isLoading = false
            )
        }
    }
}
