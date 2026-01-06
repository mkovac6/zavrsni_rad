package com.finalapp.accommodationapp.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finalapp.accommodationapp.data.repository.admin.AdminRepository
import com.finalapp.accommodationapp.data.model.admin.LandlordWithUser
import com.finalapp.accommodationapp.data.model.admin.Amenity
import com.finalapp.accommodationapp.ui.viewmodels.admin.AdminAddPropertyViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    onNavigateBack: () -> Unit,
    onPropertyAdded: () -> Unit,
    viewModel: AdminAddPropertyViewModel = viewModel {
        AdminAddPropertyViewModel(
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
                title = { Text("Add New Property") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Landlord Selection
                item {
                    Text(
                        text = "Property Owner",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = uiState.showLandlordDropdown,
                        onExpandedChange = { viewModel.toggleLandlordDropdown() }
                    ) {
                        OutlinedTextField(
                            value = uiState.landlords.find { it.landlordId == uiState.formState.selectedLandlordId }?.let {
                                "${it.firstName} ${it.lastName} - ${it.email}"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Landlord *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.showLandlordDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = uiState.showLandlordDropdown,
                            onDismissRequest = { viewModel.toggleLandlordDropdown() }
                        ) {
                            uiState.landlords.forEach { landlord ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${landlord.firstName} ${landlord.lastName}")
                                            Text(
                                                landlord.email,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectLandlord(landlord.landlordId)
                                    }
                                )
                            }
                        }
                    }
                }

                // Basic Information
                item {
                    Divider()
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Property Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                // Property Type
                item {
                    Text("Property Type", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("apartment", "house", "room", "studio", "shared").forEach { type ->
                            FilterChip(
                                selected = uiState.formState.propertyType == type,
                                onClick = { viewModel.updatePropertyType(type) },
                                label = { Text(type.capitalize()) }
                            )
                        }
                    }
                }

                // Location
                item {
                    Divider()
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.address,
                        onValueChange = { viewModel.updateAddress(it) },
                        label = { Text("Address *") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.formState.city,
                            onValueChange = { viewModel.updateCity(it) },
                            label = { Text("City *") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.postalCode,
                            onValueChange = { viewModel.updatePostalCode(it) },
                            label = { Text("Postal Code") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Details
                item {
                    Divider()
                    Text(
                        text = "Property Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.pricePerMonth,
                        onValueChange = { viewModel.updatePricePerMonth(it) },
                        label = { Text("Price per Month (€) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.formState.bedrooms,
                            onValueChange = { viewModel.updateBedrooms(it) },
                            label = { Text("Bedrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.bathrooms,
                            onValueChange = { viewModel.updateBathrooms(it) },
                            label = { Text("Bathrooms *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.formState.totalCapacity,
                            onValueChange = { viewModel.updateTotalCapacity(it) },
                            label = { Text("Capacity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.formState.availableFrom,
                        onValueChange = { }, // Make it read-only
                        label = { Text("Available From") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleDatePicker() }) {
                                Icon(Icons.Filled.DateRange, contentDescription = "Select date")
                            }
                        },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Amenities
                item {
                    Divider()
                    Text(
                        text = "Amenities",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(uiState.amenities.chunked(2)) { amenityPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amenityPair.forEach { amenity ->
                            FilterChip(
                                selected = uiState.formState.selectedAmenities.contains(amenity.amenityId),
                                onClick = {
                                    viewModel.toggleAmenity(amenity.amenityId)
                                },
                                label = { Text(amenity.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (amenityPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Submit Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.createProperty(onSuccess = onPropertyAdded)
                        },
                        enabled = viewModel.isFormValid && !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Create Property")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        DatePickerDialog(
            onDateSelected = { year, month, day ->
                viewModel.updateAvailableFrom(String.format("%04d-%02d-%02d", year, month, day))
                viewModel.toggleDatePicker()
            },
            onDismiss = { viewModel.toggleDatePicker() }
        )
    }
}

@Composable
fun DatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedYear > currentYear) selectedYear-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous year")
                        }
                        Text(
                            text = selectedYear.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { if (selectedYear < currentYear + 5) selectedYear++ }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next year")
                        }
                    }
                }

                // Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous month")
                        }
                        Text(
                            text = getMonthName(selectedMonth),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next month")
                        }
                    }
                }

                // Day selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Day:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedDay > 1) selectedDay-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous day")
                        }
                        Text(
                            text = selectedDay.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = {
                            val maxDay = getMaxDayOfMonth(selectedYear, selectedMonth)
                            if (selectedDay < maxDay) selectedDay++
                        }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next day")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedYear, selectedMonth, selectedDay) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

fun getMaxDayOfMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
}