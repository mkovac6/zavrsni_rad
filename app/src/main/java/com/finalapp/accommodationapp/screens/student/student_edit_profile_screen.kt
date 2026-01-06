package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.ui.viewmodels.student.StudentEditProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit,
    viewModel: StudentEditProfileViewModel = viewModel {
        StudentEditProfileViewModel(
            studentRepository = StudentRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load current profile data
    LaunchedEffect(Unit) {
        val userId = UserSession.currentUser?.userId ?: 0
        viewModel.loadProfile(userId)
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarShown()
        }
    }

    // Handle navigation on profile update
    LaunchedEffect(uiState.profileUpdated) {
        if (uiState.profileUpdated) {
            onProfileUpdated()
            viewModel.navigationHandled()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Information Section
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.formState.firstName,
                    onValueChange = { viewModel.updateFirstName(it) },
                    label = { Text("First Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.firstName.isEmpty(),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = uiState.formState.lastName,
                    onValueChange = { viewModel.updateLastName(it) },
                    label = { Text("Last Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.lastName.isEmpty(),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = uiState.formState.phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    label = { Text("Phone Number*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.phone.isEmpty(),
                    enabled = !uiState.isSaving
                )

                Divider()

                // Academic Information Section
                Text(
                    text = "Academic Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.formState.studentNumber,
                    onValueChange = { viewModel.updateStudentNumber(it) },
                    label = { Text("Student Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = uiState.formState.yearOfStudy,
                    onValueChange = { viewModel.updateYearOfStudy(it) },
                    label = { Text("Year of Study") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = uiState.formState.program,
                    onValueChange = { viewModel.updateProgram(it) },
                    label = { Text("Program/Major") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Budget Preferences Section
                Text(
                    text = "Budget Preferences",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.formState.budgetMin,
                        onValueChange = { viewModel.updateBudgetMin(it) },
                        label = { Text("Min Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    )

                    OutlinedTextField(
                        value = uiState.formState.budgetMax,
                        onValueChange = { viewModel.updateBudgetMax(it) },
                        label = { Text("Max Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}