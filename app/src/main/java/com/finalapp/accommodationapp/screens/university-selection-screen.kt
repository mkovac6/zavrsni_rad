package com.finalapp.accommodationapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversitySelectionScreen(
    onUniversitySelected: (Int) -> Unit, // Now passes university ID
    onNavigateBack: () -> Unit
) {
    var selectedUniversity by remember { mutableStateOf<University?>(null) }
    var universities by remember { mutableStateOf<List<University>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val universityRepository = remember { UniversityRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load universities when screen opens
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                universities = universityRepository.getAllUniversities()
                if (universities.isEmpty()) {
                    errorMessage = "No universities found"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load universities"
                snackbarHostState.showSnackbar(
                    message = "Error loading universities. Please try again.",
                    duration = SnackbarDuration.Short
                )
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Your University",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your university to see nearby accommodations",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Debug info - remove later
            if (universities.isNotEmpty()) {
                Text(
                    text = "Loaded ${universities.size} universities from database",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(universities) { university ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedUniversity = university },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedUniversity?.universityId == university.universityId) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = university.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${university.city}, ${university.country}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selectedUniversity?.universityId == university.universityId) {
                                        Text(
                                            "✓",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedUniversity?.let { university ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Selected: ${university.name}",
                                duration = SnackbarDuration.Short
                            )
                        }
                        onUniversitySelected(university.universityId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedUniversity != null && !isLoading
            ) {
                Text("Continue")
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}