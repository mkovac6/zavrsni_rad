package com.finalapp.accommodationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.data.repository.AdminRepository
import com.finalapp.accommodationapp.data.model.University

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUniversityListScreen(
    onNavigateBack: () -> Unit
) {
    val universityRepository = remember { UniversityRepository() }
    val adminRepository = remember { AdminRepository() }
    var universities by remember { mutableStateOf<List<University>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var universityToDelete by remember { mutableStateOf<University?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states for adding new university
    var newUniversityName by remember { mutableStateOf("") }
    var newUniversityCity by remember { mutableStateOf("") }
    var newUniversityCountry by remember { mutableStateOf("") }

    // Load universities
    fun loadUniversities() {
        scope.launch {
            isLoading = true
            universities = universityRepository.getAllUniversities()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadUniversities()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Universities") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add University")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Total Universities: ${universities.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(universities) { university ->
                    UniversityCard(
                        university = university,
                        onDelete = {
                            universityToDelete = university
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                universityToDelete = null
            },
            title = { Text("Delete University") },
            text = {
                Text("Are you sure you want to delete \"${universityToDelete?.name}\"? This can only be done if no students are enrolled.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        universityToDelete?.let { university ->
                            scope.launch {
                                showDeleteDialog = false
                                universityToDelete = null

                                val success = adminRepository.deleteUniversity(university.universityId)
                                if (success) {
                                    loadUniversities()
                                    snackbarHostState.showSnackbar("University deleted successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Cannot delete university with enrolled students")
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        universityToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add university dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newUniversityName = ""
                newUniversityCity = ""
                newUniversityCountry = ""
            },
            title = { Text("Add New University") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newUniversityName,
                        onValueChange = { newUniversityName = it },
                        label = { Text("University Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUniversityCity,
                        onValueChange = { newUniversityCity = it },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUniversityCountry,
                        onValueChange = { newUniversityCountry = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUniversityName.isNotBlank() &&
                            newUniversityCity.isNotBlank() &&
                            newUniversityCountry.isNotBlank()) {
                            scope.launch {
                                val success = adminRepository.addUniversity(
                                    newUniversityName.trim(),
                                    newUniversityCity.trim(),
                                    newUniversityCountry.trim()
                                )
                                if (success) {
                                    loadUniversities()
                                    snackbarHostState.showSnackbar("University added successfully")
                                    showAddDialog = false
                                    newUniversityName = ""
                                    newUniversityCity = ""
                                    newUniversityCountry = ""
                                } else {
                                    snackbarHostState.showSnackbar("Failed to add university")
                                }
                            }
                        }
                    },
                    enabled = newUniversityName.isNotBlank() &&
                            newUniversityCity.isNotBlank() &&
                            newUniversityCountry.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newUniversityName = ""
                        newUniversityCity = ""
                        newUniversityCountry = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UniversityCard(
    university: University,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = university.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${university.city}, ${university.country}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "ID: ${university.universityId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}