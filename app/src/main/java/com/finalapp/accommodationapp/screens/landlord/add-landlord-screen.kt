package com.finalapp.accommodationapp.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.ui.viewmodels.admin.AdminAddLandlordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLandlordScreen(
    onNavigateBack: () -> Unit,
    onLandlordAdded: () -> Unit,
    viewModel: AdminAddLandlordViewModel = viewModel {
        AdminAddLandlordViewModel(
            userRepository = UserRepository(),
            adminRepository = AdminRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Landlord") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = uiState.formState.email.isNotEmpty() && !viewModel.isEmailValid,
                supportingText = {
                    if (uiState.formState.email.isNotEmpty() && !viewModel.isEmailValid) {
                        Text("Please enter a valid email address")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.formState.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = if (uiState.passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = uiState.formState.password.isNotEmpty() && !viewModel.isPasswordValid,
                supportingText = {
                    if (uiState.formState.password.isNotEmpty() && !viewModel.isPasswordValid) {
                        Text("Password must be at least 6 characters")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.formState.confirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = if (uiState.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = if (uiState.confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = uiState.formState.confirmPassword.isNotEmpty() && !viewModel.doPasswordsMatch,
                supportingText = {
                    if (uiState.formState.confirmPassword.isNotEmpty() && !viewModel.doPasswordsMatch) {
                        Text("Passwords do not match")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // Personal Information Section
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.formState.firstName,
                onValueChange = { viewModel.updateFirstName(it) },
                label = { Text("First Name *") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.formState.lastName,
                onValueChange = { viewModel.updateLastName(it) },
                label = { Text("Last Name *") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.formState.companyName,
                onValueChange = { viewModel.updateCompanyName(it) },
                label = { Text("Company Name (Optional)") },
                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.formState.phone,
                onValueChange = { viewModel.updatePhone(it) },
                label = { Text("Phone Number *") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // Verification Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.formState.isVerified,
                    onCheckedChange = { viewModel.toggleVerified() }
                )
                Text("Mark as verified landlord")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.createLandlord(onSuccess = onLandlordAdded)
                },
                enabled = viewModel.isFormValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Landlord")
                }
            }
        }
    }
}