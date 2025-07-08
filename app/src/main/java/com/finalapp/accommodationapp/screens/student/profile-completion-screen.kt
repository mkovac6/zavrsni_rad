package com.finalapp.accommodationapp.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.repository.student.StudentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    universityId: Int,
    onProfileComplete: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var budgetMin by remember { mutableStateOf("") }
    var budgetMax by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val studentRepository = remember { StudentRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Help us find the perfect accommodation for you",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Required fields
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null && firstName.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null && lastName.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = errorMessage != null && phone.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Optional fields
            OutlinedTextField(
                value = studentNumber,
                onValueChange = { studentNumber = it },
                label = { Text("Student ID (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = yearOfStudy,
                onValueChange = { yearOfStudy = it.filter { char -> char.isDigit() } },
                label = { Text("Year of Study (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = program,
                onValueChange = { program = it },
                label = { Text("Program/Major (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Budget Range
            Text(
                text = "Budget Range (EUR/month) - Optional",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = budgetMin,
                    onValueChange = { budgetMin = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = budgetMax,
                    onValueChange = { budgetMax = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        firstName.isBlank() || lastName.isBlank() || phone.isBlank() -> {
                            errorMessage = "Please fill in all required fields (*)"
                        }
                        budgetMin.isNotEmpty() && budgetMax.isNotEmpty() &&
                                budgetMin.toDoubleOrNull() != null && budgetMax.toDoubleOrNull() != null &&
                                budgetMin.toDouble() > budgetMax.toDouble() -> {
                            errorMessage = "Minimum budget cannot be greater than maximum"
                        }
                        else -> {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null

                                val userId = UserSession.getUserId()
                                if (userId == 0) {
                                    errorMessage = "User session expired. Please login again."
                                    isLoading = false
                                    return@launch
                                }

                                val success = studentRepository.createStudentProfile(
                                    userId = userId,
                                    universityId = universityId,
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    phone = phone.trim(),
                                    studentNumber = studentNumber.trim().ifEmpty { null },
                                    yearOfStudy = yearOfStudy.toIntOrNull(),
                                    program = program.trim().ifEmpty { null },
                                    preferredMoveInDate = null, // Can add date picker later
                                    budgetMin = budgetMin.toDoubleOrNull(),
                                    budgetMax = budgetMax.toDoubleOrNull()
                                )

                                if (success) {
                                    snackbarHostState.showSnackbar(
                                        message = "Profile completed successfully!",
                                        duration = SnackbarDuration.Short
                                    )
                                    delay(1000)
                                    onProfileComplete()
                                } else {
                                    errorMessage = "Failed to save profile. Please try again."
                                }

                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Complete Profile")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "* Required fields",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}