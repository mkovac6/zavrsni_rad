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
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEditProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val studentRepository = remember { StudentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf(0) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var budgetMin by remember { mutableStateOf("") }
    var budgetMax by remember { mutableStateOf("") }

    // Load current profile data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true

            val userId = UserSession.currentUser?.userId ?: 0
            val profile = studentRepository.getStudentProfile(userId)

            if (profile != null) {
                studentId = profile.studentId
                firstName = profile.firstName
                lastName = profile.lastName
                phone = profile.phone
                studentNumber = profile.studentNumber ?: ""
                yearOfStudy = profile.yearOfStudy?.toString() ?: ""
                program = profile.program ?: ""
                budgetMin = profile.budgetMin?.toString() ?: ""
                budgetMax = profile.budgetMax?.toString() ?: ""
            }

            isLoading = false
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
        if (isLoading) {
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
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = firstName.isEmpty(),
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = lastName.isEmpty(),
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    isError = phone.isEmpty(),
                    enabled = !isSaving
                )

                Divider()

                // Academic Information Section
                Text(
                    text = "Academic Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = { Text("Student Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = yearOfStudy,
                    onValueChange = { yearOfStudy = it },
                    label = { Text("Year of Study") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = program,
                    onValueChange = { program = it },
                    label = { Text("Program/Major") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
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
                        value = budgetMin,
                        onValueChange = { budgetMin = it },
                        label = { Text("Min Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = budgetMax,
                        onValueChange = { budgetMax = it },
                        label = { Text("Max Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Validate required fields
                            when {
                                firstName.isEmpty() -> {
                                    snackbarHostState.showSnackbar("First name is required")
                                }

                                lastName.isEmpty() -> {
                                    snackbarHostState.showSnackbar("Last name is required")
                                }

                                phone.isEmpty() -> {
                                    snackbarHostState.showSnackbar("Phone number is required")
                                }

                                else -> {
                                    isSaving = true

                                    val success = studentRepository.updateStudentProfile(
                                        studentId = studentId,
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phone = phone.trim(),
                                        studentNumber = studentNumber.trim().ifEmpty { null },
                                        yearOfStudy = yearOfStudy.toIntOrNull(),
                                        program = program.trim().ifEmpty { null },
                                        budgetMin = budgetMin.toDoubleOrNull(),
                                        budgetMax = budgetMax.toDoubleOrNull()
                                    )

                                    if (success) {
                                        snackbarHostState.showSnackbar("Profile updated successfully")
                                        kotlinx.coroutines.delay(1000)
                                        onProfileUpdated()
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to update profile")
                                    }

                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
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