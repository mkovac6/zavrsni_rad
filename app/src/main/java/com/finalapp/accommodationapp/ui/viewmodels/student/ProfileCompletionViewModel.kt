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
 * ViewModel for Student Profile Completion Screen
 * Handles initial profile creation for new students
 */
class ProfileCompletionViewModel(
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class FormState(
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
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val profileComplete: Boolean = false,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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
        // Filter to allow only digits
        val filtered = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(yearOfStudy = filtered)
        )
    }

    fun updateProgram(value: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(program = value)
        )
    }

    fun updateBudgetMin(value: String) {
        // Filter to allow only digits and decimal point
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMin = filtered)
        )
    }

    fun updateBudgetMax(value: String) {
        // Filter to allow only digits and decimal point
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMax = filtered)
        )
    }

    fun completeProfile(userId: Int, universityId: Int) {
        viewModelScope.launch {
            val form = _uiState.value.formState

            // Validate required fields
            when {
                form.firstName.isBlank() || form.lastName.isBlank() || form.phone.isBlank() -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please fill in all required fields (*)"
                    )
                }
                form.budgetMin.isNotEmpty() && form.budgetMax.isNotEmpty() &&
                        form.budgetMin.toDoubleOrNull() != null && form.budgetMax.toDoubleOrNull() != null &&
                        form.budgetMin.toDouble() > form.budgetMax.toDouble() -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Minimum budget cannot be greater than maximum"
                    )
                }
                userId == 0 -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "User session expired. Please login again."
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        errorMessage = null
                    )

                    val success = studentRepository.createStudentProfile(
                        userId = userId,
                        universityId = universityId,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        phone = form.phone.trim(),
                        studentNumber = form.studentNumber.trim().ifEmpty { null },
                        yearOfStudy = form.yearOfStudy.toIntOrNull(),
                        program = form.program.trim().ifEmpty { null },
                        preferredMoveInDate = null,
                        budgetMin = form.budgetMin.toDoubleOrNull(),
                        budgetMax = form.budgetMax.toDoubleOrNull()
                    )

                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "Profile completed successfully!",
                            profileComplete = true
                        )
                        delay(1000)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to save profile. Please try again."
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
        _uiState.value = _uiState.value.copy(profileComplete = false)
    }
}
