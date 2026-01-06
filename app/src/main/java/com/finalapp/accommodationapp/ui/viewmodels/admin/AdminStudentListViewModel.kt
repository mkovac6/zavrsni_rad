package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.admin.StudentWithUser
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin Student List Screen
 * Manages student list display and deletion
 */
class AdminStudentListViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class UiState(
        val students: List<StudentWithUser> = emptyList(),
        val isLoading: Boolean = true,
        val showDeleteDialog: Boolean = false,
        val studentToDelete: StudentWithUser? = null,
        val snackbarMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadStudents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val students = adminRepository.getAllStudents()
                _uiState.value = _uiState.value.copy(
                    students = students,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AdminStudentList", "Error loading students", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading students: ${e.message}"
                )
            }
        }
    }

    fun showDeleteDialog(student: StudentWithUser) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            studentToDelete = student
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            studentToDelete = null
        )
    }

    fun deleteStudent() {
        viewModelScope.launch {
            val student = _uiState.value.studentToDelete ?: return@launch

            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                studentToDelete = null
            )

            try {
                val success = adminRepository.deleteStudent(student.userId)
                if (success) {
                    // Reload students list
                    val updatedStudents = adminRepository.getAllStudents()
                    _uiState.value = _uiState.value.copy(
                        students = updatedStudents,
                        snackbarMessage = "Student deleted successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to delete student"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminStudentList", "Error deleting student", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error deleting student: ${e.message}"
                )
            }
        }
    }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
