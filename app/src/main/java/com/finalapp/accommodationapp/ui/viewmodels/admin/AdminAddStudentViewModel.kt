package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.UserRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin Add Student Screen
 * Manages complex student creation form with validation
 */
class AdminAddStudentViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val universityRepository: UniversityRepository
) : ViewModel() {

    data class FormState(
        val email: String = "",
        val password: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val phone: String = "",
        val studentNumber: String = "",
        val yearOfStudy: String = "",
        val program: String = "",
        val budgetMin: String = "",
        val budgetMax: String = "",
        val selectedUniversity: University? = null
    )

    data class UiState(
        val formState: FormState = FormState(),
        val universities: List<University> = emptyList(),
        val universityDropdownExpanded: Boolean = false,
        val passwordVisible: Boolean = false,
        val isCreating: Boolean = false,
        val isLoading: Boolean = true,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadUniversities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val universities = universityRepository.getAllUniversities()
                _uiState.value = _uiState.value.copy(
                    universities = universities,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AdminAddStudent", "Error loading universities", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading universities: ${e.message}"
                )
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(email = email)
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(password = password)
        )
    }

    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(firstName = firstName)
        )
    }

    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(lastName = lastName)
        )
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(phone = phone)
        )
    }

    fun updateStudentNumber(studentNumber: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(studentNumber = studentNumber)
        )
    }

    fun updateYearOfStudy(yearOfStudy: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(yearOfStudy = yearOfStudy)
        )
    }

    fun updateProgram(program: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(program = program)
        )
    }

    fun updateBudgetMin(budgetMin: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMin = budgetMin)
        )
    }

    fun updateBudgetMax(budgetMax: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(budgetMax = budgetMax)
        )
    }

    fun selectUniversity(university: University) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(selectedUniversity = university),
            universityDropdownExpanded = false
        )
    }

    fun toggleUniversityDropdown() {
        _uiState.value = _uiState.value.copy(
            universityDropdownExpanded = !_uiState.value.universityDropdownExpanded
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    val isFormValid: Boolean
        get() {
            val formState = _uiState.value.formState
            val budgetMinValue = formState.budgetMin.toDoubleOrNull() ?: 0.0
            val budgetMaxValue = formState.budgetMax.toDoubleOrNull() ?: Double.MAX_VALUE

            return formState.email.isNotBlank() &&
                    formState.password.isNotBlank() &&
                    formState.firstName.isNotBlank() &&
                    formState.lastName.isNotBlank() &&
                    formState.phone.isNotBlank() &&
                    formState.selectedUniversity != null &&
                    formState.yearOfStudy.toIntOrNull() != null &&
                    budgetMinValue <= budgetMaxValue
        }

    fun createStudent(onSuccess: () -> Unit) {
        if (!isFormValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            val formState = _uiState.value.formState

            try {
                // Check if email already exists
                val emailExists = userRepository.checkEmailExists(formState.email)
                if (emailExists) {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        snackbarMessage = "Email already exists"
                    )
                    return@launch
                }

                // Create user account
                val user = userRepository.register(formState.email, formState.password, "student")
                if (user != null) {
                    // Create student profile
                    val profileCreated = studentRepository.createStudentProfile(
                        userId = user.userId,
                        universityId = formState.selectedUniversity!!.universityId,
                        firstName = formState.firstName.trim(),
                        lastName = formState.lastName.trim(),
                        phone = formState.phone.trim(),
                        studentNumber = formState.studentNumber.trim().ifEmpty { null },
                        yearOfStudy = formState.yearOfStudy.toIntOrNull(),
                        program = formState.program.trim().ifEmpty { null },
                        preferredMoveInDate = null,
                        budgetMin = formState.budgetMin.toDoubleOrNull(),
                        budgetMax = formState.budgetMax.toDoubleOrNull()
                    )

                    if (profileCreated) {
                        // Update profile completion status
                        userRepository.updateUserProfileStatus(user.userId, true)
                        _uiState.value = _uiState.value.copy(
                            isCreating = false,
                            snackbarMessage = "Student created successfully!"
                        )
                        onSuccess()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCreating = false,
                            snackbarMessage = "Failed to create student profile"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        snackbarMessage = "Failed to create user account"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminAddStudent", "Error creating student", e)
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    snackbarMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
