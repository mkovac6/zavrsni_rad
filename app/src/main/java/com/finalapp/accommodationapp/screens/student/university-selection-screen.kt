package com.finalapp.accommodationapp.screens.student

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finalapp.accommodationapp.data.model.University
import com.finalapp.accommodationapp.data.repository.UniversityRepository
import com.finalapp.accommodationapp.ui.viewmodels.student.UniversitySelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversitySelectionScreen(
    onUniversitySelected: (Int) -> Unit, // Now passes university ID
    onNavigateBack: () -> Unit,
    viewModel: UniversitySelectionViewModel = viewModel {
        UniversitySelectionViewModel(
            universityRepository = UniversityRepository()
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

            // Debug info
            if (uiState.universities.isNotEmpty()) {
                Text(
                    text = "Loaded ${uiState.universities.size} universities from database",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
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
                        items(uiState.universities) { university ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.selectUniversity(university) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.selectedUniversity?.universityId == university.universityId) {
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
                                    if (uiState.selectedUniversity?.universityId == university.universityId) {
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
                    uiState.selectedUniversity?.let { university ->
                        onUniversitySelected(university.universityId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedUniversity != null && !uiState.isLoading
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