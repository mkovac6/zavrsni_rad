package com.finalapp.accommodationapp.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.data.repository.landlord.LandlordRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordEditProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val landlordRepository = remember { LandlordRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form states
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var landlordId by remember { mutableStateOf(0) }
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    // Load current profile data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            
            val userId = UserSession.currentUser?.userId ?: 0
            val landlord = landlordRepository.getLandlordByUserId(userId)
            
            if (landlord != null) {
                landlordId = landlord.landlordId
                firstName = landlord.firstName
                lastName = landlord.lastName
                companyName = landlord.companyName ?: ""
                phone = landlord.phone
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
                        Icon(Icons.Filled.ArrowBack, "Back")
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
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                    }
                }
                
                // Business Information Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Business Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        )
                        
                        // Info card about verification
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Contact support to update verification status or request business verification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
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
                                    
                                    val success = landlordRepository.updateLandlordProfile(
                                        landlordId = landlordId,
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        companyName = companyName.trim().ifEmpty { null },
                                        phone = phone.trim()
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
                
                // Cancel Button
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}