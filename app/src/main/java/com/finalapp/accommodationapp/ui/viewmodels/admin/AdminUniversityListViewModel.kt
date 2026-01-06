package com.finalapp.accommodationapp.ui.viewmodels.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Admin University List Screen
 * Manages university list display, deletion, and adding new universities
 */
class AdminUniversityListViewModel(
    private val universityRepository: UniversityRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    data class FormState(
        val newUniversityName: String = "",
        val newUniversityCity: String = "",
        val newUniversityCountry: String = ""
    )

    data class UiState(
        val universities: List<University> = emptyList(),
        val isLoading: Boolean = true,
        val showDeleteDialog: Boolean = false,
        val universityToDelete: University? = null,
        val showAddDialog: Boolean = false,
        val formState: FormState = FormState(),
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
                Log.e("AdminUniversityList", "Error loading universities", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Error loading universities: ${e.message}"
                )
            }
        }
    }

    fun showDeleteDialog(university: University) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            universityToDelete = university
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            universityToDelete = null
        )
    }

    fun deleteUniversity() {
        viewModelScope.launch {
            val university = _uiState.value.universityToDelete ?: return@launch

            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                universityToDelete = null
            )

            try {
                val success = adminRepository.deleteUniversity(university.universityId)
                if (success) {
                    // Reload universities list
                    val updatedUniversities = universityRepository.getAllUniversities()
                    _uiState.value = _uiState.value.copy(
                        universities = updatedUniversities,
                        snackbarMessage = "University deleted successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Cannot delete university with enrolled students"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminUniversityList", "Error deleting university", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error deleting university: ${e.message}"
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            formState = FormState()
        )
    }

    fun updateUniversityName(name: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(newUniversityName = name)
        )
    }

    fun updateUniversityCity(city: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(newUniversityCity = city)
        )
    }

    fun updateUniversityCountry(country: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(newUniversityCountry = country)
        )
    }

    fun addUniversity() {
        val formState = _uiState.value.formState

        if (formState.newUniversityName.isBlank() ||
            formState.newUniversityCity.isBlank() ||
            formState.newUniversityCountry.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                val success = adminRepository.addUniversity(
                    formState.newUniversityName.trim(),
                    formState.newUniversityCity.trim(),
                    formState.newUniversityCountry.trim()
                )

                if (success) {
                    // Reload universities list
                    val updatedUniversities = universityRepository.getAllUniversities()
                    _uiState.value = _uiState.value.copy(
                        universities = updatedUniversities,
                        showAddDialog = false,
                        formState = FormState(),
                        snackbarMessage = "University added successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Failed to add university"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminUniversityList", "Error adding university", e)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error adding university: ${e.message}"
                )
            }
        }
    }

    val isAddFormValid: Boolean
        get() {
            val formState = _uiState.value.formState
            return formState.newUniversityName.isNotBlank() &&
                    formState.newUniversityCity.isNotBlank() &&
                    formState.newUniversityCountry.isNotBlank()
        }

    fun snackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
