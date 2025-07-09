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
import com.finalapp.accommodationapp.data.repository.UserRepository
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.model.University
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStudentScreen(
    onNavigateBack: () -> Unit,
    onStudentAdded: () -> Unit
) {
    val userRepository = remember { UserRepository() }
    val studentRepository = remember { StudentRepository() }
    val universityRepository = remember { UniversityRepository() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var budgetMin by remember { mutableStateOf("") }
    var budgetMax by remember { mutableStateOf("") }

    // University selection
    var universities by remember { mutableStateOf<List<University>>(emptyList()) }
    var selectedUniversity by remember { mutableStateOf<University?>(null) }
    var universityDropdownExpanded by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Load universities
    LaunchedEffect(Unit) {
        isLoading = true
        universities = universityRepository.getAllUniversities()
        isLoading = false
    }

    // Validation
    val isFormValid = email.isNotBlank() &&
            password.isNotBlank() &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            phone.isNotBlank() &&
            selectedUniversity != null &&
            yearOfStudy.toIntOrNull() != null &&
            (budgetMin.toDoubleOrNull() ?: 0.0) <= (budgetMax.toDoubleOrNull() ?: Double.MAX_VALUE)

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
                // Account Information Section
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(
                        email
                    ).matches()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password*") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = if (passwordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name*") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
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
                    expanded = universityDropdownExpanded,
                    onExpandedChange = { universityDropdownExpanded = !universityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedUniversity?.name ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("University*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = universityDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = universityDropdownExpanded,
                        onDismissRequest = { universityDropdownExpanded = false }
                    ) {
                        universities.forEach { university ->
                            DropdownMenuItem(
                                text = { Text("${university.name} - ${university.city}") },
                                onClick = {
                                    selectedUniversity = university
                                    universityDropdownExpanded = false
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
                        value = yearOfStudy,
                        onValueChange = { yearOfStudy = it },
                        label = { Text("Year of Study*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = program,
                        onValueChange = { program = it },
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
                        value = budgetMin,
                        onValueChange = { budgetMin = it },
                        label = { Text("Min Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = budgetMax,
                        onValueChange = { budgetMax = it },
                        label = { Text("Max Budget (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Create Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCreating = true

                            try {
                                // Check if email already exists
                                val emailExists = userRepository.checkEmailExists(email)
                                if (emailExists) {
                                    snackbarHostState.showSnackbar("Email already exists")
                                    isCreating = false
                                    return@launch
                                }

                                // Create user account
                                val user = userRepository.register(email, password, "student")
                                if (user != null) {
                                    // Create student profile
                                    val profileCreated = studentRepository.createStudentProfile(
                                        userId = user.userId,
                                        universityId = selectedUniversity!!.universityId,
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phone = phone.trim(),
                                        studentNumber = studentNumber.trim().ifEmpty { null },
                                        yearOfStudy = yearOfStudy.toIntOrNull(),
                                        program = program.trim().ifEmpty { null },
                                        preferredMoveInDate = null,
                                        budgetMin = budgetMin.toDoubleOrNull(),
                                        budgetMax = budgetMax.toDoubleOrNull()
                                    )

                                    if (profileCreated) {
                                        // Update profile completion status
                                        userRepository.updateUserProfileStatus(user.userId, true)
                                        snackbarHostState.showSnackbar("Student created successfully!")
                                        onStudentAdded()
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to create student profile")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Failed to create user account")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }

                            isCreating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid && !isCreating
                ) {
                    if (isCreating) {
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