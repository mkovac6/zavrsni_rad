package com.finalapp.accommodationapp.ui.viewmodels.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Student Edit Profile Screen
 * Manages form state and profile update logic
 */
class StudentEditProfileViewModel(
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class FormState(
        val studentId: Int = 0,
        val firstName: String = "",
        val lastName: String = "",
        val phone: String = "",
        val studentNumber: String = "",
        val yearOfStudy: String = "",
        val program: String = "",
        val budgetMin: String = "",
        val budgetMax: String = ""
    )

    data class UiState(
        val formState: FormState = FormState(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val snackbarMessage: String? = null,
        val profileUpdated: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val profile = studentRepository.getStudentProfile(userId)

            if (profile != null) {
                _uiState.value = _uiState.value.copy(
                    formState = FormState(
                        studentId = profile.studentId,
                        firstName = profile.firstName,
                        lastName = profile.lastName,
                        phone = profile.phone,
                        studentNumber = profile.studentNumber ?: "",
                        yearOfStudy = profile.yearOfStudy?.toString() ?: "",
                        program = profile.program ?: "",
                        budgetMin = profile.budgetMin?.toString() ?: "",
                        budgetMax = profile.budgetMax?.toString() ?: ""
                    ),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateFirstName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(firstName = value)
        )
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(lastName = value)
        )
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(phone = value)
        )
    }

    fun updateStudentNumber(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(studentNumber = value)
        )
    }

    fun updateYearOfStudy(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(yearOfStudy = value)
        )
    }

    fun updateProgram(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(program = value)
        )
    }

    fun updateBudgetMin(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMin = value)
        )
    }

    fun updateBudgetMax(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMax = value)
        )
    }

    fun saveProfile() {
        viewModelScope.launch {
            val form = _uiState.value.formState

            // Validate required fields
            when {
                form.firstName.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "First name is required"
                    )
                }
                form.lastName.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Last name is required"
                    )
                }
                form.phone.isEmpty() -> {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Phone number is required"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSaving = true)

                    val success = studentRepository.updateStudentProfile(
                        studentId = form.studentId,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        phone = form.phone.trim(),
                        studentNumber = form.studentNumber.trim().ifEmpty { null },
                        yearOfStudy = form.yearOfStudy.toIntOrNull(),
                        program = form.program.trim().ifEmpty { null },
                        budgetMin = form.budgetMin.toDoubleOrNull(),
                        budgetMax = form.budgetMax.toDoubleOrNull()
                    )

                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            snackbarMessage = "Profile updated successfully",
                            profileUpdated = true
                        )
                        delay(1000)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            snackbarMessage = "Failed to update profile"
                        )
                    }
                }
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun navigationHandled() {
        _uiState.value = _uiState.value.copy(profileUpdated = false)
    }
}
