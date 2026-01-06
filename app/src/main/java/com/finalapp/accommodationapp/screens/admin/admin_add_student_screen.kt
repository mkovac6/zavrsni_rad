package com.finalapp.accommodationapp.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finalapp.accommodationapp.data.repository.UserRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.ui.viewmodels.admin.AdminAddStudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStudentScreen(
    onNavigateBack: () -> Unit,
    onStudentAdded: () -> Unit,
    viewModel: AdminAddStudentViewModel = viewModel {
        AdminAddStudentViewModel(
            userRepository = UserRepository(),
            studentRepository = StudentRepository(),
            universityRepository = UniversityRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load universities
    LaunchedEffect(Unit) {
        viewModel.loadUniversities()
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Student") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                // Account Information Section
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.formState.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text("Email*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.formState.email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(
                        uiState.formState.email
                    ).matches()
                )

                OutlinedTextField(
                    value = uiState.formState.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password*") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = if (uiState.passwordVisible) "Hide password" else "Show password",
                                tint = if (uiState.passwordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                // Personal Information Section
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.formState.firstName,
                        onValueChange = { viewModel.updateFirstName(it) },
                        label = { Text("First Name*") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.formState.lastName,
                        onValueChange = { viewModel.updateLastName(it) },
                        label = { Text("Last Name*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = uiState.formState.phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    label = { Text("Phone Number*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.formState.studentNumber,
                    onValueChange = { viewModel.updateStudentNumber(it) },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                // University Information Section
                Text(
                    text = "University Information",
                    style = MaterialTheme.typography.titleMedium
                )

                // University Dropdown
                ExposedDropdownMenuBox(
                    expanded = uiState.universityDropdownExpanded,
                    onExpandedChange = { viewModel.toggleUniversityDropdown() }
                ) {
                    OutlinedTextField(
                        value = uiState.formState.selectedUniversity?.name ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("University*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.universityDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = uiState.universityDropdownExpanded,
                        onDismissRequest = { viewModel.toggleUniversityDropdown() }
                    ) {
                        uiState.universities.forEach { university ->
                            DropdownMenuItem(
                                text = { Text("${university.name} - ${university.city}") },
                                onClick = {
                                    viewModel.selectUniversity(university)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.formState.yearOfStudy,
                        onValueChange = { viewModel.updateYearOfStudy(it) },
                        label = { Text("Year of Study*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.formState.program,
                        onValueChange = { viewModel.updateProgram(it) },
                        label = { Text("Program/Major") },
                        modifier = Modifier.weight(2f)
                    )
                }

                Divider()

                // Accommodation Preferences Section
                Text(
                    text = "Accommodation Preferences",
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.formState.budgetMax,
                        onValueChange = { viewModel.updateBudgetMax(it) },
                        label = { Text("Max Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Create Button
                Button(
                    onClick = {
                        viewModel.createStudent(onSuccess = onStudentAdded)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.isFormValid && !uiState.isCreating
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Student")
                    }
                }
            }
        }
    }
}